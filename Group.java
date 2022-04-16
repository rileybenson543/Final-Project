import java.io.Serializable;
import java.util.ArrayList;

public class Group implements Serializable { 
    private String groupName;
    private ArrayList<String> groupMembers;

    public Group (String _groupName, ArrayList<String> _members) {
        groupName = _groupName;
        groupMembers = _members;
    }

    public String getGroupName() {
        return groupName;
    }
    public ArrayList<String> getGroupMembers() {
        return groupMembers;
    }
}