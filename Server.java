

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

    scene = new Scene(root, 300, 300); 
                                        
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
}	
