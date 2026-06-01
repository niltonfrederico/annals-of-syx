package dumper.cli;

/**
 * On-disk shape of {@code ~/.syx/config.json}. Mutable plain Java fields
 * so Gson can populate via reflection without boilerplate.
 *
 * Schema version 1.
 */
public final class Config {

    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public String gameInstallPath;
    public String cachedJarSha256;
    public String supportedGameVersion;
    public String defaultSave;
    public String defaultOutputDir;
    public String baseLinkMode;        // "symlink" | "junction" | "copy"

    public Config() {}
}
