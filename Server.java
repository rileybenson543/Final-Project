

import javafx.application.Application;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.io.*;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;



public class Server extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root = new VBox(8);


  private Button btnReceive = new Button("Receive Connections");
  private Button btnConnect = new Button("Connect");
  private Button btnSend = new Button("Send");
  private Button btnGenerate = new Button("Generate Key");

  private static TextArea tArea = new TextArea();

  private TextField tField = new TextField();

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;


  private byte[] initVectorBytes;
  private IvParameterSpec initVector;
  private SecretKeySpec secretKey;

  KeyData keyData;

  ServerHandler serverHandler;

  
   
  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Server");
    
    btnReceive.setOnAction(this);
    btnConnect.setOnAction(this);
    btnSend.setOnAction(this);
    btnGenerate.setOnAction(this);

    root.getChildren().addAll(btnReceive,btnConnect,btnGenerate,tField,btnSend,tArea);

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {      
          System.exit(0);
        }
    });

    scene = new Scene(root, 600, 600); 
                                        
    stage.setScene(scene);              
    stage.show();
    
    readKey();

  }
  
  public void handle(ActionEvent evt) {

    Button btn = (Button)evt.getSource();
    

    switch(btn.getText()) {
        case "Receive Connections":
          serverHandler = new ServerHandler(keyData);
          serverHandler.start();
          btnReceive.setText("Disconnect");
          break;
        case "Connect":
          connect();
          break;
        case "Send":
          send(tField.getText());
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
  private void connect() {
    try {
      socket = new Socket("localhost",12345);
      oos = new ObjectOutputStream(socket.getOutputStream());
      ois = new ObjectInputStream(socket.getInputStream());
      tArea.appendText("connected to "+socket.getInetAddress()+":"+socket.getPort()+"\n");
    }
    catch (Exception ex) {
        ex.printStackTrace();
    }
  }
  private void send(String dataToSend) {
    try {
      System.out.println("sending");
      oos.writeObject(Encrypt.encrypt(dataToSend,secretKey,initVector));
      oos.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  private void readKey() {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("key.obj")));
      keyData = (KeyData)ois.readObject();
      secretKey = keyData.getKey();
      initVectorBytes = keyData.getInitVector();
      initVector = new IvParameterSpec(initVectorBytes);

      if (initVector!=null && secretKey!=null) {
        tArea.appendText("Successfully read key file\n");
      }
      else {
        throw new Exception("File Read Error");
      }      
    }
    catch (FileNotFoundException ex) {
      tArea.appendText("Key file was not found. One will need to be generated\n");
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

  }
  private void generateKey() {

    secretKey = Encrypt.generateKey();
    initVectorBytes = Encrypt.getInitVector();
    initVector = new IvParameterSpec(initVectorBytes);

    KeyData data = new KeyData(secretKey,initVectorBytes);
    
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("key.obj")));
      oos.writeObject(data);
      oos.flush();
      oos.close();

    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  public static void writeText(String s) {
    tArea.appendText(s+"\n");
  }
  
  class ServerHandler extends Thread {

    private ServerSocket ss;
    
    private KeyData keyData;

    private SecretKeySpec secretKey;
    private IvParameterSpec initVector;
    
    private ArrayList<SocketHandler> activeClients = new ArrayList<SocketHandler>();
    
    private volatile Boolean active = true;

    ObjectOutputStream oos;

    
    public ServerHandler(KeyData _keyData) {
        keyData = _keyData;
    }
    
    public void run() {
      secretKey = keyData.getKey();
      initVector = new IvParameterSpec(keyData.getInitVector());
      try {
        ss = new ServerSocket(12345);
        currentThread().setName("ServerHandler");
        while(active) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            SocketHandler socketHandler = new SocketHandler(s,keyData);
            socketHandler.start();
            // activeClients.add(socketHandler);
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
        ex.printStackTrace();
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
          oos.writeObject(Encrypt.encryptToBytes(new Transaction(s.getName(), "CLIENTS", activeClientsStrings).getByteArray(),secretKey,initVector));
        }
        catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
    public void broadcast(String message,SocketHandler sender) {
      for (SocketHandler s : activeClients) {
          if(!s.equals(sender)) {
              ObjectOutputStream oos = s.getOutputStream();
              //generate a new initvector
              // and send it along as well
              try {oos.writeObject(Encrypt.encryptToBytes(new Transaction(sender.getClientName(),"BROADCAST",message).getByteArray(), secretKey, initVector));}
              catch (Exception ex) {ex.printStackTrace();} 
          }
        }
    }
    public void broadcast(ArrayList<String> data, String sender) {
      for (SocketHandler s : activeClients) {
          if(!s.getClientName().equals(sender)) {
              ObjectOutputStream oos = s.getOutputStream();
              //generate a new initvector
              // and send it along as well
              try {oos.writeObject(Encrypt.encryptToBytes(new Transaction(sender,"FILE",data).getByteArray(), secretKey, initVector));}
              catch (Exception ex) {ex.printStackTrace();} 
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
            oos.writeObject(Encrypt.encryptToBytes(new Transaction(sender,"DIRECT",message,recipient).getByteArray(), secretKey, initVector));
          }
          catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
      if (!found) {
        Server.writeText("Direct message request to unkown recipient: "+recipient);
      }
    }
    class SocketHandler extends Thread {

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
          clientName = Encrypt.decrypt_with_key((String)ois.readObject(), secretKey, initVector);
          if (!nameInUse(clientName)) {
            addClient(this);
            sendActiveClients();
            while (active) {
              byte[] incomingBytes = (byte[])ois.readObject();
              byte[] decryptedBytes = Encrypt.decryptToBytes(incomingBytes, secretKey, initVector);
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
              }
            }
          }
          else {
            oos.writeObject(Encrypt.encryptToBytes(new Transaction(clientName, "NAME_IN_USE", clientName).getByteArray(), secretKey, initVector));
          }
        }
        catch (EOFException ex) {
          try {
            s.close();
            setInactiveSocketHandler(this);
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
        public void receiveFile(Transaction t) {
          writeText("File from " + t.getClientName());
          broadcast(t.getData(),t.getClientName());
        }
      }
    }
  }
}	
