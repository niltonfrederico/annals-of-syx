package dumper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import init.type.HCLASSES;
import init.type.HCLASS;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test. NOT runnable via Gradle's `test` task: the game engine
 * needs a real GL context (xvfb-run), specific CWD (Steam install for
 * PATHS.init), and script jars on the classpath. After the save loads, GAME
 * spawns daemon threads (autosaver, sound) that prevent clean JVM exit, so we
 * end the run via {@code Runtime.getRuntime().halt()}.
 *
 * Invoke via {@link #main(String[])} from a shadowJar under xvfb-run. See the
 * README / T5 notes for the exact command.
 *
 * The JUnit annotation is kept so the contract is discoverable by tooling.
 */
class PopulationTest {

    private static final Path KASTRA_SAVE = Path.of(
        "/home/kuresto/Chronopolis/repos/_songofsyx/runs/current/Kastra-AI-19e80438c2f-460021-0-b1b.save"
    );

    @Test
    void popTotalMatchesFilenameGroundTruth() throws Exception {
        runAssertion(KASTRA_SAVE);
    }

    private static int runAssertion(Path save) throws Exception {
        Bootstrap.loadSave(save);
        Map<String, Map<String, Integer>> pop = StatsDumper.populationByRaceAndClass();

        // Ground-truth from filename hex is the *player* population count.
        // HCLASS.OTHER is a non-player bucket (visitors, beasts, traders) and is
        // excluded from that figure. Sum only player classes (HCLASSES.ALLP()).
        Set<String> playerClassKeys = new HashSet<>();
        for (HCLASS c : HCLASSES.ALLP()) playerClassKeys.add(c.key);

        int playerTotal = 0;
        int rawTotal = 0;
        for (Map.Entry<String, Map<String, Integer>> e : pop.entrySet()) {
            for (Map.Entry<String, Integer> cc : e.getValue().entrySet()) {
                rawTotal += cc.getValue();
                if (playerClassKeys.contains(cc.getKey())) playerTotal += cc.getValue();
            }
            int rTotal = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("  " + e.getKey() + " -> " + rTotal + "  " + e.getValue());
        }
        System.out.println("RAW_TOTAL=" + rawTotal + "  PLAYER_TOTAL=" + playerTotal);

        int expected = StatsDumper.popTotalFromFilename(save);
        if (expected < 0) {
            throw new AssertionError("could not parse pop-total hex from filename: " + save.getFileName());
        }
        assertEquals(expected, playerTotal,
            "sum population[race][playerClass] must equal filename hex");

        // Smoke: happiness + loyalty extractors return non-null, non-empty maps.
        // Shape is Race.key -> double (0..1); these standings have no HCLASS axis.
        Map<String, Double> happiness = StatsDumper.happinessByRace();
        if (happiness.isEmpty()) throw new AssertionError("happinessByRace returned empty");
        Map<String, Double> loyalty = StatsDumper.loyaltyByRace();
        if (loyalty.isEmpty()) throw new AssertionError("loyaltyByRace returned empty");
        System.out.println("HAPPINESS races: " + happiness.size() + " " + happiness);
        System.out.println("LOYALTY races: " + loyalty.size() + " " + loyalty);

        // Smoke: religion / work fulfillment / deaths by cause.
        Map<String, Map<String, Integer>> religion = StatsDumper.religionFollowersByRace();
        if (religion.isEmpty()) throw new AssertionError("religionFollowersByRace returned empty");
        Map<String, Map<String, Double>> work = StatsDumper.workFulfillmentByRaceAndClass();
        if (work.isEmpty()) throw new AssertionError("workFulfillmentByRaceAndClass returned empty");
        Map<String, Integer> deaths = StatsDumper.deathsByCause();
        // Deaths can legitimately be empty for a fresh game; just print, don't fail.
        System.out.println("RELIGION: " + religion.size() + " " + religion);
        System.out.println("WORK: " + work.size() + " " + work);
        System.out.println("DEATHS: " + deaths.size() + " " + deaths);
        return playerTotal;
    }

    public static void main(String[] args) {
        Path save = args.length > 0 ? Path.of(args[0]) : KASTRA_SAVE;
        try {
            int total = runAssertion(save);
            System.out.println("PASS: total=" + total);
            Runtime.getRuntime().halt(0);
        } catch (AssertionError ae) {
            System.out.println("FAIL: " + ae.getMessage());
            Runtime.getRuntime().halt(1);
        } catch (Throwable t) {
            System.out.println("FAIL at: " + t.getClass().getSimpleName());
            t.printStackTrace();
            Runtime.getRuntime().halt(1);
        }
    }
}
