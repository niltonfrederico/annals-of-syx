![Annals of Syx](docs/assets/header.svg)

Standalone Java CLI that reads a *Songs of Syx* `.save` and writes a JSON
sibling file with population, happiness, loyalty, religion, work
fulfillment, deaths, rooms, stockpile, law, disease, game counters,
treasury, resource flows, edicts, tech, and (opt-in) per-service coverage
stats.

Builds against, and loads, the game's own classes via a full headless
engine bootstrap under `xvfb-run`. **No re-implementation of the save
format** — the engine reads its own binary; we just pull from the
in-memory singletons it populates.

Tested against **Songs of Syx v0.70.33**.

> ⚖️ annals-of-syx is an **independent, unofficial tool**. It is not
> produced by or affiliated with the authors of *Songs of Syx*, and does
> not distribute any part of the game. See [`DISCLAIMER.md`](DISCLAIMER.md)
> and [buy the game](DISCLAIMER.md#buy-the-game) if you find this useful.

## Personal Disclaimer and Contributing

This exists because I'm a **numbers nerd** going through my "reverse-engineering phase". I'm no Java developer, and the code shows it — even with the help of my AI despots. If you want to pitch in (or just have ideas that would actually work), [contributions are welcome](https://github.com/niltonfrederico/annals-of-syx/issues). Check [ROADMAP.md](ROADMAP.md) for what's on the table, and the [Contributor Covenant](https://www.contributor-covenant.org/version/3/0/code_of_conduct/) for how we behave around here.

Hail to you, mighty despot!

## Setup

- **JDK 17+** (host JDK 26 confirmed working).
- **`xvfb-run`** (Arch: `pacman -S xorg-server-xvfb`, Debian/Ubuntu:
  `apt install xvfb`).
- **A copy of *Songs of Syx*** with its `SongsOfSyx.jar` and `base/`
  directory placed under `.source/game/` (gitignored). See
  [`docs/development.md`](docs/development.md#populating-source) for the
  full setup, including the planned `GameLocator` auto-discovery helper.

Quick env-var overrides:

- `SOS_GAME_JAR` — compile-time path to `SongsOfSyx.jar`
- `SOS_GAME_DIR` — runtime path to the game directory (must contain `base/`)
- `SOS_SAVE_DIR` — directory to scan for saves (used by `SaveLocator`)
- `SOS_XVFB_SCREEN` — Xvfb screen geometry (default `1920x1080x24`)
- `SOS_RUN_TIMEOUT` — wrap the java process in `timeout`

## Build

```bash
./scripts/build.sh
```

Produces `build/libs/annals-of-syx-all.jar` (shadowJar fat jar).

## Quick start: the `syx` command

```bash
./scripts/build.sh
./scripts/syx           # first run guides you through setup
```

First run:

1. Detects your Songs of Syx install (Steam / GOG paths probed per OS).
2. Validates the engine version (today: `0.70.33` only).
3. Mirrors the jar + symlinks `base/` into `~/.syx/source/`. Your game
   install is never written to.
4. Lists saves and asks which one to dump.
5. Writes the JSON to the directory you ran `syx` from.

Subsequent runs reuse the cached mirror (fast-path checksum) and the
default save. Force a re-pick with `--reselect`, force a re-copy with
`--resync`, start over with `--reconfigure`.

### Output directory

By default the dump lands in your current directory. To set a project-wide
default, edit `~/.syx/config.json`:

```json
{
  "defaultOutputDir": "/home/you/projects/syx-dumps"
}
```

Per-run override: `syx -o /tmp/foo.json`.

## Output schema

Full reference: [`docs/dump-schema.md`](docs/dump-schema.md).

Top-level keys (abbreviated):

```json
{
  "_meta":                { "save_path", "save_version", "played_ingame_seconds": <long>, "race", "city", "ruler", "pop_total" },
  "population":           { "<race>": { "<hclass>": <int> } },
  "happiness":            { "<race>": <double 0..1> },
  "loyalty":              { "<race>": <double 0..1> },
  "fulfillment":          { "<race>": <double 0..1> },
  "fulfillment_breakdown":{ "<race>": { "<stat>": { "name": "<str>", "current": <double>, "max": <double>, "dismiss": <bool>, "inverted": <bool> } } },
  "religion":             { "<religion>": { "<race>": <int> } },
  "work_fulfillment":     { "<race>": { "<hclass>": <double 0..1> } },
  "deaths_today":         { "<cause>": <int> },
  "deaths_history_8d":    { "<cause>": [<int>×8] },
  "rooms":                { "<bp>": { ... } },
  "stockpile":            { "<resource>": <int> },
  "law":                  { ... },
  "disease":              { "health_history_8d": [...] },
  "counters":             { "<gcount>": { ... } },
  "treasury":             { ... },
  "resource_flows":       { "PRODUCED"|"CONSUMED"|"TRADE": { "<resource>": { "in": [<int>×8], "out": [<int>×8], "net": [<int>×8] } } },
  "edicts":               { ... },
  "tech":                 { ... },
  "service_coverage":     { ... }
}
```

**Reading `fulfillment_breakdown[*]` correctly depends on `inverted`.**
The engine formula (`StatStanding.get`) is `d = clamp(input,0,1); if
inverted: d = 1-d; current = d * max`. Two distinct readings:

- `inverted: false` — coverage / provision stats (`HOME_FURNITURE`,
  `EQUIP_CIVIC_CLOTHES`, `FOOD_FOOD_RATIONS`, `WORK_RETIREMENT`, ...).
  `current = max` means fully provisioned; `current = 0` means none.
  `max - current` is the headroom.
- `inverted: true` — avoidance-of-bad stats (`BATTLE_BESIEGED`,
  `FOOD_STARVATION`, `BURIAL_DESECRATION`, `ENVIRONMENT_CANNIBALISM`,
  `POPULATION_WRONGFUL_DEATHS`, ...). `current = max` means the bad
  thing is **not** happening (full bonus from absence); `current = 0`
  is the actual crisis flag. **A maxed inverted stat is good news**, not
  a maxed penalty. The source of truth for which stats are inverted is
  `data.zip:data/assets/init/stats/loyalty/VANILLA.txt` in the game
  install.

**Time fields are not interchangeable.**
`_meta.played_ingame_seconds` is simulated game-time (accumulated
`TIME.secondsPerDay()` ticks). `counters.COUNT_TIME_PLAYED.current`
is real wall-clock seconds spent playing. Default game speed makes
ingame ≈ 60× wallclock, but the player can change speed or pause —
never assume a constant ratio. Convert to in-game years with
`played_ingame_seconds / (secondsPerHour * hoursPerDay * 16)` (engine
formula from `MenuScreenLoad`).

**Reading `resource_flows`:** for any RTYPE, `in` = gained on that
day, `out` = lost on that day (positive magnitude), `net = in - out`.
Per-RTYPE dominant side: `PRODUCED.in` = produced amount (out is
usually 0); `CONSUMED.out` = consumed amount (in is usually 0);
`TRADE.in` = bought, `TRADE.out` = sold (both active on the same
resource within a day). Race / hclass filters: only races with
settlement population > 0 appear in race-keyed blocks.

## Tests

Integration-only, run via `main()`:

```bash
./scripts/run.sh dumper.PopulationTest <save-path>
```

The integration test asserts `Σ population[race][player-class] == filename hex`
and smoke-checks all dumper methods. See
[`docs/development.md#tests`](docs/development.md#tests) for why this
isn't wired into the JUnit `test` task.

## Documentation

- [`DISCLAIMER.md`](DISCLAIMER.md) — IP notice for *Songs of Syx*
- [`docs/architecture.md`](docs/architecture.md) — pipeline overview
- [`docs/dump-schema.md`](docs/dump-schema.md) — full JSON schema reference
- [`docs/development.md`](docs/development.md) — daily dev loop, conventions
- [`docs/engine-notes.md`](docs/engine-notes.md) — gotchas, version pin,
  bootstrap invariants
- [`CHANGELOG.md`](CHANGELOG.md), [`ROADMAP.md`](ROADMAP.md)

## License

[AGPL-3.0](LICENSE). See also [`NOTICE`](NOTICE) and
[`DISCLAIMER.md`](DISCLAIMER.md).
