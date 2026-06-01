package dumper.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Locates a *Songs of Syx* install on the host. Standalone utility — not yet
 * wired into the dump pipeline.
 *
 * Resolution order:
 *   1. SOS_GAME_DIR env var (explicit override).
 *   2. Canonical Steam paths for the current OS, including extra Steam
 *      library folders parsed from libraryfolders.vdf.
 *   3. Canonical GOG paths for the current OS.
 *
 * A path is "valid" when it contains both {@code SongsOfSyx.jar} and the
 * {@code base/} directory (which holds {@code data.zip}, {@code locale.zip},
 * {@code icons/}, {@code script/}).
 */
public final class GameLocator {

    public enum Os { LINUX, WINDOWS, MACOS, OTHER }

    public record Found(Path dir, String source) { }

    public static Optional<Found> locate() {
        String envOverride = System.getenv("SOS_GAME_DIR");
        if (envOverride != null && !envOverride.isBlank()) {
            Path p = Paths.get(envOverride);
            if (isValidGameDir(p)) return Optional.of(new Found(p, "SOS_GAME_DIR"));
        }

        for (Path p : candidates(currentOs())) {
            if (isValidGameDir(p)) {
                return Optional.of(new Found(p, p.toString()));
            }
        }
        return Optional.empty();
    }

    public static boolean isValidGameDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return false;
        Path jar = dir.resolve("SongsOfSyx.jar");
        Path base = dir.resolve("base");
        return Files.isRegularFile(jar) && Files.isDirectory(base);
    }

    static Os currentOs() {
        String s = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (s.contains("linux")) return Os.LINUX;
        if (s.contains("win")) return Os.WINDOWS;
        if (s.contains("mac") || s.contains("darwin")) return Os.MACOS;
        return Os.OTHER;
    }

    static List<Path> candidates(Os os) {
        List<Path> out = new ArrayList<>();
        String home = System.getProperty("user.home");
        switch (os) {
            case LINUX -> {
                addAll(out, home,
                    ".local/share/Steam/steamapps/common/Songs of Syx",
                    ".steam/steam/steamapps/common/Songs of Syx",
                    ".steam/root/steamapps/common/Songs of Syx",
                    "GOG Games/Songs of Syx",
                    "Games/songs-of-syx");
                out.addAll(steamLibrarySubdirs(Paths.get(home, ".local/share/Steam/steamapps/libraryfolders.vdf")));
                out.addAll(steamLibrarySubdirs(Paths.get(home, ".steam/steam/steamapps/libraryfolders.vdf")));
            }
            case WINDOWS -> {
                addAll(out, null,
                    "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Songs of Syx",
                    "C:\\Program Files\\Steam\\steamapps\\common\\Songs of Syx",
                    "C:\\GOG Games\\Songs of Syx");
                out.addAll(steamLibrarySubdirs(Paths.get("C:\\Program Files (x86)\\Steam\\steamapps\\libraryfolders.vdf")));
                out.addAll(steamLibrarySubdirs(Paths.get("C:\\Program Files\\Steam\\steamapps\\libraryfolders.vdf")));
            }
            case MACOS -> addAll(out, home,
                "Library/Application Support/Steam/steamapps/common/Songs of Syx",
                "Applications/Songs of Syx.app/Contents/Resources");
            case OTHER -> { /* no candidates */ }
        }
        return out;
    }

    private static void addAll(List<Path> out, String base, String... rels) {
        for (String r : rels) {
            out.add(base == null ? Paths.get(r) : Paths.get(base, r));
        }
    }

    /**
     * Parses a Steam {@code libraryfolders.vdf} file and returns each
     * library's {@code steamapps/common/Songs of Syx} subdir. Tolerant of
     * missing/malformed files — returns empty list on any error.
     */
    static List<Path> steamLibrarySubdirs(Path vdf) {
        if (!Files.isRegularFile(vdf)) return List.of();
        List<Path> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(vdf)) {
                // Lines look like:   "path"    "/mnt/games/SteamLibrary"
                String t = line.trim();
                if (!t.startsWith("\"path\"")) continue;
                int firstQuote = t.indexOf('"', 6);
                if (firstQuote < 0) continue;
                int secondQuote = t.indexOf('"', firstQuote + 1);
                if (secondQuote < 0) continue;
                String libRoot = t.substring(firstQuote + 1, secondQuote);
                out.add(Paths.get(libRoot, "steamapps", "common", "Songs of Syx"));
            }
        } catch (IOException ignored) {
            // tolerant: malformed/unreadable libraryfolders.vdf → no extra paths
        }
        return out;
    }

    /** CLI smoke-test: {@code java dumper.setup.GameLocator} */
    public static void main(String[] args) {
        Os os = currentOs();
        System.out.println("os: " + os);
        System.out.println("candidates:");
        for (Path p : candidates(os)) {
            System.out.println("  " + (isValidGameDir(p) ? "[OK] " : "     ") + p);
        }
        Optional<Found> found = locate();
        if (found.isPresent()) {
            System.out.println("found: " + found.get().dir() + " (via " + found.get().source() + ")");
        } else {
            System.out.println("found: <none>");
        }
        if (Arrays.asList(args).contains("--exit-code")) {
            System.exit(found.isPresent() ? 0 : 1);
        }
    }

    private GameLocator() { }
}
