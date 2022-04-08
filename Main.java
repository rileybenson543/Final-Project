

import javafx.application.Application;
import javafx.collections.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
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
  private Button btnUpload = new Button("Upload File");

  private static TextArea taChat = new TextArea();
  
  private TextArea taClients = new TextArea();
  private TextArea taFileView = new TextArea("No File Available");

  private TextField tField = new TextField();
  private TextField nameInput = new TextField();

  private Label nameLbl = new Label("Name");

  private FlowPane fpChat = new FlowPane(8,8);
  private FlowPane fpMain = new FlowPane(8,8);
  private FlowPane fpFileView = new FlowPane(8,8);
  private FlowPane fp1 = new FlowPane(8,8);
  
  private ArrayList<String> activeClients = new ArrayList<String>();
  ObservableList<String> activeClientsComboList;
  ComboBox<String> comboBox = new ComboBox<String>(activeClientsComboList);
  
  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;
  IncomingMessageHandler messageHandler;
  FileEditHandler fileEditHandler;

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
    btnUpload.setOnAction(this);

    // comboBox.setValue("Not Connected"); // default value for combo box

    fp1.getChildren().addAll(btnConnect,nameLbl,nameInput);
    fpChat.getChildren().addAll(comboBox,tField,btnSend,taChat);
    fpFileView.getChildren().addAll(taFileView,btnUpload);
    fpMain.getChildren().addAll(fpChat,fpFileView);
    root.getChildren().addAll(fp1,btnGenerate,fpMain,taClients);

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
    fileEditHandler =  new FileEditHandler();
    fileEditHandler.start();

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
        case "Upload File":
          fileEditHandler.upload();
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
        taChat.appendText("connected to "+socket.getInetAddress()+":"+socket.getPort()+"\n");
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
      taChat.appendText("Please enter a name and try again\n");
    }
  }
  private void disconnect() {
    try {
      if (socket != null) {socket.close();}
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
        dataToSend = "DIRECT~" + comboBox.getValue().toString() + "~" + dataToSend;
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
        taChat.appendText("Successfully read key file\n");
      }
      else {
        throw new Exception("File Read Error");
      }      
    }
    catch (FileNotFoundException ex) {
      taChat.appendText("Key file was not found. One will need to be generated\n");
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
    taChat.appendText(s+"\n");
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
              String decrypted = Encrypt.decrypt_with_key((String)message, secretKey, initVector);
              String[] parsed = ((String)decrypted).split("~");
              if (parsed[1].equals("FILE_LINE")) {
                taFileView.appendText(parsed[2]);
              }
              else {
                taChat.appendText(message+"\n");
                taChat.appendText(Encrypt.decrypt_with_key((String)message,secretKey,initVector));
              }

            }
            else if (message instanceof ArrayList<?>) {
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
  class FileEditHandler extends Thread {
    public void run() {

    }
    public void upload() {
      FileChooser chooser = new FileChooser();  // create file chooser object
      chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files","*.txt"));  //filters to just .txt
      File fileToUpload = chooser.showOpenDialog(stage);
      try {
        taFileView.setText("");
        Scanner sc = new Scanner(new FileInputStream(fileToUpload));
        while (sc.hasNextLine()) {
          String line  = sc.nextLine();
          taFileView.appendText(line+"\n");
          try {
            oos.writeObject(Encrypt.encrypt("FILE_LINE~"+line,secretKey,initVector));
          }
          catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
      catch (FileNotFoundException ex) {
        ex.printStackTrace();
      }
    }
  }    
}	
