# World map & diplomacy

How the macro game runs around the capital: regions, factions, opinion/
rivalry, treaties, trade, vassalage, raiders. v0.70 tightened the AI
behavior model — Net Worth now directly drives rival aggression.

## World map structure

- **Regions / provinces** — discrete tiles with a name, lore, and a
  computed **Worth** score (function of minerals + fertility + area).
  AI factions use Worth to decide which regions to covet.
- **Capitals** — every faction has one. Player's capital is the city
  map you actually build.
- **Free Lands vs. Empires** — regions spawn either independent
  (single-region polity) or as members of a multi-region AI empire.
- **Connections** — arrows between regions. Diplomacy and automated
  trade **require** a connection. Two regions sharing a border but
  without an arrow can't trade or surprise-declare war on each other.
- **Terrain** — brown = farming/animal yield, green = production/
  knowledge bonuses, mountains = ore. Rivers/oceans accelerate travel
  and cut ration consumption.
- **Entry points** — v0.70 synced world map to local map. Roads
  connecting your capital to the macro map decide where immigrants,
  merchants, and standard travelers spawn in the city. Sealing them
  off has a happiness penalty. Military invasions ignore entry points
  — you cannot wall yourself in.

## Foreign factions

Generated with specific racial demographics. Each faction has a visible
**Net Worth**:

```
Net Worth = Σ (Population × Upbringing_Time × Cheapest_Food_Price × 2)
```

Slave population is included in the calculation. Net Worth governs AI
behavior, rival modifier, diplomatic weight. v0.70+ AI aggression
scales tightly with player Net Worth — getting rich attracts wars
mechanically, not just narratively.

Historic AI advantage (city pop caps bypassing player constraints)
has been mostly aligned in v0.70 patches, but AI standing armies
still routinely hit 18k+ soldiers and converge on the player as the
empire scales.

## Diplomatic stances

| Stance       | Meaning |
|--------------|---------|
| Neutral / Unknown | Default. No trade, closed borders. |
| Colleague    | Non-aggression pact. Enables basic trade. Trespassing still incurs penalty. |
| Alliance     | Military pact — shared enemies, mutual defense expectation. Expensive to buy; cheaper if you intervene in their existing war. |
| Vassalage    | They're subordinate to you. They pay tribute (~15% production yearly) and get protected. |
| Protector    | You're subordinate to an AI overlord. Pay tribute to survive overwhelming odds. |
| War          | Active hostility. Treaties must be broken first. |

Every treaty has a hidden **Minimum Opinion** threshold. Falling below
it = AI unilaterally cancels the treaty.

The diplomatic "Unite" action (instant peaceful annexation of a free
city or minor faction) costs tens of millions of Denari — usually
cheaper to conquer.

## Opinion vs. Rivalry

Two axes the AI judges you on:

- **Opinion** — how much they like you. Built by flattery, gifts,
  shared enemies; eroded by trespassing, demands ignored, broken
  treaties.
- **Rivalry** (formerly "Threat") — how geopolitically threatening
  you are. Tied directly to Net Worth — getting rich automatically
  raises Rivalry on every neighbor. Cannot be reduced except by being
  smaller.

Specific modifiers worth knowing:

- **Trespassing** — moving troops through non-allied territory
  generates massive rapid negative opinion. Ignore it and a 50-year
  treaty cancels and they declare war.
- **Poisoning the well** — distant factions that hate you but can't
  reach you spend their influence dragging your neighbors' opinion
  down, sparking proxy wars.
- **"Give me half your kingdom"** demands — high-Rivalry AIs issue
  extortion ultimatums (large resources / slaves / territory).
  Refusing = opinion crash + likely war.

## Embassies & emissaries

The **Courts Tab** is the active diplomacy layer. Emissaries dispatched
to foreign capitals do one of:

1. **Flatter** — slow opinion gain (~+0.01/point). Often loses the
   race against accumulating Rivalry.
2. **Sabotage** — damage enemy infrastructure or their relations with
   other factions.
3. **Assassinate** — target foreign nobles, disrupting their
   leadership/admin pipeline.
4. **Build regional support** — prep work before a planned conquest.

**Gifts** are the fast lever. A high-value good given to a poor
faction is worth far more than the same good given to a rich one —
relative value drives the opinion bump. Stabilizing a fragile relation
with one well-timed gift is often cheaper than weeks of flattery.

## Trade

Local infrastructure handles macro trade:

- **Export Depot** — staffed; pulls excess goods from warehouses
  beyond a threshold (e.g. 60% capacity), routes them to passing
  merchants at world-market price.
- **Import Depot** — configured per-good with an import threshold
  (e.g. 30% of allocated warehouse space). When stock dips below + you
  have Denari, traders physically path across the world map to
  deliver.
- **Logistical spoilage** — goods degrade in transit. Hauler
  efficiency + cart usage + distance minimization = profit margin.

Caravans physically traverse the map; if your storage is full on
arrival, **goods are forfeit despite the Denari being withdrawn**.

Trade scope is limited to faction reachability — connected by road
network. Expansion = conquer intermediate territory or sign Colleague
treaties to bridge the gap.

**Vassal trade tax**: 13.3% (significantly favorable). Vassals are the
canonical late-game macro — cheap raw materials in, expensive finished
goods out.

## Vassalage

Two paths in:

1. **Diplomatic** — "Unite" (massive Denari cost) on a free city /
   minor faction.
2. **Conquest** — beat the faction, liberate the city, force
   puppet-state.

Mechanics:

- **Tribute**: ~15% yearly production, format set by overlord.
- **Trade tax**: 13.3% (overlord-side advantage).
- **Rivalry**: vassals accumulate Rivalry as the player grows. They
  issue secession demands; refusing → war of independence.
- **Protection obligation**: overlord penalty for failing to defend
  vassals.

Stabilization tactics: gift a conquered hostile city to an existing
vassal (huge opinion bump), or flood them with gifted slaves/luxury
goods.

## Administration & regional upgrades

Capital generates **Admin Points** via Administration buildings
(clerks need shelf space + carpets). Admin Points are the macro
currency for:

- Regional infrastructure upgrades (capacity, production, growth).
- Sanitation upgrades (health, growth).
- Law (loyalty up, production/growth down).
- Entertainment (loyalty up, production down).
- Education (knowledge from `region_pop × avg_intelligence × 0.01`
  per level).

**Diminishing returns are exponential per region** — better to spread
~5 ticks across many regions than hyper-stack one. Most over-builders
get burned here.

**Regional taxation**: you can tax for raw resources delivered to
Import Depots. Heavy taxation drops loyalty. The "burn it for cash"
button: over-tax for a 2-year resource burst, then the region is
exhausted for 4× that duration.

**Faction policies** per region: Elevation (growth/loyalty boost on
target species), Prosecution (debuff), Exile (remove species; loyalty
hit), Massacre (genocide).

## Asymmetric threats

### Raiders / bandits

Outside the standard diplomacy graph. Threat probability scales with
**visible wealth** (opium / gem industries / treasury size) minus
**deterrence** (garrison size, weapon quality). Poor villages get
ignored; profitable mercantile hubs invite them.

Pre-raid ultimatum: pay ransom (scales with pop) and they leave. Refuse
and the army spawns on the world map. Critical asymmetry: raiders
**cross any AI territory without penalty**, ignore wall-sealing, can
spawn dynamically inside the city, and head for cash.

### World events

- **Droughts** — crop yield collapse. Reserve food via Import Depots
  or pray at shrines to offset modifier.
- **Plagues** — workforce evaporates. Hospitals + physicians +
  immediate response.
- **Resource depletion** — v0.70 normalized city ore deposits to 100%
  efficiency but you can still outpace local supply. Trade fills the
  gap.

## Religion (macro layer)

Tracked per-city, granular GUI per religion. Citizens gain fulfillment
via temples/shrines/speakers for their god (race-conditional bias —
see [races.md](races.md)).

Conversion risk: under-served religions starve out, citizens
spontaneously convert to rival faiths. Different religions harbor
mutual hostility → sectarian riots + loyalty crash if uncontrolled.

Funerary infrastructure (Graveyards, Crypts, Mass Graves) is also
religion-coded. **Desecration** (removing a graveyard incorrectly)
inflicts a massive populace penalty. Safe removal = deactivate,
empty fully, then delete.

## What to surface in dumps

Existing dump is city-local. Macro additions worth considering:

- Faction roster with Net Worth, Opinion, Rivalry, current treaty
  status — the geopolitical board snapshot.
- Per-region: ownership, loyalty, admin upgrades active, taxation
  pressure, % of demographic by race.
- Trade ledger: import/export per good per partner, vassal share.
- Active emissary actions in flight.
- Pending ultimatums / demands (existential risk indicators).
- Raider threat metric (visible wealth ÷ deterrence) — predicts
  incoming raids before they're declared.
