package dumper.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Resolves the engine version of a {@code SongsOfSyx.jar} and decides if
 * it is in the supported allowlist.
 *
 * Today only one version is supported. The allowlist lives as a static
 * field rather than a config knob — supporting a new version is a code
 * change anyway (engine API can shift).
 */
public final class VersionChecker {

    /** Manifest attribute we look for. */
    static final Attributes.Name IMPL_VERSION = new Attributes.Name("Implementation-Version");

    public static final Set<String> SUPPORTED_VERSIONS = Set.of("0.70.33");
    // Songs of Syx ships without Implementation-Version in the jar manifest,
    // so the manifest path never matches and we fall back to sha-256. Hashes
    // here map Steam-shipped jars to their game version.
    //   0.70.33 → 5d8a58ca3858dbde6a2a73ba094d7b1043944e24da38b1fb242bc94240cd004f
    public static final Set<String> SUPPORTED_SHA256 = Set.of(
        "5d8a58ca3858dbde6a2a73ba094d7b1043944e24da38b1fb242bc94240cd004f"
    );

    public record Result(String version, String source) {
        /** {@code "manifest"} or {@code "sha256"}. */
        public String source() { return source; }
    }

    public static Result inspect(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest mf = jf.getManifest();
            if (mf != null) {
                String v = mf.getMainAttributes().getValue(IMPL_VERSION);
                if (v != null && !v.isBlank()) {
                    return new Result(v.trim(), "manifest");
                }
            }
        }
        return new Result(sha256(jar), "sha256");
    }

    public static boolean isSupported(Result r, Set<String> versions, Set<String> shas) {
        return switch (r.source()) {
            case "manifest" -> versions.contains(r.version());
            case "sha256"   -> shas.contains(r.version());
            default         -> false;
        };
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder(64);
            for (byte b : dig) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private VersionChecker() {}
}
