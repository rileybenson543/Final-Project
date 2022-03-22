import java.net.*;


class ServerHandler extends Thread {

    ServerSocket ss;

    KeyData keyData;

    public ServerHandler(KeyData _keyData) {
        keyData = _keyData;
    }

    public void run() {
      try {
        ss = new ServerSocket(12345);
        while(true) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            new SocketHandler(s,keyData).start();
        }
      }
      catch (Exception ex) {
        System.out.println("Socket Closed");
      }
    }
    public void shutdown() {
      try {
        ss.close();
        
        System.out.println("shutdown");
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }