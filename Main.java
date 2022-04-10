

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
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

    comboBox.setDisable(true);

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
        oos.writeObject(Crypto.encrypt(nameInput.getText(), secretKey, initVector)); // temporary
        taChat.appendText("connected to "+socket.getInetAddress()+":"+socket.getPort()+"\n");
        messageHandler = new IncomingMessageHandler();
        messageHandler.start();
        btnConnect.setText("Disconnect");
        comboBox.setDisable(false);

        
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
      comboBox.setItems(null);
      comboBox.setDisable(true);
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
        oos.writeObject(Crypto.encryptToBytes(new Transaction(nameInput.getText(),"BROADCAST",tField.getText()).getByteArray(),secretKey, initVector));
      }
      else {
        oos.writeObject(Crypto.encryptToBytes(new Transaction(nameInput.getText(),"DIRECT",tField.getText(),comboBoxSelection).getByteArray(),secretKey, initVector));
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

    secretKey = Crypto.generateKey();
    initVectorBytes = Crypto.getInitVector();
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

  private void processActiveClients(ArrayList<String> activeClientsStrings) {
    activeClients = activeClientsStrings;
    taClients.setText("");
    taClients.appendText("Me\n");
    for (String s : activeClients) {
      if (!s.equals(nameInput.getText())) {
        taClients.appendText(s+"\n");
      }
    }
    activeClients.add("Everyone");
    activeClients.remove(nameInput.getText());
    activeClientsComboList = FXCollections.observableArrayList(activeClients);
    Platform.runLater(new Runnable() {
      public void run() {
        comboBox.setItems(activeClientsComboList);
        comboBox.setValue("Everyone");
      }
    });

   
  }

  class IncomingMessageHandler extends Thread {
    
    public void run() {
      currentThread().setName("IncomingMessageHandler"); // mostly for debugging
      while(true) {
        try {
            byte[] incomingBytes = (byte[])ois.readObject();
            byte[] decryptedBytes = Crypto.decryptToBytes(incomingBytes, secretKey, initVector);
            Transaction t = Transaction.reconstructTransaction(decryptedBytes);

            switch (t.getCommand()) {
              case "BROADCAST":
                taChat.appendText("<" + t.getClientName()+"> " + t.getMessage()+"\n");
                break;
              case "DIRECT":
                taChat.appendText("Direct - <" + t.getClientName()+"> " + t.getMessage()+"\n");
                break;
              case "CLIENTS":
                processActiveClients(t.getData());
                break;
              case "FILE":
                fileEditHandler.processFileData(t);
                break;
              case "NAME_IN_USE":
                taChat.appendText(t.getMessage()+" is in use, please try another name\n");
                Platform.runLater(new Runnable() {public void run() {disconnect();}});
                break;
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
    private String prevFileData;
    public void run() {
      while (true) {
        prevFileData = taFileView.getText();
        try{sleep(100);}catch(InterruptedException ex) {}
        if (!taFileView.getText().equals(prevFileData)) {
          ArrayList<String> fileData = new ArrayList<String>();
          Collections.addAll(fileData, taFileView.getText().split("\n"));
          sendFile(fileData);
        }
      }
    }
    public void upload() {
      FileChooser chooser = new FileChooser();  // create file chooser object
      chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files","*.txt"));  //filters to just .txt
      File fileToUpload = chooser.showOpenDialog(stage);
      try {
        taFileView.setText("");
        Scanner sc = new Scanner(new FileInputStream(fileToUpload));
        ArrayList<String> fileData = new ArrayList<String>();
        while (sc.hasNextLine()) {
          String line  = sc.nextLine();
          fileData.add(line);
          taFileView.appendText(line+"\n");
        }
          sendFile(fileData);
      }
      catch (FileNotFoundException ex) {
        ex.printStackTrace();
      }
    }
    public void sendFile(ArrayList<String> fileData) {
      try {
        oos.writeObject(Crypto.encryptToBytes(new Transaction(nameInput.getText(), "FILE", fileData).getByteArray(),secretKey,initVector));
      }
      catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    public void processFileData(Transaction t) {
      taFileView.setText("");
      ArrayList<String> fileData = t.getData();
      for (String s : fileData) {
        taFileView.appendText(s+"\n");
      }
      prevFileData = taFileView.getText();
    }
  }    
}	
