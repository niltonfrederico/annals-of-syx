package dumper.cli;

import dumper.setup.GameLocator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

@Command(
    name = "syx",
    mixinStandardHelpOptions = true,
    version = "syx 0.1.0",
    description = "Dumps Songs of Syx save data to JSON via a local source mirror.")
public final class Syx implements Callable<Integer> {

    @Option(names = "--reselect", description = "Ignore defaultSave and re-pick.")
    boolean reselect;

    @Option(names = "--resync", description = "Force re-copy of the jar.")
    boolean resync;

    @Option(names = "--reconfigure", description = "Wipe config and rerun first-run.")
    boolean reconfigure;

    @Option(names = {"-o", "--output"}, description = "Override output path for this run.")
    Path outputOverride;

    @Option(names = "--install", description = "Skip auto-detect, use this install dir.")
    Path installOverride;

    @Option(names = "--save", description = "Skip prompt, use this save name (no '.save' suffix).")
    String saveOverride;

    @Option(names = "--coverage", description = "Pass through to StatsDumper.")
    boolean coverage;

    @Option(names = "--verbose", description = "Print resolved paths and timings.")
    boolean verbose;

    public static void main(String[] args) {
        int code = new CommandLine(new Syx()).execute(args);
        System.exit(code);
    }

    @Override
    public Integer call() throws Exception {
        try (FileLock ignored = acquireLock()) {
            return run();
        } catch (SyxAbort e) {
            System.err.println("syx: " + e.getMessage());
            return 2;
        }
    }

    private Integer run() throws Exception {
        Files.createDirectories(Paths_.home());

        if (reconfigure && Files.exists(Paths_.config())) Files.delete(Paths_.config());

        Config cfg;
        Path install;
        if (!Files.exists(Paths_.config())) {
            welcome();
            install = (installOverride != null) ? installOverride
                : InstallResolver.resolve(lineReader(), System.out);
            cfg = new Config();
            cfg.gameInstallPath = install.toString();
        } else {
            cfg = ConfigStore.read(Paths_.config());
            install = (installOverride != null) ? installOverride
                : Path.of(cfg.gameInstallPath);
            if (!GameLocator.isValidGameDir(install)) {
                throw new SyxAbort("recorded install path is no longer valid: " + install
                    + ". Run with --reconfigure.");
            }
        }

        VersionChecker.Result vr = VersionChecker.inspect(install.resolve("SongsOfSyx.jar"));
        if (!VersionChecker.isSupported(vr, VersionChecker.SUPPORTED_VERSIONS, VersionChecker.SUPPORTED_SHA256)) {
            throw new SyxAbort("detected version " + vr.version() + " (" + vr.source()
                + "), supported: " + VersionChecker.SUPPORTED_VERSIONS
                + ". Open an issue: https://github.com/niltonfrederico/annals-of-syx/issues");
        }
        cfg.supportedGameVersion = "manifest".equals(vr.source()) ? vr.version() : cfg.supportedGameVersion;
        if (verbose) System.out.println("version " + vr.version() + " via " + vr.source());

        SourceSync.Result sr = SourceSync.sync(install, resync);
        cfg.cachedJarSha256 = sr.jarSha256();
        cfg.baseLinkMode = sr.baseLinkMode();
        if (verbose) System.out.println("source up to date (" + sr.baseLinkMode() + ")");

        Path savePath = pickSave(cfg);
        Path mirroredSave = SourceSync.copySave(savePath);

        Path outDir = outputDir(cfg);
        Path outPath = (outputOverride != null) ? outputOverride
            : outDir.resolve(savePath.getFileName().toString().replaceFirst("\\.save$", "") + ".json");

        Path written = DumpRunner.run(mirroredSave, outPath, coverage);

        ConfigStore.write(Paths_.config(), cfg);
        System.out.println("done. output: " + written + " (" + Files.size(written) + " bytes)");
        return 0;
    }

    private Path pickSave(Config cfg) throws IOException {
        if (saveOverride != null) {
            return dumper.setup.SaveLocator.listSaves().stream()
                .filter(s -> s.file().getFileName().toString().startsWith(saveOverride))
                .findFirst()
                .orElseThrow(() -> new SyxAbort("no save matches --save '" + saveOverride + "'"))
                .file();
        }

        if (!reselect && cfg.defaultSave != null) {
            String line = lineReader().readLine("Use saved default '" + cfg.defaultSave + "'? [Y/n] ").trim();
            if (line.isEmpty() || line.equalsIgnoreCase("y")) {
                return dumper.setup.SaveLocator.listSaves().stream()
                    .filter(s -> s.file().getFileName().toString().startsWith(cfg.defaultSave))
                    .findFirst()
                    .orElseThrow(() -> new SyxAbort("default save '" + cfg.defaultSave + "' no longer in game saves dir"))
                    .file();
            }
        }

        Terminal terminal = SavePicker.terminal();
        Path chosen = SavePicker.pick(terminal, System.out);
        cfg.defaultSave = chosen.getFileName().toString().replaceFirst("\\.save$", "");
        return chosen;
    }

    private Path outputDir(Config cfg) {
        if (cfg.defaultOutputDir != null && !cfg.defaultOutputDir.isBlank()) {
            return Path.of(cfg.defaultOutputDir);
        }
        // SYX_INVOKE_CWD is set by scripts/syx before it `cd`s into ~/.syx/source/.
        String invoked = System.getenv("SYX_INVOKE_CWD");
        return (invoked != null) ? Path.of(invoked) : Path.of(System.getProperty("user.dir"));
    }

    private LineReader lineReader() throws IOException {
        return LineReaderBuilder.builder().terminal(SavePicker.terminal()).build();
    }

    private FileLock acquireLock() throws IOException {
        Files.createDirectories(Paths_.home());
        FileChannel ch = FileChannel.open(Paths_.lock(),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock = ch.tryLock();
        if (lock == null) {
            throw new SyxAbort("another syx run is in progress (lock held on " + Paths_.lock() + ")");
        }
        return lock;
    }

    private void welcome() {
        System.out.println("syx first run.");
        System.out.println("Setting up a local source mirror at " + Paths_.source() + " so we");
        System.out.println("never touch your game install. This takes ~30s on first run.");
    }
}
