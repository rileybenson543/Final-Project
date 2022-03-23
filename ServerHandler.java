import java.net.*;
import java.util.ArrayList;


class ServerHandler extends Thread {

    private ServerSocket ss;
    
    private KeyData keyData;
    
    private ArrayList<SocketHandler> socketHandlers = new ArrayList<SocketHandler>();
    
    private volatile Boolean active = true;
    
    public ServerHandler(KeyData _keyData) {
        keyData = _keyData;
    }
    
    public void run() {
      try {
        ss = new ServerSocket(12345);
        while(active) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            SocketHandler socketHandler = new SocketHandler(s,keyData);
            socketHandler.start();
            socketHandlers.add(socketHandler);
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
        for (SocketHandler s : socketHandlers) {
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
  }