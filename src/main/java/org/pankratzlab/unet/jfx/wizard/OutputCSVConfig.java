package org.pankratzlab.unet.jfx.wizard;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import org.pankratzlab.unet.validation.ValidationTesting.OutputConfig;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class OutputCSVConfig {

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private CheckBox includeIDs;

  @FXML
  private CheckBox maskIDs;

  @FXML
  private TextField outputFileField;

  @FXML
  private Button selectFileBtn;

  @FXML
  void initialize() {
    assert includeIDs != null : "fx:id=\"includeIDs\" was not injected: check your FXML file 'Untitled'.";
    assert maskIDs != null : "fx:id=\"maskIDs\" was not injected: check your FXML file 'Untitled'.";
    assert outputFileField != null : "fx:id=\"outputFileField\" was not injected: check your FXML file 'Untitled'.";
    assert selectFileBtn != null : "fx:id=\"selectFileBtn\" was not injected: check your FXML file 'Untitled'.";

    maskIDs.disableProperty().bind(Bindings.createBooleanBinding(() -> !includeIDs.isSelected(), includeIDs.selectedProperty()));

    selectFileBtn.setOnAction((e) -> {
      FileChooser fc = new FileChooser();
      fc.setTitle("Select output file");
      ExtensionFilter filter = new ExtensionFilter("Excel XLSX file", "*.xlsx");
      fc.getExtensionFilters().clear();
      fc.getExtensionFilters().add(filter);
      fc.setSelectedExtensionFilter(filter);
      File file = fc.showSaveDialog(outputFileField.getScene().getWindow());
      if (file != null) {
        outputFileField.setText(file.getAbsolutePath());
      }
    });
  }



  public OutputConfig getConfig() {
    return new OutputConfig(outputFileField.getText(), includeIDs.isSelected(), maskIDs.isSelected());
  }



  public void setOKButton(Button okBtn) {
    ((Button) okBtn).disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return outputFileField.getText().isEmpty();
    }, outputFileField.textProperty()));
  }

}
