import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class Crypto {

  private static SecureRandom random = new SecureRandom();

  public static SecretKeySpec generateKey() {
    final String SECRET_KEY = "secret key"; // temp

    byte[] salt = new byte[16]; // random sequence that makes the hash output unique
    random.nextBytes(salt); // fills salt array with random data

    try {
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      KeySpec keySpecifications = new PBEKeySpec(SECRET_KEY.toCharArray(), salt, 65536, 256);
      SecretKey tempSecret = secretKeyFactory.generateSecret(keySpecifications);
      SecretKeySpec secretKey = new SecretKeySpec(tempSecret.getEncoded(), "AES");
      return secretKey;
    } catch (Exception ex) {
      DispAlert.alertException(ex);
    }
    return null;
  }

  public static byte[] getInitVector() {
    byte[] bytesInitVector = new byte[16]; // an array of bytes to hold IV data
    random.nextBytes(bytesInitVector); // fills array with random data
    return bytesInitVector;

  }

  public static String encrypt(String strToEncrypt, SecretKeySpec secretKey, IvParameterSpec initVector) {
    try {

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // creates cipher object with specified algorithim
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, initVector); // initializes the cipher object
      return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8))); // returns
                                                                                                                // encrypted
                                                                                                                // text
    } catch (Exception ex) {
      DispAlert.alertException(ex);
    }
    return null;
  }

  public static String decrypt_with_key(String encrypted, SecretKeySpec secretKey, IvParameterSpec initVector) { // unused will need to be removed
    try {

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, initVector);

      return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    } catch (Exception ex) {

      DispAlert.alertException(ex);
      return null;
    }
  }
  public static byte[] encryptToBytes(byte[] toEncrypt, SecretKeySpec secretKey, IvParameterSpec initVector) {
    try {

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // creates cipher object with specified algorithim
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, initVector); // initializes the cipher object
      return cipher.doFinal(toEncrypt);
                                    
    } catch (Exception ex) {
      DispAlert.alertException(ex);
    }
    return null;
  }

  public static byte[] decryptToBytes(byte[] encrypted, SecretKeySpec secretKey, IvParameterSpec initVector) {
    try {

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, initVector);

      return cipher.doFinal(encrypted);

    } 
    catch (Exception ex) {

      DispAlert.alertException(ex);
      return null;
    }
  }
}