package settlement.stats;

import java.io.IOException;

import snake2d.util.file.FileGetter;

/**
 * Package-private trampoline exposing STATS.load (protected) to dumper.SaveReader.
 * Lives in package settlement.stats so it can invoke the protected loader.
 */
public final class StatsLoader {

    private StatsLoader() {}

    public static void load(STATS stats, FileGetter fg) throws IOException {
        stats.load(fg);
    }
}
