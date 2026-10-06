import { spawn } from "node:child_process";
import { resolve } from "node:path";
import process from "node:process";

const projectRoot = process.cwd();
const children = [
  spawn(process.execPath, [resolve(projectRoot, "node_modules/tsx/dist/cli.mjs"), "watch", "server/index.ts"], {
    cwd: projectRoot,
    stdio: "inherit",
    env: { ...process.env, NODE_ENV: "development" },
  }),
  spawn(process.execPath, [resolve(projectRoot, "node_modules/vite/bin/vite.js"), "--host", "0.0.0.0", "--port", "5173", "--strictPort"], {
    cwd: projectRoot,
    stdio: "inherit",
    env: { ...process.env, NODE_ENV: "development" },
  }),
];

let shuttingDown = false;
function stopChildren(exitCode = 0) {
  if (shuttingDown) return;
  shuttingDown = true;
  for (const child of children) {
    if (child.exitCode === null) child.kill("SIGTERM");
  }
  process.exitCode = exitCode;
}

for (const child of children) {
  child.on("error", (error) => {
    console.error("Could not start a dashboard process:", error.message);
    stopChildren(1);
  });
  child.on("exit", (code, signal) => {
    if (!shuttingDown) {
      const exitCode = typeof code === "number" ? code : signal === "SIGINT" ? 0 : 1;
      stopChildren(exitCode);
    }
  });
}

process.on("SIGINT", () => stopChildren(0));
process.on("SIGTERM", () => stopChildren(0));
