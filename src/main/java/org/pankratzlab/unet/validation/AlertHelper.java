package org.pankratzlab.unet.validation;

import java.io.File;
import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public final class AlertHelper {

  public static void showMessage_ErrorUpdatingTestProperties(
      ValidationTestFileSet rowValue, Throwable cause) {
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - unable to update test properties for test "
                + rowValue.id.get()
                + ". An exception occurred:\n"
                + cause.getMessage(),
            ButtonType.CLOSE);
    alert1.setTitle("Failed to update test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  public static void showMessage_ErrorUpdatingTestID(String id, String newId, Throwable cause) {
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - unable to rename test from "
                + id
                + " to "
                + newId
                + ". An exception occurred when moving the directory:\n"
                + cause.getMessage(),
            ButtonType.CLOSE);
    alert1.setTitle("Failed to rename test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static Optional<ButtonType> showMessage_PII() {
    Alert alert =
        new Alert(
            AlertType.WARNING,
            "Caution - DonorCheck will retain a copy of the current input files here:\n\n"
                + ValidationTesting.VALIDATION_DIRECTORY
                + "\n\nPlease ensure that:\n\n"
                + "1) The given input files do not contain Personally Identifiable Information (PII), or\n\n"
                + "2) If the input files do contain PII, that this location is safe to store PII.",
            ButtonType.OK,
            ButtonType.CANCEL);
    alert.setTitle("PII warning");
    alert.setHeaderText("");
    Optional<ButtonType> selVal = alert.showAndWait();
    return selVal;
  }

  static Optional<String> showMessage_TestAlreadyExists(TestInfo file1, String subdir) {
    // notify user, show directory path and cancel operation
    TextInputDialog dialog = new TextInputDialog(file1.label);
    dialog.setTitle("Test already exists");
    dialog.setHeaderText("A test with the same ID / Label (" + file1.label + ") already exists.");
    String contentText =
        "If this is an error, please remove the test in the DonorCheck Testing Management tool.\n\nOr remove the test manually by deleting this directory:\n\n"
            + new File(subdir).getAbsolutePath()
            + File.separator
            + "\n\nOtherwise, provide a different name for the new test:";

    TextField textField = dialog.getEditor();
    Label label = new Label(contentText);

    VBox vBox = new VBox(label, textField);
    vBox.setAlignment(Pos.CENTER_LEFT);
    vBox.setSpacing(10);

    dialog.getDialogPane().setContent(vBox);
    Optional<String> result = dialog.showAndWait();
    return result;
  }

  static void showMessage_ErrorCopyingRelFile(String relFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - could not copy serotype lookup file to validation testing directory.\n\nFile: "
                + relFile,
            ButtonType.CLOSE);
    alert1.setTitle("Failed to add test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorWritingRemapXMLFile(String remapFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - could not write locus remappings file to validation testing directory.\n\nFile: "
                + remapFile,
            ButtonType.CLOSE);
    alert1.setTitle("Failed to add test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorWritingPropertiesFile(String propsFile) {
    // notify user, show directory path and cancel operation
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - could not write test properties file to validation testing directory.\n\nFile: "
                + propsFile,
            ButtonType.CLOSE);
    alert1.setTitle("Failed to add test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }

  static void showMessage_ErrorCopyingInputFile(TestInfo file, String subdir) {
    Alert alert1 =
        new Alert(
            AlertType.ERROR,
            "Error - could not copy input file to validation testing directory.\n\nFile: "
                + file.file
                + "\n\nDirectory: "
                + subdir,
            ButtonType.CLOSE);
    alert1.setTitle("Failed to add test");
    alert1.setHeaderText("");
    alert1.showAndWait();
  }
}
