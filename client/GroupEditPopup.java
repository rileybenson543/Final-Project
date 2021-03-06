package client;
import java.util.ArrayList;


import common.Group;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This class facilitates creating a popup for editing a group.
 * It is given the ArrayList of the active clients and the 
 * group that is to be edited
 */
public class GroupEditPopup implements EventHandler<ActionEvent> {

    private Stage stage = new Stage();
    private ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
    private TextField tfGroupName = new TextField();
    private Group group;
    /**
     * Constructor for the class that builds the gui and
     * displays it
     * @param activeClients
     */
    public GroupEditPopup(ArrayList<String> activeClients, Group g) {
        BorderPane bp = new BorderPane();
        VBox checkBoxVBox = new VBox(8);

        for (String s : activeClients) {
            CheckBox cb = new CheckBox(s);
            cb.setPrefWidth(250);
            cb.setId("checkbox");
            
            if (g.getGroupMembers().contains(s)) {
                cb.setSelected(true);
            }
            checkBoxes.add(cb);
            checkBoxVBox.getChildren().add(cb);
        }
        checkBoxVBox.setAlignment(Pos.CENTER);
        bp.setCenter(checkBoxVBox);

        FlowPane fp = new FlowPane(8,8);
        Label groupNameLbl = new Label ("Group Name");
        tfGroupName.setText(g.getGroupName());

        
        Button createBtn = new Button("Save Group");
        createBtn.setOnAction(this);
        fp.getChildren().addAll(groupNameLbl,tfGroupName,createBtn);
        fp.setAlignment(Pos.CENTER);
        bp.setBottom(fp);
        
        if (activeClients.isEmpty()) {
            bp.setCenter(new Text("No Clients To Add"));
        }

        Scene scene = new Scene(bp,400,400);
        scene.getStylesheets().add
            (GroupEditPopup.class.getResource("/common/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setOnCloseRequest(
            new EventHandler<WindowEvent>() {
                public void handle(WindowEvent wevt) {
                    stage.close();
                }
            });
        stage.showAndWait();

    }
    /**
     * Handles the button click for the
     * "Save Group" button
     */
    public void handle(ActionEvent evt) {
        getSelections();
        stage.close();
    }
    /**
     * Method that gathers the selected checkboxes and
     * uses that to create a group object
     */
    private void getSelections() {
        ArrayList<String> selectedUsers = new ArrayList<String>();
        for (CheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                selectedUsers.add(cb.getText());
            }
        }
        group = new Group(tfGroupName.getText(), selectedUsers);
    }
    /**
     * Accessor method for the main class to 
     * retrieve the created group object
     * @return group
     */
    public Group getGroup() {
        return group;
    }
}
