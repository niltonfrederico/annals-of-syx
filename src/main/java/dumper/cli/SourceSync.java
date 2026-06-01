package dumper.cli;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Mirrors the user's game install into {@code ~/.syx/source/}. Copies the
 * jar (with checksum caching), links {@code base/}, and copies the chosen
 * save. All operations idempotent: re-running on an unchanged install is
 * a single checksum read.
 */
public final class SourceSync {

    public record Result(String jarSha256, String baseLinkMode) {}

    /**
     * @param force if true, skip the fast-path checksum check and re-copy.
     */
    public static Result sync(Path install, boolean force) throws IOException {
        Files.createDirectories(Paths_.source());

        Path srcJar = install.resolve("SongsOfSyx.jar");
        String installSha = VersionChecker.sha256(srcJar);

        boolean needCopy = force || !Files.exists(Paths_.jar())
            || !VersionChecker.sha256(Paths_.jar()).equals(installSha);

        if (needCopy) {
            copyWithBar(srcJar, Paths_.jar(), "syncing jar");
        }

        String linkMode = linkBase(install.resolve("base"), Paths_.base());

        Files.createDirectories(Paths_.savesDir());
        return new Result(installSha, linkMode);
    }

    static String linkBase(Path src, Path dst) throws IOException {
        if (Files.exists(dst, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            // Already linked or copied — leave as is for idempotency.
            if (Files.isSymbolicLink(dst)) return "symlink";
            return Files.isDirectory(dst) ? "copy" : "unknown";
        }
        try {
            Files.createSymbolicLink(dst, src);
            return "symlink";
        } catch (UnsupportedOperationException | IOException e) {
            // Windows without dev mode lands here; try a directory junction next.
            if (tryWindowsJunction(src, dst)) return "junction";
            recursiveCopy(src, dst);
            return "copy";
        }
    }

    static boolean tryWindowsJunction(Path src, Path dst) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return false;
        try {
            Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                dst.toString(), src.toString())
                .redirectErrorStream(true)
                .start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static void recursiveCopy(Path src, Path dst) throws IOException {
        try (Stream<Path> walk = Files.walk(src)) {
            walk.forEach(p -> {
                try {
                    Path target = dst.resolve(src.relativize(p).toString());
                    if (Files.isDirectory(p)) Files.createDirectories(target);
                    else Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    static void copyWithBar(Path src, Path dst, String label) throws IOException {
        long size = Files.size(src);
        try (ProgressBar bar = new ProgressBarBuilder()
                .setTaskName(label)
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(size)
                .build();
             InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                bar.stepBy(n);
            }
        }
    }

    /** Copy a save under {@code ~/.syx/source/saves/}. Always re-copies. */
    public static Path copySave(Path saveFile) throws IOException {
        Files.createDirectories(Paths_.savesDir());
        Path dst = Paths_.savesDir().resolve(saveFile.getFileName().toString());
        copyWithBar(saveFile, dst, "copying save");
        return dst;
    }

    private SourceSync() {}
}
