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

    public GroupCreatePopup(ArrayList<String> activeClients) {
        Stage stage = new Stage();
        BorderPane bp = new BorderPane();
        VBox checkBoxVBox = new VBox();

        ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
        for (String s : activeClients) {
            CheckBox cb = new CheckBox(s);
            checkBoxes.add(cb);
            checkBoxVBox.getChildren().add(cb);
        }
        bp.setCenter(checkBoxVBox);

        FlowPane fp = new FlowPane(8,8);
        Label groupNameLbl = new Label ("Group Name");
        TextField tfGroupName = new TextField();
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
        stage.show();

    }
    public void handle(ActionEvent evt) {
        
    }
}
