import { randomUUID } from "node:crypto";
import { Effect } from "effect";
import type { FleetAction, FleetOperation, OperationTarget } from "../shared/types.js";
import type { SoulFireInstance } from "@soulfiremc/sdk";
import { assertInstanceId, withSoulFire } from "./soulfire.js";
import { formatError } from "./error-format.js";

interface DesiredState {
  action: FleetAction;
  generation: number;
}

interface QueuedJob {
  operationId: string;
  instanceId: string;
  action: FleetAction;
  generation: number;
  target: OperationTarget;
}

const operations: FleetOperation[] = [];
const desiredStates = new Map<string, DesiredState>();
const startQueue: QueuedJob[] = [];
const stopQueue: QueuedJob[] = [];
const maxConcurrentStarts = 2;
const maxConcurrentStops = 8;
let runningStarts = 0;
let runningStops = 0;

export function submitFleetOperation(
  action: FleetAction,
  inputs: Array<{ instanceId: string; label: string }>,
): FleetOperation {
  if (inputs.length === 0) throw new Error("Выберите хотя бы одну серверную группу.");
  if (inputs.length > 50) throw new Error("За одну команду можно выбрать не более 50 серверных групп.");

  const seen = new Set<string>();
  const targets: OperationTarget[] = inputs.map((input) => {
    const instanceId = assertInstanceId(input.instanceId);
    if (seen.has(instanceId)) throw new Error("В списке есть повторяющиеся серверные группы.");
    seen.add(instanceId);
    const label = input.label.trim().slice(0, 64) || instanceId.slice(0, 8);
    return {
      instanceId,
      label,
      status: "queued",
      updatedAt: new Date().toISOString(),
    };
  });

  const operation: FleetOperation = {
    id: randomUUID(),
    action,
    status: "queued",
    createdAt: new Date().toISOString(),
    targets,
  };
  operations.unshift(operation);
  trimHistory();

  targets.forEach((target) => {
    const previous = desiredStates.get(target.instanceId);
    const generation = (previous?.generation ?? 0) + 1;
    desiredStates.set(target.instanceId, { action, generation });
    cancelQueuedJobs(target.instanceId);

    const job: QueuedJob = {
      operationId: operation.id,
      instanceId: target.instanceId,
      action,
      generation,
      target,
    };
    (action === "stop" ? stopQueue : startQueue).push(job);
  });

  refreshOperationStatuses();
  drainQueues();
  return structuredClone(operation);
}

export function listOperations(): FleetOperation[] {
  refreshOperationStatuses();
  return structuredClone(operations);
}

export function hasActiveOperations(): boolean {
  return operations.some((operation) => operation.status === "queued" || operation.status === "running");
}

function cancelQueuedJobs(instanceId: string): void {
  for (const queue of [startQueue, stopQueue]) {
    for (let index = queue.length - 1; index >= 0; index -= 1) {
      const job = queue[index];
      if (job.instanceId !== instanceId) continue;
      queue.splice(index, 1);
      job.target.status = "cancelled";
      job.target.message = "Заменено более новой командой.";
      job.target.updatedAt = new Date().toISOString();
    }
  }
}

function drainQueues(): void {
  while (runningStops < maxConcurrentStops && stopQueue.length > 0) {
    const job = stopQueue.shift();
    if (!job) break;
    runningStops += 1;
    void runJob(job).finally(() => {
      runningStops -= 1;
      refreshOperationStatuses();
      drainQueues();
    });
  }

  while (runningStarts < maxConcurrentStarts && startQueue.length > 0) {
    const job = startQueue.shift();
    if (!job) break;
    runningStarts += 1;
    void runJob(job).finally(() => {
      runningStarts -= 1;
      refreshOperationStatuses();
      drainQueues();
    });
  }
}

async function runJob(job: QueuedJob): Promise<void> {
  job.target.status = "running";
  job.target.message = job.action === "start" ? "Отправляем команду SoulFire…" : "Отправляем команду остановки…";
  job.target.updatedAt = new Date().toISOString();
  refreshOperationStatuses();

  try {
    await withSoulFire((client) => Effect.gen(function* () {
      const instance = client.instance(job.instanceId);
      let appliedGeneration = job.generation;
      yield* applyAction(instance, job.action);

      // If Start and Stop overlap, converge to the most recent user intent.
      // SoulFire changes desired state durably and reconciles connections in the background.
      while (true) {
        const latest = desiredStates.get(job.instanceId);
        if (!latest || latest.generation === appliedGeneration) break;
        yield* applyAction(instance, latest.action);
        appliedGeneration = latest.generation;
      }
    }));

    const latest = desiredStates.get(job.instanceId);
    if (latest && latest.generation !== job.generation) {
      job.target.status = "cancelled";
      job.target.message = `Заменено командой «${latest.action === "start" ? "Запустить" : "Остановить"}».`;
    } else {
      job.target.status = "applied";
      job.target.message = job.action === "start"
        ? "Команда запуска принята; подключение идёт в фоне."
        : "Команда остановки принята; отключение идёт в фоне.";
    }
  } catch (error) {
    const latest = desiredStates.get(job.instanceId);
    if (latest && latest.generation !== job.generation) {
      job.target.status = "cancelled";
      job.target.message = "Заменено более новой командой.";
    } else {
      job.target.status = "failed";
      job.target.error = formatError(error);
    }
  } finally {
    job.target.updatedAt = new Date().toISOString();
    refreshOperationStatuses();
  }
}

function refreshOperationStatuses(): void {
  for (const operation of operations) {
    const statuses = operation.targets.map((target) => target.status);
    const hasPending = statuses.some((status) => status === "queued" || status === "running");
    if (hasPending) {
      operation.status = statuses.some((status) => status === "running") ? "running" : "queued";
      continue;
    }

    const failed = statuses.some((status) => status === "failed");
    const cancelled = statuses.some((status) => status === "cancelled");
    if (failed) operation.status = "partial";
    else if (cancelled && statuses.every((status) => status === "cancelled")) operation.status = "cancelled";
    else operation.status = "completed";
  }
}

function trimHistory(): void {
  if (operations.length <= 50) return;
  operations.splice(50);
}

function applyAction(instance: SoulFireInstance, action: FleetAction) {
  return action === "start" ? instance.start() : instance.stop();
}
