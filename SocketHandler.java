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

  ServerFileEditHandler fileEditHandler;
    
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
    fileEditHandler = new ServerFileEditHandler();
    fileEditHandler.start();
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
          String[] parsed = decrypted.split("~");
          if (parsed[0].equals("BROADCAST")) {
            ServerHandler.broadcast(parsed[1],this);
          }
          else if (parsed[0].equals("DIRECT")) {
            ServerHandler.sendDirect(clientName,parsed[1],parsed[2]);
          }
          else if (parsed[0].equals("FILE_LINE")) {
            fileEditHandler.receiveFile(decrypted,this);
          }

          Server.writeText("<"+ clientName +"> " + dataIn); // temperorary for debugging
          Server.writeText("<"+ clientName +"> " + decrypted);
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
  class ServerFileEditHandler extends Thread {
    public void run() {

    }
    public void receiveFile(String line, SocketHandler s) {
      Server.writeText(line);
      ServerHandler.broadcast(line,s);
    }
  }
} 