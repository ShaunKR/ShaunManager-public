package knr.shaunmanager.amaranth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AmaranthSignatureTest {
    @Test void independentSha256Vector() {
        assertEquals("0hm3JDQRUZ10H53t5x2moeqnbNrHh0PLjurGoPZBXPA=",
            AmaranthSignature.login("seed", "20260919", "tid", "/proxymgw/proxymgw17A08"));
    }
    @Test void independentHmacVector() {
        assertEquals("cDucAav8x8KMYtoYl7BnsrjtvXDpP/ABaJmnCsZFfJc=",
            AmaranthSignature.authenticated("access-token", "tid", "123", "/path", "key"));
    }
}
