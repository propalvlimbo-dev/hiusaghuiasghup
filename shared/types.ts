export type FleetAction = "start" | "stop";

export type TargetOperationStatus =
  | "queued"
  | "running"
  | "applied"
  | "failed"
  | "cancelled";

export type FleetOperationStatus =
  | "queued"
  | "running"
  | "completed"
  | "partial"
  | "cancelled";

export interface BotSummary {
  totalBots: number;
  desiredBots: number;
  onlineBots: number;
  startingBots: number;
  retryingBots: number;
  failedBots: number;
}

export interface InstanceSummary {
  id: string;
  name: string;
  icon: string;
  serverAddress?: string;
  botSummary: BotSummary;
}

export interface BotRow {
  id: string;
  name: string;
  online: boolean;
  pingMs?: number;
  connectionPhase: string;
  desiredState: string;
  runtimeState: string;
  lastError?: string;
  health?: number;
  dimension?: string;
  yaw?: number;
  pitch?: number;
}

export interface SessionInfo {
  connected: boolean;
  baseUrl?: string;
}

export interface OperationTarget {
  instanceId: string;
  label: string;
  status: TargetOperationStatus;
  message?: string;
  error?: string;
  updatedAt: string;
}

export interface FleetOperation {
  id: string;
  action: FleetAction;
  status: FleetOperationStatus;
  createdAt: string;
  targets: OperationTarget[];
}

export interface ApiError {
  error: string;
}
