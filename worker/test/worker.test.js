import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";

const BASE = "https://telemetry.example.com";

function call(method, path, body, headers = {}) {
  return worker.fetch(new Request(BASE + path, { method, body, headers }), {});
}

function event(name, props = {}) {
  return { name, ts: Date.now(), props };
}

function validPayload(events = [event("app_start")]) {
  return JSON.stringify({ app: "0.1.0", sdk: 34, install: "abc", events });
}

test("health returns ok", async () => {
  const res = await call("GET", "/v1/health");
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true, service: "xddbot-telemetry" });
});

test("unknown route returns 404", async () => {
  const res = await call("GET", "/nope");
  assert.equal(res.status, 404);
});

test("accepts a valid batch with 204", async () => {
  const res = await call("POST", "/v1/events", validPayload(), { "content-type": "application/json" });
  assert.equal(res.status, 204);
});

test("rejects wrong content type", async () => {
  const res = await call("POST", "/v1/events", validPayload(), { "content-type": "text/plain" });
  assert.equal(res.status, 415);
});

test("rejects invalid json", async () => {
  const res = await call("POST", "/v1/events", "{not json", { "content-type": "application/json" });
  assert.equal(res.status, 400);
});

test("rejects empty events", async () => {
  const res = await call("POST", "/v1/events", validPayload([]), { "content-type": "application/json" });
  assert.equal(res.status, 400);
  assert.equal((await res.json()).error, "bad_events");
});

test("rejects sensitive prop values", async () => {
  const body = validPayload([event("handshake", { value: "device code abc" })]);
  const res = await call("POST", "/v1/events", body, { "content-type": "application/json" });
  assert.equal(res.status, 400);
  assert.equal((await res.json()).error, "sensitive_data");
});

test("rejects sensitive prop keys", async () => {
  const body = validPayload([event("handshake", { code: "nope" })]);
  const res = await call("POST", "/v1/events", body, { "content-type": "application/json" });
  assert.equal(res.status, 400);
  assert.equal((await res.json()).error, "sensitive_data");
});

test("writes counters to KV when bound", async () => {
  const store = new Map();
  const kv = {
    get: async (k) => (store.has(k) ? store.get(k) : null),
    put: async (k, v) => void store.set(k, v),
  };
  const req = new Request(BASE + "/v1/events", {
    method: "POST",
    body: validPayload([event("command", { opcode: "unlock" })]),
    headers: { "content-type": "application/json" },
  });
  const res = await worker.fetch(req, { TELEMETRY: kv });
  assert.equal(res.status, 204);
  const keys = [...store.keys()];
  assert.ok(keys.some((k) => k.endsWith(":command")));
  assert.ok(store.get(keys.find((k) => k.endsWith(":command"))) === "1");
});
