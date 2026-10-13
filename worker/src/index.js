// XDDBOT telemetry collector.
//
// Minimal, privacy-preserving ingest endpoint for OPT-IN anonymous usage events.
// The client never sends keys, codes, BLE addresses, device names or the user's
// scooter identity — and this worker defensively rejects payloads that look like
// they do. Storage via KV is optional; without a binding the worker still
// validates and acknowledges events (useful for smoke testing).

const MAX_BODY_BYTES = 8 * 1024;
const MAX_EVENTS = 50;
const MAX_PROP_KEYS = 12;
const MAX_STR = 96;
const MAX_NAME = 64;
const EVENT_RE = /^[a-z0-9_]{1,64}$/;

// Whole tokens that must never appear in a telemetry prop key or value.
// Token matching (split on non-alphanumerics) deliberately avoids false
// positives such as "opcode". This is a best-effort backstop: the client is
// designed never to send keys/codes/addresses in the first place.
const FORBIDDEN = new Set([
  "key", "keys", "code", "codes", "secret", "token", "password", "passwd",
  "imei", "mac", "address", "serial", "pin",
  "apikey", "secretkey", "privatekey", "accesskey", "unlockcode", "devicecode",
]);

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/v1/health") {
      return json({ ok: true, service: "xddbot-telemetry" });
    }
    if (request.method === "POST" && url.pathname === "/v1/events") {
      return handleEvents(request, env);
    }
    return json({ error: "not_found" }, 404);
  },
};

async function handleEvents(request, env) {
  const contentType = request.headers.get("content-type") || "";
  if (!contentType.toLowerCase().includes("application/json")) {
    return json({ error: "unsupported_media_type" }, 415);
  }

  const raw = await request.text();
  if (raw.length > MAX_BODY_BYTES) {
    return json({ error: "payload_too_large" }, 413);
  }

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  const error = validate(payload);
  if (error) {
    return json({ error }, 400);
  }

  const day = new Date().toISOString().slice(0, 10);
  if (env && env.TELEMETRY && typeof env.TELEMETRY.put === "function") {
    try {
      await Promise.all(
        payload.events.map((event) => bump(env.TELEMETRY, `d:${day}:${event.name}`, event.props)),
      );
    } catch (e) {
      console.error("kv_write_failed", e && e.message);
    }
  }

  return new Response(null, { status: 204 });
}

/** Validates and normalizes a batch; returns an error string or null. */
function validate(payload) {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) return "bad_payload";
  if (typeof payload.app !== "string" || payload.app.length > MAX_STR) return "bad_app";
  if (!Number.isInteger(payload.sdk) || payload.sdk < 12 || payload.sdk > 200) return "bad_sdk";
  if (payload.install !== undefined && (typeof payload.install !== "string" || payload.install.length > MAX_STR)) {
    return "bad_install";
  }
  if (!Array.isArray(payload.events) || payload.events.length === 0 || payload.events.length > MAX_EVENTS) {
    return "bad_events";
  }
  for (const event of payload.events) {
    if (!event || typeof event !== "object" || Array.isArray(event)) return "bad_event";
    if (typeof event.name !== "string" || !EVENT_RE.test(event.name)) return "bad_event_name";
    if (!Number.isFinite(event.ts)) return "bad_event_ts";
    const props = event.props ?? {};
    if (typeof props !== "object" || Array.isArray(props)) return "bad_props";
    const keys = Object.keys(props);
    if (keys.length > MAX_PROP_KEYS) return "too_many_props";
    for (const key of keys) {
      const value = props[key];
      const text = typeof value === "string" ? value : String(value);
      if (!isPrimitive(value)) return "bad_prop_value";
      if (text.length > MAX_STR) return "prop_too_long";
      if (isSensitive(key) || isSensitive(text)) return "sensitive_data";
    }
  }
  return null;
}

function isPrimitive(value) {
  return typeof value === "string" || typeof value === "number" || typeof value === "boolean" || value === null;
}

function isSensitive(text) {
  return tokens(text).some((token) => FORBIDDEN.has(token));
}

function tokens(text) {
  return String(text).toLowerCase().split(/[^a-z0-9]+/).filter(Boolean);
}

async function bump(kv, key, props) {
  const current = Number(await kv.get(key)) || 0;
  await kv.put(key, String(current + 1), { expirationTtl: 60 * 60 * 24 * 90 });
  if (props && typeof props === "object") {
    for (const [propKey, propValue] of Object.entries(props)) {
      if (isSensitive(propKey) || isSensitive(propValue)) continue;
      const propCounterKey = `${key}:${propKey}=${String(propValue).slice(0, MAX_STR)}`;
      const propCount = Number(await kv.get(propCounterKey)) || 0;
      await kv.put(propCounterKey, String(propCount + 1), { expirationTtl: 60 * 60 * 24 * 90 });
    }
  }
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}
