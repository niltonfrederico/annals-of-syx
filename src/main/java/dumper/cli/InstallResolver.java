package dumper.cli;

import dumper.setup.GameLocator;
import org.jline.reader.LineReader;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the game install path, prompting the user when auto-detection
 * fails. Re-prompts up to 3 times on invalid input.
 *
 * Validation rule (delegated): {@link GameLocator#isValidGameDir(Path)} —
 * directory must contain {@code SongsOfSyx.jar} and {@code base/}.
 */
public final class InstallResolver {

    private static final int MAX_PROMPTS = 3;

    public static Path resolve(LineReader reader, java.io.PrintStream out) {
        Optional<GameLocator.Found> auto = GameLocator.locate();
        if (auto.isPresent()) {
            Path dir = auto.get().dir();
            String line = reader.readLine("Found install at " + dir + ". Use this? [Y/n/other] ").trim();
            if (line.isEmpty() || line.equalsIgnoreCase("y")) return dir;
            if (!line.equalsIgnoreCase("other") && !line.equalsIgnoreCase("n")) {
                // unknown input -> treat as "n", fall through to manual
            }
        }

        for (int attempt = 0; attempt < MAX_PROMPTS; attempt++) {
            String entered = reader.readLine("Path to your Songs of Syx install: ").trim();
            if (entered.isEmpty()) {
                out.println("path cannot be empty.");
                continue;
            }
            Path p = Path.of(expandHome(entered));
            if (GameLocator.isValidGameDir(p)) return p;
            out.println("'" + p + "' isn't a Songs of Syx install (missing SongsOfSyx.jar or base/).");
        }
        throw new SyxAbort("could not resolve a valid install after " + MAX_PROMPTS
            + " attempts. Set SOS_GAME_DIR env var or pass --install <path>.");
    }

    static String expandHome(String s) {
        if (s.startsWith("~/")) return System.getProperty("user.home") + s.substring(1);
        return s;
    }

    private InstallResolver() {}
}
