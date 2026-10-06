import { spawn } from "node:child_process";
import { resolve } from "node:path";
import process from "node:process";

const child = spawn(process.execPath, [resolve(process.cwd(), "node_modules/tsx/dist/cli.mjs"), "server/index.ts"], {
  cwd: process.cwd(),
  stdio: "inherit",
  env: { ...process.env, NODE_ENV: "production" },
});

child.on("error", (error) => {
  console.error("Could not start the dashboard:", error.message);
  process.exitCode = 1;
});
child.on("exit", (code, signal) => {
  process.exitCode = typeof code === "number" ? code : signal === "SIGINT" ? 0 : 1;
});
process.on("SIGINT", () => child.kill("SIGINT"));
process.on("SIGTERM", () => child.kill("SIGTERM"));
