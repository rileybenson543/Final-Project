import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {

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