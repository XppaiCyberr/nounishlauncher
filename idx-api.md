# IDX Market Data JSON: Guide for AI Agents

Read-only Indonesian Stock Exchange (IDX / Bursa Efek Indonesia) data: stock quotes, index values, top gainers and losers. Source: Yahoo Finance. No API key.

Base URL: `https://idx.xpp.wtf/`

There are two ways to get the data. **Prefer the static cache files.**

| Method | URL pattern | Hits Yahoo? | Use when |
|---|---|---|---|
| Static cache files | `https://idx.xpp.wtf/cache/<file>.json` | No | Default. Fast, safe to poll. |
| Query API | `https://idx.xpp.wtf/?cmd=<command>&...` | Only if the cache is stale | You need a single filtered result, or search. |

Do not call `?cmd=cache`. It rebuilds the cache, needs a secret token, and is for the cron job only.

---

## 1. Common envelope

Every JSON response (cache files and API) has the same shape.

Success:

```json
{
  "ok": true,
  "generatedAt": "2026-11-02T21:41:20+00:00",
  "data": { }
}
```

API responses also include `"source": "cache"` or `"source": "live"`. Cache files do not have `source`.

Failure (API only, HTTP 4xx/5xx):

```json
{ "ok": false, "error": "Invalid symbol: \"BB;CA\"" }
```

Rules for parsing:

1. Check `ok === true` first. If false, read `error` (a human-readable string) and do not read `data`.
2. `generatedAt` is an ISO 8601 timestamp in **UTC**. It is when the data was fetched from Yahoo, not when you downloaded it. WIB (Jakarta) is UTC+7, so `21:41 UTC` is `04:41 WIB` the next day.
3. `data` is an object or an array depending on the file (see below). It can be an empty array `[]`.

---

## 2. Cache files

All files live under `https://idx.xpp.wtf/cache/`.

| File | `data` type | Contents |
|---|---|---|
| `quotes.json` | object keyed by ticker | All tracked stocks: `{ "BBCA": Quote, "BBRI": Quote, ... }` |
| `quotes/<SYMBOL>.json` | Quote | One stock, e.g. `quotes/BBCA.json` |
| `gainers.json` | array of Quote | Every tracked stock with `changePercent > 0`, sorted **descending** |
| `losers.json` | array of Quote | Every tracked stock with `changePercent < 0`, sorted **ascending** (most negative first) |
| `indices.json` | object keyed by index name | `{ "IHSG": Index, "LQ45": Index }` |
| `meta.json` | Meta | Summary of the last cache build |

Notes:

- `gainers.json` and `losers.json` are **complete lists**, not top 10. Take the first N items yourself (`data.slice(0, 10)`).
- Tracked stocks are a fixed list of about 47 liquid tickers (not the whole market). So "top gainers" means top gainers among those.
- Unchanged stocks (`changePercent == 0`) appear in `quotes.json` but in neither gainers nor losers.
- A ticker missing from `quotes.json` means Yahoo returned no data for it in the last build. Check `meta.json` → `failedSymbols`.
- `indices.json` currently contains **IHSG and LQ45 only**. IDX30 is not available because Yahoo returns no data for it. Never assume a key exists; check for it before reading.

---

## 3. Data shapes

### Quote

```json
{
  "symbol": "BBCA",
  "name": "Bank Central Asia Tbk",
  "price": 9725,
  "change": 50,
  "changePercent": 0.52,
  "volume": 15200000,
  "high": 9800,
  "low": 9675,
  "open": 9700,
  "prevClose": 9675,
  "marketCap": 1200000000000000,
  "currency": "IDR"
}
```

(Values above are illustrative.)

| Field | Type | Meaning |
|---|---|---|
| `symbol` | string | IDX ticker without the `.JK` suffix |
| `name` | string | Company name (falls back to the ticker if unknown) |
| `price` | number | Last price in IDR (rupiah) |
| `change` | number | `price - prevClose`, in IDR. Negative = down |
| `changePercent` | number | **Percent units.** `0.52` means +0.52%, not 0.0052 |
| `volume` | integer | Shares traded today (not lots; 1 lot = 100 shares) |
| `high`, `low`, `open` | number | Day high, low, open in IDR |
| `prevClose` | number | Previous session close in IDR |
| `marketCap` | number | **Optional.** IDR. The key is absent when unavailable |
| `currency` | string | Normally `"IDR"` |

### Index

```json
{
  "name": "IHSG",
  "value": 5096.83,
  "change": -1.2271289062500728,
  "changePercent": -0.024070520891030973
}
```

| Field | Type | Meaning |
|---|---|---|
| `name` | string | `"IHSG"` (Jakarta Composite) or `"LQ45"` |
| `value` | number | Index level in points |
| `change` | number | Points change vs previous close |
| `changePercent` | number | Percent units |

Numbers can have long floating-point tails (like `-1.2271289062500728`). Round for display: prices to 0 decimals (or 2 if fractional), percentages to 2 decimals, index values to 2 decimals.

### SearchResult (API only, `cmd=search`)

```json
{ "symbol": "BBCA", "name": "Bank Central Asia Tbk", "exchange": "Jakarta" }
```

Search results are only returned for IDX-listed stocks. Search is never cached.

### Meta (`meta.json`)

```json
{
  "generatedAt": "2026-11-02T21:41:20+00:00",
  "durationSeconds": 7.92,
  "symbolsRequested": 47,
  "symbolsCached": 47,
  "failedSymbols": [],
  "indicesCached": ["IHSG", "LQ45"],
  "failedIndices": ["IDX30"],
  "files": ["quotes.json", "gainers.json", "..."]
}
```

Use `failedSymbols` and `failedIndices` to know what is missing and why a key is absent elsewhere.

---

## 4. Query API (optional)

All parameters go in the query string. Responses use the common envelope plus `source`.

| Command | Parameters | `data` |
|---|---|---|
| `?cmd=price&symbol=BBCA` | `symbol` required | Quote |
| `?cmd=quote&symbol=BBCA` | `symbol` required | Quote (same as `price`) |
| `?cmd=gainers&limit=10` | `limit` optional, 1 to 100, default 10 | array of Quote |
| `?cmd=losers&limit=10` | `limit` optional, 1 to 100, default 10 | array of Quote |
| `?cmd=index&name=IHSG` | `name` optional, default `IHSG`; `IHSG` or `LQ45` | Index |
| `?cmd=search&q=bank` | `q` required, max 50 chars | array of SearchResult |

Parameter rules:

- `symbol`: 1 to 10 characters, A-Z and 0-9, case-insensitive. A trailing `.JK` is accepted and removed.
- Add `&fresh=1` to skip the cache and fetch live from Yahoo. Live `gainers`/`losers` take several seconds, so avoid it unless you need it.

HTTP status codes:

| Code | Meaning | What to do |
|---|---|---|
| 200 | Success | Read `data` |
| 400 | Bad input (bad symbol, unknown cmd, empty search) | Fix the request. Retrying unchanged will not help |
| 403 | Only for `cmd=cache` (no or wrong token) | Not relevant when reading data |
| 502 | Yahoo unreachable or no data for that symbol or index | Safe to retry later. If it persists, the symbol may not exist |

`source` and freshness: `"source": "cache"` means the answer came from the cache files and `generatedAt` is the build time. `"source": "live"` means it was fetched just now. The API serves cache data only while it is at most 15 minutes old, then falls back to live.

---

## 5. Freshness and market hours

- IDX trades Monday to Friday, roughly 09:00 to 16:00 WIB (UTC+7), with a midday break. It is closed on weekends and Indonesian market holidays.
- The cache is rebuilt by a cron job during market hours. Outside those hours the files keep the **last session's data**, so an old `generatedAt` after the close or on a weekend is normal, not an error.
- Yahoo data may be delayed. Do not treat prices as real-time and do not present them as trading advice.
- To judge staleness: `age = now_utc - generatedAt`. During market hours, an age over about 15 minutes suggests the cron job has stopped. Mention the timestamp to the user.
- A per-symbol file such as `quotes/XXXX.json` can keep an old `generatedAt` if that symbol failed in later builds. Check its own `generatedAt`.

---

## 6. Parsing examples

### JavaScript

```js
const BASE = "https://idx.xpp.wtf/cache";

async function getJson(path) {
  const res = await fetch(`${BASE}/${path}`);
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${path}`);
  const body = await res.json();
  if (!body.ok) throw new Error(body.error ?? "unknown error");
  return body; // { ok, generatedAt, data }
}

const { data: quotes, generatedAt } = await getJson("quotes.json");
const bbca = quotes["BBCA"];                    // may be undefined
if (bbca) {
  console.log(bbca.symbol, bbca.price, `${bbca.changePercent.toFixed(2)}%`);
}

const { data: gainers } = await getJson("gainers.json");
const top5 = gainers.slice(0, 5);

const { data: indices } = await getJson("indices.json");
const ihsg = indices.IHSG ?? null;              // check before use
const ageMinutes = (Date.now() - Date.parse(generatedAt)) / 60000;
```

### Python

```python
import requests
from datetime import datetime, timezone

BASE = "https://idx.xpp.wtf/cache"

def get_json(path):
    r = requests.get(f"{BASE}/{path}", timeout=10)
    r.raise_for_status()
    body = r.json()
    if not body.get("ok"):
        raise RuntimeError(body.get("error", "unknown error"))
    return body  # {"ok", "generatedAt", "data"}

body = get_json("quotes.json")
quotes = body["data"]
bbca = quotes.get("BBCA")            # None if missing
if bbca:
    print(bbca["symbol"], bbca["price"], f'{bbca["changePercent"]:.2f}%')

top5 = get_json("gainers.json")["data"][:5]
ihsg = get_json("indices.json")["data"].get("IHSG")

generated = datetime.fromisoformat(body["generatedAt"])
age_min = (datetime.now(timezone.utc) - generated).total_seconds() / 60
```

---

## 7. Checklist for agents

1. Fetch from `/cache/` files first. Use the query API only for search or a one-off lookup.
2. Always check `ok` before reading `data`.
3. Treat `changePercent` as percent units, and prices as IDR.
4. Use `.get()` or optional chaining for tickers, index names, and `marketCap`; any of them may be missing.
5. Read `generatedAt` (UTC) and tell the user how fresh the data is, converting to WIB when displaying.
6. Round numbers for display; do not print raw floating-point tails.
7. Do not poll faster than about once per minute. The data only changes when the cron job rebuilds the cache.
8. Do not call `?cmd=cache`.
9. Remind users that data comes from Yahoo Finance, may be delayed, and is informational, not financial advice.

---

## 8. Known limitations

- Only about 47 liquid tickers are tracked. Other IDX stocks are available through `?cmd=quote&symbol=XXXX` (live) but not in the cache files, unless the cache owner adds them.
- IDX30 is currently unavailable (Yahoo returns no data for it). Only IHSG and LQ45 are supported.
- Gainers and losers are computed over the tracked list, not the entire exchange.
- Volume is in shares, and values are not adjusted for stock splits or dividends.
