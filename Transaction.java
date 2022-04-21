//@ver 2.2.1


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
/**
 * Object that contains the data exchanged between the client and
 * server. All of the exchanges use a transaction object
 */
public class Transaction implements Serializable {
    private String clientName;
    private String recipient;
    private String command;
    private String message;
    private Group group;
    private Group oldGroup;
    private Group newGroup;
    private ArrayList<String> data;
    private HashMap<String,Group> groups;

    /**
     * Constrcutor used to set active typing
     * @param _clientName
     * @param _command
     */
    public Transaction(String _clientName, String _command) {
        clientName = _clientName;
        command = _command;
    }
    /**
     * Constructor for direct messages
     * @param _clientName
     * @param _command
     * @param _message
     * @param _recipient
     */
    public Transaction(String _clientName, String _command, String _message, String _recipient) {
        clientName = _clientName;
        command = _command;
        message = _message;
        recipient = _recipient;
    }
    /**
     * Constructor for group updates
     * @param _clientName
     * @param _command
     * @param _oldGroup
     * @param _newGroup
     */
    public Transaction(String _clientName, String _command, Group _oldGroup, Group _newGroup) {
        clientName = _clientName;
        command = _command;
        oldGroup = _oldGroup;
        newGroup = _newGroup; 
    }
     /**
     * Constructor for messages to a group
     * @param _clientName
     * @param _command
     * @param _message
     * @param _group
     */
    public Transaction(String _clientName, String _command, String _message, Group _group) {
        clientName = _clientName;
        command = _command;
        message = _message;
        group = _group;
    }
    /**
     * Constructor for broadcast messages
     * @param _clientName
     * @param _command
     * @param _message
     */
    public Transaction(String _clientName, String _command, String _message) { // broadcast messages
        clientName = _clientName;
        command = _command;
        message = _message;
    }
    /**
     * Constructor for file data exchanges 
     * and active client updates
     * @param _clientName
     * @param _command
     * @param _data
     */
    public Transaction(String _clientName, String _command, ArrayList<String> _data) { // active client updates and file data
        clientName = _clientName;
        command = _command;
        data = _data;
    }
    /**
     * Constructor for sending the client name
     * @param _clientName
     */
    public Transaction(String _clientName) {
      clientName = _clientName;
    }
    /**
     * Constructor for sending all the groups
     * @param _command
     * @param _groups
     */
    public Transaction(String _command, HashMap<String,Group> _groups) {
        command = _command;
        groups = _groups;
    }
    /**
     * Constructor for creating a new group
     * @param _command
     * @param _group
     */
    public Transaction(String _command, Group _group) {
        command = _command;
        group = _group;
    }
    /**
     * Returns client name
     * @return client name
     */
    public String getClientName() {
        return clientName;
    }
    /**
     * Returns recipient name
     * @return recipient name
     */
    public String getRecipient() {
        return recipient;
    }
    /**
     * Returns the command
     * @return command
     */
    public String getCommand() {
        return command;
    }
    /**
     * Returns the message
     * @return message
     */
    public String getMessage() {
        return message;
    }
    /**
     * Returns the data 
     * @return ArrayList<String> data
     */
    public ArrayList<String> getData() {
        return data;
    }
    /**
     * Returns the groups hashmap
     * @return HashMap<String,Group> groups
     */
    public HashMap<String,Group> getGroups() {
        return groups;
    }
    /**
     * Returns the Group object
     * @return Group
     */
    public Group getGroup() {
        return group;
    }
    /**
     * Returns the old Group object
     * @return Group
     */
    public Group getOldGroup() {
        return oldGroup;
    }
    /**
     * Returns the new Group object
     * @return Group
     */
    public Group getNewGroup() {
        return newGroup;
    }
    /**
     * Converts transaction object to byte[]
     * @return byte[]
     * @throws IOException
     */
    public byte[] getByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
    /**
     * Converts byte[] back into a transaction object
     * @param bytes
     * @return Transaction
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Transaction reconstructTransaction(byte[] bytes) throws IOException,ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput oi = null;
        oi = new ObjectInputStream(bis);
        return (Transaction)oi.readObject(); 
    }
}