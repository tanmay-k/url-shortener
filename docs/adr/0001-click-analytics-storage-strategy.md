# ADR 0001: Click Analytics Storage Strategy

**Status:** Accepted
**Related roadmap phase:** [Phase 2 — Click Analytics / Audit Log](../ROADMAP.md#phase-2--click-analytics--audit-log)

## Context
Every redirect needs to be recorded as a click event for analytics/audit purposes.
Common solutions for this kind of event-capture problem include streaming platforms
(e.g. Kafka), managed queue services, or simpler in-database approaches.

## Options Considered
1. **Kafka (or similar streaming platform)** — industry-standard for high-throughput
   event capture with strong durability and consumer-group semantics.
2. **Managed queue service** — offloads operational overhead but adds an external
   dependency and cost.
3. **Database table used as a queue** — a table holding unprocessed click events,
   drained periodically by a worker process.

## Decision
Use option 3: a database table as a lightweight queue, drained by a scheduled
worker.

## Rationale
- Current and projected traffic does not approach the scale where Kafka's
  throughput and durability guarantees are needed — introducing it now would add
  operational complexity (a broker to run, partition/consumer-group design) without
  a corresponding benefit.
- A DB-table queue keeps the project on infrastructure it already has, which is
  appropriate for a project at this stage and traffic level.
- The approach is well understood and easy to reason about, monitor, and debug —
  useful properties while the analytics feature is still new.

## Consequences
- **Trade-off accepted:** a DB-table queue does not scale the way Kafka would;
  write contention and table growth become real concerns well before Kafka's limits
  would be an issue.
- A retention/archival policy is required for the queue table (raw event rows should
  be pruned or archived after aggregation) to avoid unbounded growth — tracked as an
  open decision in the roadmap.
- If click volume grows materially (e.g. orders of magnitude beyond current
  projections), this decision should be revisited — the migration path from
  "DB table queue" to a real streaming platform is a known, deferred cost, taken on
  deliberately rather than by accident.

## Revisit Trigger
Re-evaluate this decision if: (a) click volume approaches a level where the worker
cannot drain the table faster than it fills, or (b) multiple consumers/analytics
pipelines need to read the same event stream independently.
