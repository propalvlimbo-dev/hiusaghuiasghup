import { Effect, Stream, type Scope } from "effect";
import {
  decodeCameraImage,
  BotDesiredState,
  BotRuntimeState,
  SoulFire,
  type BotMovement,
  type SoulFireClient,
  type SoulFireOptions,
} from "@soulfiremc/sdk";
import type { BotRow, BotSummary, InstanceSummary } from "../shared/types.js";

export interface SoulFireCredentials {
  baseUrl: string;
  token: string;
}

export interface NewGroupInput {
  name: string;
  serverAddress: string;
  botNames: string[];
}

let activeCredentials: SoulFireCredentials | null = null;

type SoulFireProgram<T> = Effect.Effect<T, any, Scope.Scope>;
type SoulFireCallback<T> = (client: SoulFireClient) => SoulFireProgram<T>;

export function validateCredentials(input: {
  baseUrl: unknown;
  token: unknown;
}): SoulFireCredentials {
  if (typeof input.baseUrl !== "string" || typeof input.token !== "string") {
    throw new Error("Укажите адрес SoulFire и API-токен.");
  }

  let parsed: URL;
  try {
    parsed = new URL(input.baseUrl.trim());
  } catch {
    throw new Error("Не удалось разобрать адрес SoulFire. Пример: http://127.0.0.1:38765");
  }

  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    throw new Error("Адрес SoulFire должен начинаться с http:// или https://.");
  }
  if (parsed.username || parsed.password || parsed.search || parsed.hash) {
    throw new Error("Укажите только базовый URL SoulFire — без логина, пароля, query-параметров и hash.");
  }

  const hostname = parsed.hostname.toLowerCase();
  const isLoopback = hostname === "localhost"
    || hostname === "127.0.0.1"
    || hostname === "[::1]"
    || hostname === "::1";
  if (parsed.protocol === "http:" && !isLoopback) {
    throw new Error("Для удалённого SoulFire используйте HTTPS. HTTP разрешён только для локального сервера.");
  }

  const token = input.token.trim();
  if (token.length < 8 || token.length > 4096) {
    throw new Error("Проверьте API-токен SoulFire.");
  }

  const baseUrl = parsed.toString().replace(/\/+$/, "");
  return { baseUrl, token };
}

export async function withSoulFire<T>(callback: SoulFireCallback<T>): Promise<T> {
  if (!activeCredentials) {
    throw new Error("Сначала подключитесь к серверу SoulFire.");
  }
  return withSoulFireCredentials(activeCredentials, callback);
}

export async function withSoulFireCredentials<T>(
  credentials: SoulFireCredentials,
  callback: SoulFireCallback<T>,
): Promise<T> {
  const options: SoulFireOptions = {
    baseUrl: credentials.baseUrl,
    token: credentials.token,
    defaultTimeoutMs: 12_000,
  };

  const program = Effect.scoped(
    Effect.gen(function* () {
      const client = yield* SoulFire.connect(options);
      return yield* callback(client);
    }),
  );
  return Effect.runPromise(program);
}

export async function testAndSaveConnection(
  rawCredentials: { baseUrl: unknown; token: unknown },
): Promise<{ baseUrl: string; instanceCount: number }> {
  const credentials = validateCredentials(rawCredentials);
  const instances = await withSoulFireCredentials(credentials, (client) => client.instances());
  activeCredentials = credentials;
  return { baseUrl: credentials.baseUrl, instanceCount: instances.length };
}

export function getSession(): { connected: boolean; baseUrl?: string } {
  return activeCredentials
    ? { connected: true, baseUrl: activeCredentials.baseUrl }
    : { connected: false };
}

export function disconnectSoulFire(): void {
  activeCredentials = null;
}

export function hasConnection(): boolean {
  return activeCredentials !== null;
}

export function assertInstanceId(value: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)) {
    throw new Error("Некорректный ID инстанса SoulFire.");
  }
  return value;
}

function enumName(enumValue: object, value: number | undefined): string {
  if (value === undefined) return "UNSPECIFIED";
  return (enumValue as Record<number, string>)[value] ?? "UNSPECIFIED";
}

function connectionPhaseName(value: number): string {
  return ["UNSPECIFIED", "CONNECTING", "CONNECTED", "SPAWNED", "DIED", "DISCONNECTED"][value] ?? "UNSPECIFIED";
}

function mapSummary(summary?: {
  totalBots: number;
  desiredBots: number;
  onlineBots: number;
  startingBots: number;
  retryingBots: number;
  failedBots: number;
}): BotSummary {
  return {
    totalBots: summary?.totalBots ?? 0,
    desiredBots: summary?.desiredBots ?? 0,
    onlineBots: summary?.onlineBots ?? 0,
    startingBots: summary?.startingBots ?? 0,
    retryingBots: summary?.retryingBots ?? 0,
    failedBots: summary?.failedBots ?? 0,
  };
}

export async function listInstances(
  addressForInstance: (instanceId: string) => string | undefined,
): Promise<InstanceSummary[]> {
  const instances = await withSoulFire((client) => client.instances());
  return instances.map((instance) => ({
    id: instance.id,
    name: instance.friendlyName,
    icon: instance.icon,
    serverAddress: addressForInstance(instance.id),
    botSummary: mapSummary(instance.botSummary),
  }));
}

export async function listBots(instanceId: string): Promise<BotRow[]> {
  const id = assertInstanceId(instanceId);
  const bots = await withSoulFire((client) => client.instance(id).bots());
  return bots.map((bot) => ({
    id: bot.profileId,
    name: bot.accountName || bot.profileId.slice(0, 8),
    online: bot.isOnline,
    pingMs: bot.pingMs,
    connectionPhase: connectionPhaseName(bot.connectionPhase),
    desiredState: enumName(BotDesiredState, bot.status?.desiredState),
    runtimeState: enumName(BotRuntimeState, bot.status?.runtimeState),
    lastError: bot.status?.lastError,
    health: bot.liveState?.health,
    dimension: bot.liveState?.dimension,
    yaw: bot.liveState?.yRot,
    pitch: bot.liveState?.xRot,
  }));
}

export type ManualMovementKey = "forward" | "backward" | "left" | "right" | "jump" | "sneak" | "sprint";

export function assertBotId(value: string): string {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)) {
    throw new Error("Некорректный ID бота SoulFire.");
  }
  return value;
}

export async function captureBotPov(instanceId: string, botId: string): Promise<Uint8Array> {
  const instance = assertInstanceId(instanceId);
  const bot = assertBotId(botId);
  return withSoulFire((client) => client.instance(instance).bot(bot).camera.captureBytes({
    width: 960,
    height: 540,
    includeHud: true,
    includeHands: true,
  }));
}

export async function streamBotPov(
  instanceId: string,
  botId: string,
  writeFrame: (image: Uint8Array) => Promise<void>,
  signal: AbortSignal,
): Promise<void> {
  const instance = assertInstanceId(instanceId);
  const bot = assertBotId(botId);
  await withSoulFire((client) => Stream.runForEach(
    client.instance(instance).bot(bot).camera.frames({
      width: 960,
      height: 540,
      intervalMs: 1_000,
      includeHud: true,
      includeHands: true,
      includeDebugTrace: false,
      call: { signal },
    }),
    (frame) => {
      if (!frame.render || signal.aborted) return Effect.void;
      const image = decodeCameraImage(frame.render);
      return Effect.promise(() => writeFrame(image));
    },
  ));
}

export async function pulseBotMovement(
  instanceId: string,
  botId: string,
  key: ManualMovementKey,
  durationMs: number,
): Promise<void> {
  const instance = assertInstanceId(instanceId);
  const botIdValidated = assertBotId(botId);
  if (!Number.isFinite(durationMs) || durationMs < 100 || durationMs > 1_000) {
    throw new Error("Длительность нажатия должна быть от 100 до 1000 мс.");
  }

  const movement = (key === "sprint" ? { forward: true, sprint: true } : { [key]: true }) as BotMovement;
  await withSoulFire((client) => Effect.gen(function* () {
    const bot = client.instance(instance).bot(botIdValidated);
    yield* bot.setMovement(movement);
    yield* Effect.ensuring(
      Effect.sleep(durationMs),
      Effect.ignore(bot.resetMovement()),
    );
  }));
}

export async function lookBot(
  instanceId: string,
  botId: string,
  yaw: number,
  pitch: number,
): Promise<void> {
  const instance = assertInstanceId(instanceId);
  const bot = assertBotId(botId);
  if (!Number.isFinite(yaw) || !Number.isFinite(pitch) || yaw < -180 || yaw > 180 || pitch < -90 || pitch > 90) {
    throw new Error("Угол камеры вне допустимого диапазона.");
  }
  await withSoulFire((client) => client.instance(instance).bot(bot).look(yaw, pitch));
}

export async function resetBotMovement(instanceId: string, botId: string): Promise<void> {
  const instance = assertInstanceId(instanceId);
  const bot = assertBotId(botId);
  await withSoulFire((client) => client.instance(instance).bot(bot).resetMovement());
}

export async function createServerGroup(input: NewGroupInput): Promise<{
  instanceId: string;
  botCount: number;
}> {
  const name = input.name.trim();
  const serverAddress = input.serverAddress.trim();
  if (name.length < 2 || name.length > 64) {
    throw new Error("Название группы должно содержать от 2 до 64 символов.");
  }
  if (!/^(?:\[[0-9a-fA-F:]+\]|[a-zA-Z0-9.-]+)(?::\d{1,5})?$/.test(serverAddress)) {
    throw new Error("Введите адрес Minecraft-сервера в формате play.example.net или play.example.net:25565.");
  }
  const portMatch = serverAddress.match(/:(\d{1,5})$/);
  if (portMatch && (Number(portMatch[1]) < 1 || Number(portMatch[1]) > 65_535)) {
    throw new Error("Порт должен быть в диапазоне 1–65535.");
  }
  if (input.botNames.length > 100) {
    throw new Error("За один раз можно добавить не более 100 offline-ботов.");
  }

  const botNames = [...new Set(input.botNames.map((value) => value.trim()).filter(Boolean))];
  for (const botName of botNames) {
    if (!/^[A-Za-z0-9_]{1,16}$/.test(botName)) {
      throw new Error(`Имя «${botName}» должно содержать 1–16 символов: латинские буквы, цифры или _ .`);
    }
  }

  return withSoulFire((client) => Effect.gen(function* () {
    const instance = yield* client.getOrCreateInstance(name, { server: serverAddress });
    for (const botName of botNames) {
      yield* instance.getOrCreateBot(botName, { auth: "offline", start: false });
    }
    return { instanceId: instance.id, botCount: botNames.length };
  }));
}
