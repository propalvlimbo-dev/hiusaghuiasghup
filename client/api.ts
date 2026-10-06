import type {
  BotRow,
  FleetAction,
  FleetOperation,
  InstanceSummary,
  SessionInfo,
} from "../shared/types";

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
      cache: "no-store",
    });
  } catch {
    throw new Error("Панель не смогла связаться со своим API. Обновите страницу и проверьте, что сервер запущен.");
  }

  const payload = await response.json().catch(() => ({})) as { error?: string } & T;
  if (!response.ok) {
    throw new Error(payload.error || `Ошибка запроса (${response.status}).`);
  }
  return payload;
}

export const api = {
  session: () => requestJson<SessionInfo>("/api/session"),

  connect: (baseUrl: string, token: string) => requestJson<{
    connected: true;
    baseUrl: string;
    instanceCount: number;
  }>("/api/connect", {
    method: "POST",
    body: JSON.stringify({ baseUrl, token }),
  }),

  disconnect: () => requestJson<{ connected: false }>("/api/disconnect", {
    method: "POST",
    body: "{}",
  }),

  instances: async () => {
    const result = await requestJson<{ instances: InstanceSummary[] }>("/api/instances");
    return result.instances;
  },

  bots: async (instanceId: string) => {
    const result = await requestJson<{ bots: BotRow[] }>(`/api/instances/${encodeURIComponent(instanceId)}/bots`);
    return result.bots;
  },

  povStream: (instanceId: string, botId: string) =>
    `/api/instances/${encodeURIComponent(instanceId)}/bots/${encodeURIComponent(botId)}/pov/stream`,

  pov: async (instanceId: string, botId: string) => {
    const path = `/api/instances/${encodeURIComponent(instanceId)}/bots/${encodeURIComponent(botId)}/pov`;
    const response = await fetch(path, { cache: "no-store", headers: { Accept: "image/png" } });
    if (!response.ok) {
      const payload = await response.json().catch(() => ({})) as { error?: string };
      throw new Error(payload.error || `Не удалось получить кадр (${response.status}).`);
    }
    return response.blob();
  },

  moveBot: (instanceId: string, botId: string, key: string, durationMs = 350) =>
    requestJson<{ ok: true }>(`/api/instances/${encodeURIComponent(instanceId)}/bots/${encodeURIComponent(botId)}/control/move`, {
      method: "POST",
      body: JSON.stringify({ key, durationMs }),
    }),

  lookBot: (instanceId: string, botId: string, yaw: number, pitch: number) =>
    requestJson<{ ok: true }>(`/api/instances/${encodeURIComponent(instanceId)}/bots/${encodeURIComponent(botId)}/control/look`, {
      method: "POST",
      body: JSON.stringify({ yaw, pitch }),
    }),

  resetBot: (instanceId: string, botId: string) =>
    requestJson<{ ok: true }>(`/api/instances/${encodeURIComponent(instanceId)}/bots/${encodeURIComponent(botId)}/control/reset`, {
      method: "POST",
      body: "{}",
    }),

  createGroup: (input: {
    name: string;
    serverAddress: string;
    botNames: string[];
    authorized: boolean;
  }) => requestJson<{ instanceId: string; botCount: number }>("/api/instances", {
    method: "POST",
    body: JSON.stringify(input),
  }),

  operate: (action: FleetAction, targets: Array<{ instanceId: string; label: string }>) =>
    requestJson<{ operation: FleetOperation }>("/api/operations", {
      method: "POST",
      body: JSON.stringify({ action, targets }),
    }),

  operations: async () => {
    const result = await requestJson<{ operations: FleetOperation[] }>("/api/operations");
    return result.operations;
  },
};
