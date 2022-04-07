

import javafx.application.Application;
import javafx.collections.*;
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



public class Main extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root = new VBox(8);

  private Button btnConnect = new Button("Connect");
  private Button btnSend = new Button("Send");
  private Button btnGenerate = new Button("Generate Key");

  private static TextArea tArea = new TextArea();
  
  private TextArea taClients = new TextArea();

  private TextField tField = new TextField();
  private TextField nameInput = new TextField();

  private Label nameLbl = new Label("Name");

  private FlowPane fp1 = new FlowPane(8,8);
  
  private ArrayList<String> activeClients = new ArrayList<String>();
  ObservableList<String> activeClientsComboList;
  ComboBox<String> comboBox = new ComboBox<String>(activeClientsComboList);
  


  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;
  IncomingMessageHandler messageHandler;

  private byte[] initVectorBytes;
  private IvParameterSpec initVector;
  private SecretKeySpec secretKey;

  KeyData keyData;

  //ServerHandler serverHandler;

  
   
  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Client");
    
    btnConnect.setOnAction(this);
    btnSend.setOnAction(this);
    btnGenerate.setOnAction(this);

    // comboBox.setValue("Not Connected"); // default value for combo box


    fp1.getChildren().addAll(btnConnect,nameLbl,nameInput);

    root.getChildren().addAll(fp1,btnGenerate,comboBox,tField,btnSend,tArea,taClients);

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {   
          disconnect();   
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
        case "Connect":
          connect();
          break;
        case "Disconnect":
          disconnect();
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
    String name = nameInput.getText();
    if (!name.isEmpty()) {
      try {
        socket = new Socket("localhost",12345);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        tArea.appendText("connected to "+socket.getInetAddress()+":"+socket.getPort()+"\n");
        messageHandler = new IncomingMessageHandler();
        messageHandler.start();
        btnConnect.setText("Disconnect");

        oos.writeObject(nameInput.getText());

      }
      catch (Exception ex) {
          ex.printStackTrace();
      }
    }
    else {
      tArea.appendText("Please enter a name and try again\n");
    }
  }
  private void disconnect() {
    try {
      socket.close();
      btnConnect.setText("Connect");
      taClients.setText("Not Connected");
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  }
  private void send(String dataToSend) {
    try {
      System.out.println("sending");
      // generate new init vector

      String comboBoxSelection = comboBox.getValue().toString();

      if (comboBoxSelection.equals("Everyone")) {
        dataToSend = "BROADCAST~" + dataToSend;
        oos.writeObject(Encrypt.encrypt(dataToSend,secretKey,initVector));
      }
      else {
        dataToSend = comboBox.getValue().toString() + "~" + dataToSend;
        oos.writeObject(Encrypt.encrypt(dataToSend,secretKey,initVector));
      }

      
      // need to send init vector as well

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

  private void processActiveClients() {
    taClients.setText("");
    taClients.appendText("Me\n");
    for (String s : activeClients) {
      if (!s.equals(nameInput.getText())) {
        taClients.appendText(s+"\n");
      }
      // else {
      //   activeClients.remove(s);
      // }
    }

    activeClients.add("Everyone");
    activeClientsComboList = FXCollections.observableArrayList(activeClients);

    comboBox.setItems(activeClientsComboList);
  }

  class IncomingMessageHandler extends Thread {
    
    public void run() {
      currentThread().setName("IncomingMessageHandler"); // mostly for debugging
      while(true) {
        try {
            Object message = ois.readObject();
            if (message instanceof String ) {
              tArea.appendText(message+"\n");
              tArea.appendText(Encrypt.decrypt_with_key((String)message,secretKey,initVector));
            }
            else if (message instanceof ArrayList) {
              activeClients = (ArrayList<String>)message;
              processActiveClients();
            }
        }
        catch (SocketException ex) {
          break;
        } 
        catch (Exception ex) {
            ex.printStackTrace();
        }
      }
    }
  }    
}	
