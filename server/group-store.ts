import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

interface GroupMetadata {
  baseUrl: string;
  instanceId: string;
  serverAddress: string;
}

const directory = join(process.cwd(), "data");
const filePath = join(directory, "server-groups.json");
let groups: GroupMetadata[] = [];

try {
  const parsed: unknown = JSON.parse(readFileSync(filePath, "utf8"));
  if (Array.isArray(parsed)) {
    groups = parsed.filter(isGroupMetadata);
  }
} catch {
  // A missing local data file is expected on first launch.
}

function isGroupMetadata(value: unknown): value is GroupMetadata {
  if (!value || typeof value !== "object") return false;
  const item = value as Record<string, unknown>;
  return typeof item.baseUrl === "string"
    && typeof item.instanceId === "string"
    && typeof item.serverAddress === "string";
}

export function rememberServerAddress(
  baseUrl: string,
  instanceId: string,
  serverAddress: string,
): void {
  groups = groups.filter((item) => !(item.baseUrl === baseUrl && item.instanceId === instanceId));
  groups.push({ baseUrl, instanceId, serverAddress });
  persist();
}

export function getServerAddress(baseUrl: string | undefined, instanceId: string): string | undefined {
  if (!baseUrl) return undefined;
  return groups.find((item) => item.baseUrl === baseUrl && item.instanceId === instanceId)?.serverAddress;
}

function persist(): void {
  mkdirSync(directory, { recursive: true });
  writeFileSync(filePath, JSON.stringify(groups, null, 2), { encoding: "utf8", mode: 0o600 });
}
