# Roadmap

Direction for annals-of-syx. Items move from **Ideas** → **Mid-term** →
**Next** as scope sharpens. Anything **Next** should land in `CHANGELOG.md`
under `[Unreleased]` when shipped.

## Next

- Detect engine version automatically (replace the hardcoded sha-256 allowlist).
- Support multiple engine versions, with v0.70.33 as the minimum.
- Windows support: replace the bash wrapper with a PowerShell entrypoint (or drop the wrapper entirely).
- Dump map information (terrain, regions, settlement positions).
- Split and refine the output JSON schema into focused sections.
- GitHub Actions workflow for tagged releases.

## Mid-term

- Graphical viewer for the dumped JSON.
- Clean up all compiler/runtime warnings.

## Ideas

- Evaluate reading the save in-place (directly from the install directory, possibly while the game is running) — risk vs. convenience.
- Human readable JSON - convenience vs. results.
