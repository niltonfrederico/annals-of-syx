# Engine notes

Gotchas, invariants, and load-bearing contracts when working against the
*Songs of Syx* engine via its own bytecode. Read before touching
`Bootstrap.java`, `StatsDumper.java`, or anything that calls into the
game's classes.

The historical audit trail that led to these notes lives in
[`archived/bootstrap-chain.md`](archived/bootstrap-chain.md) (gitignored,
local-only).

## Version pin

Everything below is verified against **Songs of Syx v0.70.33**. Any newer
version may shift class names, init order, or singleton lifecycles. If
you upgrade, run `dumper.BootstrapProbe <save>` first — failures there
isolate engine drift cleanly.

## Bootstrap init order (load-bearing)

`Bootstrap.initEngine()` must execute these steps in this order:

1. `PreLoader.load(...)` — game version banner + icon
2. `CORE.init(new ErrorHandler())` — install the global error handler
3. `PATHS.init(mods, lang, easy)` — resolves `./base/{data,locale,icons,script}`
4. `D.init()` — text/font subsystem
5. `CORE.create(S.get().make())` — opens GLFW window via LWJGL
6. `new GlJob() { ... new Initer() { new INIT(); }; }` — wraps `INIT`
   inside an `Initer` so `util.spritecomposer.Resources.c` is set before
   UI/font composition runs
7. `installInertUpdater()` — reflection into private API to prevent the
   game loop from advancing time during dump

**Do not** reorder. **Do not** extract these into smaller helper methods.
The order was pinned across 6 iterations against the real engine; the
audit trail is in `archived/bootstrap-chain.md` if you need the why.

The bootstrap **must** run under `xvfb-run` — step 5 opens a real
window. `scripts/run.sh` handles this.

## Key stability

All map keys in the JSON dump come from the game's own stable
identifiers:

- ✅ `race.key`, `hclass.key`, `resource.key`, `tech.key`, `religion.key`
- ❌ `name.toString()` — display-localized, unstable across locales,
  potentially null in stats with no localized name

If you find yourself reaching for `.name.toString()`, stop and find the
`.key` field. The one place this is currently unsafe is
`fulfillmentBreakdownByRace` — see `docs/TODO.txt` carryover from the
original code review.

## Reflection into private API

`Bootstrap.installInertUpdater()` uses reflection to plug a no-op updater
into the engine's private update loop. This is intentional: there is no
public API for "load a save without playing it." If a future game release
renames the private fields, the reflection will throw
`ClassNotFoundException` / `NoSuchFieldException` — that is the **correct
failure mode**. Don't silently fall back.

## Idempotency caveat

`Bootstrap.engineUp` is a simple boolean guard. If `initEngine()` throws
**partway through**, `engineUp` is never set, but global state (CORE,
PATHS, INIT) may be partially initialized. The current entrypoint is
one-shot (`Main` calls `Runtime.halt` on exit), so this hasn't been a
problem. If you ever embed `Bootstrap` in a long-running process, this
will bite — fix it before then.

## Engine singletons referenced by the dumper

```
game.GameSpec                  — entry point passed to all sections
settlement.stats.STATS         — population, deaths, religion, etc.
settlement.stats.STAT          — per-stat aggregates (FULFILLMENT, WORK_FULFILLMENT)
settlement.stats.standing.StandingCitizen  — happiness/loyalty aggregates
settlement.room.ROOMS          — building census
settlement.misc.util.HCLASSES  — class enum (NOBLE/CITIZEN/SLAVE/CHILD/OTHER)
game.faction.FACTIONS          — race lookup
init.race.RACES                — race definitions
```

When in doubt about a section's source, the table in
[`dump-schema.md`](dump-schema.md#per-section-sources) is canonical.

## When something breaks after a game upgrade

In order of likely failure:

1. **`Bootstrap.initEngine()` throws** → init order or class rename.
   Diff the new game jar against the v0.70.33 contract. Re-run
   `BootstrapProbe`.
2. **`installInertUpdater` reflection fails** → private field renamed.
   Find the new name in the new jar; update the reflection lookup.
3. **A `StatsDumper` section returns garbage or NPEs** → singleton
   structure shifted. Check the source table above; the field probably
   moved.
4. **JSON keys change** → you grabbed a `.name.toString()` somewhere it
   wasn't safe. Find the `.key`.
