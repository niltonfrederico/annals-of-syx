# Dump schema

Authoritative reference for the JSON written by `dumper.cli.Syx` /
`dumper.cli.DumpRunner`. For the pipeline that produces it, see
[`architecture.md`](architecture.md).

> **Version pin**: this schema reflects the dump as of **Songs of Syx
> v0.70.33**. Game upgrades may add/remove sections; check the `_meta`
> block first.

## Top-level shape

```json
{
  "_meta":                { ... },
  "population":           { "<race>": { "<hclass>": <int> } },
  "happiness":            { "<race>": <double 0..1> },
  "loyalty":              { "<race>": <double 0..1> },
  "fulfillment":          { "<race>": <double 0..1> },
  "expectation":          { "<race>": <double 0..1> },
  "loyalty_target":       { "<race>": <double 0..1> },
  "fulfillment_breakdown":{ "<race>": { "<stat>": { ... } } },
  "religion":             { "<religion>": { "<race>": <int> } },
  "work_fulfillment":     { "<race>": { "<hclass>": <double 0..1> } },
  "deaths":               { "<cause>": <int> },
  "rooms":                { "<bp>": { ... } },
  "stockpile":            { "<resource>": <int> },
  "law":                  { ... },
  "disease":              { "health_history_8d": [<double> x 8] },
  "counters":             { "<gcount>": { "current": <int>, "all_time_high": <int> } },
  "treasury":             { ... },
  "resource_flows":       { "PRODUCED|CONSUMED|TRADE": { "<res>": { "in": [...], "out": [...] } } },
  "edicts":               { "<stat>": { "<race>": <int> } },
  "tech":                 { "<tech>": { "level": <int>, "level_max": <int>, "tree": "<key>" } },
  "service_coverage":     { "<svc>": { "<race>": { ... } } }
}
```

## Keys

All map keys are **stable game identifiers** (`race.key`, `hclass.key`,
`resource.key`, …). They survive locale changes but are not guaranteed
stable across game versions.

## Per-section sources

| Section | StatsDumper method | Source singleton |
| --- | --- | --- |
| `_meta` | `dumpAll` (inline) | `GameSpec`, save filename |
| `population` | `populationByRaceAndClass` | `STATS.POP.POP` over `HCLASSES.ALL()` |
| `happiness` | `happinessByRace` | `StandingCitizen` aggregate |
| `loyalty` | `loyaltyByRace` | `StandingCitizen` aggregate |
| `fulfillment` | `fulfillmentByRace` | `StandingCitizen` aggregate |
| `expectation` | `expectationByRace` | `StandingCitizen` aggregate |
| `loyalty_target` | `loyaltyTargetByRace` | `StandingCitizen` aggregate |
| `fulfillment_breakdown` | `fulfillmentBreakdownByRace` | `STAT.FULFILLMENT` per-stat |
| `religion` | `religionFollowersByRace` | `STATS.RELIGION` |
| `work_fulfillment` | `workFulfillmentByRaceAndClass` | `STAT.WORK_FULFILLMENT` |
| `deaths` | `deathsByCause` | `STATS.DEATH` filtered to `c.death == true` |
| `rooms` | `roomsCensus` | `ROOMS.blueprints()` iteration |
| `stockpile` | `stockpileByResource` | `STATS.RESOURCE.STOCKPILE` |
| `law` | `lawSnapshot` | `STATS.LAW` + `STATS.CRIME` |
| `disease` | `diseaseSnapshot` | `STATS.DISEASE` history (8d) |
| `counters` | `gameCounters` | `STATS.GCOUNT` current + all-time-high |
| `treasury` | `treasury` | `STATS.CREDITS` snapshot + 8d history |
| `resource_flows` | `resourceFlows8d` | `STATS.RESOURCE` per-flow-class |
| `edicts` | `edicts` | `STATS.EDICTS` per race |
| `tech` | `techLevels` | `STATS.TECH` per tree |
| `service_coverage` | `serviceCoverage` | `STATS.SERVICE` (opt-in via `--coverage`) |

## Shape notes

- `population.<race>.<hclass>` sums **player classes** only
  (`HCLASSES.ALLP()`: `NOBLE`, `CITIZEN`, `SLAVE`, `CHILD`). The total
  matches the trailing hex in the save filename. The raw `ALL()` sum
  would include `OTHER` (visitors, beasts, foreign races).
- `happiness` and `loyalty` are `StandingCitizen` aggregates — there is
  **no HCLASS axis** for these. Empty races report the engine's default
  boostable (~0.91), not zero — cross-reference `population` to filter.
- `religion.<religion>.<race>` is total followers across all classes.
- `work_fulfillment` is the `STAT.WORK_FULFILLMENT` per-(class, race)
  mean in `[0..1]`.
- `deaths.<cause>` filters `CAUSE_LEAVES` to entries with `c.death == true`,
  excluding non-death exits (army, emigration, sold, insane).
- `disease.health_history_8d` is hardcoded to an 8-day window — this is
  the engine's minimum `HISTORY_INT` granularity.
- `resource_flows` buckets into `PRODUCED`, `CONSUMED`, `TRADE`; each
  resource has `in`/`out` lists (8 daily samples).

## Adding a new section

1. Add a static method to `StatsDumper` named `<thing>By<axis>` or
   `<thing>Snapshot`. Return `Map<String, ...>` with stable game keys.
2. Add a short comment at the top: **what** it captures, **which
   singleton** it pulls from.
3. Wire it into `dumpAll`. Pick a stable top-level key (snake_case).
4. Update this file (`dump-schema.md`) — add to the top-level shape AND
   to the source-table.
5. Update `README.md` if the top-level shape moved.

**Don't** introduce a generic helper that hides per-section structure —
each section is a small contract, grep-able by name.
