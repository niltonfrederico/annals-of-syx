package dumper.cli;

import dumper.Bootstrap;
import dumper.Json;
import dumper.StatsDumper;
import game.GameSpec;

import java.nio.file.Path;
import java.util.Map;

/**
 * Drives the dump pipeline: load save via {@link Bootstrap#loadSave(Path)},
 * collect stats via {@link StatsDumper#dumpAll}, write JSON via {@link Json}.
 *
 * Caller is responsible for the CWD being correct (engine reads {@code base/}
 * from CWD). The {@code scripts/syx} wrapper handles this.
 */
public final class DumpRunner {

    public static Path run(Path savePath, Path outPath, boolean coverage) throws Exception {
        if (savePath == null) throw new SyxAbort("save path required");
        if (!java.nio.file.Files.isReadable(savePath)) {
            throw new SyxAbort("save not readable: " + savePath);
        }
        Path out = (outPath != null) ? outPath
            : savePath.resolveSibling(savePath.getFileName() + ".json");

        GameSpec spec = Bootstrap.loadSave(savePath);
        Map<String, Object> dump = StatsDumper.dumpAll(spec, savePath, coverage);
        Json.write(dump, out);
        return out;
    }

    private DumpRunner() {}
}
