import assert from "node:assert/strict";
import test from "node:test";
import { formatError } from "../server/error-format.js";
import { assertBotId, assertInstanceId, createServerGroup, lookBot, pulseBotMovement, validateCredentials } from "../server/soulfire.js";

test("accepts local SoulFire URLs and removes trailing slashes", () => {
  assert.deepEqual(
    validateCredentials({ baseUrl: " http://127.0.0.1:38765/ ", token: "test-token-123" }),
    { baseUrl: "http://127.0.0.1:38765", token: "test-token-123" },
  );
});

test("requires HTTPS for remote SoulFire servers", () => {
  assert.throws(
    () => validateCredentials({ baseUrl: "http://soulfire.example.net:38765", token: "test-token-123" }),
    /используйте HTTPS/i,
  );
});

test("does not accept credentials or query parameters embedded in the URL", () => {
  assert.throws(
    () => validateCredentials({ baseUrl: "https://user:pass@soulfire.example.net", token: "test-token-123" }),
    /без логина/i,
  );
  assert.throws(
    () => validateCredentials({ baseUrl: "https://soulfire.example.net?token=secret", token: "test-token-123" }),
    /без логина/i,
  );
});

test("validates SoulFire instance identifiers", () => {
  assert.equal(assertInstanceId("9eac3db6-7d20-4f78-bd1c-3a467c721daa"), "9eac3db6-7d20-4f78-bd1c-3a467c721daa");
  assert.throws(() => assertInstanceId("not-an-id"), /ID инстанса/i);
});

test("validates bot identifiers before POV or control operations", () => {
  assert.equal(assertBotId("9eac3db6-7d20-4f78-bd1c-3a467c721daa"), "9eac3db6-7d20-4f78-bd1c-3a467c721daa");
  assert.throws(() => assertBotId("not-a-bot-id"), /ID бота/i);
});

test("bounds manual movement pulses and camera angles", async () => {
  const instanceId = "9eac3db6-7d20-4f78-bd1c-3a467c721daa";
  const botId = "6c9f2b30-55c1-4c60-9bc7-9ebf79aa234b";
  await assert.rejects(pulseBotMovement(instanceId, botId, "forward", 99), /от 100 до 1000 мс/i);
  await assert.rejects(pulseBotMovement(instanceId, botId, "forward", 1_001), /от 100 до 1000 мс/i);
  await assert.rejects(pulseBotMovement(instanceId, botId, "forward", Infinity), /от 100 до 1000 мс/i);
  await assert.rejects(lookBot(instanceId, botId, 181, 0), /вне допустимого диапазона/i);
  await assert.rejects(lookBot(instanceId, botId, 0, -91), /вне допустимого диапазона/i);
});

test("validates Java server addresses and offline profile names before provisioning", async () => {
  await assert.rejects(
    createServerGroup({ name: "Test group", serverAddress: "server.example.net:0", botNames: [] }),
    /Порт должен быть/i,
  );
  await assert.rejects(
    createServerGroup({ name: "Test group", serverAddress: "server.example.net", botNames: ["not a username"] }),
    /Имя «not a username»/,
  );
});

test("converts nested network errors into a clear connection message", () => {
  const error = {
    _tag: "SoulFireConnectionError",
    cause: {
      _tag: "SoulFireRpcError",
      message: "[unknown] fetch failed",
      cause: {
        message: "fetch failed",
        cause: { code: "ECONNREFUSED" },
      },
    },
  };
  assert.match(formatError(error), /Не удалось связаться с SoulFire/i);
});
