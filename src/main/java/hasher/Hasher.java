package hasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Hashes strings with the JDK's built-in message digests. */
public final class Hasher {

    /** Every algorithm the app offers, in display order (JCA names). */
    public static final List<String> ALGORITHMS = List.of(
            "MD5", "SHA-1",
            "SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA-512/224", "SHA-512/256",
            "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512");

    /** Checked on first launch: the same set the old CLI printed with --all. */
    public static final List<String> DEFAULTS = List.of("MD5", "SHA-1", "SHA-256", "SHA-512");

    private Hasher() {}

    /** Return the lowercase hex digest of {@code text} (as UTF-8) under {@code algo}. */
    public static String hash(String text, String algo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);
        return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
