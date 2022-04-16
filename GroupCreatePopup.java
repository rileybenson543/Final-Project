import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GroupCreatePopup implements EventHandler<ActionEvent> {

    private Stage stage = new Stage();
    private ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
    private TextField tfGroupName = new TextField();
    private Group group;

    public GroupCreatePopup(ArrayList<String> activeClients) {
        BorderPane bp = new BorderPane();
        VBox checkBoxVBox = new VBox();

        for (String s : activeClients) {
            CheckBox cb = new CheckBox(s);
            checkBoxes.add(cb);
            checkBoxVBox.getChildren().add(cb);
        }
        bp.setCenter(checkBoxVBox);

        FlowPane fp = new FlowPane(8,8);
        Label groupNameLbl = new Label ("Group Name");
        
        Button createBtn = new Button("Create Group");
        createBtn.setOnAction(this);
        fp.getChildren().addAll(groupNameLbl,tfGroupName,createBtn);
        fp.setAlignment(Pos.CENTER);
        bp.setBottom(fp);

        Scene scene = new Scene(bp,400,400);
        stage.setScene(scene);
        stage.setOnCloseRequest(
            new EventHandler<WindowEvent>() {
                public void handle(WindowEvent wevt) {
                    stage.close();
                }
            });
        stage.showAndWait();

    }
    public void handle(ActionEvent evt) {
        getSelections();
        stage.close();
    }
    private void getSelections() {
        ArrayList<String> selectedUsers = new ArrayList<String>();
        for (CheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                selectedUsers.add(cb.getText());
            }
        }
        group = new Group(tfGroupName.getText(), selectedUsers);
    }
    public Group getGroup() {
        return group;
    }
}
