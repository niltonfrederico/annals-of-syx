# Game mechanics — player-facing reference

Notes on how *Songs of Syx* (v0.70+) actually plays, written for the
person staring at a `.save` JSON and trying to figure out *what* a given
field represents and *why* it matters. This is not engine internals
(see [`../engine-notes.md`](../engine-notes.md) for that). This is the
game from the player's chair, distilled down to what informs dump
decisions: which keys are stable, which stats drive the simulation,
which deltas are worth surfacing.

Sourced from a v0.70+ deep research pass (Gemini), cross-checked
against the [official wiki](https://songsofsyx.com/wiki) and the
[fandom wiki](https://songsofsyx.fandom.com/wiki/). When the two
disagree, the official wiki and direct engine class names (visible in
`_songofsyx/.game-sources`) win.

## Index

- [Races](races.md) — 8 inhabitable species, food/climate preferences,
  combat roles, slave dynamics, breeding rules. Read this first; almost
  every other stat is conditioned on race.
- [City & economy](city-economy.md) — workforce, fulfillment, food
  chain, services, housing tiers, sanitation, immigration vs. breeding.
- [Military & battle](military-battle.md) — armies, divisions,
  weapons/armor matrix, supply depots, morale/exhaustion, sieges.
- [World & diplomacy](world-diplomacy.md) — world map, factions,
  opinion/rivalry, treaties, trade routes, vassalage, raiders.

## Version pin

All notes target **v0.70.33** (same pin as
[`../engine-notes.md`](../engine-notes.md)). v0.70 is the "Riders of
Doom" military overhaul; pre-v0.70 notes about cavalry, spearmen,
formation defense, breachable walls do **not** apply.

## Race name disambiguation

The eight playable/inhabitable races, by their canonical wiki names
(and the keys you will see in `race.key` in dumps):

| Wiki name  | `race.key` | Archetype                       |
|------------|------------|---------------------------------|
| Human      | `HUMAN`    | Adaptable jack-of-all-trades    |
| Cretonian  | `CRETONIA` | Pig-folk pacifist farmers       |
| Dondorian  | `DONDORI`  | Dwarven industry/heavy infantry |
| Tilapi     | `TILAPI`   | Sylvan elves, archers           |
| Garthimi   | `GARTHIMI` | Insectoid swarm                 |
| Amevia     | `AMEVIA`   | Aquatic reptilian / lizardmen   |
| Cantor     | `CANTOR`   | Giant, near-immortal, Athurist  |
| Argonosh   | `ARGONO`   | Ancient predator demon-bugs     |

Two recurring confusions worth flagging up front:

- **Cretonians are pig-folk** (suid), not lizards. Lizardmen = Amevia.
- **Tilapis are forest elves**, not fish-folk. Fish/aquatic = Amevia.
- **"Drep" is not a race.** The word appears in Norwegian forum
  threads (*drep* = "kill") and in *Dream Engines: Nomad Cities*. If
  you see it referenced as a Syx race, the source is wrong.

The exact key strings above are the *expected* engine keys based on
naming conventions in `_songofsyx/.game-sources/src/init/race/`. The
canonical truth is whatever `race.key` your dumper sees from
[`StatsDumper`](../../src/main/java/dumper/StatsDumper.java) at
runtime — trust that over this table.
