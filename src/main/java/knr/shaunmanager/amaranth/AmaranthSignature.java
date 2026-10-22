package knr.shaunmanager.amaranth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AmaranthSignature {
    private AmaranthSignature() { }
    public static String login(String token, String serverDate, String transactionId, String path) {
        try {
            byte[] input = (token + serverDate + transactionId + path).getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
    public static String authenticated(String token, String transactionId, String timestamp,
                                       String path, String signKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(
                    (token + transactionId + timestamp + path).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable");
        }
    }
}
