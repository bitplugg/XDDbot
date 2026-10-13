# XDDBOT telemetry worker

Cloudflare Worker that collects **opt-in, anonymous** usage events from the app.
It never receives keys, codes, BLE addresses or device identity; the worker
additionally rejects any payload that looks like it contains such data.

## API

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| `GET` | `/v1/health` | – | `200 {"ok":true}` |
| `POST` | `/v1/events` | JSON batch | `204` (no content) |

Request body:

```json
{
  "app": "0.1.0",
  "sdk": 34,
  "install": "<random per-install id, no PII>",
  "events": [
    { "name": "handshake_ok", "ts": 1730000000000, "props": { "device_type": 66 } }
  ]
}
```

Rules enforced by the worker:

- `content-type: application/json`, body ≤ 8 KiB, ≤ 50 events.
- `name` matches `^[a-z0-9_]{1,64}$`; `props` ≤ 12 keys of primitives.
- any key/value containing `key`, `code`, `secret`, `token`, `password`, `imei`,
  `mac`, `address` or `serial` is rejected with `400 sensitive_data`.

## Deploy

```bash
cd worker
npm install
npx wrangler login          # once
npm test                    # node --test
npx wrangler deploy         # prints the workers.dev URL
```

Then build the app with the endpoint baked in:

```bash
TELEMETRY_URL="https://xddbot-telemetry.<account>.workers.dev/v1/events" \
  ./gradlew assembleRelease
```

Telemetry in the app stays **off** until the user enables it in Settings, and
does nothing at all when `TELEMETRY_URL` is empty.

## Optional counters

Counters (per day, per event, plus `props` breakdown) are stored in a KV
namespace with a 90-day TTL. Enable it in `wrangler.toml`:

```bash
npx wrangler kv namespace create TELEMETRY
```
