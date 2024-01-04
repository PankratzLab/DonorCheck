/*-
 * #%L
 * DonorCheck
 * %%
 * Copyright (C) 2018 - 2019 Computational Pathology - University of Minnesota
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.pankratzlab.unet.jfx;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;

import org.pankratzlab.unet.deprecated.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;

import com.google.common.base.Strings;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/** Controller to facilitate the user changing the directory of the nmdp input. */
public class DownloadNMDPController {

  private static final Hyperlink NMDP_URL =
      new Hyperlink("http://frequency.nmdp.org/NMDPFrequencies2011/");

  private StringProperty cbString;
  private StringProperty drdqString;
  private boolean dirty;

  @FXML private ResourceBundle resources;

  @FXML private URL location;

  @FXML private AnchorPane rootPane;

  @FXML private TextField cbTable;

  @FXML private TextField drdqTable;

  @FXML
  void openDownload(ActionEvent event) {
    try {
      TypeValidationApp.hostServices.showDocument(NMDP_URL.getText());
    } catch (Exception exc) {
      // Probably no hostservices in this JVM due to openjdk issue
      Hyperlink copyLink = new Hyperlink(NMDP_URL.getText());
      copyLink.setOnAction(
          e -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(NMDP_URL.getText());
            clipboard.setContent(content);
            copyLink.setText("copied!");
            copyLink.setVisited(true);
          });
      JFXUtilHelper.makeContentOnlyAlert(
              AlertType.INFORMATION,
              "Please visit this URL to download NMDP codes",
              copyLink,
              ButtonType.OK)
          .showAndWait();
    }
  }

  @FXML
  void selectCBTable(ActionEvent event) {
    selectFile(event, "CB", cbString);
  }

  @FXML
  void selectDRDQTable(ActionEvent event) {
    selectFile(event, "DRB345-DRB1-DQB1", drdqString);
  }

  void selectFile(ActionEvent event, String tableTitle, StringProperty localProp) {
    FileChooser fc = CurrentDirectoryProvider.getFileChooser();
    fc.setTitle("Select " + tableTitle + " Frequency Table");
    fc.getExtensionFilters().setAll(new ExtensionFilter("Excel", "*.xls"));
    File tableFile = fc.showOpenDialog(rootPane.getScene().getWindow());
    if (Objects.nonNull(tableFile) && tableFile.exists()) {
      localProp.setValue(tableFile.getAbsolutePath());
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null
        : "fx:id=\"rootPane\" was not injected: check your FXML file 'NMDPDownloadPrompt.fxml'.";
    assert cbTable != null
        : "fx:id=\"cbTable\" was not injected: check your FXML file 'NMDPDownloadPrompt.fxml'.";
    assert drdqTable != null
        : "fx:id=\"drdqTable\" was not injected: check your FXML file 'NMDPDownloadPrompt.fxml'.";
    dirty = false;

    cbString = new SimpleStringProperty();
    init(cbString, cbTable, HaplotypeFrequencies.NMDP_CB_PROP);

    drdqString = new SimpleStringProperty();
    init(drdqString, drdqTable, HaplotypeFrequencies.NMDP_DRDQ_PROP);
  }

  /** Link together a local property, text display and global property */
  private void init(StringProperty localProp, TextField tableField, String propertyName) {
    tableField.textProperty().bind(localProp);
    String nmdpProp = HLAProperties.get().getProperty(propertyName);

    if (!Strings.isNullOrEmpty(nmdpProp)) {
      if (!new File(nmdpProp).exists()) {
        nmdpProp = null;
      } else {
        localProp.set(nmdpProp);
      }
    }

    localProp.addListener(
        (obs, o, n) -> {
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
