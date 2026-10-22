package knr.shaunmanager.amaranth;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Versioned AES-256-GCM envelope. Key is supplied separately from the file. */
public final class LoginConfigCrypto {
    private static final byte[] MAGIC = "SHAUNCFG1".getBytes(StandardCharsets.US_ASCII);
    private LoginConfigCrypto() {}
    private static byte[] key(String encoded) {
        if (encoded == null || encoded.trim().isEmpty())
            throw new AmaranthLoginService.LoginFailure("CONFIG_KEY_MISSING");
        try {
            byte[] key = Base64.getDecoder().decode(encoded.trim());
            if (key.length != 32) throw new IllegalArgumentException();
            return key;
        } catch (IllegalArgumentException ex) {
            throw new AmaranthLoginService.LoginFailure("CONFIG_KEY_INVALID");
        }
    }
    public static byte[] encrypt(byte[] plaintext, String encodedKey) {
        byte[] secret = key(encodedKey);
        try {
            byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret,"AES"), new GCMParameterSpec(128,nonce));
            cipher.updateAAD(MAGIC);
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] envelope = new byte[MAGIC.length + nonce.length + encrypted.length];
            System.arraycopy(MAGIC,0,envelope,0,MAGIC.length);
            System.arraycopy(nonce,0,envelope,MAGIC.length,nonce.length);
            System.arraycopy(encrypted,0,envelope,MAGIC.length+nonce.length,encrypted.length);
            return envelope;
        } catch (java.security.GeneralSecurityException ex) {
            throw new AmaranthLoginService.LoginFailure("CONFIG_ENCRYPT_FAILED");
        } finally { Arrays.fill(secret,(byte)0); }
    }
    public static byte[] decrypt(byte[] envelope, String encodedKey) {
        byte[] secret = key(encodedKey);
        try {
            if(envelope.length < MAGIC.length+12+16 || !Arrays.equals(MAGIC,Arrays.copyOf(envelope,MAGIC.length)))
                throw new java.security.GeneralSecurityException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(secret,"AES"),
                new GCMParameterSpec(128,Arrays.copyOfRange(envelope,MAGIC.length,MAGIC.length+12)));
            cipher.updateAAD(MAGIC);
            return cipher.doFinal(envelope,MAGIC.length+12,envelope.length-MAGIC.length-12);
        } catch (java.security.GeneralSecurityException ex) {
            throw new AmaranthLoginService.LoginFailure("CONFIG_DECRYPT_FAILED");
        } finally { Arrays.fill(secret,(byte)0); }
    }
    public static void main(String[] args) {
        try {
            if(args.length != 2) throw new IllegalArgumentException();
            byte[] plain=Files.readAllBytes(Paths.get(args[0]));
            try {
                byte[] encrypted=encrypt(plain,System.getenv("SHAUN_CONFIG_KEY"));
                byte[] check=decrypt(encrypted,System.getenv("SHAUN_CONFIG_KEY"));
                try { if(!Arrays.equals(plain,check)) throw new IllegalStateException(); }
                finally { Arrays.fill(check,(byte)0); }
                Files.write(Paths.get(args[1]),encrypted,StandardOpenOption.CREATE_NEW);
            } finally { Arrays.fill(plain,(byte)0); }
            System.out.println("Encrypted configuration created and verified.");
        } catch(Exception ex) {
            System.err.println("Configuration encryption failed; check key, paths and output existence.");
            System.exit(1);
        }
    }
}
