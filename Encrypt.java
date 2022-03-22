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



public class Encrypt {

    private static final String SECRET_KEY = "secret key";
    private static byte[] salt; // random sequence that makes the hash output unique
    private static SecureRandom random;

    public static SecretKeySpec generateKey() {
        random = new SecureRandom(); // creating random generator
		
		salt = new byte[16]; // byte array to hold salt
		random.nextBytes(salt); // fills salt array with random data

        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpecifications = new PBEKeySpec(SECRET_KEY.toCharArray(), salt, 65536, 256);
            SecretKey tempSecret = secretKeyFactory.generateSecret(keySpecifications);
            SecretKeySpec secretKey = new SecretKeySpec(tempSecret.getEncoded(), "AES");
            return secretKey;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    public static byte[] getInitVector() {
        byte[] bytesInitVector = new byte[16]; // an array of bytes to hold IV data
	    random.nextBytes(bytesInitVector); // fills array with random data
	    // IvParameterSpec initVector = new IvParameterSpec(bytesInitVector);
        return bytesInitVector;

    }
    public static String encrypt(String strToEncrypt,SecretKeySpec secretKey, IvParameterSpec initVector) {
		try {

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // creates cipher object with specified algorithim
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, initVector); // initializes the cipher object
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8))); // returns encrypted text
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}
	public static String decrypt_with_key(String encrypted, SecretKeySpec secretKey, IvParameterSpec initVector) {
		try {

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, initVector);

			return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
		}
		catch (Exception ex) {

			ex.printStackTrace();
			return null;
		}
	}

}