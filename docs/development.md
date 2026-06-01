# Development

Setup, daily loop, and conventions for working on annals-of-syx.

## Prerequisites

- **JDK 17+** (target is 17; host JDK 26 confirmed working).
- **`xvfb-run`** — runtime requirement, engine bootstrap opens a real
  GLFW window.
  - Arch: `pacman -S xorg-server-xvfb`
  - Debian/Ubuntu: `apt install xvfb`
- **A legitimate copy of *Songs of Syx*** (Steam, GOG, …). See
  [`../DISCLAIMER.md`](../DISCLAIMER.md).

## Workspace layout

```
annals-of-syx/
├── src/main/java/dumper/      — main sources
├── src/test/java/dumper/      — integration tests (run via main(), not JUnit)
├── docs/                       — public-facing docs (this folder)
│   └── archived/               — historical audit-trail (gitignored)
├── scripts/                    — bash wrappers (build, run)
└── .source/                    — local workspace (gitignored)
    ├── game/                   — copy of SongsOfSyx.jar + base/ assets
    └── saves/                  — local copies of save files
```

`.source/` is **gitignored on purpose** — game assets are copyrighted and
save files are personal. Populate it locally; never commit the contents.

## Populating `.source/`

You need a copy of `SongsOfSyx.jar` and the game's `base/` directory
under `.source/game/`. Two paths:

- **Manual**: copy from your Steam/GOG install:
  ```
  cp /path/to/Songs\ of\ Syx/SongsOfSyx.jar .source/game/
  cp -r /path/to/Songs\ of\ Syx/base .source/game/
  ```
- **Helper** (TODO, see `docs/TODO.txt` item 3): a `GameLocator` utility
  that auto-detects canonical Steam/GOG paths across Linux/Windows/macOS.

Saves go under `.source/saves/`. Default discovery (TODO, item 4) will
prefer the most recent file.

## Build

```bash
./scripts/build.sh
```

Produces `build/libs/annals-of-syx-all.jar` (shadowJar fat jar).

The build wires `SongsOfSyx.jar` as a compile-time dependency. Resolution
order:
1. `SOS_GAME_JAR` env var, if set
2. `.source/game/SongsOfSyx.jar` (default)

## Run

```bash
./scripts/syx                        # interactive: guided setup + save picker
./scripts/syx --save <name>          # non-interactive: pick save by name
./scripts/syx -o /path/to/out.json   # explicit output path
```

`scripts/syx` handles classpath, `xvfb-run`, and the CWD switch into the
game directory (so `PATHS.init` finds `./base/`). The entrypoint is
`dumper.cli.Syx`; the engine call is delegated through `dumper.cli.DumpRunner`.

Runtime resolution for game dir:
1. `SOS_GAME_DIR` env var, if set
2. `.source/game/` (default)

## Tests

Tests are **integration-only**: they need a real engine, a real save, and
`xvfb`. They're driven by `main()`, not the JUnit `test` task (see header
of `PopulationTest.java` for why).

```bash
./scripts/run.sh dumper.PopulationTest <save-path>
```

`dumper.BootstrapProbe` is a separate diagnostic that validates the
engine init order without running the dumper. Use it after any change to
`Bootstrap.java`.

## Conventions

- **English everywhere in code**: identifiers, comments, log strings,
  commit messages. Portuguese is fine in personal docs (this folder
  follows the existing file's language).
- **Comments are why-heavy**: this codebase keeps long comments where
  they explain invariants of the game engine. Don't remove them, even
  if they look verbose.
- **No generic helpers in `StatsDumper`**: each section is a small,
  grep-able contract. See [`dump-schema.md`](dump-schema.md#adding-a-new-section).
- **Stable keys only** in JSON: `race.key`, `hclass.key`, `resource.key`.
  Never `name.toString()` (display-localized, unstable).
- **No hardcoded `$HOME` paths** in committed code — use env vars or
  `.source/`. The repo is public.

## Quality gates

Per-language guidance lives in `~/.claude/languages/` for the maintainer's
local setup; CI does not enforce these yet. Minimum bar before a PR:

- `./scripts/build.sh` succeeds.
- If you touched `Bootstrap.java`: `./scripts/run.sh dumper.BootstrapProbe <save>`
  succeeds.
- If you touched a dump section: `./scripts/run.sh dumper.PopulationTest <save>`
  succeeds and the resulting JSON validates against
  [`dump-schema.md`](dump-schema.md).
