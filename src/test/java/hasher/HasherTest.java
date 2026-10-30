package hasher;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HasherTest {

    @Test
    void knownDigests() throws Exception {
        assertEquals("5d41402abc4b2a76b9719d911017c592", Hasher.hash("hello", "MD5"));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                Hasher.hash("hello", "SHA-256"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hasher.hash("", "MD5"));
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
                Hasher.hash("", "SHA3-256"));
    }

    @Test
    void everyOfferedAlgorithmIsAvailable() throws Exception {
        for (String algo : Hasher.ALGORITHMS) {
            Hasher.hash("hello", algo);
        }
    }

    @Test
    void unknownAlgorithmThrows() {
        assertThrows(NoSuchAlgorithmException.class, () -> Hasher.hash("hello", "NOPE"));
    }
}
