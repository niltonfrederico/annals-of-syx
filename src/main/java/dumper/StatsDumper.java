package dumper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import game.GAME;
import game.GameSpec;
import game.faction.FACTIONS;
import game.faction.FResources.RTYPE;
import game.faction.player.Player;
import game.values.GCOUNTS;
import init.race.RACES;
import init.race.Race;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.tech.TECH;
import init.tech.TECHS;
import init.type.CAUSE_LEAVE;
import init.type.CAUSE_LEAVES;
import init.type.HCLASS;
import init.type.HCLASSES;
import settlement.main.SETT;
import settlement.room.main.RoomBlueprint;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.RoomBlueprintIns;
import settlement.room.main.employment.RoomEmploymentSimple;
import settlement.stats.STATS;
import settlement.stats.colls.StatsReligion.StatReligion;
import settlement.stats.law.LAW;
import settlement.stats.law.PRISONER_TYPE;
import settlement.stats.law.PRISONER_TYPE.CRIME;
import settlement.stats.service.StatService;
import settlement.stats.service.StatServiceImp;
import settlement.stats.service.StatServiceRoom;
import settlement.stats.stat.STAT;
import settlement.stats.stat.StatDecree;
import settlement.stats.standing.STANDINGS;
import util.statistics.HISTORY_COLLECTION;
import util.statistics.HISTORY_INT;

public final class StatsDumper {

    private StatsDumper() {}

    public static Map<String, Map<String, Integer>> populationByRaceAndClass() {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            Map<String, Integer> per = new LinkedHashMap<>();
            for (HCLASS c : HCLASSES.ALL()) {
                per.put(c.key, STATS.POP().POP.data(c).get(r));
            }
            out.put(r.key, per);
        }
        return out;
    }

    // Races with zero settlement population are filtered from happiness /
    // loyalty / fulfillment / expectation / loyalty_target outputs. The
    // engine returns BEHAVIOUR().HAPPI defaults (commonly 1.0) for empty
    // races, which would otherwise look like "all 8 races are happy" in
    // the dump — a misleading reading when 6 of them aren't even present.
    private static boolean racePresent(Race r) {
        return STATS.POP().POP.data().get(r) > 0;
    }

    // Happiness/loyalty are CITIZEN-only standings in StandingCitizen — they
    // have no HCLASS dimension. Shape is Race.key -> double (0..1).
    public static Map<String, Double> happinessByRace() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            out.put(r.key, STANDINGS.CITIZEN().happiness.getD(r));
        }
        return out;
    }

    public static Map<String, Double> loyaltyByRace() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            out.put(r.key, STANDINGS.CITIZEN().loyalty.getD(r));
        }
        return out;
    }

    // Smoothed scalars StandingCitizen tracks alongside happiness/loyalty.
    // Read these together — the absolute numbers look tiny in isolation
    // but compose like this in the engine (StandingCitizen.hap):
    //   happiness = (fulfillment / expectation) * HAPPI_boost, clamped [0,1]
    // So fulfillment=0.015 with expectation=0.017 and HAPPI_boost≈1.1
    // produces happiness=1.0 — the *ratio* matters, not absolute values.
    //   - fulfillment: weighted sum of standing contributions (small early game).
    //   - expectation: population-driven demand (grows with pop).
    //   - loyalty_target: happiness * LOYALTY boost; loyalty drifts toward it.
    public static Map<String, Double> fulfillmentByRace() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            out.put(r.key, STANDINGS.CITIZEN().fullfillment.getD(r));
        }
        return out;
    }

    public static Map<String, Double> expectationByRace() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            out.put(r.key, STANDINGS.CITIZEN().expectation.getD(r));
        }
        return out;
    }

    public static Map<String, Double> loyaltyTargetByRace() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            out.put(r.key, STANDINGS.CITIZEN().loyaltyTarget.getD(r));
        }
        return out;
    }

    // Per-stat fulfillment breakdown for CITIZEN class:
    // race.key -> stat.key -> {name, current, max, dismiss, inverted}.
    // current/max are the modifier contributions UI shows as bars (StatRow:104-107).
    // Sum of `current` across stats ≈ what feeds Fulfillment.fullfillment(r).
    //
    // CRITICAL: how to read `current` depends on `inverted` (StandingDef:230,
    // formula in StatStanding.get:139-145):
    //   d = clamp(raw_input, 0, 1)
    //   if (inverted) d = 1 - d
    //   return d * max
    //
    // - inverted=false (coverage / provision stats — FURNITURE, CLOTHES,
    //   FOOD_RATIONS, RETIREMENT, ...): `current=max` means fully
    //   provisioned, `current=0` means none. Gap (max-current) is the
    //   upside left on the table.
    // - inverted=true (avoidance-of-bad stats — BATTLE_BESIEGED,
    //   FOOD_STARVATION, BURIAL_DESECRATION, ENVIRONMENT_CANNIBALISM,
    //   POPULATION_WRONGFUL_DEATHS, ...): `current=max` means the bad
    //   thing is NOT happening (full bonus from absence), `current=0`
    //   means the bad thing is maxed out. A maxed value here is GOOD; a
    //   zero value here is the actual crisis flag. The "INVERTED: true"
    //   declarations live in data.zip:data/assets/init/stats/loyalty/VANILLA.txt.
    public static Map<String, Map<String, Map<String, Object>>> fulfillmentBreakdownByRace() {
        HCLASS cl = HCLASSES.CITIZEN();
        Map<String, Map<String, Map<String, Object>>> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            Map<String, Map<String, Object>> per = new LinkedHashMap<>();
            for (STAT ss : r.stats().standings(cl)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", ss.info().name.toString());
                row.put("current", ss.standing().get(cl, r));
                row.put("max", ss.standing().definition(r).get(cl).max);
                row.put("dismiss", ss.standing().definition(r).get(cl).dismiss);
                row.put("inverted", ss.standing().definition(r).inverted);
                per.put(ss.key(), row);
            }
            out.put(r.key, per);
        }
        return out;
    }

    // Religion followers: religion.key -> race.key -> raw follower count.
    // StatReligion.followers is a SettStatistics whose data() (HCLASS=null)
    // gives the total-across-classes count per race.
    public static Map<String, Map<String, Integer>> religionFollowersByRace() {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        for (StatReligion sr : STATS.RELIGION().ALL) {
            Map<String, Integer> per = new LinkedHashMap<>();
            for (Race r : RACES.all()) {
                per.put(r.key, sr.followers.data().get(r));
            }
            out.put(sr.religion.key, per);
        }
        return out;
    }

    // Work fulfillment: race.key -> hclass.key -> mean fulfillment (0..1).
    // STATS.WORK().WORK_FULFILLMENT is a STAT with per (HCLASS, Race) axis,
    // values are doubles in [0, 1] (heart-icon stat).
    public static Map<String, Map<String, Double>> workFulfillmentByRaceAndClass() {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            if (!racePresent(r)) continue;
            Map<String, Double> per = new LinkedHashMap<>();
            for (HCLASS c : HCLASSES.ALL()) {
                per.put(c.key, STATS.WORK().WORK_FULFILLMENT.data(c).getD(r));
            }
            out.put(r.key, per);
        }
        return out;
    }

    // Deaths by cause — rolling-window snapshot, NOT lifetime.
    // STATS.POP().COUNT.leaves().get(c).statistics(null) is a
    // HISTORY_COLLECTION<Race>; `.get(null)` reads its current bucket and
    // `.total()` exposes the underlying HISTORY_INT (32-day rolling
    // ringbuffer, see STATS.DAYS_SAVED). The previous "total" reading was
    // misleading: it gave the count for "now" only, so a city with a
    // historic raid showed deaths.SLAYED=0 the moment the bucket rolled
    // past. We expose two honest views instead:
    //   - deaths_today:        last bucket per cause.
    //   - deaths_history_8d:   8 most recent daily buckets per cause
    //                          (index 0 = today, growing into the past).
    // The engine does NOT keep an all-time deaths-by-cause counter; the
    // closest lifetime data is in `counters.COUNT_ACCIDENTS` /
    // `COUNT_ROYALTIES_KILLED`, which only cover a narrow subset.
    public static Map<String, Integer> deathsToday() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (CAUSE_LEAVE c : CAUSE_LEAVES.ALL()) {
            if (!c.death) continue;
            out.put(c.key, STATS.POP().COUNT.leaves().get(c.index()).statistics(null).get(null));
        }
        return out;
    }

    public static Map<String, List<Integer>> deathsHistory8d() {
        Map<String, List<Integer>> out = new LinkedHashMap<>();
        for (CAUSE_LEAVE c : CAUSE_LEAVES.ALL()) {
            if (!c.death) continue;
            out.put(c.key, historyInt8d(STATS.POP().COUNT.leaves().get(c.index()).statistics(null).total()));
        }
        return out;
    }

    // Rooms census — per-blueprint physical state of the city.
    // Iterates ROOMS().collection.all() (all blueprints) and casts to
    // RoomBlueprintIns to get instance count, area, employment, degrade.
    // Blueprints that aren't Ins (rare; some singletons) are recorded with
    // count=0/area=0 and a "kind" hint so they're still discoverable.
    //
    // CAUTION on employees_max for system rooms (key starts with `_`):
    // `_STOCKPILE`, `_HAULER`, `_BUILDER`, `_JANITOR`, `_EXPORT`, `_SLAVER`,
    // etc. are not real buildings — they're pseudo-rooms representing
    // settlement-wide policies/capacities. Their `employees_max` is a
    // policy-derived target (e.g. `_STOCKPILE` reports total storage tiles,
    // not worker slots). Treating `(max - current) / max` as
    // "% understaffed" yields nonsense for these. Compare worker fill
    // only on rooms whose key does NOT start with `_`.
    public static Map<String, Map<String, Object>> roomsCensus() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (RoomBlueprint bp : SETT.ROOMS().collection.all()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String key = bp.key();
            row.put("key", key);
            if (bp instanceof RoomBlueprintImp imp) {
                row.put("type", imp.type);
                row.put("category", imp.cat == null ? null : imp.cat.name().toString());
            } else {
                row.put("type", null);
                row.put("category", null);
            }
            if (bp instanceof RoomBlueprintIns<?> ins) {
                row.put("count", ins.instancesSize());
                row.put("total_area", ins.totalArea());
                row.put("degrade_avg", ins.degradeAverage());
                RoomEmploymentSimple emp = ins.employment();
                if (emp != null) {
                    row.put("employees_current", emp.employed());
                    row.put("employees_max", emp.employedMax());
                    row.put("employees_needed", emp.neededWorkers());
                    row.put("employees_fill", emp.getFill());
                } else {
                    row.put("employees_current", 0);
                    row.put("employees_max", 0);
                    row.put("employees_needed", 0);
                    row.put("employees_fill", 0.0);
                }
                row.put("kind", "ins");
            } else {
                row.put("count", 0);
                row.put("total_area", 0);
                row.put("degrade_avg", 0.0);
                row.put("employees_current", 0);
                row.put("employees_max", 0);
                row.put("employees_needed", 0);
                row.put("employees_fill", 0.0);
                row.put("kind", "singleton");
            }
            out.put(key, row);
        }
        return out;
    }

    // Current-day stockpile per resource (count of items stored across all
    // STOCKPILE rooms). Source: ROOMS().STOCKPILE.tally().amountsDay() — the
    // same HistoryResource that backs StatsStored.
    public static Map<String, Integer> stockpileByResource() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (RESOURCE res : RESOURCES.ALL()) {
            int amount = SETT.ROOMS().STOCKPILE.tally().amountsDay().get(res.bIndex()).get(0);
            out.put(res.key, amount);
        }
        return out;
    }

    // Law subsystem snapshot — the engine's enforcement state, not the
    // CITIZEN-fulfillment LAW_LAW which the breakdown already covers. Lets you
    // correlate the fulfillment bar with actual crime/prisoner volume.
    public static Map<String, Object> lawSnapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("law_today", LAW.law().today());
        out.put("law_increase", LAW.law().increase());

        HISTORY_INT total = (HISTORY_INT) LAW.crimes().crimes(null);
        out.put("crimes_total_today", total.get(0));
        out.put("crimes_total_history_8d", historyInt8d(total));

        Map<String, Integer> byCrime = new LinkedHashMap<>();
        Map<String, List<Integer>> byCrimeHist = new LinkedHashMap<>();
        for (CRIME c : PRISONER_TYPE.CRIMES) {
            HISTORY_INT h = LAW.crimes().crimes(c);
            byCrime.put(c.key, h.get(0));
            byCrimeHist.put(c.key, historyInt8d(h));
        }
        out.put("crimes_today_by_type", byCrime);
        out.put("crimes_history_8d_by_type", byCrimeHist);

        Map<String, Integer> crimesByRace = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            HISTORY_INT rh = LAW.crimes().perRace().history(r);
            crimesByRace.put(r.key, rh.get(0));
        }
        out.put("crimes_today_by_race", crimesByRace);

        out.put("prisoners_total", LAW.prisoners().amount());
        Map<String, Integer> prByType = new LinkedHashMap<>();
        for (PRISONER_TYPE t : PRISONER_TYPE.ALL) {
            prByType.put(t.key, LAW.prisoners().amount(t));
        }
        out.put("prisoners_by_type", prByType);

        Map<String, Integer> prByRace = new LinkedHashMap<>();
        for (Race r : RACES.all()) {
            prByRace.put(r.key, LAW.prisoners().amount(r));
        }
        out.put("prisoners_by_race", prByRace);

        return out;
    }

    // Disease snapshot. The engine doesn't expose a simple "N sick" count via
    // public API — sick()/incubating() are STATs whose per-Induvidual storage
    // would require iterating pops. Health history is the cheap, available
    // signal: settlement-wide HEALTH boostable score (`StatsDisease:29`,
    // value = BOOSTABLES.PHYSICS().HEALTH / 1024). Baseline is ~1.0 and
    // there is no upper cap — values above 1.0 mean healthier than
    // baseline (boost from baths/physicians), values below 1.0 mean
    // worse (disease pressure, no hospital coverage). Index 0 = today,
    // growing into the past.
    public static Map<String, Object> diseaseSnapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Double> hist = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            hist.add(STATS.DISEASE().healthHistory.getD(i));
        }
        out.put("health_history_8d", hist);
        return out;
    }

    // GCOUNTS — game-wide accumulators (lifetime/current counters for events
    // like EXECUTIONS, RIOTS, INVASIONS_WON, ROOMS_BUILT, etc.). Reflection
    // would be brittle to repackaging; the ALL list on GCOUNTS is the stable
    // public iteration point.
    public static Map<String, Map<String, Integer>> gameCounters() {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        GCOUNTS gc = GAME.count();
        for (GCOUNTS.SAccumilator s : gc.ALL) {
            Map<String, Integer> row = new LinkedHashMap<>();
            row.put("current", s.current());
            row.put("all_time_high", s.allTimeHigh());
            out.put(s.key, row);
        }
        return out;
    }

    // Player treasury — Denari credits and 8-day spending history.
    public static Map<String, Object> treasury() {
        Map<String, Object> out = new LinkedHashMap<>();
        Player p = FACTIONS.player();
        out.put("credits", p.credits().credits());
        List<Integer> hist = new ArrayList<>(8);
        HISTORY_INT ch = p.credits().creditsH();
        for (int i = 0; i < 8; i++) {
            hist.add(ch.get(i));
        }
        out.put("credits_history_8d", hist);
        return out;
    }

    // Production / consumption / trade flows from the player faction's
    // resource ledger (8-day window, index 0 = today, growing into past).
    //
    // FResources.inc(res, type, am) routes signed deltas into two
    // per-RTYPE histograms: positive deltas go to in(t), negative deltas
    // are stored as positive magnitude in out(t). So for ANY RTYPE:
    //   in[i]  = amount gained on day i (added to player's resource pool)
    //   out[i] = amount lost on day i (removed from the pool)
    //   net[i] = in[i] - out[i]
    //
    // Per-RTYPE usage patterns (one side usually dominates):
    //   PRODUCED: in = produced amount, out = production rollbacks/waste
    //             (rare; usually near 0). Use in - out for true output.
    //   CONSUMED: in = usually 0 (you don't "un-consume"),
    //             out = consumption amount. Use out directly.
    //   TRADE:    in = bought from traders, out = sold to traders.
    //             Both can be active on the same resource within a day.
    //             Use net to see net trade balance.
    //
    // Only PRODUCED / CONSUMED / TRADE are dumped — the other RTYPEs
    // (TAX, MAINTENANCE, SPOILAGE, CONSTRUCTION, EQUIPPED, ARMY_SUPPLY,
    // SPOILS, DIPLOMACY, THEFT) are available in FResources but bloat
    // the dump; add on demand.
    public static Map<String, Map<String, Map<String, List<Integer>>>> resourceFlows8d() {
        Map<String, Map<String, Map<String, List<Integer>>>> out = new LinkedHashMap<>();
        RTYPE[] tracked = { RTYPE.PRODUCED, RTYPE.CONSUMED, RTYPE.TRADE };
        for (RTYPE t : tracked) {
            HISTORY_COLLECTION<RESOURCE> inFlow = FACTIONS.player().res().in(t);
            HISTORY_COLLECTION<RESOURCE> outFlow = FACTIONS.player().res().out(t);
            Map<String, Map<String, List<Integer>>> byRes = new LinkedHashMap<>();
            for (RESOURCE res : RESOURCES.ALL()) {
                List<Integer> inH = historyInt8d(inFlow.history(res));
                List<Integer> outH = historyInt8d(outFlow.history(res));
                if (allZero(inH) && allZero(outH)) continue;
                List<Integer> netH = new ArrayList<>(8);
                for (int i = 0; i < 8; i++) {
                    netH.add(inH.get(i) - outH.get(i));
                }
                Map<String, List<Integer>> row = new LinkedHashMap<>();
                row.put("in", inH);
                row.put("out", outH);
                row.put("net", netH);
                byRes.put(res.key, row);
            }
            out.put(t.name(), byRes);
        }
        return out;
    }

    // Edicts / decree state — per-stat policy knobs (LAW_EXECUTION mode, etc.).
    // Iterates STATS.all(), filters to stats with a decree, dumps value per race.
    // The decree value is a small int (often 0/1 toggle, sometimes a tier).
    public static Map<String, Map<String, Integer>> edicts() {
        HCLASS cl = HCLASSES.CITIZEN();
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        for (STAT s : STATS.all()) {
            StatDecree d = s.decree();
            if (d == null) continue;
            Map<String, Integer> per = new LinkedHashMap<>();
            for (Race r : RACES.all()) {
                per.put(r.key, d.getI(cl).get(r));
            }
            out.put(s.key(), per);
        }
        return out;
    }

    // Tech tree progress — player level per TECH, plus max and tree key.
    // Tech.levelMax > 1 for stepped techs (most have levelMax=1 binary unlock).
    public static Map<String, Map<String, Object>> techLevels() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        Player p = FACTIONS.player();
        for (TECH t : TECHS.ALL()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("level", p.tech.level(t));
            row.put("level_max", t.levelMax);
            row.put("tree", t.tree == null ? null : t.tree.key);
            out.put(t.key, row);
        }
        return out;
    }

    // Per-service standing breakdown — opt-in via `--coverage`. For each
    // StatServiceImp (rooms + simple services + hospital + bench), dumps the
    // race-aggregate STAT values. StatServiceRoom adds access/quality/
    // proximity/upgrade slots; others only have total.
    //
    // "Coverage" here is the standing aggregate (mean over pops), not a
    // per-tile reachability map. True per-tile dumps would multiply size by
    // ~tile-count and aren't useful without a renderer. The values still
    // answer the practical question: "is service X reaching my pops, and at
    // what quality / distance?"
    public static Map<String, Map<String, Map<String, Double>>> serviceCoverage() {
        HCLASS cl = HCLASSES.CITIZEN();
        Map<String, Map<String, Map<String, Double>>> out = new LinkedHashMap<>();
        for (StatServiceImp svc : STATS.SERVICE().ALL) {
            STAT total = svc.total();
            Map<String, Map<String, Double>> perRace = new LinkedHashMap<>();
            for (Race r : RACES.all()) {
                Map<String, Double> metrics = new LinkedHashMap<>();
                // total is a fulfillment contributor, so its standing is
                // meaningful (matches the fulfillment_breakdown value).
                // Some services lack an aggregate total (serviceKey handles
                // the same null case) — skip the field rather than NPE.
                if (total != null) {
                    metrics.put("total_fulfillment", total.standing().get(cl, r));
                }
                if (svc instanceof StatServiceRoom sr) {
                    // access/quality/proximity/upgrade don't contribute to
                    // standing — read the population-aggregate data() instead.
                    // getD returns the normalized mean (0..1) for today.
                    metrics.put("access", sr.access().data(cl).getD(r, 0));
                    metrics.put("quality", sr.quality().data(cl).getD(r, 0));
                    metrics.put("proximity", sr.proximity().data(cl).getD(r, 0));
                    metrics.put("upgrade", sr.upgrade().data(cl).getD(r, 0));
                }
                perRace.put(r.key, metrics);
            }
            out.put(serviceKey(svc), perRace);
        }
        return out;
    }

    private static String serviceKey(StatService svc) {
        STAT t = svc.total();
        return t == null ? svc.name.toString() : t.key();
    }

    private static boolean allZero(List<Integer> xs) {
        for (Integer x : xs) if (x != 0) return false;
        return true;
    }

    private static List<Integer> historyInt8d(HISTORY_INT h) {
        List<Integer> out = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            out.add(h.get(i));
        }
        return out;
    }

    /** Default dump — everything except opt-in heavy sections. */
    public static Map<String, Object> dumpAll(GameSpec spec, Path savePath) {
        return dumpAll(spec, savePath, false);
    }

    /** Full dump with optional coverage section. */
    public static Map<String, Object> dumpAll(GameSpec spec, Path savePath, boolean includeCoverage) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("_meta", meta(spec, savePath));
        out.put("population", populationByRaceAndClass());
        out.put("happiness", happinessByRace());
        out.put("loyalty", loyaltyByRace());
        out.put("fulfillment", fulfillmentByRace());
        out.put("expectation", expectationByRace());
        out.put("loyalty_target", loyaltyTargetByRace());
        out.put("fulfillment_breakdown", fulfillmentBreakdownByRace());
        out.put("religion", religionFollowersByRace());
        out.put("work_fulfillment", workFulfillmentByRaceAndClass());
        out.put("deaths_today", deathsToday());
        out.put("deaths_history_8d", deathsHistory8d());
        out.put("rooms", roomsCensus());
        out.put("stockpile", stockpileByResource());
        out.put("law", lawSnapshot());
        out.put("disease", diseaseSnapshot());
        out.put("counters", gameCounters());
        out.put("treasury", treasury());
        out.put("resource_flows", resourceFlows8d());
        out.put("edicts", edicts());
        out.put("tech", techLevels());
        if (includeCoverage) {
            out.put("service_coverage", serviceCoverage());
        }
        return out;
    }

    // _meta block. The two time fields are NOT the same thing:
    //   played_ingame_seconds = spec.playSeconds — simulated game-time
    //     (accumulated TIME.secondsPerDay() ticks). Used by the game UI
    //     to compute in-game years via:
    //       years = playSeconds / (secondsPerHour * hoursPerDay * 16)
    //   counters.COUNT_TIME_PLAYED.current = real wall-clock seconds
    //     spent playing (persisted across sessions).
    // Default game speed makes ingame ≈ 60× wallclock, but the player
    // can change speed or pause — never assume a constant ratio.
    private static Map<String, Object> meta(GameSpec spec, Path savePath) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("save_path", savePath.toAbsolutePath().toString());
        m.put("save_version", String.format("0.%d.%d", (spec.version >> 16) & 0xFF, spec.version & 0xFF));
        m.put("played_ingame_seconds", (long) spec.playSeconds);
        m.put("race", spec.race.toString());
        m.put("city", spec.city.toString());
        m.put("ruler", spec.ruler.toString());
        m.put("pop_total", popTotalFromFilename(savePath));
        return m;
    }

    static int popTotalFromFilename(Path savePath) {
        String name = savePath.getFileName().toString();
        if (name.endsWith(".save")) name = name.substring(0, name.length() - 5);
        int dash = name.lastIndexOf('-');
        if (dash < 0) return -1;
        try {
            return Integer.parseInt(name.substring(dash + 1), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
