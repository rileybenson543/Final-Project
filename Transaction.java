//@ver 2.2.1


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class Transaction implements Serializable {
    private String clientName;
    private String recipient;
    private String command;
    private String message;
    private Group group;
    private ArrayList<String> data;
    private HashMap<String,Group> groups;
    // init vector
    public Transaction(String _clientName, String _command) {
        clientName = _clientName;
        command = _command;
    }
    public Transaction(String _clientName, String _command, String _message, String _recipient) { // for direct messages
        clientName = _clientName;
        command = _command;
        message = _message;
        recipient = _recipient;
    }
    public Transaction(String _clientName, String _command, String _message, Group _group) { // for direct messages
        clientName = _clientName;
        command = _command;
        message = _message;
        group = _group;
    }
    public Transaction(String _clientName, String _command, String _message) { // broadcast messages
        clientName = _clientName;
        command = _command;
        message = _message;
    }
    public Transaction(String _clientName, String _command, ArrayList<String> _data) { // active client updates and file data
        clientName = _clientName;
        command = _command;
        data = _data;
    }
    public Transaction(String _clientName) { // sending name
      clientName = _clientName;
    }
    public Transaction(String _command, HashMap<String,Group> _groups) { // creating new group
        command = _command;
        groups = _groups;
    }
    public Transaction(String _command, Group _group) {
        command = _command;
        group = _group;
    }
    public String getClientName() {
        return clientName;
    }
    public String getRecipient() {
        return recipient;
    }
    public String getCommand() {
        return command;
    }
    public String getMessage() {
        return message;
    }
    public ArrayList<String> getData() {
        return data;
    }
    public HashMap<String,Group> getGroups() {
        return groups;
    }
    public Group getGroup() {
        return group;
    }
    public byte[] getByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
    public static Transaction reconstructTransaction(byte[] bytes) throws IOException,ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput oi = null;
        oi = new ObjectInputStream(bis);
        return (Transaction)oi.readObject(); 
    }
}