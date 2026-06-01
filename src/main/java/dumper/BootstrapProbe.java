package dumper;

import java.nio.file.Path;

public final class BootstrapProbe {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: BootstrapProbe <save-path>");
            Runtime.getRuntime().halt(2);
        }
        System.out.println("Probing full bootstrap + save load...");
        try {
            Bootstrap.loadSave(Path.of(args[0]));
            System.out.println("SUCCESS: save loaded.");
            // halt skips shutdown hooks; GAME spawns daemon threads (autosaver, sound)
            // that don't terminate cleanly via System.exit.
            Runtime.getRuntime().halt(0);
        } catch (Throwable t) {
            System.out.println("FAIL at: " + t.getClass().getSimpleName());
            t.printStackTrace();
            Runtime.getRuntime().halt(1);
        }
    }
}
