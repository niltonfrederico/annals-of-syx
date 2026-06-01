package dumper;

import java.io.IOException;
import java.nio.file.Path;

import game.GameSpec;
import settlement.stats.STATS;
import settlement.stats.StatsLoader;
import snake2d.util.file.FileGetter;

/**
 * Reads a SoS .save up to the STATS savable. Does NOT load SETT or the rest of all-map.
 */
public final class SaveReader implements AutoCloseable {

    private final FileGetter fg;
    private final GameSpec spec;

    public SaveReader(Path savePath) throws IOException {
        this.fg = new FileGetter(savePath, true);
        this.spec = GameSpec.get(fg, new String[]{});
    }

    public GameSpec spec() {
        return spec;
    }

    /**
     * Walks the before-map until "STATS", calls stats.load(fg), returns.
     * Throws if STATS not found.
     *
     * Length field in each entry is INCLUSIVE of the int itself
     * (STATS.save writes setAtPosition(pos, getPosition()-pos) where pos is the
     * position of the placeholder int, so length = 4 + payload bytes).
     */
    public void loadStats(STATS stats) throws IOException {
        int n = fg.i();
        for (int i = 0; i < n; i++) {
            String key = fg.chars();
            int pos = fg.getPosition();
            int len = fg.i();
            if ("STATS".equals(key)) {
                StatsLoader.load(stats, fg);
                if (fg.getPosition() != pos + len) {
                    throw new IllegalStateException(
                        "STATS load consumed " + (fg.getPosition() - pos) + " bytes, expected " + len
                    );
                }
                return;
            }
            fg.setPosition(pos + len);
        }
        throw new IllegalStateException("STATS savable not found in before-map");
    }

    @Override
    public void close() {
        fg.close();
    }
}
