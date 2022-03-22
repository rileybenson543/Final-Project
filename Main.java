

import javafx.application.Application;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.io.*;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;



public class Main extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root = new VBox(8);


  private Button btnReceive = new Button("Recieve Connections");
  private Button btnConnect = new Button("Connect");
  private Button btnSend = new Button("Send");
  private Button btnGenerate = new Button("Generate Key");

  private TextArea tArea = new TextArea();

  private TextField tField = new TextField();

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;


  private byte[] initVectorBytes;
  private IvParameterSpec initVector;
  private SecretKeySpec secretKey;

   
  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Secure Chat DEMO");
    
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

    scene = new Scene(root, 300, 300); 
                                        
    stage.setScene(scene);              
    stage.show();
    
    readKey();

  }
  
  public void handle(ActionEvent evt) {

    Button btn = (Button)evt.getSource();
    

    switch(btn.getText()) {
        case "Recieve Connections":
          new ServerHandler().start();
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
  public void readKey() {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("key.obj")));
      KeyData data = (KeyData)ois.readObject();
      secretKey = data.getKey();
      initVectorBytes = data.getInitVector();
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
  public void generateKey() {

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
  class ServerHandler extends Thread {

    ServerSocket ss;
    public void run() {
      try {
        ss = new ServerSocket(12345);
        while(true) {
            System.out.println("waiting for connection");
            Socket s = ss.accept();
            System.out.println("Accepted connection from "+s.getInetAddress().getHostName());
            new SocketHandler(s).start();
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
  class SocketHandler extends Thread {

    Socket s;
      ObjectInputStream ois;
      ObjectOutputStream oos;
      
    public SocketHandler(Socket _s) {
        s = _s;
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
          tArea.appendText(dataIn+"\n");
          tArea.appendText(Encrypt.decrypt_with_key(dataIn, secretKey, initVector)+"\n");

        }
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }   
}	
