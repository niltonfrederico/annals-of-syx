# Military & battle

The v0.70 "Riders of Doom" update made warfare a first-class pillar of
empire management. Pre-v0.70 knowledge about combat (no cavalry, no
spearmen formation defense, indestructible walls) is stale — this doc
targets v0.70+ exclusively.

## Unit classes

| Class           | Role                                       | Introduced |
|-----------------|--------------------------------------------|------------|
| Light infantry  | Mobile melee                               | Pre-v0.70  |
| Heavy infantry  | Shield wall / anvil                        | Pre-v0.70  |
| Archers         | Ranged disruption                          | Pre-v0.70  |
| **Spearmen**    | Formation defense — frontal block bonus    | v0.70      |
| **Cavalry**     | Shock flank — collapses morale from rear  | v0.70      |

Divisions are 15–150 personnel, max 119 divisions. Racial composition
within a division is enforced homogeneous — mixed-race divisions are
not allowed.

## Racial combat profiles

| Race      | Base traits                          | Best role                | Trap |
|-----------|--------------------------------------|--------------------------|------|
| Human     | Average all stats                    | Combined-arms infantry   | No standout — equip well or get out-matched. |
| Dondorian | Slow, very high morale + resilience  | Heavy infantry, anvil    | Pursuit is hopeless; don't chase. |
| Garthimi  | Natural armor, +1.8 speed, **half base morale** | Shock / cannon fodder | Routs catastrophically if pressed without reinforcement. |
| Tilapi    | High accuracy + speed, low durability | Archer / skirmisher     | Melts in melee. Keep on flanks. |
| Argonosh  | Extremely fast + high damage         | Elite vanguard / pursuit | Refuses civilian labor — pure combat asset. |
| Amevia    | Natural scale armor, large           | Frontline shock          | Slow breeders → casualties hard to replace. |
| Cantor    | Gigantic, blunt-force monsters       | Single-unit army-breaker | Cannot be mass-recruited. |
| Cretonian | Cowardly, fragile                    | Militia / garrison only  | Will not hold against a real army. |

## Weapons matrix

| Weapon     | Damage     | Hits      | Notes |
|------------|------------|-----------|-------|
| Warhammer  | High Blunt | Heavy inf | Bypasses plate. **Disrupts enemy formations on impact** — key v0.70 mechanic. |
| Flanx      | High Cut   | Light inf | Two-handed. Shreds unarmored; no shield slot. |
| Falcata    | Cut/Pierce | Mixed     | One-handed → shield-compatible. Survivability multiplier. |
| Spear      | Pierce     | Cav/Charge | Massive frontal block + formation defense when on guard. v0.70 anvil core. |
| Bow        | Pierce-ranged | Light inf | ~40 shots per unit. Useless vs. shielded testudo. Devastating in early engagements. |

## Armor

| Tier    | Defense                       | Cost |
|---------|-------------------------------|------|
| Leather | Low Cut/Pierce                | Minimal mobility/stamina hit. Archer + cavalry default. |
| Plate   | High Cut/Pierce + charge res. | Heavy stamina drain — fatigues fast on the move. |
| Shield  | +300% Force block             | Stacks multiplicatively with plate. Parries save 1–4 HP per block. |

Helmets are folded into the armor calc. Hammer-and-anvil doctrine
(Dondorian shield wall pinning, Garthimi warhammers flanking) is the
canonical v0.70 winning template.

## Recruitment

Three avenues:

1. **Conscript militia** — train to 20–30%, return to civilian
   workforce when not deployed. Cheap on the economy, useless against
   pros.
2. **Professional standing army** — train to 100%, never re-enter
   civilian workforce. Cannot die from civilian ailments while
   deployed (crime, insanity, brawls). Only die from combat or old
   age, *if* supply holds.
3. **Mercenaries** — spawn fully-trained, fully-equipped. Bypass
   smithies/training entirely. Brutal upfront fee + daily upkeep:
   ~116k Denari hire + ~18k/day for 100 men. v0.70 added scaling caps,
   long disband cooldown, slow casualty replacement — they're no
   longer infinitely spammable.

Training mechanics:
- Citizens at Training Grounds level up via dummy practice. Level 1
  = deployable soldier.
- Skills decay: Level 1 retains for 5 in-game years unused; Level 15
  needs near-daily practice.
- Knowledge points buy global modifiers: +20% training speed, +17%
  battle damage/defense.

## Supply lines

This is where most player empires actually fall apart.

**Army Supply Depot** is the logistical building. It collects from
local warehouses, packs supply carts, dispatches to the army on the
world map. Required cargo:

| Cargo    | Status     | Effect |
|----------|------------|--------|
| Rations  | Mandatory  | Starvation → morale collapse → mass desertion. |
| Clothes  | Mandatory  | Exposure on the march. |
| Drinks   | Optional   | Morale boost. |
| Luxuries (gems, alcohol) | Optional | Strong morale boost. |
| Sithilon ore | Optional | Dondorian-specific morale boost. |
| Artillery | v0.70 mandatory for fast sieges | Treated as consumable supply. |

Equipment wears on the field; the depot must continuously ship
replacement weapons/armor. Domestic supply hiccup (coal shortage
stalling the forges) = gradual loss of statistical equipment bonuses
on campaign.

Replenishment requires the army to be on friendly/owned territory and
**stationary for ≥1 full day**. Movement breaks the supply train.
Disbanding/reforming armies wastes the initial lump-sum gear cost —
keep armies standing.

## Tactical battle mechanics

When two armies meet: Auto-Resolve / Manual / Retreat.

### Morale

Invisible per-division HP bar. Drains from:
- Casualties (linear-ish).
- Flanking / encirclement (catastrophic).
- Formation disruption (warhammers, cavalry impact).
- Racial baseline (Garthimi snap fastest, Dondorian last).

When morale breaks, the unit "flees to the void." Fast pursuers can
cut them down; otherwise they exit the battlefield and survive for
later recovery.

### Exhaustion

Stamina marker that goes red under load. Effects:
- Slower movement / attack frequency.
- **Significantly increased blunt damage taken** — exhausted plate
  infantry is a hammer target.

Plate armor + long sprint = fatigued arrival = vulnerable. Keep
reserves; cycle fresh troops in.

### Formations

Tight formations max frontal defense and trigger spearman block
bonuses. They're also slow and flank-vulnerable. Archers fired into a
dense formation hit reliably; into a loose formation, mostly waste
arrows.

### Manual combat best practice

1. **Move** (not Attack) into position — preserves formation.
2. **Charge** when in striking distance — momentum bonus, impact
   disruption.
3. **Attack** only after the lines have collided.
4. Archers on elevation or wide flanks; never behind your own line.

Auto-Resolve is fine for steamrolling raiders. Manual wins when the
math says you should lose — terrain, flanking, and bait-and-switch
aren't in the auto-resolve formula.

Retreat is not free — auto-calculated casualties based on pursuer
speed/strength.

## Sieges

### Defending your capital

The siege happens on the player's own city map. Your architecture is
the defense plan.

- **Walls are destructible in v0.70**. Enemies attack outer tiles;
  once an outer layer falls, infantry climbs the rubble.
- **Wall thickness matters now**. Single-tile walls breach almost
  instantly. Thick walls take longer *and* give you a top surface to
  station archers/spearmen.
- Defensive catapults auto-place but cause friendly fire near the
  impact zone.

If walls fall, enemies sweep toward the Throne Room — that's the loot
target. Once they reach it, they sack a significant chunk of the
treasury and resource stockpile before retreating.

### Attacking an AI city

Currently abstracted on the world map.

1. Move army to enemy region, initiate **Siege** (wait command).
2. Siege progress bar fills via starvation/bombardment.
   - Small settlement: 1–5 days.
   - Fortified capital: up to half a year.
3. Bringing Mechanic-unlocked artillery accelerates progress
   dramatically.
4. AI **sallies out** — defenders + nearby auxiliary armies will try
   to break the siege. Open-field battles before assault are normal.
5. Recommended: wear the bar to ≥50% before assaulting. Premature
   assault eats large auto-resolve penalties.

### Post-conquest options

1. **Mercy** — peaceful occupation, no slaves, infrastructure
   preserved.
2. **Sack** — massive loot, native population hates you, riot risk.
3. **Annihilate / Raze** — maximum extraction, nothing remains.

Captives → Prison → Slaver → workforce / arena / sacrifice. Selling
surplus slaves and captured gear is the canonical post-war
profit lever.

## War exhaustion & recovery

Game tracks empire-wide conflict fatigue. Sustained war = unrest +
loyalty drops + passive army XP. Recovering manpower needs
breeding-building throughput (16d Garthimi vs. 80d Human is the
gulf) or aggressive immigration policy.

## What to surface in dumps

Existing dump covers basic happiness/population. Combat-relevant
candidates:

- Per-division training level, equipment quality, racial composition.
- Army Supply Depot stockpile vs. consumption rate (early-warning
  for starvation cascade).
- War exhaustion score + sustained-deployment days.
- POW / Slaver throughput from current war.
- Wall integrity by segment (for defensive readiness assessment).
