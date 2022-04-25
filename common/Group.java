package common;
import java.io.Serializable;
import java.util.ArrayList;
/**
 * Class that contains information about a group
 * It contains a String of the group name
 * and an ArrayList of the members name's
 */
public class Group implements Serializable { 
    private String groupName;
    private ArrayList<String> groupMembers;
    /**
     * Constructor for the object that takes 
     * the groupName and the members
     * @param _groupName String group name
     * @param _members ArrayList<String> member names
     */
    public Group (String _groupName, ArrayList<String> _members) {
        groupName = _groupName;
        groupMembers = _members;
    }
    /**
     * Returns the group name
     * @return String of group name
     */
    public String getGroupName() {
        return groupName;
    }
    /**
     * Returns the members of the group
     * @return ArrayList of member names
     */
    public ArrayList<String> getGroupMembers() {
        return groupMembers;
    }
    public void addMember(String member) {
        groupMembers.add(member);
    }
}