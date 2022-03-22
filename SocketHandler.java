import java.io.*;
import java.net.*;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class SocketHandler extends Thread {

  Socket s;

  SecretKeySpec secretKey;
  IvParameterSpec initVector;

  ObjectInputStream ois;
  ObjectOutputStream oos;
    
  public SocketHandler(Socket _s, KeyData _keyData) {
      s = _s;
      KeyData keyData = _keyData;
      initVector = new IvParameterSpec(keyData.getInitVector());
      secretKey = keyData.getKey();
      try {
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
  }
  public void run() {

    System.out.println("Connect");
    try {
      while(true) {
        String dataIn = ois.readObject().toString();
        Main.writeText(dataIn);
        Main.writeText(Encrypt.decrypt_with_key(dataIn, secretKey, initVector));
        }
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  } 