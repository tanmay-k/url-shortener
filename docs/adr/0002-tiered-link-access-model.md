# ADR 0002: Tiered Link Access Model (Temporary vs. Permanent)

**Status:** Proposed
**Related roadmap phase:** [Phase 3 — Temporary vs. Permanent Links](../ROADMAP.md#phase-3--temporary-vs-permanent-links-tiered-access)

## Context
The project wants to introduce a distinction between temporary and permanent short
links, and use that distinction as the basis for a free/paid usage model — without
building actual payment/billing infrastructure at this stage.

## Options Considered
1. **Full payment gateway integration** — real billing tied to link type.
2. **No tiering** — all links behave identically, no free/paid concept.
3. **Tier as a data flag** — a `plan`/`tier` field on the user or link record
   (e.g. `FREE`, `PAID`) that gates link type and usage limits, set manually or via
   a simple mechanism (invite code, admin flag) rather than a payment flow.

## Decision
Use option 3: represent the tier as a flag on the user (or link) record, with no
payment gateway behind it for now.

## Rationale
- The learning objective here is the access-control and limit-enforcement logic
  (how tier gates behavior), not payment processing — building a full billing
  integration would be significant scope for no proportional learning value at this
  project's stage.
- A flag-based approach still forces the real design questions a TPM/engineer needs
  to answer: how limits are enforced, what happens when a temporary link expires,
  and how the system behaves at the free/paid boundary — without the distraction of
  payment provider integration, webhooks, and reconciliation.
- Keeps the door open to plugging in a real payment gateway later without
  re-architecting the tiering logic itself — only the mechanism that *sets* the flag
  would change.

## Consequences
- The "paid" state is trust-based (manually or trivially settable) until a real
  payment flow exists — this is acceptable for a learning project but would be a
  real product/security gap if this were ever exposed publicly with real users.
- Temporary links need an explicit expiry/cleanup mechanism (TTL field + a cleanup
  job or lazy expiry check on access) — this is new scope introduced by this
  decision.
- Custom short codes (Phase 4) and batch link creation (Phase 5) must respect these same tier limits, so this decision
  directly constrains how that phase is designed.

## Explicitly Out of Scope
Payment gateway integration, invoicing, subscription management, and refunds are
not addressed by this ADR and are not currently planned.
