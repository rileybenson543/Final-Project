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

  Boolean active = true;
    
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

    Server.writeText("Accepted a connection from "+s.getInetAddress()+":"+s.getPort());
    while(active) {
      try {
          String dataIn = ois.readObject().toString();
          Server.writeText("<"+s.getInetAddress().getHostAddress()+":"+s.getPort()+"> " + dataIn);
          Server.writeText("<"+s.getInetAddress().getHostAddress()+":"+s.getPort()+"> " + Encrypt.decrypt_with_key(dataIn, secretKey, initVector));
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
      if (!active) {System.out.println("inactive");}
    }
  public void setInactive() {
    active = false;
  }
} 