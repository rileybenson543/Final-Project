package client;
import java.util.ArrayList;

import common.Group;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This class facilitates creating a popup for creating a new group.
 * It is given the ArrayList of the active clients for the gui
 * and returns a group object
 */
public class GroupCreatePopup implements EventHandler<ActionEvent> {

    private Stage stage = new Stage();
    private ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
    private TextField tfGroupName = new TextField();
    private Group group;
    /**
     * Constructor for the class that builds the gui and
     * displays it
     * @param activeClients
     */
    public GroupCreatePopup(ArrayList<String> activeClients) {
        BorderPane bp = new BorderPane();
        bp.setId("bp");
        VBox checkBoxVBox = new VBox(8);
        checkBoxVBox.setId("checkBoxVBox");

        for (String s : activeClients) {
            CheckBox cb = new CheckBox(s);
            cb.setPrefWidth(250);
            cb.setId("checkbox");
            checkBoxes.add(cb);
            checkBoxVBox.getChildren().add(cb);
        }
        checkBoxVBox.setAlignment(Pos.CENTER);
        bp.setCenter(checkBoxVBox);

        FlowPane fp = new FlowPane(8,8);
        Label groupNameLbl = new Label ("Group Name");
        
        Button createBtn = new Button("Create Group");
        createBtn.setOnAction(this);
        fp.getChildren().addAll(groupNameLbl,tfGroupName,createBtn);
        fp.setAlignment(Pos.CENTER);
        bp.setBottom(fp);
        
        if (activeClients.isEmpty()) {
            Text text = new Text("No Clients To Add");
            text.setFill(Color.WHITE);
            bp.setCenter(text);
        }

        Scene scene = new Scene(bp,400,400);
        scene.getStylesheets().add
            (GroupCreatePopup.class.getResource("../styles.css").toExternalForm());
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
     * "Create Group" button
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
