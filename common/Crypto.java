package common;
import java.util.Base64;
import javax.crypto.Cipher;
import java.security.*;
import javax.crypto.KeyGenerator;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

//@version 2.2.1

public class Crypto {
   
   private PrivateKey privKey;//RSA encryption 2048 key size 
   private PublicKey pubKey;
   private SecretKey secKey;
   
 
  /**
   * This class handles encryption and decryption for the clients
   * and servers. It uses RSA for asymmetric encryption and AES for the
   * symmetric encryption
   * <p>RSA is used at first but due to limitations of RSA it can't be
   * used to exchange the larger amounts of data so AES symmetric encryption
   * is used. The AES key is exchanged securely via the RSA protocol</p>
   * 
   */
   public void init() {
      try {
         KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
         keyGen.initialize(2048);   
         //instantiate keys
         KeyPair pair = keyGen.generateKeyPair();
         privKey = pair.getPrivate();
         pubKey = pair.getPublic();
         
         KeyGenerator AESgen = KeyGenerator.getInstance("AES");
         AESgen.init(128);
         secKey = AESgen.generateKey();
      
      }catch(NoSuchAlgorithmException nsae) {
         nsae.printStackTrace();
      }
   }
   
   public void genAES() throws Exception{

   }
   
   
   /**
    * Returns the public key
    * @return pubKey
    */
   public PublicKey getPublicKey() {return pubKey;}
   /**
    * Returns the private key
    * @return privKey
    */
   public PrivateKey getPrivateKey() {return privKey;} // may need to be removed, used for testing
   /**
    * Returns the AES secret key
    * @return
    */
   public SecretKey getSecretKey() {return secKey;}
   
   
   /**
    * This method encrypts to a byte[] given the input String and
    * the secret key
    * @param data String of data to be encrypted
    * @param _secKey The AES secret key
    * @return encrypted byte[]
    * @throws Exception
    */
   public byte[] encrypt(String data, SecretKey _secKey) throws Exception{ 
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, _secKey);
      
      byte[] encryptedBytes = aesCipher.doFinal(data.getBytes());
      return encryptedBytes;
   }
   /**
    * This method encrypts a transaction object into a byte[]
    * @param data input Transaction object
    * @param secKey AES secret key
    * @return decrypted byte[]
    * @throws Exception
    */
   public byte[] encrypt(Transaction data, SecretKey secKey) throws Exception{ 
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
      
      byte[] encryptedBytes = aesCipher.doFinal(data.getByteArray());
      return encryptedBytes;
   }
   /**
    * This method encrypts to a byte[] given the input byte[] and
    * the secret key
    * @param data String of data to be encrypted
    * @param secKey The AES secret key
    * @return encrypted byte[]
    * @throws Exception
    */
   public byte[] encrypt(byte[] data, SecretKey secKey) throws Exception{ 
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
      
      byte[] encryptedBytes = aesCipher.doFinal(data);
      return encryptedBytes;
   }
   /**
    * This method uses a cyphertext String input to decrypt 
    * and return a decrypted String
    * @param encryptedData cyphertext String
    * @param _secKey AES secret key
    * @return String of decrypted plain text
    * @throws Exception
    */
   public String decrypt(String encryptedData, SecretKey _secKey) throws Exception{
      byte[] encryptedBytes = decode(encryptedData);
      
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE,_secKey);//create decrypt cipher
      
      byte[] decryptedData = cipher.doFinal(encryptedBytes);
      return new String(decryptedData, "UTF8");
   }
   /**
    * This method decrypts an input byte[] into a 
    * decrypted byte[]
    * @param encryptedData byte[] of encrypted data
    * @param secKey AES secret key
    * @return decrypted byte[]
    * @throws Exception
    */
   public byte[] decrypt(byte[] encryptedData, SecretKey secKey) throws Exception{
      byte[] encryptedBytes = encryptedData;
      
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE,secKey);//create decrypt cipher
      
      byte[] decryptedData = cipher.doFinal(encryptedBytes);
      return decryptedData;
   }

   
   /**
    * This method uses RSA encryption to create an encrypted
    * AES key that will be used for future encryption
    * @param _pubKey RSA public key
    * @return byte[] of encrypted AES key
    * @throws Exception
    */
   public byte[] encryptKey(PublicKey _pubKey) throws Exception{
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            
      cipher.init(Cipher.ENCRYPT_MODE, _pubKey);
      byte[] encryptedKey = cipher.doFinal(secKey.getEncoded());
      return encryptedKey;
      
   }
   
   /**
    * This method decrypts the AES key using RSA so that it can be used
    * encrypt/decrypt data
    * @param encryptedKey encrypted input byte[]
    * @param _privKey RSA private key
    * @return AES SecretKey object
    * @throws Exception
    */
   public SecretKey decryptKey(byte[] encryptedKey, PrivateKey _privKey) throws Exception {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.PRIVATE_KEY, _privKey);
      
      byte[] decryptedKey = cipher.doFinal(encryptedKey);
      SecretKey original = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES"); 
      
      return original;
   }
   //server hands out public key,  
   
   /**
    * Encodes byte[] into string
    * @param data input byte[]
    * @return ouptut String
    */
   public static String encode(byte[] data) {
      return Base64.getEncoder().encodeToString(data);
   }
   
   /**
    * Decodes String into a byte[]
    * @param data input String
    * @return output byte[]
    */
   public static byte[] decode(String data) {
      return Base64.getDecoder().decode(data);
   }
}//end 
   
  
  