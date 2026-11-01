# URL Shortener + Analytics

A URL shortener built to demonstrate the design lesson that matters more than the feature itself:
**separating a hot, latency-critical read path from a high-volume, latency-tolerant write path.**

A redirect is trivial. A redirect that stays fast *while* every click is being recorded for analytics —
even when traffic spikes — is where the real engineering lives. This project is built around that idea.

**Stack:** Java 17 · Spring Boot 3 · PostgreSQL · Redis

---

## Architecture

```
                    ┌──────────────────────────────────────────────┐
  POST /api/shorten │  UrlController → UrlService                   │
  (create, rare) ──►│                     │ code_seq → Base62 code   │──► PostgreSQL
                    │                     │                          │    url_mapping (source of truth)
                    │                                                │
  GET /{code} ─────►│  UrlController → UrlService.resolve()          │
  (redirect, HOT)   │                     │                          │
        ▲           │                     ▼                          │
        │  302      │              Redis cache  ◄── hit ~99% ────────┼──► Redis (code → URL)
        └───────────│                     │ miss                     │
                    │                     └─► PostgreSQL (backfill)   │
                    │                                                │
                    │  ── on each redirect, publish a click event ── │
                    │        │ (fire-and-forget, off the hot path)   │
                    │        ▼                                       │
                    │  in-memory queue → @Async ClickAnalyticsListener│──► PostgreSQL
                    │                                                │    click_event (analytics)
  GET /api/stats/{code} ──► StatsController ──────────────────────────┘
```

The redirect (read path) and the click recording (write path) share only the moment "a click happened,"
then split. The redirect returns immediately; the analytics write happens on a separate thread.

---

## Design decisions

### 1. Base62 short codes, decoupled from the primary key
Codes use the alphabet `[0-9A-Za-z]` (62 symbols). A 7-char code addresses `62^7 ≈ 3.5 trillion` URLs.

Codes are generated from a **dedicated database sequence** (`code_seq`), *not* from the row's primary key.
Two reasons:
- **Correctness/simplicity:** the code number is drawn *before* the insert, so the complete row
  (including a non-null `short_code`) is written in a **single INSERT** — no null-code window, no
  redundant UPDATE. (Deriving the code from the PK forces an insert-then-update, because the PK only
  exists after insert.)
- **Design:** the public code should not leak the internal row id / total row count, nor couple the
  public API to a storage detail.

A configurable offset (`app.id-offset`) is added before encoding so codes are not `0, 1, 2, …` and
therefore not trivially enumerable.

> **Collision note:** because we encode a monotonic sequence value, collisions are impossible by
> construction — no "generate random, check, retry" loop. The random approach is only preferable when
> you must not reveal how many URLs exist; it trades collision-freedom for unpredictability (and runs
> into the birthday paradox as the keyspace fills).

### 2. Cache-aside reads (Redis)
`resolve(code)` checks Redis first; on a miss it reads Postgres and backfills the cache. URL mappings
are read-heavy and rarely change — the ideal cache profile — so once warm, almost every redirect is
served from memory. Unknown codes are **negatively cached** briefly so a flood of junk codes can't
hammer Postgres.

### 3. 302, not 301
Redirects use `302 Found`, not `301 Moved Permanently`. A 301 is cached by browsers, which would skip
our server on repeat clicks and hide analytics. 302 keeps every click flowing through us. The cost —
more redirect traffic — is exactly why the read path is cached and the write path is async.

### 4. Async analytics (the core lesson)
On each redirect we publish a `ClickRecordedEvent` and return immediately. A separate `@Async` listener
drains the events and writes `click_event` rows on its own thread pool. The redirect's latency is
therefore **independent of analytics speed** — if the analytics DB is slow or bursting, redirects stay
fast; clicks buffer and the consumer catches up.

This is currently an **in-process** queue (a bounded thread-pool queue). The natural next step is a
durable queue (Kafka), which survives restarts and scales across processes — swapping it in only
touches the listener, not the redirect path.

---

## Data model

`url_mapping` (source of truth): `id` PK, `short_code` (unique), `long_url`, `created_at`.
`click_event` (append-only analytics): `id` PK, `short_code` (indexed), `clicked_at`, `ip_address`,
`user_agent`, `referrer`.

Two tables because the access patterns are opposite: `url_mapping` is tiny and cache-served;
`click_event` grows forever and is write-only (a natural fit for a columnar/time-series store later).

---

## API

| Method & path | Purpose | Request | Response |
|---|---|---|---|
| `POST /api/shorten` | create a link | `{ "url": "...", "customAlias"?: "..." }` | `201 { shortCode, shortUrl, longUrl }` |
| `GET /{code}` | redirect | — | `302` + `Location` (or `404`) |
| `GET /api/stats/{code}` | analytics | — | `200 { shortCode, totalClicks, topReferrers[] }` |

The redirect lives at the root (`/{code}`) to keep short URLs short; API endpoints are under `/api/`
so they never collide with codes.

---

## Running locally

Requires a running PostgreSQL and Redis (defaults: `localhost:5432` db/user/pass `shortener`, and
`localhost:6379`). See `application.yml` to adjust. The `code_seq` sequence is created automatically
on startup.

```bash
mvn spring-boot:run
```

Quick smoke test:
```bash
# create
curl -X POST http://localhost:8085/api/shorten \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com"}'

# redirect (use the returned code)
curl -i http://localhost:8085/<code>

# stats
curl http://localhost:8085/api/stats/<code>
```

---

## Roadmap

- [ ] Replace the in-process queue with **Kafka** (durable, multi-consumer) — the production form of the write path.
- [ ] **Load test** the redirect endpoint (k6/Gatling); record req/s and p50/p95/p99 with cache warm vs cold.
- [ ] Time-bucketed analytics (clicks over time) + pre-aggregation for heavy stats.
- [ ] Rate limiting, link expiry, and a deployment (Railway/Render/Fly).
