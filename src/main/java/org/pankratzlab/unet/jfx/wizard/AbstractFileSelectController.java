/*
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
package org.pankratzlab.unet.jfx.wizard;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import org.pankratzlab.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;

/**
 * Abstract superclass for controller instances controlling file-selection wizard pages.
 */
public abstract class AbstractFileSelectController extends AbstractValidatingWizardController {

  private ReadOnlyObjectWrapper<File> selectedFileProperty = new ReadOnlyObjectWrapper<>();

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private TextField donorFileDisplay;

  @FXML
  private ValidatingWizardPane rootPane;

  @FXML
  void selectDonorFile(ActionEvent event) {
    Optional<File> optionalFile = DonorNetUtils.getFile(event, fileChooserHeader(), initialName(),
        extensionDesc(), extension(), true);

    if (optionalFile.isPresent()) {
      File selectedFile = optionalFile.get();

      ValidationModelBuilder builder = new ValidationModelBuilder();

      try {
        parseModel(builder, selectedFile);
        setModel().accept(getTable(), builder.build());
        selectedFileProperty.set(selectedFile);
      } catch (Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setHeaderText("Error reading donor typing: " + selectedFile.getName());
        alert.showAndWait();
      }

      CurrentDirectoryProvider.setBaseDir(selectedFile.getParentFile());
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'StepThreeInputPDF.fxml'.";
    assert donorFileDisplay != null : "fx:id=\"donorPDFFile\" was not injected: check your FXML file 'StepThreeInputPDF.fxml'.";

    rootPane.setInvalidBinding(Bindings.createBooleanBinding(
        () -> Objects.isNull(selectedFileProperty.get()), selectedFileProperty));

    donorFileDisplay.textProperty().bind(Bindings
        .createStringBinding(() -> getName(selectedFileProperty.get()), selectedFileProperty));

    rootPane.setUserData(wizardPaneTitle());
  }

  private String getName(File file) {
    if (Objects.isNull(file)) {
      return "";
    }
    return file.getName();
  }

  /**
   * @return The string to set as the Wizard title
   */
  protected abstract String wizardPaneTitle();

  /**
   * @return File filter description when selecting a file
   */
  protected abstract String extensionDesc();

  /**
   * @return Display header when selecting a file
   */
  protected abstract String fileChooserHeader();

  /**
   * @return Initial file name when selecting a file
   */
  protected abstract String initialName();

  /**
   * @return File filter extension. Used to restrict file types.
   */
  protected abstract String extension();

  /**
   * @return {@link ValidationTable} method to call after parsing the model (e.g.
   *         {@link ValidationTable#setPdfModel} for a PDF input controller
   */
  protected abstract BiConsumer<ValidationTable, ValidationModel> setModel();

  /**
   * Populate the given {@link ValidationModelBuilder} based on the File's contents. Only types
   * allowed by the {@link #extension()} filter will be passed here.
   */
  protected abstract void parseModel(ValidationModelBuilder builder, File file);
}
