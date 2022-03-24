import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class ServerHandler extends Thread {

    private ServerSocket ss;
    
    private KeyData keyData;

    private static SecretKeySpec secretKey;
    private static IvParameterSpec initVector;
    
    private static ArrayList<SocketHandler> activeClients = new ArrayList<SocketHandler>();
    
    private volatile Boolean active = true;

    
    public ServerHandler(KeyData _keyData) {
        keyData = _keyData;
    }
    
    public void run() {
      secretKey = keyData.getKey();
      initVector = new IvParameterSpec(keyData.getInitVector());
      try {
        ss = new ServerSocket(12345);
        while(active) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            SocketHandler socketHandler = new SocketHandler(s,keyData);
            socketHandler.start();
            activeClients.add(socketHandler);
        }
      }
      catch (Exception ex) {
        System.out.println("Socket Closed");
      }
    }

    public void shutdown() {
      try {
        active = false;
        ss.close();
        for (SocketHandler s : activeClients) {
            //s.setInactive();
            s.stop(); //need to find a better way to do this
        }
        System.out.println("shutdown");
        Thread.currentThread().interrupt();
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public static void broadcast(String message,SocketHandler sender) {
        for (SocketHandler s : activeClients) {
            if(!s.equals(sender)) {
                ObjectOutputStream oos = s.getOutputStream();
                try {oos.writeObject(Encrypt.encrypt("<"+sender.getSocket().getInetAddress().getHostAddress()+":"+sender.getSocket().getPort()+"> - " + message + "\n",secretKey,initVector));}
                catch (Exception ex) {ex.printStackTrace();} 
            }
        }
    }
  }