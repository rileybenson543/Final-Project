import java.util.Base64;
import javax.crypto.Cipher;
import java.security.*;
import javax.crypto.KeyGenerator;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

//@version 2.0.1

public class Crypto {
   
   private PrivateKey privKey;//RSA encryption 2048 key size 
   private PublicKey pubKey;
   private SecretKey secKey;
   
 
  
  public Crypto() { //constructor to initialize and create key pair

 }//end constructor
   
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
         System.out.print(secKey);
      
      }catch(NoSuchAlgorithmException nsae) {
         nsae.printStackTrace();
      }
   }
   
   public void genAES() throws Exception{

   }
   
   
   //accessor methods
   public PublicKey getPublicKey() {return pubKey;}
   public PrivateKey getPrivateKey() {return privKey;} // may need to be removed, used for testing
   public SecretKey getSecretKey() {return secKey;}
   
   
   //encrypt
   //returns an encrypted byte array using AES symmetric key
   public byte[] encrypt(String data, SecretKey _secKey) throws Exception{ 
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, _secKey);
      
      byte[] encryptedBytes = aesCipher.doFinal(data.getBytes());
      return encryptedBytes;
   }
   //encrypt
   //returns an encrypted byte array using AES symmetric key
   public byte[] encrypt(Transaction data, SecretKey secKey) throws Exception{ 
      Cipher aesCipher = Cipher.getInstance("AES");
      aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
      
      byte[] encryptedBytes = aesCipher.doFinal(data.getByteArray());
      return encryptedBytes;
   }
   
   //decrypt
   //method to convert input ciphertext into plain text\
   //@param - encryptedData - data recieved in ciphertext by client or server
   public byte[] decrypt(byte[] encryptedData, SecretKey secKey) throws Exception{
      byte[] encryptedBytes = encryptedData;
      
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE,secKey);//create decrypt cipher
      
      byte[] decryptedData = cipher.doFinal(encryptedBytes);
      return decryptedData;
   }
   
   //decrypt
   //method to convert input ciphertext into plain text\
   //@param - encryptedData - data recieved in ciphertext by client or server   
   public String decrypt(String encryptedData, SecretKey _secKey) throws Exception{
      byte[] encryptedBytes = decode(encryptedData);
      
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE,_secKey);//create decrypt cipher
      
      byte[] decryptedData = cipher.doFinal(encryptedBytes);
      return new String(decryptedData, "UTF8");
   }
   
   
   //encryptKey()
   //returns the encrypted AES symmetric key
   public byte[] encryptKey(PublicKey _pubKey) throws Exception{
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      
      
      cipher.init(Cipher.ENCRYPT_MODE, _pubKey);
      byte[] encryptedKey = cipher.doFinal(secKey.getEncoded());
      System.out.print(secKey.getEncoded());
      return encryptedKey;
      
   }
   
   
   //decryptKey()
   //returns the decrypted AES symmetric key
   public SecretKey decryptKey(byte[] encryptedKey, PrivateKey _privKey) throws Exception {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.PRIVATE_KEY, _privKey);
      
      byte[] decryptedKey = cipher.doFinal(encryptedKey);
      SecretKey original = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES"); 
      
      return original;
   }
   
   //server hands out public key, 
     
  
   
   //encode
   public static String encode(byte[] data) {
      return Base64.getEncoder().encodeToString(data);
   }
   
   
   //decode
   public static byte[] decode(String data) {
      return Base64.getDecoder().decode(data);
   }
}//end 
   
  
  