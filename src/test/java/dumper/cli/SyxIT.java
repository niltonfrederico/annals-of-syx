package dumper.cli;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end: first run creates ~/.syx/, mirrors jar + base/, copies the
 * save, runs the dump. Skipped without SOS_GAME_JAR + SOS_TEST_SAVE.
 *
 * Run via:  ./scripts/run.sh dumper.cli.SyxIT
 * (Not via {@code ./gradlew test} — the engine needs xvfb-run.)
 */
class SyxIT {

    @TempDir Path tmpHome;

    @Test
    void firstRunMirrorsAndDumps() throws Exception {
        String gameJar  = System.getenv("SOS_GAME_JAR");
        String testSave = System.getenv("SOS_TEST_SAVE");
        Assumptions.assumeTrue(gameJar != null && testSave != null,
            "needs SOS_GAME_JAR + SOS_TEST_SAVE");

        String savedHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpHome.toString());
        try {
            Path install = Path.of(gameJar).getParent();
            Path outJson = tmpHome.resolve("out.json");
            String saveName = Path.of(testSave).getFileName().toString()
                .replaceFirst("\\.save$", "");

            int rc = new CommandLine(new Syx()).execute(
                "--install", install.toString(),
                "--save",    saveName,
                "--output",  outJson.toString());

            assertEquals(0, rc);
            assertTrue(Files.exists(Paths_.jar()),  "jar mirrored");
            assertTrue(Files.exists(Paths_.base()), "base linked or copied");
            assertTrue(Files.exists(outJson),        "dump written");
        } finally {
            System.setProperty("user.home", savedHome);
        }
    }
}
