package dumper.setup;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Locates *Songs of Syx* save files on the host. Standalone utility — not yet
 * wired into the dump pipeline.
 *
 * Resolution order for the saves directory:
 *   1. SOS_SAVE_DIR env var.
 *   2. Canonical user-data path per OS:
 *      - Linux:   ~/.local/share/songsofsyx/saves/saves
 *      - Windows: %APPDATA%\songsofsyx\saves\saves
 *      - macOS:   ~/Library/Application Support/songsofsyx/saves/saves
 */
public final class SaveLocator {

    public record Save(Path file, long mtimeMillis) { }

    public static Optional<Path> savesDir() {
        String env = System.getenv("SOS_SAVE_DIR");
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env);
            if (Files.isDirectory(p)) return Optional.of(p);
        }
        Path def = defaultSavesDir();
        return Files.isDirectory(def) ? Optional.of(def) : Optional.empty();
    }

    static Path defaultSavesDir() {
        String home = System.getProperty("user.home");
        return switch (GameLocator.currentOs()) {
            case LINUX -> Paths.get(home, ".local/share/songsofsyx/saves/saves");
            case WINDOWS -> {
                String appdata = System.getenv("APPDATA");
                yield appdata != null
                    ? Paths.get(appdata, "songsofsyx", "saves", "saves")
                    : Paths.get(home, "AppData", "Roaming", "songsofsyx", "saves", "saves");
            }
            case MACOS -> Paths.get(home, "Library/Application Support/songsofsyx/saves/saves");
            case OTHER -> Paths.get(home, ".songsofsyx", "saves", "saves");
        };
    }

    /** All {@code *.save} files under {@link #savesDir()}, newest first. */
    public static List<Save> listSaves() {
        return savesDir().map(SaveLocator::listSaves).orElse(List.of());
    }

    public static List<Save> listSaves(Path dir) {
        List<Save> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.save")) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) continue;
                out.add(new Save(p, Files.getLastModifiedTime(p).toMillis()));
            }
        } catch (IOException ignored) {
            // tolerant: unreadable dir → empty list
        }
        out.sort(Comparator.comparingLong(Save::mtimeMillis).reversed());
        return out;
    }

    /** Most-recent save by mtime under {@link #savesDir()}, if any. */
    public static Optional<Save> latest() {
        List<Save> all = listSaves();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /** CLI smoke-test: {@code java dumper.setup.SaveLocator} */
    public static void main(String[] args) {
        Optional<Path> dir = savesDir();
        System.out.println("saves dir: " + dir.map(Path::toString).orElse("<not found>"));
        List<Save> saves = listSaves();
        if (saves.isEmpty()) {
            System.out.println("no saves found");
            return;
        }
        System.out.println("saves (newest first):");
        for (Save s : saves) {
            System.out.printf(Locale.ROOT, "  %tF %tT  %s%n", s.mtimeMillis(), s.mtimeMillis(), s.file());
        }
    }

    private SaveLocator() { }
}
