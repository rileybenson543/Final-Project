//@ver 2.0.1

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.event.*;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.io.*;

import java.security.*;
import javax.crypto.SecretKey;



public class Main extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root;

  private Button btnConnect = new Button("Connect");
  private Button btnSend = new Button("Send");
  private Button btnUpload = new Button("Upload File");

  private static TextArea taChat = new TextArea();
  
  private TextArea taClients = new TextArea();
  private TextArea taFileView = new TextArea("No File Available");

  private TextField tField = new TextField();
  private TextField nameInput = new TextField();

  private Label nameLbl = new Label("Name");
  private Label fileEditUser = new Label("");

  private FlowPane fpChat = new FlowPane(8,8);
  private FlowPane fpMain = new FlowPane(8,8);
  private FlowPane fpFileView = new FlowPane(8,8);
  private FlowPane fp1 = new FlowPane(8,8);

  private MenuBar mBar = new MenuBar();
  private Menu menu = new Menu("Options");
  private MenuItem miGenKey = new MenuItem("Generate a Key");
  
  private ArrayList<String> activeClients = new ArrayList<String>();
  ObservableList<String> activeClientsComboList;
  ComboBox<String> comboBox = new ComboBox<String>(activeClientsComboList);
  
  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;
  IncomingMessageHandler messageHandler;
  FileEditHandler fileEditHandler;

  //keys
  private PublicKey pubKey;
  private PrivateKey privKey;
  private PublicKey serverPubKey;
  private SecretKey secKey;
  
  Crypto crypto;
  

  //ServerHandler serverHandler;

  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Client");
    root = new VBox(8,mBar);

    mBar.getMenus().add(menu);
    menu.getItems().addAll(miGenKey);
    
    btnConnect.setOnAction(this);
    btnSend.setOnAction(this);
    miGenKey.setOnAction(this);
    btnUpload.setOnAction(this);

    tField.setPrefColumnCount(25);

    comboBox.setDisable(true);

    fpFileView.setAlignment(Pos.CENTER_RIGHT);

    fp1.getChildren().addAll(btnConnect,nameLbl,nameInput);
    fpChat.getChildren().addAll(taChat,comboBox,tField,btnSend);
    fpFileView.getChildren().addAll(taFileView,btnUpload,fileEditUser);
    fpMain.getChildren().addAll(fpChat,fpFileView);
    root.getChildren().addAll(fp1,fpMain,taClients);

    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {   
          disconnect();   
          System.exit(0);
        }
    });

    scene = new Scene(root, 1000, 600); 
                                        
    stage.setScene(scene);              
    stage.show();
    
    
    fileEditHandler =  new FileEditHandler();
    fileEditHandler.start();

  }
  
  public void handle(ActionEvent evt) {

    if (evt.getSource() instanceof Button) {
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
    if (evt.getSource() instanceof MenuItem) {
      MenuItem mi = (MenuItem)evt.getSource();
      switch(mi.getText()) {
        case "Generate a Key":
          generateKey();
          break;
      }
    }

   }
  private void connect() {
    generateKey();
    String name = nameInput.getText();
    if (!name.isEmpty()) {
      try {
        socket = new Socket("localhost",12345);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        taChat.appendText("connected to "+socket.getInetAddress()+":"+socket.getPort()+"\n");
        doKeyExchange();
        
        messageHandler = new IncomingMessageHandler();
        messageHandler.start();
        btnConnect.setText("Disconnect");
        comboBox.setDisable(false);
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
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
      fileEditUser.setText("");
      comboBox.setItems(null);
      comboBox.setDisable(true);
    }
    catch (IOException ex) {
      DispAlert.alertException(ex);
    }
  }
  private void send(String dataToSend) {
    try {
      System.out.println("sending");
      // generate new init vector

      String comboBoxSelection = comboBox.getValue().toString();

      if (comboBoxSelection.equals("Everyone")) {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(),"BROADCAST",tField.getText()), secKey));
      }
      else {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(),"DIRECT",tField.getText(),comboBoxSelection), secKey));
      }

      // need to send init vector as well

      oos.flush();
    }
    catch (Exception ex) {
      DispAlert.alertException(ex);
    }
  }
  
  //generateKey()
  //creates Crypto object to insantiate keys
  private void generateKey() {
      crypto = new Crypto();
      crypto.init();
      pubKey = crypto.getPublicKey();
      privKey = crypto.getPrivateKey();
      secKey = crypto.getSecretKey();
      //taChat.appendText("\nKeys Generated" + "\n"+  pubKey + "\n" + privKey); //used for testing, can be removed
  }//end generateKey()  
  
  
  //doKeyExchange
  //gets key from server, responds with client name and public key
  private void doKeyExchange() {
      //get public key from server
     try {
        String clientName = nameInput.getText();
        serverPubKey = (PublicKey)ois.readObject();
        
        byte[] encryptedKey = crypto.encryptKey(serverPubKey);//encrypt symmetric key

        oos.writeObject(encryptedKey);//send key to server
        oos.flush();  
        oos.writeObject(crypto.encrypt(new Transaction(clientName), secKey));//send name to server
        oos.flush();
        
     }
     catch (Exception e) {
        e.printStackTrace();
     }   
  }//end doKeyExchange()

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
            byte[] decryptedBytes = (crypto.decrypt(incomingBytes, secKey));
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
          DispAlert.alertException(ex);
        }
      }
    }
  }
  class FileEditHandler extends Thread {
    private String prevFileData;
    public void run() {
      while (true) {
        prevFileData = taFileView.getText();
        try{sleep(500);}catch(InterruptedException ex) {}  // slow down the text area polling
        try {
          if (!taFileView.getText().equals(prevFileData)) {
            ArrayList<String> fileData = new ArrayList<String>();
            Collections.addAll(fileData, taFileView.getText().split("\n"));
            sendFile(fileData);
          }
        }
        catch (Exception ex) {
          ex.printStackTrace();
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
        DispAlert.alertException(ex);
      }
    }
    public void sendFile(ArrayList<String> fileData) {
      try {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(), "FILE", fileData), secKey));
        Platform.runLater(new Runnable() {
          public void run() {
            fileEditUser.setText("Edited by: You");
          }
        }); 
      }
      catch (IOException ex) {
        DispAlert.alertException(ex);
      }catch (Exception e) {
        DispAlert.alertException(e);
      }
    }
    public void processFileData(Transaction t) {
      Platform.runLater(new Runnable() {public void run() {
        taFileView.setText("");
        fileEditUser.setText("Edited by: " + t.getClientName());
        ArrayList<String> fileData = t.getData();
        for (String s : fileData) {
          taFileView.appendText(s+"\n");
        }
        prevFileData = taFileView.getText();
      }});

    }
  }    
}	
