//@ver 2.2.1

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.event.*;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.geometry.Side;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import java.security.*;
import javax.crypto.SecretKey;

import java.time.*;



public class Main extends Application implements EventHandler<ActionEvent> {

  private Stage stage;
  private Scene scene;

  private VBox root;
  private BorderPane bPane = new BorderPane();

  private Button btnConnect = new Button("Connect");
  private Button btnSend = new Button("Send");
  
  private TextArea taFileView = new TextArea("No File Available");

  private TextField tField = new TextField();
  private TextField nameInput = new TextField();

  private Label nameLbl = new Label("Name");
  private Label fileEditUser = new Label("");
  private Label typingLbl = new Label("");

  private TabPane tabPane = new TabPane();


  private FlowPane fpChat = new FlowPane(8,8);
  private FlowPane fpFileView = new FlowPane(8,8);
  private FlowPane fpRegister = new FlowPane(8,8);
  private FlowPane fpActiveClients = new FlowPane(8,8);

  private MenuBar mBar = new MenuBar();
  private Menu menu = new Menu("Options");
  private MenuItem miGenKey = new MenuItem("Generate a Key");
  private Menu mnuFile = new Menu("File");
  private MenuItem miSave = new MenuItem("Save File");
  private MenuItem miUpload = new MenuItem("Upload File");
  
  private ArrayList<String> activeClients = new ArrayList<String>();
  private HashMap<String,StackPane> clientGraphicsMap = new HashMap<String,StackPane>();
  ObservableList<String> activeClientsComboList;
  ComboBox<String> comboBox = new ComboBox<String>(activeClientsComboList);

  HashMap<String,Tab> tabs;
  // ArrayList<Tab> tabs;

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;
  IncomingMessageHandler messageHandler;
  FileEditHandler fileEditHandler;
  ChatFieldHandler chatHandler;

  //keys
  private PublicKey pubKey;
  private PrivateKey privKey;
  private PublicKey serverPubKey;
  private SecretKey secKey;
  Crypto crypto;

  String name;
  
  //ServerHandler serverHandler;

  public static void main(String[] args) {
    launch(args);
  }
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Client");
    root = new VBox(8,mBar);

    //menu items
    mBar.getMenus().addAll(mnuFile, menu);
    menu.getItems().addAll(miGenKey);
    mnuFile.getItems().addAll(miUpload, miSave);
    miSave.setOnAction(this);
    miUpload.setOnAction(this);
    
    btnConnect.setOnAction(this);
    btnSend.setOnAction(this);
    miGenKey.setOnAction(this);
    

    // Main tab creation
    Tab tMain = new Tab("Main");
    tMain.setContent(new TextArea());
    tMain.setClosable(false);
    
    tabPane.getTabs().addAll(tMain);
    tabs = new HashMap<String,Tab>();
    tabs.put("Main",tMain);
    tabPane.setSide(Side.LEFT);

    
    
    //BorderPane Format
    bPane.setTop(fpRegister);
    bPane.setLeft(tabPane);
    bPane.setCenter(taFileView);
    bPane.setBottom(fpChat);
    // bPane.setRight(taClients);
    bPane.setRight(fpActiveClients);
    
    
    //TabPane
    tabPane.setPrefHeight(800);
    tabPane.setPrefWidth(200);
    fpActiveClients.setPrefWidth(200);
    fpChat.setAlignment(Pos.CENTER);
    
    

    //t1.setClosable(arg0);
    tField.setPrefColumnCount(25);

    comboBox.setDisable(true);

    //add to flowpanes
    fpFileView.setAlignment(Pos.CENTER_RIGHT);
    fpRegister.getChildren().addAll(btnConnect,nameLbl,nameInput);
    fpChat.getChildren().addAll(typingLbl,comboBox,tField,btnSend);
    root.getChildren().addAll(bPane);
     
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

        }
    }
    if (evt.getSource() instanceof MenuItem) {
      MenuItem mi = (MenuItem)evt.getSource();
      switch(mi.getText()) {
        case "Generate a Key":
          generateKey();
          break;
        case "Upload File":
          fileEditHandler.upload();
          break;
       case "Save File":
          fileEditHandler.save();
      }
    }
  }
  private void connect() {
    generateKey();
    name = nameInput.getText();
    if (!name.isEmpty()) {
      try {
        socket = new Socket("localhost",12345);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        writeText("connected to "+socket.getInetAddress()+":"+socket.getPort(),"Main");
        doKeyExchange();
        
        messageHandler = new IncomingMessageHandler();
        messageHandler.start();
        chatHandler = new ChatFieldHandler();
        chatHandler.start();
        btnConnect.setText("Disconnect");
        comboBox.setDisable(false);
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    else {
      writeText("Please enter a name and try again","Main");
    }
  }
  private void disconnect() {
    try {
      if (socket != null) {socket.close();}
      Platform.runLater(new Runnable() {
        public void run() {
          btnConnect.setText("Connect");
          fileEditUser.setText("");
          comboBox.setItems(null);
          comboBox.setDisable(true);
          fpActiveClients.getChildren().clear();
        }
      });
    }
    catch (IOException ex) {
      DispAlert.alertException(ex);
    }
  }
  private void send(String dataToSend) {
    try {
      System.out.println("sending");
      String comboBoxSelection = comboBox.getValue().toString();

      if (comboBoxSelection.equals("Everyone")) {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(),"BROADCAST",tField.getText()), secKey));
      }
      else {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(),"DIRECT",tField.getText(),comboBoxSelection), secKey));
      }
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

  public void writeText(String s, String tabName) {
    if (!tabs.containsKey(tabName)) {
      Tab t = new Tab(tabName);
      t.setContent(new TextArea());
      t.setClosable(false);
      tabs.put(tabName,t);
      Platform.runLater(new Runnable() {
        public void run() {
          tabPane.getTabs().add(t);
        }
      });
    }
    TextArea ta = (TextArea)tabs.get(tabName).getContent();
    ta.appendText(s+"\n");
    // taChat.appendText(s+"\n");
  }

  private void processActiveClients(ArrayList<String> activeClientsStrings) {
    activeClients = activeClientsStrings;
    for (String s : activeClients) {
      if (!clientGraphicsMap.containsKey(s)) {
        Text t;
        if(s.equals(name)) {t = new Text("You");} // determines whether the active client is you
        else {t = new Text(s);}
        t.setFill(Color.WHITE); // set text to white
        t.setFont(Font.font("Verdana",20));
        clientGraphicsMap.put(s,new StackPane(new Rectangle(250, 35, Color.GREY), t)); // creates the client circle if
                                                                                        // they are not already there
        //Platform.runLater(new Runnable() {public void run() {fpActiveClients.getChildren().add(clientGraphicsMap.get(s));}});
      }
    }
    ArrayList<String> remove = new ArrayList<String>();
    for (String s : clientGraphicsMap.keySet()) { // iterate through the keys
      if (!activeClients.contains(s)) { // removes clients from map that are 
        remove.add(s);    // no longer active
      }
    }
    for (String s : remove) {
      clientGraphicsMap.remove(s);
    }
    activeClients.add("Everyone");
    activeClients.remove(name);
    activeClientsComboList = FXCollections.observableArrayList(activeClients);


    Platform.runLater(new Runnable() {
      public void run() {
        comboBox.setItems(activeClientsComboList);
        comboBox.setValue("Everyone");
        // for (Node n : fpActiveClients.getChildren()) {
        //   fpActiveClients.getChildren().remove(n);
        // }
        // fpActiveClients.getChildren().set
        //fpActiveClients.getChildren().removeAll();
        fpActiveClients.getChildren().setAll(clientGraphicsMap.values()); 
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
                writeText("<" + t.getClientName()+"> " + t.getMessage(),"Main");
                break;
              case "DIRECT":
                writeText("<" + t.getClientName()+"> " + t.getMessage(),t.getClientName());
                break;
              case "CLIENTS":
                processActiveClients(t.getData());
                break;
              case "FILE":
                fileEditHandler.processFileData(t);
                break;
              case "NAME_IN_USE":
                writeText(t.getMessage()+" is in use, please try another name","Main");
                Platform.runLater(new Runnable() {public void run() {disconnect();}});
                break;
              case "TYPING":
                chatHandler.setActiveTyping(t.getClientName());
                break;
              case "NOT_TYPING":
                chatHandler.setInactiveTyping();
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
    public void save() {
      FileChooser chooser = new FileChooser();  // create file chooser object
      chooser.setInitialDirectory(new File("."));
      String date = LocalDate.now().toString();
      chooser.setInitialFileName("document-"+date);
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text File", ".txt"));
      File save = chooser.showSaveDialog(stage);
      if (save != null) {
        try{
          PrintWriter pw = new PrintWriter(save);
          for (String line : taFileView.getText().split("\n")) {
            pw.println(line);
          }
          pw.flush();
          pw.close();
          DispAlert.alertInfo("File saved successfully to "+save.getAbsolutePath());
        }
        catch (FileNotFoundException ex) {
          DispAlert.alertException(ex);
        }
      }
    }
    public void sendFile(ArrayList<String> fileData) {
      try {
        oos.writeObject(crypto.encrypt(new Transaction(nameInput.getText(), "FILE", fileData), secKey));
        Platform.runLater(new Runnable() {
          public void run() {
            miSave.setDisable(false);
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
        miSave.setDisable(false);
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
  class ChatFieldHandler extends Thread {
    private boolean typing = false;
    public void run() {
      while (true) {
        if (tField.isFocused() && typing == false) {
          isTyping();
        }
        else if (tField.isFocused() == false && typing == true) {
          isNotTyping();
        }
        try {sleep(500);}catch(InterruptedException ie) {}
      }
    }
    private void isTyping() {
      try {
        typing = true;
        oos.writeObject(crypto.encrypt(new Transaction(name,"TYPING","",comboBox.getValue().toString()),secKey));
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    private void isNotTyping() {
      try {
        typing = false;
        oos.writeObject(crypto.encrypt(new Transaction(name,"NOT_TYPING","",comboBox.getValue().toString()),secKey));
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    public void setActiveTyping(String clientName) {
      Platform.runLater(new Runnable() {
        public void run() {
          typingLbl.setText(clientName + " is typing ...");
        }
      });
    }
    public void setInactiveTyping() {
      Platform.runLater(new Runnable() {
        public void run() {
          typingLbl.setText("");
        }
      });
    }
  }    
}	
