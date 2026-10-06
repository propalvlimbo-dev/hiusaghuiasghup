import express, { type Request, type Response, type NextFunction } from "express";
import { resolve } from "node:path";
import type { FleetAction } from "../shared/types.js";
import { getServerAddress, rememberServerAddress } from "./group-store.js";
import { formatError } from "./error-format.js";
import { hasActiveOperations, listOperations, submitFleetOperation } from "./operation-manager.js";
import {
  assertBotId,
  assertInstanceId,
  captureBotPov,
  createServerGroup,
  disconnectSoulFire,
  getSession,
  hasConnection,
  listBots,
  listInstances,
  lookBot,
  pulseBotMovement,
  resetBotMovement,
  streamBotPov,
  testAndSaveConnection,
  type ManualMovementKey,
} from "./soulfire.js";

const app = express();
const activePovStreams = new Set<AbortController>();
app.disable("x-powered-by");
app.use(express.json({ limit: "128kb" }));
app.use("/api", (_request, response, next) => {
  response.setHeader("Cache-Control", "no-store");
  next();
});

app.use("/api", requireSameOriginForMutations);

app.get("/api/session", (_request, response) => {
  response.json(getSession());
});

app.post("/api/connect", asyncRoute(async (request, response) => {
  if (hasConnection() && hasActiveOperations()) {
    response.status(409).json({ error: "Дождитесь завершения текущих команд перед сменой подключения." });
    return;
  }

  try {
    const result = await testAndSaveConnection({
      baseUrl: request.body?.baseUrl,
      token: request.body?.token,
    });
    stopActivePovStreams();
    response.json({ connected: true, ...result });
  } catch (error) {
    response.status(400).json({ error: safeError(error) });
  }
}));

app.post("/api/disconnect", (_request, response) => {
  if (hasActiveOperations()) {
    response.status(409).json({ error: "Сначала дождитесь завершения команд запуска или остановки." });
    return;
  }
  stopActivePovStreams();
  disconnectSoulFire();
  response.json({ connected: false });
});

app.get("/api/instances", asyncRoute(async (_request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const baseUrl = getSession().baseUrl;
  const instances = await listInstances((instanceId) => getServerAddress(baseUrl, instanceId));
  response.json({ instances });
}));

app.get("/api/instances/:instanceId/bots", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const rawInstanceId = request.params.instanceId;
  const instanceId = assertInstanceId(Array.isArray(rawInstanceId) ? rawInstanceId[0] : rawInstanceId);
  const bots = await listBots(instanceId);
  response.json({ bots });
}));

app.get("/api/instances/:instanceId/bots/:botId/pov", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const instanceId = assertInstanceId(routeParam(request.params.instanceId));
  const botId = assertBotId(routeParam(request.params.botId));
  const image = await captureBotPov(instanceId, botId);
  response.setHeader("Cache-Control", "no-store");
  response.type("png").send(Buffer.from(image));
}));

app.get("/api/instances/:instanceId/bots/:botId/pov/stream", (request, response, next) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const instanceId = assertInstanceId(routeParam(request.params.instanceId));
  const botId = assertBotId(routeParam(request.params.botId));
  const abortController = new AbortController();
  activePovStreams.add(abortController);
  response.status(200);
  response.setHeader("Content-Type", "multipart/x-mixed-replace; boundary=sf-frame");
  response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
  response.setHeader("Connection", "keep-alive");
  response.setHeader("X-Accel-Buffering", "no");
  response.on("close", () => {
    abortController.abort();
    activePovStreams.delete(abortController);
  });

  void streamBotPov(instanceId, botId, async (image) => {
    const header = Buffer.from(`--sf-frame\r\nContent-Type: image/png\r\nContent-Length: ${image.byteLength}\r\n\r\n`);
    await writeResponseChunk(response, header, abortController.signal);
    await writeResponseChunk(response, Buffer.from(image), abortController.signal);
    await writeResponseChunk(response, Buffer.from("\r\n"), abortController.signal);
  }, abortController.signal).then(() => {
    activePovStreams.delete(abortController);
    if (!response.destroyed && !response.writableEnded) response.end();
  }).catch((error: unknown) => {
    activePovStreams.delete(abortController);
    if (abortController.signal.aborted || response.destroyed) return;
    if (response.headersSent) {
      console.error("POV stream failed:", safeError(error));
      response.destroy();
      return;
    }
    next(error);
  });
});

app.post("/api/instances/:instanceId/bots/:botId/control/move", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const instanceId = assertInstanceId(routeParam(request.params.instanceId));
  const botId = assertBotId(routeParam(request.params.botId));
  const validKeys: ManualMovementKey[] = ["forward", "backward", "left", "right", "jump", "sneak", "sprint"];
  const key = request.body?.key;
  if (typeof key !== "string" || !validKeys.includes(key as ManualMovementKey)) {
    response.status(400).json({ error: "Неизвестная клавиша управления." });
    return;
  }
  const durationMs = typeof request.body?.durationMs === "number" ? request.body.durationMs : 350;
  await pulseBotMovement(instanceId, botId, key as ManualMovementKey, durationMs);
  response.json({ ok: true });
}));

app.post("/api/instances/:instanceId/bots/:botId/control/look", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const instanceId = assertInstanceId(routeParam(request.params.instanceId));
  const botId = assertBotId(routeParam(request.params.botId));
  const { yaw, pitch } = request.body ?? {};
  if (typeof yaw !== "number" || typeof pitch !== "number") {
    response.status(400).json({ error: "Передайте углы yaw и pitch." });
    return;
  }
  await lookBot(instanceId, botId, yaw, pitch);
  response.json({ ok: true });
}));

app.post("/api/instances/:instanceId/bots/:botId/control/reset", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  const instanceId = assertInstanceId(routeParam(request.params.instanceId));
  const botId = assertBotId(routeParam(request.params.botId));
  await resetBotMovement(instanceId, botId);
  response.json({ ok: true });
}));

app.post("/api/instances", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }
  if (request.body?.authorized !== true) {
    response.status(400).json({ error: "Подтвердите, что сервер ваш или у вас есть разрешение на тестирование." });
    return;
  }

  const botNames = request.body?.botNames;
  if (!Array.isArray(botNames) || botNames.some((name: unknown) => typeof name !== "string")) {
    response.status(400).json({ error: "Список имён ботов должен быть массивом строк." });
    return;
  }

  const result = await createServerGroup({
    name: typeof request.body?.name === "string" ? request.body.name : "",
    serverAddress: typeof request.body?.serverAddress === "string" ? request.body.serverAddress : "",
    botNames,
  });
  const baseUrl = getSession().baseUrl;
  if (baseUrl) rememberServerAddress(baseUrl, result.instanceId, request.body.serverAddress.trim());
  response.status(201).json(result);
}));

app.post("/api/operations", asyncRoute(async (request, response) => {
  if (!hasConnection()) {
    response.status(409).json({ error: "Сначала подключитесь к SoulFire." });
    return;
  }

  const action = request.body?.action;
  if (action !== "start" && action !== "stop") {
    response.status(400).json({ error: "Команда должна быть start или stop." });
    return;
  }

  const targets = request.body?.targets;
  if (!Array.isArray(targets) || targets.some((target: unknown) => !isTarget(target))) {
    response.status(400).json({ error: "Передайте список серверных групп." });
    return;
  }

  const operation = submitFleetOperation(action as FleetAction, targets);
  response.status(202).json({ operation });
}));

app.get("/api/operations", (_request, response) => {
  response.json({ operations: listOperations() });
});

app.use("/api", (_request, response) => {
  response.status(404).json({ error: "API-маршрут не найден." });
});

if (process.env.NODE_ENV === "production") {
  const distDirectory = resolve(process.cwd(), "dist");
  app.use(express.static(distDirectory, { index: false, maxAge: "1h" }));
  app.get("*path", (_request, response) => response.sendFile(resolve(distDirectory, "index.html")));
}

app.use((error: unknown, _request: Request, response: Response, _next: NextFunction) => {
  console.error("Request failed:", safeError(error));
  if (response.headersSent) return;
  response.status(500).json({ error: safeError(error) });
});

function stopActivePovStreams(): void {
  for (const controller of activePovStreams) controller.abort();
  activePovStreams.clear();
}

function writeResponseChunk(response: Response, chunk: Buffer, signal: AbortSignal): Promise<void> {
  if (signal.aborted || response.destroyed) return Promise.resolve();
  if (response.write(chunk)) return Promise.resolve();

  return new Promise((resolve) => {
    const cleanup = () => {
      response.off("drain", finish);
      response.off("close", finish);
      response.off("error", finish);
      signal.removeEventListener("abort", finish);
    };
    const finish = () => {
      cleanup();
      resolve();
    };
    response.once("drain", finish);
    response.once("close", finish);
    response.once("error", finish);
    signal.addEventListener("abort", finish, { once: true });
  });
}

function routeParam(value: string | string[] | undefined): string {
  return Array.isArray(value) ? value[0] ?? "" : value ?? "";
}

function isTarget(value: unknown): value is { instanceId: string; label: string } {
  if (!value || typeof value !== "object") return false;
  const target = value as Record<string, unknown>;
  return typeof target.instanceId === "string"
    && typeof target.label === "string"
    && target.label.length <= 128;
}

function requireSameOriginForMutations(
  request: Request,
  response: Response,
  next: NextFunction,
): void {
  if (request.method === "GET" || request.method === "HEAD" || request.method === "OPTIONS") {
    next();
    return;
  }

  const origin = request.get("origin");
  const forwardedHost = process.env.NODE_ENV === "development"
    ? request.get("x-forwarded-host")?.split(",")[0]?.trim()
    : undefined;
  const expectedHost = forwardedHost || request.get("host");
  if (!origin || !expectedHost) {
    response.status(403).json({ error: "Запрос отклонён: отсутствует same-origin заголовок." });
    return;
  }

  try {
    if (new URL(origin).host !== expectedHost) {
      response.status(403).json({ error: "Запрос отклонён: источник не совпадает с панелью." });
      return;
    }
  } catch {
    response.status(403).json({ error: "Запрос отклонён: некорректный источник." });
    return;
  }
  next();
}

function asyncRoute(
  handler: (request: Request, response: Response) => Promise<void>,
) {
  return (request: Request, response: Response, next: NextFunction) => {
    void handler(request, response).catch(next);
  };
}

function safeError(error: unknown): string {
  return formatError(error);
}

const port = Number(process.env.PORT || 4173);
const host = process.env.HOST || "127.0.0.1";
app.listen(port, host, () => {
  console.log(`SoulFire Fleet API listening on http://${host}:${port}`);
});
