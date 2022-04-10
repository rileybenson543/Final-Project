import javafx.application.Platform;
import javafx.scene.control.Alert;

public class DispAlert {
  public static void alert(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setContentText(message);
    Platform.runLater(new Runnable() {
      public void run() {
        alert.showAndWait();
      }
    });
  }
  public static void alertException(Exception ex) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setContentText(ex.getMessage());
    Platform.runLater(new Runnable() {
      public void run() {
        alert.showAndWait();
      }
    });
  }
}