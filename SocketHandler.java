import java.io.*;
import java.net.*;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class SocketHandler extends Thread {

  private Socket s;

  private SecretKeySpec secretKey;
  private IvParameterSpec initVector;

  private ObjectInputStream ois;
  private ObjectOutputStream oos;

  private String clientName;

  private Boolean active = true;
    
  public SocketHandler(Socket _s, KeyData _keyData) {
    s = _s;
    KeyData keyData = _keyData;
    initVector = new IvParameterSpec(keyData.getInitVector());
    secretKey = keyData.getKey();
    try {
      ois = new ObjectInputStream(s.getInputStream());
      oos = new ObjectOutputStream(s.getOutputStream());
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  public void run() {

    Server.writeText("Accepted a connection from "+s.getInetAddress()+":"+s.getPort());
    currentThread().setName("SocketHandler"); // mostly for debugging
    try {
      clientName = (String)ois.readObject();
      ServerHandler.sendActiveClients();
      while (active) {
          String dataIn = ois.readObject().toString();

          // reads init vector
          String decrypted = Encrypt.decrypt_with_key(dataIn, secretKey, initVector);

          ServerHandler.broadcast(decrypted,this);

          Server.writeText("<"+s.getInetAddress().getHostAddress()+":"+s.getPort()+"> " + dataIn);
          Server.writeText("<"+s.getInetAddress().getHostAddress()+":"+s.getPort()+"> " + decrypted);
      }
    }
    catch (EOFException ex) {
      try {
        s.close();
        ServerHandler.setInactiveSocketHandler(this);
      }
      catch (IOException io) {
        
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
      // if (!active) {System.out.println("inactive");}
  }
  public void setInactive() {      // Part of a current bug
    active = false;                // involving disconnecting clients
  }

  public ObjectOutputStream getOutputStream() {
    return oos;
  }
  public Socket getSocket() {
    return s;
  }
  public String getClientName() {
    return clientName;
  }

} 