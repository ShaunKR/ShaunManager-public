package knr.shaunmanager.amaranth;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LoginConfigCryptoTest {
 private final String key=Base64.getEncoder().encodeToString(new byte[32]);
 @Test void randomNonceAndRoundTrip() {
  byte[] plain="private login data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  byte[] first=LoginConfigCrypto.encrypt(plain,key),second=LoginConfigCrypto.encrypt(plain,key);
  assertFalse(Arrays.equals(first,second));assertArrayEquals(plain,LoginConfigCrypto.decrypt(first,key));
 }
 @Test void rejectsWrongKeyTamperingAndPlaintext() {
  byte[] encrypted=LoginConfigCrypto.encrypt(new byte[]{1,2,3},key);
  byte[] other=new byte[32];other[0]=1;
  assertThrows(AmaranthLoginService.LoginFailure.class,()->LoginConfigCrypto.decrypt(encrypted,Base64.getEncoder().encodeToString(other)));
  encrypted[encrypted.length-1]^=1;
  assertThrows(AmaranthLoginService.LoginFailure.class,()->LoginConfigCrypto.decrypt(encrypted,key));
  assertThrows(AmaranthLoginService.LoginFailure.class,()->LoginConfigCrypto.decrypt(new byte[50],key));
 }
 @Test void requiresValidSeparateKey() {
  assertEquals("CONFIG_KEY_MISSING",assertThrows(AmaranthLoginService.LoginFailure.class,()->LoginConfigCrypto.encrypt(new byte[1],null)).getMessage());
  assertEquals("CONFIG_KEY_INVALID",assertThrows(AmaranthLoginService.LoginFailure.class,()->LoginConfigCrypto.encrypt(new byte[1],"bad")).getMessage());
 }
}
