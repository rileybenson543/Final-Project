//ver 2.2.1

import javafx.application.Platform;
import javafx.scene.control.Alert;
/**
 * This class helps simplify creating an alert given 
 * a specific message or an exception.
 * It has two options, and alert as an ERROR or
 * as INFORMATION
 */
public class DispAlert {

  public static void alert(String message) {
    Platform.runLater(new Runnable() {
      public void run() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
      }
    });
  }
  /**
   * 
   */
  public static void alertException(Exception ex) {
    Platform.runLater(new Runnable() {
      public void run() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
      }
    });
  }
  public static void alertInfo(String message) {
    Platform.runLater(new Runnable() {
      public void run() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
      }
    });
  }
}