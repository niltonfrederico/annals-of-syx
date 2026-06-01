package dumper.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCheckerTest {

    private Path fixture(String name) {
        return Path.of("src/test/resources/jars", name);
    }

    @Test
    void readsManifestVersionWhenPresent() throws Exception {
        VersionChecker.Result r = VersionChecker.inspect(fixture("with-version.jar"));
        assertEquals("0.70.33", r.version());
        assertEquals("manifest", r.source());
    }

    @Test
    void fallsBackToSha256WhenManifestLacksVersion() throws Exception {
        VersionChecker.Result r = VersionChecker.inspect(fixture("no-version.jar"));
        assertEquals("sha256", r.source());
        assertEquals(64, r.version().length(), "sha-256 hex is 64 chars");
    }

    @Test
    void acceptsSupportedManifestVersion() throws Exception {
        VersionChecker.Result r = VersionChecker.inspect(fixture("with-version.jar"));
        Set<String> allowlist = Set.of("0.70.33");
        assertTrue(VersionChecker.isSupported(r, allowlist, Set.of()));
    }

    @Test
    void acceptsSupportedSha() throws Exception {
        VersionChecker.Result r = VersionChecker.inspect(fixture("no-version.jar"));
        Set<String> shaAllowlist = Set.of(r.version());
        assertTrue(VersionChecker.isSupported(r, Set.of(), shaAllowlist));
    }

    @Test
    void rejectsUnknown() throws Exception {
        VersionChecker.Result r = VersionChecker.inspect(fixture("with-version.jar"));
        assertFalse(VersionChecker.isSupported(r, Set.of("9.9.9"), Set.of()));
    }
}
