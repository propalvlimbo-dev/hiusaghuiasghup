interface ErrorLike {
  message?: unknown;
  rawMessage?: unknown;
  cause?: unknown;
  code?: unknown;
  _tag?: unknown;
}

export function formatError(error: unknown): string {
  const chain: ErrorLike[] = [];
  const seen = new Set<unknown>();
  let current: unknown = error;

  while (current && typeof current === "object" && !seen.has(current)) {
    seen.add(current);
    const item = current as ErrorLike;
    chain.push(item);
    current = item.cause;
  }

  if (chain.some((item) => item.code === "ECONNREFUSED" || item.code === "ENOTFOUND" || item.code === "ETIMEDOUT")) {
    return "Не удалось связаться с SoulFire. Проверьте адрес и порт, доступность backend с этой машины и то, что SoulFire запущен.";
  }
  if (chain.some((item) => item._tag === "SoulFireCompatibilityError")) {
    return "Версия SDK панели несовместима с SoulFire. Обновите SoulFire или используйте совпадающие версии.";
  }

  const messages = chain
    .flatMap((item) => [item.message, item.rawMessage])
    .filter((message): message is string => typeof message === "string" && message.trim().length > 0)
    .map((message) => message.trim());
  const usefulMessage = [...messages].reverse().find((message) => message !== "fetch failed" && message !== "[unknown] fetch failed");
  if (usefulMessage) return usefulMessage.slice(0, 500);
  if (messages.length > 0) {
    return "Не удалось выполнить запрос к SoulFire. Проверьте адрес, токен и совместимость версий.";
  }
  if (error instanceof Error && error.message.trim()) return error.message.slice(0, 500);
  return "Произошла неизвестная ошибка.";
}
