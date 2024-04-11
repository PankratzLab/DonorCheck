package org.pankratzlab.unet.jfx;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import com.google.common.base.Strings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class SerotypeLookupFileController {

  private static final Hyperlink FILE_URL =
      new Hyperlink("https://github.com/ANHIG/IMGTHLA/blob/Latest/wmda/rel_dna_ser.txt");

  private StringProperty fileString;
  private boolean dirty;

  @FXML
  private AnchorPane rootPane;

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private TextField relserFile;

  @FXML
  private Label relserVer;

  @FXML
  void openDownload(ActionEvent event) {
    try {
      TypeValidationApp.hostServices.showDocument(FILE_URL.getText());
    } catch (Exception exc) {
      // Probably no hostservices in this JVM due to openjdk issue
      Hyperlink copyLink = new Hyperlink(FILE_URL.getText());
      copyLink.setOnAction(e -> {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(FILE_URL.getText());
        clipboard.setContent(content);
        copyLink.setText("copied!");
        copyLink.setVisited(true);
      });
      JFXUtilHelper.makeContentOnlyAlert(AlertType.INFORMATION,
          "Please visit this URL to download the HLA Serotype Lookup file", copyLink, ButtonType.OK)
          .showAndWait();
    }
  }

  @FXML
  void selectFile(ActionEvent event) {
    FileChooser fc = CurrentDirectoryProvider.getFileChooser();
    fc.setTitle("Select HLA Serotype Lookup file");
    fc.getExtensionFilters().setAll(new ExtensionFilter("Text", "*.txt"));
    File relSerFile = fc.showOpenDialog(rootPane.getScene().getWindow());
    if (Objects.nonNull(relSerFile) && relSerFile.exists()) {
      fileString.setValue(relSerFile.getAbsolutePath());
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'RelDnaSerDownloadPrompt.fxml'.";
    assert relserFile != null : "fx:id=\"relserFile\" was not injected: check your FXML file 'RelDnaSerDownloadPrompt.fxml'.";
    assert relserVer != null : "fx:id=\"relserVer\" was not injected: check your FXML file 'RelDnaSerDownloadPrompt.fxml'.";
    dirty = false;

    fileString = new SimpleStringProperty();
    init(fileString, relserFile, AntigenDictionary.REL_DNA_SER_PROP);
    relserVer.setText(AntigenDictionary.getVersion());
  }

  /** Link together a local property, text display and global property */
  private void init(StringProperty localProp, TextField tableField, String propertyName) {
    tableField.textProperty().bind(localProp);
    String fileProp = HLAProperties.get().getProperty(propertyName);

    if (!Strings.isNullOrEmpty(fileProp)) {
      if (!new File(fileProp).exists()) {
        fileProp = null;
      } else {
        localProp.set(fileProp);
      }
    }

    localProp.addListener((obs, o, n) -> {
      if (!Strings.isNullOrEmpty(n) && Files.exists(Paths.get(n))) {
        HLAProperties.get().setProperty(propertyName, n);
        dirty = true;
      }
    });
  }


  /** @return True if this controller was used to modify any properties */
  public boolean isDirty() {
    return dirty;
  }

}
