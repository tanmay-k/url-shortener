# URL Shortener — Product Requirements Document

## 1. Problem Statement
Long URLs are hard to share, track, and manage. This project provides a self-hosted
URL shortening service, built as a hands-on learning project to practice both backend
engineering (Spring Boot) and structured product/program thinking (scoping, phased
delivery, trade-off documentation).

## 2. Goals
- Provide a reliable way to convert long URLs into short, shareable links.
- Support distinct link types (temporary vs. permanent) to enable a tiered access model.
- Capture click-level analytics for each short link without introducing infrastructure
  complexity disproportionate to the project's scale.
- Let users personalise links (custom short codes, QR codes).
- Optionally support batch link creation (CSV import) for programmatic use cases.
- Demonstrate a realistic, phased delivery plan — not just a finished feature set.

## 3. Non-Goals (Out of Scope for now)
- Payment processing / billing — "paid plan" is a usage-tier concept only; no payment
  gateway integration is planned in the near term.
- Multi-region / high-availability deployment.
- Kafka or other streaming infrastructure for analytics (see ADR 0001).

## 4. Target Users / Personas
- **Individual user** — creates a handful of short links, wants basic click counts.
- **Power user / small team** — wants custom codes, QR codes, longer link retention and (stretch) batch creation.
- **Future paid user (conceptual)** — gets permanent links and higher usage limits,
  ahead of any actual billing implementation.

## 5. Success Metrics
| Metric | Target (directional) |
|---|---|
| Redirect latency (p95) | < 100 ms |
| Short code collision rate | ~0% (collision-checked on generation) |
| Click events captured vs. click events served | 100% (no analytics data loss under normal load) |
| Batch creation throughput (stretch) | Support batches in the thousands per job without timing out |

## 6. Current Scope (v1 — as of this doc)
- Core short-link creation and redirect service (Spring Boot).
- Basic persistence layer for link storage.

## 7. Planned Scope (v2+)
See [`ROADMAP.md`](./ROADMAP.md) for phased breakdown. At a high level:
1. Account/auth API completion (password change, user deactivation, refresh token
   handling) — largely implemented.
2. Click analytics / audit log per link.
3. Temporary vs. permanent links, mapped to a free/paid usage model (no payment
   gateway).
4. Link customisation: custom short codes and a QR code API.
5. Batch link creation via CSV import (a few thousand rows per job) — stretch goal.

## 8. Open Questions
Each question is tied to the roadmap phase it blocks.

| Phase | Question | Options and trade-offs |
|---|---|---|
| 2 | How long do we keep raw click events after they are aggregated? | Delete after N days (cheapest), archive to a cold table (keeps history, grows storage), or keep forever (simple, but the queue table grows without bound). Needed before Phase 2 ships. |
| 2 | What should the redirect do if recording a click fails? | Fail open (redirect anyway, accept some lost clicks) or fail closed (guarantee capture, but analytics can break redirects). The 100% capture target conflicts with the p95 latency target. |
| 3 | How is a user marked "paid" without a payment gateway? | Admin-set flag (simple, needs an admin path), invite or coupon code (self-service, can be shared), or a config allowlist (no schema change, not scalable). |
| 3 | What happens to a temporary link at expiry? | Return 404 or 410, soft-delete and keep for analytics, or hard-delete. This also decides whether the short code can ever be reused. |
| 4 | Which short codes are reserved? | A denylist of route-like codes (`api`, `login`, `swagger-ui`) and a minimum length, so custom codes can't collide with application routes. |
| 5 | Is batch creation synchronous or an async job? | Synchronous is simple but times out on large batches. An async job with a status endpoint scales better but adds job state and polling. A design note is needed if the phase starts. |
