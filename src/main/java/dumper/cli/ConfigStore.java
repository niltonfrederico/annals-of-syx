package dumper.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reads, writes, and migrates {@code ~/.syx/config.json}. Pure file I/O —
 * no engine, no interactive prompts, safe to unit-test.
 */
public final class ConfigStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config read(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file)) {
            Config c = GSON.fromJson(r, Config.class);
            if (c == null) throw new IOException("empty config: " + file);
            return migrate(c);
        } catch (JsonSyntaxException e) {
            Path backup = file.resolveSibling(file.getFileName().toString()
                + ".broken-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            Files.move(file, backup);
            throw new IOException("config.json corrupted; moved to " + backup
                + ". Re-run syx --reconfigure.", e);
        }
    }

    public static void write(Path file, Config cfg) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(cfg, w);
        }
    }

    /** No-op for v1. Future schema bumps mutate fields here in place. */
    static Config migrate(Config c) {
        if (c.version == 0) c.version = 1;     // pre-v1 files lacked the field
        return c;
    }

    private ConfigStore() {}
}
