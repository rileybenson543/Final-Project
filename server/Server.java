package server;
//@ver 2.2.1

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.security.*;

import javax.crypto.SecretKey;

import common.Compression;
import common.Crypto;
import common.DispAlert;
import common.Group;
import common.Transaction;

/**
 * Server class that facilitates client connections 
 * and handles routing messages to the intended recipients
 * @author Riley Basile-Benson, Mark Stubble
 */
public class Server extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root = new VBox(8);

  private BorderPane bpControlBar = new BorderPane();

  private Button btnReceive = new Button("Receive Connections");
  private Button btnGenerate = new Button("Generate Key");

  private TextArea tArea = new TextArea();

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;

  //instantiate Crypto class
  Crypto crypto;
  private PrivateKey privKey;
  private PublicKey pubKey;

  ServerHandler serverHandler;
  Compression comp;

  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Server");
    
    btnReceive.setOnAction(this);
    btnGenerate.setOnAction(this);

    tArea.setPrefRowCount(50);
    bpControlBar.setLeft(btnReceive);
    bpControlBar.setRight(btnGenerate);
    bpControlBar.setPadding(new Insets(16));

    root.getChildren().addAll(bpControlBar,tArea);

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {      
          System.exit(0);
        }
    });

    scene = new Scene(root, 600, 600); 
    scene.getStylesheets().add
    (Server.class.getResource("/common/styles.css").toExternalForm());
                                        
    stage.setScene(scene);              
    stage.show();
    // btnConnect.setStyle("-fx-background-color: black");
    
    comp = new Compression();
  }
  /**
   * Handles the button presses
   */
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
   
  /**
   * Generates a key pair using the Crypto class
   * The server creates one public and distributes
   * to the clients
   */
  private void generateKey() {     
      crypto = new Crypto();
      crypto.init();
      privKey =  crypto.getPrivateKey();
      pubKey = crypto.getPublicKey();
      
  }//end generateKey()
  
  /**
   * Allows writing text to the text area
   * from other threads and ensuring it executes 
   * on main thread
   * @param s text to write
   */
  public void writeText(String s) {
    Platform.runLater(new Runnable() {
      public void run() {
        tArea.appendText(s+"\n");
      }
    });
    
  }
  /**
   * Class that runs in seperate thread and handles 
   * accepting new client connections
   * It also interacts with the clients by 
   * allowing direct messages or broadcasting
   */
  class ServerHandler extends Thread {

    private ServerSocket ss;
        
    private ArrayList<SocketHandler> activeClients = new ArrayList<SocketHandler>();
    private HashMap<String,Group> groups = new HashMap<String,Group>();
    
    private volatile Boolean active = true;

    ObjectOutputStream oos;
    
    /**
     * Infinite loop that waits for a new
     * client connection and creates a socket handler
     */
    public void run() {
      try {
        ss = new ServerSocket(12345);
        currentThread().setName("ServerHandler");
        writeText("Ready for connections");
        while(active) {
            Socket s = ss.accept();
            SocketHandler socketHandler = new SocketHandler(s);
            socketHandler.start();
        }
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    /**
     * Handles shutting down the server
     * It will disconnect all clients and
     * clear the active client lists
     */
    public void shutdown() {
      try {
        active = false;
        ss.close();
        for (SocketHandler s : activeClients) {
            s.setInactive();
        }
        activeClients.clear();
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    /**
     * Adds a client to the list
     * @param s
     */
    public void addClient(SocketHandler s) {
      activeClients.add(s);  
    }
    /**
     * Sets a client as inactive by removing it from the
     * list and updating all other clients
     * @param _s the client to remove
     */
    public void setInactiveSocketHandler(SocketHandler _s) {
      activeClients.remove(_s);
      sendActiveClients();
      writeText(_s.getClientName()+" Disconnected");
    }
    /**
     * Sends all the clients an ArrayList<String>
     * of all the active users
     */
    public void sendActiveClients() {
      ArrayList<String> activeClientsStrings = new ArrayList<String>();
      for (SocketHandler s : activeClients) {
        activeClientsStrings.add(s.getClientName());
      }
      for (SocketHandler s : activeClients) {
        ObjectOutputStream oos = s.getOutputStream();
        try {
          oos.writeObject(crypto.encrypt(comp.compress(new Transaction(s.getName(), "CLIENTS", activeClientsStrings).getByteArray()), s.secKey));
        }
        catch (IOException ex) {
          DispAlert.alertException(ex);
        }catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    /**
     * Sends the clients the new group 
     * that was made so they have access
     * @param g the Group object
     */
    public void sendNewGroup(Group g) {
      for (SocketHandler s : activeClients) {
        ObjectOutputStream oos = s.getOutputStream();
        try {
          oos.writeObject(crypto.encrypt(comp.compress(new Transaction("NEW_GROUP", g).getByteArray()), s.secKey));
        }
        catch (IOException ex) {
          DispAlert.alertException(ex);
        }catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void editGroup(Transaction t) {
      for (SocketHandler s : activeClients) {
        if (!s.getClientName().equals(t.getClientName())) {
          ObjectOutputStream oos = s.getOutputStream();
          try {
            oos.writeObject(crypto.encrypt(comp.compress(t.getByteArray()), s.secKey));
          }
          catch (IOException ex) {
            DispAlert.alertException(ex);
          }catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      groups.remove(t.getOldGroup().getGroupName()); // remove old group 
      groups.put(t.getNewGroup().getGroupName(),t.getNewGroup()); // insert the new one
    }
    /**
     * Sends the client all of the active groups
     * @param s the person to send to
     */
    public void sendAllGroups(SocketHandler s) {
      ObjectOutputStream oos = s.getOutputStream();
      try {
        oos.writeObject(crypto.encrypt(comp.compress(new Transaction("NEW_GROUP", groups).getByteArray()), s.secKey));
      }
      catch (IOException ex) {
        DispAlert.alertException(ex);
      }catch (Exception e) {
        e.printStackTrace();
      }
    }
    /**
     * Sends a String message to all users
     * @param message
     * @param sender
     */
    public void broadcast(String message,SocketHandler sender) {
      for (SocketHandler s : activeClients) {
        if(!s.equals(sender)) {
            ObjectOutputStream oos = s.getOutputStream();

            try {
              oos.writeObject(crypto.encrypt(
                comp.compress(
                  new Transaction(sender.getClientName(),"BROADCAST",message).getByteArray()), s.secKey));}
            catch (Exception ex) {DispAlert.alertException(ex);} 
        }
      }
    }
    /**
     * Sends data to all clients
     * @param data ArrayList<String>
     * @param sender
     */
    public void broadcast(ArrayList<String> data, String sender) {
      for (SocketHandler s : activeClients) {
          if(!s.getClientName().equals(sender)) {
              ObjectOutputStream oos = s.getOutputStream();

              try {oos.writeObject(crypto.encrypt(
                comp.compress(new Transaction(sender,"FILE",data).getByteArray()), s.secKey));}
              catch (Exception ex) {DispAlert.alertException(ex);} 
          }
        }
    }
    /**
     * Sends an active typing to signal
     * to all active clients
     * @param sender
     * @param typing
     */
    public void broadcastTyping(String sender,boolean typing) { // for broadcasting active typing
      for (SocketHandler s : activeClients) {
        if(!s.getClientName().equals(sender)) {
            ObjectOutputStream oos = s.getOutputStream();
            
            try {
              if (typing) {
                oos.writeObject(crypto.encrypt(
                  comp.compress(new Transaction(sender,"TYPING","","Main").getByteArray()), s.secKey));
              }
              else if (!typing) {
                oos.writeObject(crypto.encrypt(
                  comp.compress(new Transaction(sender,"NOT_TYPING","","Main").getByteArray()), s.secKey));
              }
            }
            
            catch (Exception ex) {DispAlert.alertException(ex);} 
        }
      }
    }
    /**
     * Sends a given message to one client
     * @param sender
     * @param recipient
     * @param message
     */
    public void sendDirect(String sender,String recipient,String message) {
      boolean found = false;
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(recipient)) {
          found = true;
          ObjectOutputStream oos = s.getOutputStream();
          try {
            oos.writeObject(crypto.encrypt(
              comp.compress(new Transaction(sender,"DIRECT",message,recipient).getByteArray()), s.secKey));
            oos.flush();
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
    /**
     * Sends an active typing signal directly to one person
     * @param sender
     * @param recipient
     * @param typing boolean
     */
    public void sendDirectTyping(String sender, String recipient, boolean typing) {
      boolean found = false;
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(recipient)) {
          found = true;
          ObjectOutputStream oos = s.getOutputStream();
          try {
            if (typing) {
              oos.writeObject(crypto.encrypt(
                comp.compress(
                  new Transaction(sender,"TYPING",recipient).getByteArray()), s.secKey));
            }
            else {
              oos.writeObject(crypto.encrypt(
                comp.compress(new Transaction(sender,"NOT_TYPING",recipient).getByteArray()), s.secKey));
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

    public void sendTypingGroup(Transaction t, boolean typing) {
      if (groups.containsKey(t.getRecipient())) {
        Group g = groups.get(t.getRecipient());
        for (String s : g.getGroupMembers()) {
          for (SocketHandler sh : activeClients) {
            if (s.equals(sh.getClientName()) && !sh.getClientName().equals(t.getClientName())) {
              ObjectOutputStream oos = sh.getOutputStream();
              try {
                if (typing) {
                  oos.writeObject(crypto.encrypt(
                    comp.compress(
                      new Transaction(t.getClientName(),"TYPING","",t.getRecipient()).getByteArray()), sh.secKey));
                }
                else {
                  oos.writeObject(crypto.encrypt(
                    comp.compress(
                      new Transaction(t.getClientName(),"NOT_TYPING","",t.getRecipient()).getByteArray()), sh.secKey));
                }
              }
              catch (Exception ex) {
                DispAlert.alertException(ex);
              }
            }
          }
        }
      }
    }
    /**
     * Sends a message to memebers of a specifc group
     * @param sender 
     * @param group
     * @param message
     */
    public void sendToGroup(String sender, Group group, String message) {
      ArrayList<SocketHandler> groupSockets = new ArrayList<SocketHandler>();
      for (String s : group.getGroupMembers()) { //iterate through all the group members
        for (SocketHandler sh : activeClients) {
          if (sh.getClientName().equals(s)) {
            groupSockets.add(sh);
          }
        }
      }
      for (SocketHandler s : groupSockets) {
        try {
          if (!s.getClientName().equals(sender)) {
            ObjectOutputStream oos = s.getOutputStream();
            oos.writeObject(crypto.encrypt(
              comp.compress(
                new Transaction(sender,"GROUP_MESSAGE",message,group.getGroupName()).getByteArray()), s.secKey));
            oos.flush();
            //oos.close();
          }
        }
        catch (Exception ex) {
          DispAlert.alertException(ex);
        }
      }
    }
    /**
     * Determines whether a name is already
     * in use by another client or group
     * and returns a boolean value
     * @param name the name to check
     * @return boolean
     */
    public boolean validateName(String name) {
      if (groups.keySet().contains(name)) {
        return false;
      }
      for (SocketHandler s : activeClients) {
        if (s.getClientName().equals(name)) {
          return false;
        }
      }
      return true;
      
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
          if (validateName(clientName)) {
            addClient(this);
            sendActiveClients();
            while (active) {
              byte[] incomingBytes = (byte[])ois.readObject();
              byte[] decryptedBytes = (crypto.decrypt(incomingBytes, secKey));
              Transaction t = Transaction.reconstructTransaction(comp.decompress(decryptedBytes));
    
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
                  if (t.getRecipient().equals("Main")) {
                    broadcastTyping(t.getClientName(),true);
                  }
                  else if (groups.keySet().contains(t.getRecipient())) {
                    sendTypingGroup(t,true);
                  }
                  else {
                    sendDirectTyping(t.getClientName(),t.getRecipient(),true);
                  }
                  break;
                case "NOT_TYPING":
                  if (t.getRecipient().equals("Main")) {
                    broadcastTyping(t.getClientName(),false);
                  }
                  else if (groups.keySet().contains(t.getRecipient())) {
                    sendTypingGroup(t,false);
                  }
                  else {
                    sendDirectTyping(t.getClientName(),t.getRecipient(),false);
                  }
                  break;
                case "NEW_GROUP":
                  if (validateName(t.getGroup().getGroupName())) {
                    groups.put(t.getGroup().getGroupName(),t.getGroup());
                    oos.writeObject(crypto.encrypt(
                      comp.compress(
                        new Transaction(clientName, "OK", t.getGroup().getGroupName()).getByteArray()), secKey));
                    sendNewGroup(t.getGroup());
                  }
                  else {
                    oos.writeObject(crypto.encrypt(
                      comp.compress(
                        new Transaction(clientName, "GROUP_NAME_IN_USE", t.getGroup().getGroupName()).getByteArray()), secKey));
                  }
                  break;
                case "GROUP_EDIT":
                  editGroup(t);
                  break;
                case "GROUP_MESSAGE":
                  sendToGroup(t.getClientName(), t.getGroup(), t.getMessage());
                  break;
              }
            }
          }
          else {
            oos.writeObject(crypto.encrypt(
              comp.compress(new Transaction(clientName, "NAME_IN_USE", clientName).getByteArray()), secKey));
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
        catch (SocketException ex) {
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
