package client;
//@ver 2.2.1

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.*;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.geometry.Side;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;


import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import javafx.scene.input.KeyCode;


import java.security.*;
import javax.crypto.SecretKey;

import common.Compression;
import common.Crypto;
import common.DispAlert;
import common.Group;
import common.Transaction;

import java.time.*;
import java.time.format.DateTimeFormatter;



/**
 * Main class that creates a client gui and handles all
 * the features associated with the server connection
 * @author Riley Basile-Benson, Mark Stubble
 */
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
  private TextField serverInput = new TextField();

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
  private MenuItem miCreateGroup = new MenuItem("Create A Group");
  private MenuItem miEditGroup = new MenuItem("Edit Group");
  
  private ArrayList<String> activeClients = new ArrayList<String>();
  private HashMap<String,Group> groups = new HashMap<String,Group>();
  private HashMap<String,StackPane> clientGraphicsMap = new HashMap<String,StackPane>();


  HashMap<String,Tab> tabs;

  Socket socket;

  ObjectInputStream ois;
  ObjectOutputStream oos;
  IncomingMessageHandler messageHandler;
  FileEditHandler fileEditHandler;
  ChatFieldHandler chatHandler;
  Compression comp;

  //keys
  private PublicKey pubKey;
  private PrivateKey privKey;
  private PublicKey serverPubKey;
  private SecretKey secKey;
  Crypto crypto;

  String name;
  

  public static void main(String[] args) {
    launch(args);
  }
  /**
   * Method that starts the gui and get gui elements
   * ready before displaying the stage
   */
  public void start(Stage _stage) throws Exception {
    stage = _stage;
    stage.setTitle("Client");
    root = new VBox(8,mBar);

    //menu items
    mBar.getMenus().addAll(mnuFile, menu);
    menu.getItems().addAll(miGenKey,miCreateGroup,miEditGroup);
    mnuFile.getItems().addAll(miUpload, miSave);
    miSave.setOnAction(this);
    miUpload.setOnAction(this);
    
    
    btnConnect.setOnAction(this);
    btnSend.setOnAction(this);
    //add functionality of send button to pressing enter key
    tField.setOnKeyPressed(new EventHandler<KeyEvent>() {
      public void handle(KeyEvent kevt) {
         if ((kevt.getCode() == KeyCode.ENTER)) {
            send(tField.getText());
            tField.setText("");
         }
      }
    });
      
    miGenKey.setOnAction(this);
    miCreateGroup.setOnAction(this);
    miEditGroup.setOnAction(this);
    


    // Main tab creation
    Tab tMain = new Tab("Main");
    TextArea taMain = new TextArea();
    taMain.setEditable(false);
    taMain.setWrapText(true);
    tMain.setContent(taMain);
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
    bPane.setRight(fpActiveClients);
    
    
    //TabPane
    tabPane.setPrefHeight(800);
    tabPane.setPrefWidth(250);
    
    fpActiveClients.setPrefWidth(200);
    fpChat.setAlignment(Pos.CENTER);
    fpChat.setPrefWidth(taFileView.getWidth());
    
    typingLbl.setAlignment(Pos.BASELINE_LEFT);
    fileEditUser.setMinWidth(100);
    

    //t1.setClosable(arg0);
    tField.setPrefColumnCount(25);


    //add to flowpanes
    fpFileView.setAlignment(Pos.CENTER_RIGHT);
    fpRegister.getChildren().addAll(btnConnect,nameLbl,nameInput, new Label("Server IP "), serverInput);
    FlowPane fpTyping = new FlowPane(typingLbl);
    fpTyping.setPrefWidth(150);
    fpTyping.setAlignment(Pos.BASELINE_LEFT);
    fpChat.getChildren().addAll(fpTyping,tField,btnSend,fileEditUser);
    fileEditUser.setAlignment(Pos.BASELINE_RIGHT);
    root.getChildren().addAll(bPane);
    
    fpRegister.setAlignment(Pos.CENTER);
    stage.setMinWidth(1100);
    stage.setMinHeight(600);

    fpFileView.setStyle("-fx-background-color: #1e2124");
    taFileView.setEditable(false);
    
    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent evt) {   
          disconnect();   
          System.exit(0);
        }
    });

    scene = new Scene(root, 1000, 600); 

   //css
    scene.getStylesheets().add
      (Main.class.getResource("/common/styles.css").toExternalForm());
    tField.setId("chat-field");
    fpActiveClients.setId("flow-pane-clients");
    
                  
    stage.setScene(scene);              
    stage.show();
    
    fileEditHandler =  new FileEditHandler();
    fileEditHandler.start();

    comp = new Compression();
    
    
  }
  /**
   * Handles Action Events for the buttons and 
   * menu items
   * @param evt the ActionEvent to be handled
   */
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
            tField.clear();
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
          break;
        case "Create A Group":
          createGroup();
          break;
        case "Edit Group":
          editGroup();
          break;
      }
    }
  }//end EventHandler

  
  
  
  /**
   * Connects to a server on a specified ip address. It creates
   * necessary IO streams and encryption keys. It also starts 
   * messageHandler and chatHandler
  */
  private void connect() {
    generateKey();
    name = nameInput.getText();
    
    if (!name.isEmpty() && !(name.length() > 20)) {
      try {
        socket = new Socket(serverInput.getText(),12345);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        writeText("connected to "+socket.getInetAddress()+":"+socket.getPort(),"Main");
        doKeyExchange();
        
        messageHandler = new IncomingMessageHandler();
        messageHandler.start();
        chatHandler = new ChatFieldHandler();
        chatHandler.start();
        btnConnect.setText("Disconnect");
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    else {
      writeText("Name must be between 1 and 20 characters\n","Main");
    }
  }
   /**
   * Handles disconnecting from the server and
   * resetting gui elements back to the original state
   */
  private void disconnect() {
    try {
      if (socket != null) {socket.close();}
      Platform.runLater(new Runnable() {
        public void run() {
          btnConnect.setText("Connect");
          fileEditUser.setText("");
          fpActiveClients.getChildren().clear();
        }
      });
    }
    catch (IOException ex) {
      DispAlert.alertException(ex);
    }
  }
   /**
   * This method handles sending chat messages. 
   * It determines whether it is a broadcast, geoup or
   * direct message and creates the transaction object 
   * accordingly
   * @param dataToSend the message to send
   */
  private void send(String dataToSend) {
    try {
      String time = "<"+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")).toString() + "> ";
      String directMessageSelection = tabPane.getSelectionModel().getSelectedItem().getText();
      
      if (directMessageSelection.equals("Main")) { // over here
        writeText(time + "You" + ": " + dataToSend, directMessageSelection);
        oos.writeObject(crypto.encrypt(
          comp.compress(
            new Transaction(
              nameInput.getText(),"BROADCAST",tField.getText()).getByteArray()), secKey));
      }
      else if (activeClients.contains(directMessageSelection)){
        writeText(time + "You" + ": " + dataToSend, directMessageSelection);
        oos.writeObject(crypto.encrypt(
          comp.compress(
            new Transaction(nameInput.getText(),"DIRECT",tField.getText(),directMessageSelection).getByteArray()), secKey));
      }
      if (groups.keySet().contains(directMessageSelection)) {
        writeText(time + "You" + ": " + dataToSend, directMessageSelection);
        oos.writeObject(crypto.encrypt(
          comp.compress(
            new Transaction(nameInput.getText(),"GROUP_MESSAGE",tField.getText(),groups.get(directMessageSelection)).getByteArray()), secKey));
      }

      oos.flush();
    }
    catch (Exception ex) {
      DispAlert.alertException(ex);
    }
  }
  
  /*
  *Returns a shorter version of a clients
  *for formatting purposes
  */
  
  private String shorten(String _clientName) {
        String clientName;
        if (_clientName.length() > 10  && !_clientName.contains(" ")) {
            clientName = _clientName.substring(0,10);
        }
        else if (_clientName.contains(" ") && _clientName.length() > 10){
            String[] temp = _clientName.split(" ");
            clientName = temp[0].substring(0,10) + " " + ( temp[1].substring(0,1).toUpperCase());
        }
        
        else if (_clientName.contains(" ")){
            String[] temp = _clientName.split(" ");
            clientName = temp[0].substring(0,temp[0].length()) + " " + ( temp[1].substring(0,1).toUpperCase());
        }
        else {
            clientName = _clientName;
        }
        return clientName;
  }
  
   /**
   * This method uses the Crypto class to create
   * its own public key, private key and symmetric
   * secret key
   */
  private void generateKey() {
      crypto = new Crypto();
      crypto.init();
      pubKey = crypto.getPublicKey();
      privKey = crypto.getPrivateKey();
      secKey = crypto.getSecretKey();
  }//end generateKey()  
  
  
  /**
   * Handles the key exchange between the server.
   * Reads the server's RSA public key and uses it to encrypt
   * a new AES symetric key that is then sent to the server.
   * The name of the client is then sent to the server to finish
   * making the connection
   */
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
        DispAlert.alertException(e);
     }   
  }//end doKeyExchange()
   /**
   * Creates a group by creating a GroupCreatePopup
   * and then sending the received data to the server
   */
  public void createGroup() {
    GroupCreatePopup gp = new GroupCreatePopup(activeClients);
    Group group = gp.getGroup();
    if (group != null) {
      group.addMember(name); // adds self to group
      try {
        oos.writeObject(crypto.encrypt(
          comp.compress(
            new Transaction(
              "NEW_GROUP",group).getByteArray()), secKey));
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
  }
  public void editGroup() {
    if (groups.keySet().contains(tabPane.getSelectionModel().getSelectedItem().getText())) {
      Group oldGroup = groups.get(tabPane.getSelectionModel().getSelectedItem().getText());
      GroupEditPopup gep = new GroupEditPopup(activeClients, oldGroup);
      Group newGroup = gep.getGroup();
      if (newGroup != null) {
        newGroup.addMember(name); // automatically add self to group
        groups.remove(oldGroup.getGroupName());
        groups.put(newGroup.getGroupName(),newGroup);
        try {
          oos.writeObject(crypto.encrypt(
            comp.compress(  
              new Transaction(name,"GROUP_EDIT",oldGroup,newGroup).getByteArray()),secKey));
        }
        catch (Exception ex) {
          DispAlert.alertException(ex);
        }
      }
    }
    else {
      DispAlert.alertInfo("Current tab is not a valid group");
    }
  }
  /**
   * Simplifies writing text to the chat area. 
   * It allows you to specify the tab name and if it
   * doesn't exist it will create one.
   * @param s The text to be written
   * @param tabName The name if the tab to put it in
   */
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
    Tab t = tabs.get(tabName);
    if (!t.isSelected() && !s.equals("")) { //tab 
      t.setStyle("-fx-background-color:#e34236; -fx-border-radius:10;");
      t.setOnSelectionChanged(new EventHandler<Event>() {
        public void handle(Event evt) {
            Tab selectedTab = (Tab)evt.getSource(); 
            if (selectedTab.isSelected()) {
              selectedTab.setStyle("-fx-background-color:#424549;-fx-border-radius:10;");
            }
          }
        });
      }

    TextArea ta = (TextArea)tabs.get(tabName).getContent();
    ta.setEditable(false);
    ta.setWrapText(true);
    taFileView.setWrapText(true);
    ta.appendText(s+"\n");
    
  }

   //class to display create the graphic for active clients on the right of the screen
   class ClientGraphic extends FlowPane {
      private String clientName;//name of the client the pane belongs to
      
      //Constructor
      //@param _clientName - name of client pane belongs to

      public ClientGraphic(String _clientName) {
        String origClientName = _clientName;
        String clientName = shorten(_clientName);
      

        this.setMaxWidth(200);
        this.setHgap(10);
        
        this.getChildren().addAll(new Circle(18, Color.color(Math.random(), Math.random(), Math.random())), new Label(clientName));
        this.setId("client-graphic");
        
        this.setOnMouseClicked(new EventHandler<MouseEvent>() { 
            public void handle(MouseEvent mevt) {
               if(!clientName.equals("You")) {
                  writeText("",origClientName);
               }
            }
         });
      }
      //@override
      public void run() {
      } 
   }//end ClientGraphic


  
  /**
   * This method does three things:
   * <ul>
   * <li>Sets the graphics for the active clients</li>
   * <li>Sets the selction for the "to" combo box</li>
   * <li>Removes stale clients that are no longer active from lists</li>
   * </ul>
   * It takes an arraylist containing all of the active users and it will
   * automatically filter out itself from the lists
   * @param _activeClients
   */
  private void processActiveClients(ArrayList<String> activeClientsStrings) {
    activeClients = activeClientsStrings;
    for (String s : activeClients) {
      if (!clientGraphicsMap.containsKey(s)) {
        Text t;
        if(s.equals(name)) {t = new Text("You");} // determines whether the active client is you
        else {t = new Text(s);}

        StackPane sPane = new StackPane();
        sPane.getChildren().addAll(new ClientGraphic(t.getText()));
        
        clientGraphicsMap.put(s,sPane); // creates the client circle if
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
    activeClients.remove(name); // remove client name from options


    Platform.runLater(new Runnable() {
      public void run() {
        fpActiveClients.getChildren().setAll(clientGraphicsMap.values()); 
      }
    });
  }

  public void processNewGroup(Group g) {
    groups.put(g.getGroupName(),g);
    if (g.getGroupMembers().contains(name)) { // if the client is a member of the group
      writeText("", g.getGroupName());
    }
  }
  /**
     * This is the main method for processing incoming data
     * It first processes data in this order:
     * <ol>
     * <li>Read incoming byte array</li>
     * <li>Decrypt bytes</li>
     * <li>Decompress bytes</li>
     * <li>Reconstruct transaction object from bytes</li>
     * </ol>
     * The actions are then determined by use of a switch-case
     * based on the command
   */
  class IncomingMessageHandler extends Thread {
    
    public void run() {
      currentThread().setName("IncomingMessageHandler"); // mostly for debugging
      while(true) {
        try {
            oos.reset();
            byte[] incomingBytes = (byte[])ois.readObject();
            byte[] decryptedBytes = (crypto.decrypt(incomingBytes, secKey));
            byte[] decompressed = comp.decompress(decryptedBytes);
            Transaction t = Transaction.reconstructTransaction(decompressed);

            switch (t.getCommand()) {
              case "BROADCAST":
                  writeText("<" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) .toString() +"> " + t .getClientName() + ": " + t.getMessage(),"Main");
                break;
              case "DIRECT":
                  writeText("<" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) .toString() +"> " + t .getClientName() + ": " + t.getMessage(),t.getClientName());
                break;
              case "CLIENTS":
                processActiveClients(t.getData());
                break;
              case "FILE":
                fileEditHandler.processFileData(t);
                break;
              case "NAME_IN_USE":
                DispAlert.alert(t.getMessage() + " is in use, please try another name");
                Platform.runLater(new Runnable() {public void run() {disconnect();}});
                break;
              case "GROUP_NAME_IN_USE":
                DispAlert.alert(t.getMessage() + " is in use, please try another name");
                break;
              case "OK":
                DispAlert.alertInfo("Successfully created group: " + t.getMessage());
                break;
              case "TYPING":
                chatHandler.setActiveTyping(t.getClientName(),t.getRecipient());
                break;
              case "NOT_TYPING":
                chatHandler.setInactiveTyping(t.getClientName());
                break;
              case "NEW_GROUP":
                groups.put(t.getGroup().getGroupName(),t.getGroup());
                processNewGroup(t.getGroup());
                break;
              case "GROUP_EDIT":
                groups.remove(t.getOldGroup().getGroupName());
                groups.put(t.getNewGroup().getGroupName(), t.getNewGroup());
                if (t.getNewGroup().getGroupMembers().contains(name)) {
                  if (t.getOldGroup().getGroupName().equals(t.getNewGroup().getGroupName())) {
                    writeText("Group members updated", t.getNewGroup().getGroupName());
                  }
                  else {
                    for (Tab tab : tabPane.getTabs()) {
                      if (tab.getText().equals(t.getOldGroup().getGroupName())) {
                        tab.setText(t.getNewGroup().getGroupName());
                      }
                    }
                  }
                }
                else {
                  writeText("You have been removed from the group",t.getNewGroup().getGroupName());
                }
                break;
              case "GROUP_MESSAGE":
                writeText("<" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")).toString() +"> " + t .getClientName() + ": " + t.getMessage(),t.getRecipient());
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
  /**
   * This class runs in a seperate thread and handles activities 
   * related to the file editor. It will check for changes and
   * send them to server and also takes in new file data and records
   * it to the text area
   */
  class FileEditHandler extends Thread implements EventHandler<KeyEvent> {
    private String prevFileData;
    public void run() {
      taFileView.setOnKeyTyped(this);
    }
    public void handle(KeyEvent kevt) {
      pollTextArea();
    } 
    /**
     * This method checks the file edit text area for changes
     * and if they exist updates the file and sends the 
     * file data to the server
     *  
     */ 
    public void pollTextArea() {
      String fileString = taFileView.getText();
      if (!fileString.equals(prevFileData)) {
        ArrayList<String> fileData = new ArrayList<String>();
        Collections.addAll(fileData, fileString.split("\n"));
        sendFile(fileData);
        prevFileData = fileString;
      }
    }
    /**
     * This method contains the process for allowing a user to 
     * upload a file to the file edit screen. It allows the user to
     * read a .txt file into the text area and will also send
     * it to the server
     */
    public void upload() {
      FileChooser chooser = new FileChooser();  // create file chooser object
      chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files","*.txt"));  //filters to just .txt
      File fileToUpload = chooser.showOpenDialog(stage);
      try {
        taFileView.setEditable(true);
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
    /**
     * This method allows the user to save the current state of the 
     * text area to a .txt file. It will save the file as 
     * "document-<date>.txt" by default
     */
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
    /**
     * Sends the given file data to the server.
     * Takes the data as an arraylist of strings
     * each String represents one line on the text area
     * @param fileData the data for the shared file
     */
    public void sendFile(ArrayList<String> fileData) {
      try {
        oos.writeObject(
          crypto.encrypt(  
            comp.compress(new Transaction(nameInput.getText(), "FILE", fileData).getByteArray()), secKey));
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
    /**
     * This method will take a transaction set the text area
     * to be the data from the transaction
     * It also sets the last edit by portion of the gui
     * @param t the transaction object that contains the file data
     */
    public void processFileData(Transaction t) {
      ArrayList<String> fileData = t.getData();
      String fileString = String.join("\n",fileData);
      Platform.runLater(new Runnable() {
        public void run() {
          miSave.setDisable(false);
          fileEditUser.setText("Edited by: " + t.getClientName());
          taFileView.setText(fileString);
          taFileView.setEditable(true);
          prevFileData = fileString;
      }});
    }
  }
  /**
   * This class handles actions relating to the chat field specifically 
   * determining whether it is being in or not. If it is being typed in 
   * it sends a message so that the other client display that the user is typing 
   */
  class ChatFieldHandler extends Thread implements EventHandler<KeyEvent>{
    private int cooldown = 2000; // amount of time after typing a key that 
                                  // it takes before the client is no longer considered typing
    private Timer timer;
    // private Tab tabSelection;
    /**
     * Handles a key being typed in the chat text field
     * It starts a timer for 2 seconds and if the timer expires
     * the client is then set as not typing. If another key is 
     * pressed before it expires then the timer is cancelled 
     * and a new one is started
     * @param KeyEvent
     */
    public void handle(KeyEvent ke) { 
      timer.cancel(); // cancels current timer
      timer.purge(); // removes cancelled timers
      timer = new Timer();
      isTyping();
      timer.schedule(new TimerTask() {
        public void run() {
          isNotTyping();
        }
      }, cooldown);
    }
    public void run() {
      tField.setOnKeyTyped(this); // sets this class to handle keys typed in the chat field
      timer = new Timer(); // creates first timer
    }
    /**
     * Sends a message to say that the client is typing
     */
    private void isTyping() {
      try {
        oos.writeObject(crypto.encrypt(
          comp.compress(  
            new Transaction(name,"TYPING","",tabPane.getSelectionModel().getSelectedItem().getText()).getByteArray()),secKey));
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    /**
     * Sends a message to say that the client is no longer typing
     */
    private void isNotTyping() {
      try {
        oos.writeObject(crypto.encrypt(
          comp.compress(  
            new Transaction(name,"NOT_TYPING","",tabPane.getSelectionModel().getSelectedItem().getText()).getByteArray()),secKey));
      }
      catch (Exception ex) {
        DispAlert.alertException(ex);
      }
    }
    /**
     * This sets the display for the client that is actively typing
     * It takes the client name as a paramter and it will be 
     * displayed in the typingLbl
     * @param clientName
    */
    public void setActiveTyping(String clientName, String recipient) {
      if (tabPane.getSelectionModel().getSelectedItem().getText().equals(recipient)) {
        Platform.runLater(new Runnable() {
          public void run() {
            typingLbl.setText(shorten(clientName) + " is typing ...");
          }
        });
      }
    }
    /**
     * Sets the typingLbl back to blank to indicate
     * that no one is typing
    */
    public void setInactiveTyping(String clientName) {
      // if (tabPane.getSelectionModel().getSelectedItem().getText().equals(clientName)) {
        Platform.runLater(new Runnable() {
          public void run() {
            typingLbl.setText("");
          }
        });
      }
    // }
  }    
}	
