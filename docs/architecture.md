# Architecture

annals-of-syx is a **batch JSON dumper** for *Songs of Syx* save files. It is
not a parser — it bootstraps the actual game engine headlessly, lets it load
the save into its own singletons, then walks those singletons and writes
their state to JSON.

## Pipeline

```
CLI args (scripts/syx)
   │
   ▼
dumper.cli.Syx          ←── picocli entrypoint, setup wizard, config
   │  parse args, run setup if needed
   ▼
dumper.cli.DumpRunner   ←── validate save path, delegate to engine
   │
   ▼
dumper.Bootstrap        ←── runs under xvfb-run; opens a real GLFW context
   │  initEngine()      ←── 6-step ordered init (see engine-notes.md)
   │  loadSave(path)    ←── drives GameLoader programmatically
   ▼
GameSpec (in-memory)    ←── all game singletons now populated
   │
   ▼
dumper.StatsDumper.dumpAll(spec, save, coverage)
   │  one method per section, each reads its own singletons
   ▼
LinkedHashMap<String, Object>
   │
   ▼
dumper.Json.write(map, out)
   │  Gson pretty-print
   ▼
<save>.json
```

## Source layout

```
src/main/java/dumper/
  cli/
    Syx.java         — picocli entrypoint, setup wizard, config management
    DumpRunner.java  — validates save path, drives Bootstrap + StatsDumper
  Bootstrap.java     — engine init + save-load orchestration (LOAD-BEARING)
  BootstrapProbe.java — diagnostic: validates init order without dumping
  SaveReader.java    — minimal FileGetter wrapper that produces GameSpec
  StatsDumper.java   — per-section dump methods, walks singletons
  Json.java          — Gson writer

src/main/java/settlement/stats/
  StatsLoader.java   — bridge into protected STATS.load (same-package shim)
```

## Section pattern in StatsDumper

Each dump section follows the same shape:

- one static method per section, named `<thing>By<axis>` or `<thing>Snapshot`
- returns a `Map<String, ...>` keyed by **stable game keys** (race.key,
  hclass.key, resource.key) — never `name.toString()` which is localized
- short comment at the top explaining **what** the section captures and
  **where in the engine it comes from**

This repetition is **deliberate**. The codebase resists "smart" helpers
that abstract over sections — each section is a small grep-able contract.
Adding a new section means adding a new method, not extending a framework.

## Why the engine has to be live

The save file is a binary dump of Java object state, written by the game's
own serializer. The format is not documented, changes between game versions,
and depends on construction order of dozens of singletons. Re-implementing
a parser would be a moving target.

Instead, we use the game's own bytecode to read its own save. The cost is
a heavy bootstrap (full LWJGL window, OpenAL audio context, sprite composer,
font loader) under `xvfb-run`. The benefit is correctness by construction.

See [`docs/archived/bootstrap-abort-races-needs-ui-icons.md`](archived/bootstrap-abort-races-needs-ui-icons.md)
for the abandoned "parse directly" plan and why it was abandoned.

## Stability contract

- **JSON keys** are stable identifiers from the game's own constants —
  they survive locale changes but **not** game upgrades. The dump is
  version-pinned to **Songs of Syx v0.70.33**.
- **Section names** at the top of the JSON are stable across patch
  versions of annals-of-syx (additive only; renames bump a major).
- **Engine init order** is fixed; see [engine-notes.md](engine-notes.md).
