package dumper.cli;

import dumper.setup.SaveLocator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Interactive save selector. Lists saves via {@link SaveLocator}, presents
 * an arrow-key menu via JLine, falls back to a numbered prompt on non-TTY.
 *
 * Returns the chosen save's {@link Path} on the host (NOT the mirrored
 * path under {@code ~/.syx/source/saves/}). Caller copies via
 * {@link SourceSync#copySave(Path)}.
 */
public final class SavePicker {

    public static Path pick(Terminal terminal, java.io.PrintStream out) throws IOException {
        List<SaveLocator.Save> saves = SaveLocator.listSaves();
        if (saves.isEmpty()) {
            throw new SyxAbort("no saves found in "
                + SaveLocator.savesDir().map(Path::toString).orElse("<unknown>")
                + ". Play the game, save first, then rerun syx.");
        }

        if (terminal == null || !terminal.getType().equals(Terminal.TYPE_DUMB)) {
            return ttyMenu(terminal, out, saves);
        }
        return numberedFallback(saves, out);
    }

    static Path ttyMenu(Terminal terminal, java.io.PrintStream out, List<SaveLocator.Save> saves) {
        // JLine 3 does not ship a stock arrow-key list. We render manually:
        // print all options, read a line; user types index or partial name.
        // Trade-off: this is a numbered menu with JLine's line editing
        // (history, completion). Genuine arrow-key list would need
        // raw-mode handling we do not need for v1.
        out.println("Select save to analyze:");
        printList(saves, out);
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        for (int attempt = 0; attempt < 3; attempt++) {
            String line = reader.readLine("# ").trim();
            int idx = parseIndex(line, saves.size());
            if (idx >= 0) return saves.get(idx).file();
            out.println("enter a number between 1 and " + saves.size());
        }
        throw new SyxAbort("save selection aborted after 3 invalid inputs");
    }

    static Path numberedFallback(List<SaveLocator.Save> saves, java.io.PrintStream out) {
        // JLine falls back to dumb mode under xvfb-run even though stdin is a
        // real TTY. Read directly from System.in so the prompt still works;
        // only abort if stdin is closed (piped/CI with no input).
        out.println("Select save to analyze (number, or set --save <name> to skip):");
        printList(saves, out);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        for (int attempt = 0; attempt < 3; attempt++) {
            out.print("# ");
            out.flush();
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                throw new SyxAbort("failed to read save selection: " + e.getMessage());
            }
            if (line == null) {
                throw new SyxAbort("no input available. Pass --save <name> for non-interactive use.");
            }
            int idx = parseIndex(line.trim(), saves.size());
            if (idx >= 0) return saves.get(idx).file();
            out.println("enter a number between 1 and " + saves.size());
        }
        throw new SyxAbort("save selection aborted after 3 invalid inputs");
    }

    static void printList(List<SaveLocator.Save> saves, java.io.PrintStream out) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        for (int i = 0; i < saves.size(); i++) {
            SaveLocator.Save s = saves.get(i);
            out.printf("  [%d] %-30s  %s%n", i + 1,
                s.file().getFileName().toString(),
                fmt.format(new Date(s.mtimeMillis())));
        }
    }

    static int parseIndex(String input, int size) {
        try {
            int n = Integer.parseInt(input);
            return (n >= 1 && n <= size) ? n - 1 : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Build a JLine terminal (system terminal if available, dumb otherwise). */
    public static Terminal terminal() throws IOException {
        return TerminalBuilder.builder().system(true).dumb(true).build();
    }

    private SavePicker() {}
}
