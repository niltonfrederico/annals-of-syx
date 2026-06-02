# City & economy

How the simulation actually drives a city: workforce assignment,
fulfillment, food chain, services, housing, sanitation, demographic
flow. The save file holds the *result*; this doc explains the *why*
behind each number so the dumper can pick stats that actually inform
decisions.

## Social classes

Four mechanical strata stacked on top of every race:

| Class    | Behavior                                                              |
|----------|-----------------------------------------------------------------------|
| Noble    | Generates **Government Points** (admin) or boosts industry. Assigned manually. Death = systemic disruption, auto-replace toggle exists. |
| Citizen  | Standard worker. Sub-tagged Native / Immigrant / Former Slave / Ex-convict (1y tag). Eligible for school, military draft, full fulfillment expectations. |
| Slave    | Manual labor only. Bypasses fulfillment slots; tracked by Submission + collective happiness. Cannot be educated or drafted. |
| Child    | Non-working. Childhood length varies by race (Garthimi 16d, Tilapi 48d, Human 80d). Schools convert children into permanently more-productive adults. |

The Noble layer is what the world map runs on — admin points cost Nobles
in the capital, regional upgrades cost admin points. A capital with
zero Nobles can hold territory but can't level it up.

## Workforce model

Every subject has an assigned employment vector. The "Odd Jobber" class
is a residual pool of any citizen/slave not bound to a specific
workstation:

- Odd Jobbers execute global commands (tree felling, building
  construction, hauling).
- Their count = `total_workforce - sum(employed)`. Over-assigning
  workstations produces a negative Odd Jobber count and a UI warning.
- Workstations can radius-bound an employee, turning them into a
  scoped Odd Jobber — important when reading "employment" in dumps;
  proximity matters.

Workers run an internal sleep/eat/drink/lavatory/hearth/recreation
cycle regardless of employment — a "100% staffed" workshop is never
producing at full theoretical throughput.

Skill modifier on production scales by intelligence × accumulated
education × room tier. Education is a one-shot childhood investment
that compounds across the adult lifespan.

## Fulfillment / happiness

The single most-load-bearing metric in the game. Drives loyalty,
immigration, productivity. Per-slot formula approximates
`(current + 1) / (max + 1)`, then multiplied together.

Driver categories:

- **Environmental**: building material match (Cretonian wood vs.
  Dondorian stone), roads quality, "squareness" / "roundness"
  preference per race, neighboring race composition.
- **Consumptive**: food preference match (race-conditional),
  ration multiplier (>1 is a big happiness/health boost at heavy
  stockpile cost), clothing presence (0→1 nearly doubles happiness in
  that slot), furniture (late-game luxury, fast decay — a common
  early-game noob trap).
- **Psychological/Systemic**: service access (entertainment, religion,
  education, medical), tax rate (lowers loyalty), incident pressure
  (crime, riots, unburied corpses).

When loyalty drops below 100%, riot probability scales exponentially.
Riots can cascade across races if a hated cohabitant joins in.

Immigration of race X kicks in when X's happiness exceeds 80%, scaling
toward 100%. So a save with X at 75% happiness is *barely* not
attracting new immigrants — useful signal for the dumper.

## Food production

| Source            | Output                       | Notes |
|-------------------|------------------------------|-------|
| Farms             | Veg, Grain, Cotton, Opiates, Fruit | Single water tile irrigates 4 adjacent (100% moisture bonus). Grain → Bread at Bakery (cost: Coal or Wood). |
| Mushroom farms    | Mushrooms                    | Indoor, cold-preferring. Trade-valued; Human/Dondorian favorite. |
| Pastures          | Meat, Leather, Eggs          | Aurochs, Entelodonts, Globdiens (Amevia only), Onx. Low staff, high footprint. |
| Fisheries         | Fish                         | Fastest spoilage in the game. |
| Hunters           | Meat, Leather                | Can over-hunt local herds to extinction. |

Spoilage scales with the **Conservation** civic modifier. Order
slowest → fastest: Eggs / Rations > Bread > Meat / Veg > Fish.
Upgraded warehouses extend shelf life significantly.

## Services

| Service       | Buildings (sample)                          | Notes |
|---------------|----------------------------------------------|-------|
| Entertainment | Stages, Arenas, Taverns, Eateries, Canteens, Restaurants | Restaurant ≈ 1 in 8 food-stall visits. |
| Religion      | Shrines, Temples (Crator / Shmalor / Athuri / Aminion) | Mixing religions in one shrine is inefficient. |
| Knowledge     | Schools (children), Universities (adults), Libraries, Laboratories | Labs generate Innovation (boosted by clay tablets). Libraries need Leather (v0.71). |
| Medical       | Physicians, Hospitals, Asylums                | Physician = preventative checkup multiplier. Hospital = treats active disease (consumes Opiates + Fabric). Asylum = treats insanity. |
| Death rites   | Graveyards, Crypts, Mass Graves               | Unburied corpse = massive Health malus. Tilapi can eat corpses, skipping the need. |

Distinct service types are tracked independently — a citizen with
maxed-out religion access and zero entertainment access is *not*
"satisfied on average," they're carrying a happiness debuff.

## Housing tiers

Bunkhouse < Dormitory < Chambers < Flat Houses / Apartments < Private
Housing.

Private Housing (post v0.62) is decentralized — citizens claim plots
and decorate with race-specific furniture, shifting the wood/cloth/
furniture economy from central-supply to per-household demand. A
Garthimi private home looks structurally different from a Human one.

All rooms degrade. Janitors repair using maintenance materials; the
**Robustness** civic modifier slows decay. Without janitors, a room
degrades from "2 years" lifespan to "8 days" — invisible in headcount,
visible in resource burn.

## Immigration vs. breeding

Two parallel population inflows:

- **Immigration**: arrives at the map's edge when race happiness >80%.
  Cap-able, auto-acceptable. Free demographic growth but requires
  satisfying that race's preferences first.
- **Breeding**: race-specific buildings consume race-specific food.
  Human Nursery → Fruit. Tilapi Nursery → Fruit. Cretonian Breeder →
  **Vegetables** (the common foot-gun). Garthimi Hatchery → Meat
  (and the fastest at 16 days). Amevia hatcheries are slow regardless
  of supply. Dondorians **don't breed at all** — Mt. Cerebus spawns
  them divinely, immigration only.

Practical dumper implication: a city's "expected demographic
trajectory" requires reading both inflows. A city at 79% Human
happiness running a fully-supplied nursery is growing slowly; the same
city at 81% with no nursery is growing fast.

## Sanitation, disease, health

Health and Medicine are mechanically distinct.

- **Health Score** = epidemic *prevention*. Target > 1.0. Modifiers:
  pop size (debuff), unburied corpses (massive debuff), wells/lavatories
  (baseline), bathhouses (remove "dirty" status from mine workers),
  food rations >1 (very powerful), physician checkups (massively
  multiplicative).
- **Hospitals** do **not** contribute to Health Score. They treat
  *active* disease — speed recovery, raise survival. Consume Opiates
  (pain relief) + Fabric (bandages).
- **Insanity (Neuroticism)** — hidden per-subject trait. Bad conditions
  trigger insanity; deranged citizens wander uselessly until treated
  in an **Asylum**.

Plague can wipe a 8k-population city in days if Health Score is
below 1.0 and no Asylums/Hospitals exist.

## Logistics

- Warehouses store per-good quotas. v0.71+ supports prioritized
  fetching from peripheral warehouses to central ones without manual
  hauling orders.
- **Haulers** are zero-cost drop-off points. Odd-jobbers move goods to
  the hauler; dedicated logistics workers do bulk transport from
  there.
- **Transports** are freight-train equivalents. Min 6 workers + 100
  tiles to break even vs. deliverymen, but at full capacity replace
  100-200 deliverymen.

## What to surface in dumps

Existing dumps already cover population/happiness by race. High-value
additions:

- Per-service satisfaction breakdown (entertainment / religion /
  knowledge / medical) — independent of food/clothing.
- Food production vs. consumption ratio per race (validates preference
  alignment with available chains).
- Immigration delta per race — directly answers "is this city growing
  via attraction or breeding."
- Health Score components (prevention vs. treatment capacity).
- Janitor coverage vs. room count (early warning for decay cascade).
