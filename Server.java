//@ver 2.0.1

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.io.*;
import java.security.*;

import javax.crypto.SecretKey;


public class Server extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root = new VBox(8);


  private Button btnReceive = new Button("Receive Connections");
  private Button btnGenerate = new Button("Generate Key");

  private TextArea tArea = new TextArea();

  private TextField tField = new TextField();

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;


  //instantiate Crypto class
  Crypto crypto;
  private PrivateKey privKey;
  private PublicKey pubKey;


  ServerHandler serverHandler;

  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Server");
    
    btnReceive.setOnAction(this);
    btnGenerate.setOnAction(this);

    root.getChildren().addAll(btnReceive,btnGenerate,tField,tArea);

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {      
          System.exit(0);
        }
    });

    scene = new Scene(root, 600, 600); 
                                        
    stage.setScene(scene);              
    stage.show();
    

  }
  
  public void handle(ActionEvent evt) {

    Button btn = (Button)evt.getSource();
    

    switch(btn.getText()) {
        case "Receive Connections":
          generateKey();
          serverHandler = new ServerHandler();
          serverHandler.start();
          btnReceive.setText("Disconnect");
          break;
        case "Generate Key":
          generateKey();
          break;
        case "Disconnect":
          serverHandler.shutdown();
          btnReceive.setText("Receive Connections");
          break;
      }
   }
   
  //generateKey()
  //method to generate a keypair for the server
  //server creates ONE key and distributes to clients
  private void generateKey() {     
      crypto = new Crypto();
      crypto.init();
      privKey =  crypto.getPrivateKey();
      pubKey = crypto.getPublicKey();
      //tArea.appendText("\nKeys Generated" + "\n"+  pubKey + "\n" + privKey); //used for testing, can be removed
  }//end generateKey()
  
  
  public void writeText(String s) {
    Platform.runLater(new Runnable() {
      public void run() {
        tArea.appendText(s+"\n");
      }
    });
    
  }
  
  class ServerHandler extends Thread {

    private ServerSocket ss;
        
    private ArrayList<SocketHandler> activeClients = new ArrayList<SocketHandler>();
    
    private volatile Boolean active = true;

    ObjectOutputStream oos;

    
    public ServerHandler() {

    }
    
    public void run() {
      try {
        ss = new ServerSocket(12345);
        currentThread().setName("ServerHandler");
        while(active) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            SocketHandler socketHandler = new SocketHandler(s);
            socketHandler.start();
            //activeClients.add(socketHandler);
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
            s.setInactive();
            // s.stop(); //need to find a better way to do this
        }
        System.out.println("shutdown");
        activeClients.clear();
        // Thread.currentThread().interrupt();
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    public boolean nameInUse(String name) {
      boolean inUse = false;
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(name)) {
          inUse = true;
        }
      }
      return inUse;
    }
    public void addClient(SocketHandler s) {
      activeClients.add(s);  
    }
    public void setInactiveSocketHandler(SocketHandler _s) {
      activeClients.remove(_s);
      sendActiveClients();
      System.out.println(_s.getClientName()+" Disconnected");

    }
    public void sendActiveClients() {
      ArrayList<String> activeClientsStrings = new ArrayList<String>();
      for (SocketHandler s : activeClients) {
        activeClientsStrings.add(s.getClientName());
      }
      for (SocketHandler s : activeClients) {
        ObjectOutputStream oos = s.getOutputStream();
        try {
          oos.writeObject(crypto.encrypt(new Transaction(s.getName(), "CLIENTS", activeClientsStrings), s.secKey));
        }
        catch (IOException ex) {
          DispAlert.alertException(ex);
        }catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    public void broadcast(String message,SocketHandler sender) {
      for (SocketHandler s : activeClients) {
          if(!s.equals(sender)) {
              ObjectOutputStream oos = s.getOutputStream();

              try {oos.writeObject(crypto.encrypt(new Transaction(sender.getClientName(),"BROADCAST",message), s.secKey));}
              catch (Exception ex) {DispAlert.alertException(ex);} 
          }
        }
    }
    public void broadcast(ArrayList<String> data, String sender) {
      for (SocketHandler s : activeClients) {
          if(!s.getClientName().equals(sender)) {
              ObjectOutputStream oos = s.getOutputStream();

              try {oos.writeObject(crypto.encrypt(new Transaction(sender,"FILE",data), s.secKey));}
              catch (Exception ex) {DispAlert.alertException(ex);} 
          }
        }
    }
    public void broadcastTyping(String sender,boolean typing) { // for broadcasting active typing
      for (SocketHandler s : activeClients) {
        if(!s.getClientName().equals(sender)) {
            ObjectOutputStream oos = s.getOutputStream();
            
            try {
              if (typing) {
                oos.writeObject(crypto.encrypt(new Transaction(sender,"TYPING"), s.secKey));
              }
              else if (!typing) {
                oos.writeObject(crypto.encrypt(new Transaction(sender,"NOT_TYPING"), s.secKey));
              }
            }
            
            catch (Exception ex) {DispAlert.alertException(ex);} 
        }
      }
    }
    public void sendDirect(String sender,String recipient,String message) {
      boolean found = false;
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(recipient)) {
          found = true;
          ObjectOutputStream oos = s.getOutputStream();
          try {
            oos.writeObject(crypto.encrypt(new Transaction(sender,"DIRECT",message,recipient), s.secKey));
          }
          catch (IOException ex) {
            DispAlert.alertException(ex);
          }catch (Exception e) {
            e.printStackTrace();
          }

        }
      }
      if (!found) {
        writeText("Direct message request to unkown recipient: "+recipient);
      }
    }
    public void sendDirectTyping(String sender, String recipient, boolean typing) {
      boolean found = false;
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(recipient)) {
          found = true;
          ObjectOutputStream oos = s.getOutputStream();
          try {
            if (typing) {
              oos.writeObject(crypto.encrypt(new Transaction(sender,"TYPING",recipient), s.secKey));
            }
            else {
              oos.writeObject(crypto.encrypt(new Transaction(sender,"NOT_TYPING",recipient), s.secKey));
            }
          }
          catch (IOException ex) {
            DispAlert.alertException(ex);
          }catch (Exception e) {
            e.printStackTrace();
          }

        }
      }
      if (!found) {
        writeText("Direct message request to unkown recipient: "+recipient);
      }
    }
    class SocketHandler extends Thread {

      private Socket s;
    
      private PublicKey clientPublicKey;
      private SecretKey secKey;
    
      private ObjectInputStream ois;
      private ObjectOutputStream oos;
      private String clientName;
    
      private Boolean active = true;
    
      ServerFileEditHandler fileEditHandler;
        
      public SocketHandler(Socket _s) {
        s = _s;
        try {

          ois = new ObjectInputStream(s.getInputStream());
          oos = new ObjectOutputStream(s.getOutputStream());
          doKeyExchange();
        }
        catch (Exception ex) {
          DispAlert.alertException(ex);
        }
        fileEditHandler = new ServerFileEditHandler();
        fileEditHandler.start();
      }
      public void run() {
    
        writeText("Accepted a connection from "+s.getInetAddress()+":"+s.getPort());
        currentThread().setName("SocketHandler"); // mostly for debugging
        try {
          if (!nameInUse(clientName)) {
            addClient(this);
            sendActiveClients();
            while (active) {
              byte[] incomingBytes = (byte[])ois.readObject();
              byte[] decryptedBytes = (crypto.decrypt(incomingBytes, secKey));
              Transaction t = Transaction.reconstructTransaction(decryptedBytes);
    
              switch (t.getCommand()) {
                case "DIRECT":
                  sendDirect(clientName,t.getRecipient(),t.getMessage());
                  break;
                case "BROADCAST":
                  broadcast(t.getMessage(), this);
                  break;
                case "FILE":
                  fileEditHandler.receiveFile(t);
                  break;
                case "TYPING":
                  if (t.getRecipient().equals("Everyone")) {
                    broadcastTyping(t.getClientName(),true);
                  }
                  else {
                    sendDirectTyping(t.getClientName(),t.getRecipient(),true);
                  }
                  break;
                case "NOT_TYPING":
                  if (t.getRecipient().equals("Everyone")) {
                    broadcastTyping(t.getClientName(),false);
                  }
                  else {
                    sendDirectTyping(t.getClientName(),t.getRecipient(),false);
                  }
                  break;
              }
            }
          }
          else {
            oos.writeObject(crypto.encrypt(new Transaction(clientName, "NAME_IN_USE", clientName), secKey));
          }
        }
        catch (EOFException ex) {
          try {
            s.close();
            setInactiveSocketHandler(this);
          }
          catch (IOException io) {
            DispAlert.alertException(ex);
          }
        }
        catch (Exception ex) {
          DispAlert.alertException(ex);
        }
          // if (!active) {System.out.println("inactive");}
      }
      
      public void doKeyExchange() {
          try {
             //send client a public key
             oos.writeObject(pubKey);
             oos.flush();
             
            //getEncrypted key from client
            byte[] encryptedKey = (byte[])ois.readObject();
            
            //decrypt secret key
            secKey = crypto.decryptKey(encryptedKey, privKey);
            //tArea.appendText("\n" + secKey); //used for testing, can be removed
            
            //decrypt name
            byte[] encryptedName = ((byte[])ois.readObject());
            byte[] clientBytes = crypto.decrypt(encryptedName, secKey);
            Transaction t = Transaction.reconstructTransaction(clientBytes);
            clientName = t.getClientName(); 
             
          }catch (IOException ioe) {
            ioe.printStackTrace();
          }catch (ClassNotFoundException cnfe) {
             cnfe.printStackTrace();
          }catch (Exception e) {
             e.printStackTrace();
          }
      }//end doKeyExchange()
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
      public PublicKey getPublicKey() {
         return pubKey;
      }
      class ServerFileEditHandler extends Thread {
        public void run() {
    
        }
        public void receiveFile(Transaction t) {
          writeText("File from " + t.getClientName());
          broadcast(t.getData(),t.getClientName());
        }
      }
    }
  }
}	
