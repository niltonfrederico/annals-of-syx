package dumper.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {

    @Test
    void roundTripsConfigV1(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("config.json");
        Config in = new Config();
        in.gameInstallPath = "/games/sos";
        in.cachedJarSha256 = "deadbeef";
        in.supportedGameVersion = "0.70.33";
        in.defaultSave = "Kastra";
        in.baseLinkMode = "symlink";

        ConfigStore.write(file, in);

        assertTrue(Files.exists(file), "config file written");
        Config out = ConfigStore.read(file);
        assertEquals(1, out.version);
        assertEquals("/games/sos", out.gameInstallPath);
        assertEquals("deadbeef", out.cachedJarSha256);
        assertEquals("0.70.33", out.supportedGameVersion);
        assertEquals("Kastra", out.defaultSave);
        assertEquals("symlink", out.baseLinkMode);
    }

    @Test
    void backsUpCorruptedJson(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("config.json");
        Files.writeString(file, "{not json");

        try {
            ConfigStore.read(file);
            org.junit.jupiter.api.Assertions.fail("expected IOException");
        } catch (java.io.IOException expected) {
            org.junit.jupiter.api.Assertions.assertTrue(
                expected.getMessage().contains("corrupted"),
                "message mentions corrupted: " + expected.getMessage());
        }

        assertTrue(java.util.stream.Stream.of(tmp.toFile().listFiles())
            .anyMatch(f -> f.getName().startsWith("config.json.broken-")),
            "backup file created");
    }
}
