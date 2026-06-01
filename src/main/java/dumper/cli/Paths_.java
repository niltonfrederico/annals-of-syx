package dumper.cli;

import java.nio.file.Path;

/**
 * Storage layout under {@code ~/.syx/}. All paths derived from
 * {@code user.home}, so unit tests can override via system property.
 */
public final class Paths_ {

    public static Path home() {
        return Path.of(System.getProperty("user.home"), ".syx");
    }

    public static Path config()   { return home().resolve("config.json"); }
    public static Path source()   { return home().resolve("source"); }
    public static Path jar()      { return source().resolve("SongsOfSyx.jar"); }
    public static Path base()     { return source().resolve("base"); }
    public static Path savesDir() { return source().resolve("saves"); }
    public static Path lock()     { return home().resolve(".lock"); }

    private Paths_() {}
}
