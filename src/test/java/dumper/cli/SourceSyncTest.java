package dumper.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceSyncTest {

    @TempDir Path tmpHome;
    @TempDir Path tmpInstall;
    private String savedHome;

    @BeforeEach
    void overrideHome() {
        savedHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpHome.toString());
    }

    @AfterEach
    void restoreHome() {
        System.setProperty("user.home", savedHome);
    }

    private void seedInstall(byte[] jarBytes) throws Exception {
        Files.write(tmpInstall.resolve("SongsOfSyx.jar"), jarBytes);
        Files.createDirectories(tmpInstall.resolve("base"));
        Files.writeString(tmpInstall.resolve("base/data.zip"), "dummy");
    }

    @Test
    void copiesJarAndRecordsChecksum() throws Exception {
        byte[] payload = "fake-jar-bytes".getBytes();
        seedInstall(payload);

        SourceSync.Result r = SourceSync.sync(tmpInstall, /*force=*/ false);

        assertTrue(Files.exists(Paths_.jar()));
        assertEquals(new String(payload), Files.readString(Paths_.jar()));
        assertEquals(VersionChecker.sha256(Paths_.jar()), r.jarSha256());
    }

    @Test
    void skipsCopyWhenChecksumMatches() throws Exception {
        seedInstall("fake-jar".getBytes());

        SourceSync.sync(tmpInstall, false);
        java.nio.file.attribute.FileTime before = Files.getLastModifiedTime(Paths_.jar());

        // Wait a tick so a fresh copy would have a strictly newer mtime.
        Thread.sleep(20);

        SourceSync.sync(tmpInstall, false);
        java.nio.file.attribute.FileTime after = Files.getLastModifiedTime(Paths_.jar());

        assertEquals(before, after, "fast-path: jar not rewritten");
    }

    @Test
    void rewritesJarWhenForced() throws Exception {
        seedInstall("fake-jar".getBytes());

        SourceSync.sync(tmpInstall, false);
        java.nio.file.attribute.FileTime before = Files.getLastModifiedTime(Paths_.jar());

        Thread.sleep(20);
        SourceSync.sync(tmpInstall, /*force=*/ true);
        java.nio.file.attribute.FileTime after = Files.getLastModifiedTime(Paths_.jar());

        assertTrue(after.compareTo(before) > 0, "forced re-copy: mtime advanced");
    }

    @Test
    void symlinksBaseOnPosix() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
            System.getProperty("os.name", "").toLowerCase().contains("win"),
            "POSIX-only test");
        seedInstall("fake-jar".getBytes());

        SourceSync.Result r = SourceSync.sync(tmpInstall, false);

        assertEquals("symlink", r.baseLinkMode());
        assertTrue(Files.isSymbolicLink(Paths_.base()));
        assertEquals(tmpInstall.resolve("base"), Files.readSymbolicLink(Paths_.base()));
    }

    @Test
    void copiesSaveIntoSavesDir() throws Exception {
        Path save = tmpInstall.resolve("Kastra.save");
        Files.writeString(save, "save-bytes");

        Path dst = SourceSync.copySave(save);

        assertEquals(Paths_.savesDir().resolve("Kastra.save"), dst);
        assertEquals("save-bytes", Files.readString(dst));
    }
}
