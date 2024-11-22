package org.pankratzlab.unet.validation;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public final class AlertHelper {

  static Optional<ButtonType> showMessage_PII() {
    Alert alert = new Alert(AlertType.WARNING,
        "Caution - DonorCheck will retain a copy of the current input files here:\n"
            + ValidationTesting.VALIDATION_DIRECTORY
            + "\n\nPlease ensure that input files do not contain Personally Identifiable Information (PII).",
        ButtonType.OK, ButtonType.CANCEL);
    alert.setTitle("PII Warning");
    alert.setHeaderText("");
    Optional<ButtonType> selVal = alert.showAndWait();
    return selVal;
  }

  static void showMessage_TestAlreadyExists(TestInfo file1, String subdir) {
    // notify user, show directory path and cancel operation
    Alert alert1 = new Alert(AlertType.ERROR, "Error - a test with the same ID / Label ("
        + file1.label
        + ") already exists.\n\nIf this is an error, please remove the test in the DonorCheck Testing Management tool.\n\nOr remove the test manually by deleting this directory:\n\n"
        + subdir, ButtonType.CLOSE);
    alert1.setTitle("Failed to Add Test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorCopyingRelFile(String relFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 = new Alert(AlertType.ERROR,
        "Error - could not copy serotype lookup file to validation testing directory.\n\nFile: "
            + relFile,
        ButtonType.CLOSE);
    alert1.setTitle("Failed to Add Test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorWritingRemapXMLFile(String remapFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 = new Alert(AlertType.ERROR,
        "Error - could not write locus remappings file to validation testing directory.\n\nFile: "
            + remapFile,
        ButtonType.CLOSE);
    alert1.setTitle("Failed to Add Test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorWritingPropertiesFile(String propsFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 = new Alert(AlertType.ERROR,
        "Error - could not write test properties file to validation testing directory.\n\nFile: "
            + propsFile,
        ButtonType.CLOSE);
    alert1.setTitle("Failed to Add Test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorCopyingInputFile(TestInfo file, String subdir) {
    // notify user, show directory path and cancel operation
    Alert alert1 = new Alert(AlertType.ERROR,
        "Error - could not copy input file to validation testing directory.\n\nFile: " + file.file
            + "\n\nDirectory: " + subdir,
        ButtonType.CLOSE);
    alert1.setTitle("Failed to Add Test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

}
