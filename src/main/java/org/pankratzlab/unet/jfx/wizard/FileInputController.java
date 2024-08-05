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
package org.pankratzlab.unet.jfx.wizard;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.pankratzlab.BackgroundDataProcessor;
import org.pankratzlab.unet.deprecated.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.jfx.JFXPropertyHelper;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.ValidationResult;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.model.remap.GUIRemapProcessor;
import org.pankratzlab.unet.model.remap.GUIRemapProcessor.PresentableAlleleChoices;
import org.pankratzlab.unet.parser.DonorFileParser;
import org.pankratzlab.unet.parser.HtmlDonorParser;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.collect.ImmutableList;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Controller for adding donor data to the current {@link ValidationTable}
 *
 * @see ValidatingWizardController
 */
public class FileInputController extends AbstractValidatingWizardController {

  private static final String FILE_DISPLAY_CLASS = "file-display";

  private ObservableList<ReadOnlyObjectWrapper<File>> selectedFileProperties =
      FXCollections.observableArrayList();

  private ObservableList<DonorFileParser> availableFileTypes = FXCollections.observableArrayList(
      ImmutableList.of(new PdfDonorParser(), new XmlDonorParser(), new HtmlDonorParser()));

  private ObservableBooleanValue invalidBinding;

  @FXML
  private VBox inputFiles_VBox;

  @FXML
  void initialize() {
    invalidBinding = Bindings.createBooleanBinding(() -> selectedFileProperties.isEmpty(),
        selectedFileProperties);

    inputFiles_VBox.getChildren()
        .add(createFileBox(PdfDonorParser.class, ValidationTable::setFirstModel));
    inputFiles_VBox.getChildren()
        .add(createFileBox(XmlDonorParser.class, ValidationTable::setSecondModel));

    rootPane().setInvalidBinding(invalidBinding);
  }

  private HBox createFileBox(Class<? extends DonorFileParser> defaultSelected,
      BiConsumer<ValidationTable, ValidationModel> setter) {
    ReadOnlyObjectWrapper<File> linkedFile = new ReadOnlyObjectWrapper<>();

    // Update the invalidation binding
    invalidBinding = JFXPropertyHelper.orHelper(invalidBinding,
        Bindings.createBooleanBinding(() -> Objects.isNull(linkedFile.get()), linkedFile));

    rootPane().setInvalidBinding(invalidBinding);

    selectedFileProperties.add(linkedFile);

    HBox hbox = new HBox(15.0);
    hbox.setAlignment(Pos.BASELINE_RIGHT);

    ComboBox<DonorFileParser> comboBox = new ComboBox<>(availableFileTypes);
    for (DonorFileParser file : availableFileTypes) {
      if (Objects.equals(file.getClass(), defaultSelected)) {
        comboBox.getSelectionModel().select(file);
        break;
      }
    }
    comboBox.getSelectionModel().selectedItemProperty()
        .addListener((v, o, n) -> linkedFile.set(null));
    comboBox.setPrefWidth(100);

    hbox.getChildren().add(comboBox);

    TextField fileDisplay = new TextField();
    fileDisplay.setDisable(true);
    fileDisplay.setEditable(false);
    fileDisplay.setPrefColumnCount(20);
    fileDisplay.setPromptText("no file selected");

    // Link text field and file
    linkedFile.getReadOnlyProperty()
        .addListener((b, o, n) -> updateFileDisplay(fileDisplay, linkedFile.get()));

    hbox.getChildren().add(fileDisplay);

    Button chooseFileButton = new Button("Choose File");
    chooseFileButton.setFont(Font.font(16.0));
    chooseFileButton.setOnAction(e -> selectDonorFile(e,
        comboBox.getSelectionModel().getSelectedItem(), setter, linkedFile));
    hbox.getChildren().add(chooseFileButton);

    return hbox;
  }

  private void selectDonorFile(ActionEvent event, DonorFileParser donorParser,
      BiConsumer<ValidationTable, ValidationModel> setter, ReadOnlyObjectWrapper<File> linkedFile) {

    Optional<File> optionalFile = DonorNetUtils.getFile(((Node) event.getSource()),
        donorParser.fileChooserHeader(), donorParser.initialName(),
        donorParser.extensionDescription(), donorParser.extensionFilter(), true);

    if (optionalFile.isPresent()) {

      // load data
      Task<Void> loadFileTask = JFXUtilHelper.createProgressTask(() -> {
        File selectedFile = optionalFile.get();

        ValidationModelBuilder builder = new ValidationModelBuilder();

        try {
          // parse the model
          donorParser.parseModel(builder, selectedFile);

          // check that the model is valid
          ValidationResult validationResult = builder.validate();

          if (!validationResult.valid) {
            // if a value is present, show error message
            alertInvalid(donorParser, selectedFile, validationResult);
          }

          // check for corrections
          if (builder.hasCorrections()) {

            BackgroundDataProcessor<HLALocus, PresentableAlleleChoices> choiceSupplier =
                new BackgroundDataProcessor<>(builder.getNonCWDLoci(),
                    (locus) -> PresentableAlleleChoices.create(locus,
                        builder.getAllelePairs(locus)),
                    (t) -> {
                      t.printStackTrace();
                      return null;
                    });
            GUIRemapProcessor processor = new GUIRemapProcessor(choiceSupplier);

            Platform.runLater(() -> {
              try {
                ValidationResult validationResult1 = builder.processCorrections(processor);
                if (!validationResult1.valid) {
                  alertInvalid(donorParser, selectedFile, validationResult);
                } else {
                  finish(donorParser, setter, linkedFile, selectedFile, builder);
                }
              } catch (Throwable e) {
                Platform.runLater(() -> {
                  Alert alert = new Alert(AlertType.ERROR);
                  alert.setHeaderText(donorParser.getErrorText()
                      + "\nPlease notify the developers as this may indicate the data has changed."
                      + "\nOffending file: " + selectedFile.getName());
                  alert.showAndWait();
                  e.printStackTrace();
                });
              }
            });

          } else {
            Platform.runLater(() -> {
              finish(donorParser, setter, linkedFile, selectedFile, builder);
            });
          }

        } catch (Throwable e) {
          Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setHeaderText(donorParser.getErrorText()
                + "\nPlease notify the developers as this may indicate the data has changed."
                + "\nOffending file: " + selectedFile.getName());
            alert.showAndWait();
            e.printStackTrace();
          });
        }

        CurrentDirectoryProvider.setBaseDir(selectedFile.getParentFile());
      });

      new Thread(loadFileTask).start();
    }
  }

  private void alertInvalid(DonorFileParser donorParser, File selectedFile,
      ValidationResult validationResult) {
    if (validationResult.validationMessage.isPresent()) {
      Platform.runLater(() -> {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setHeaderText(donorParser.getErrorText()
            + "\nPlease notify the developers as this may indicate the data has changed."
            + "\nOffending file: " + selectedFile.getName());
        alert.showAndWait();
      });
    }
  }

  private void finish(DonorFileParser donorParser,
      BiConsumer<ValidationTable, ValidationModel> setter, ReadOnlyObjectWrapper<File> linkedFile,
      File selectedFile, ValidationModelBuilder builder) {
    Task<Void> buildModelText = JFXUtilHelper.createProgressTask(() -> {
      // valid model, build and set
      try {
        setter.accept(getTable(), builder.build());
        linkedFile.set(selectedFile);
      } catch (Throwable e) {
        Platform.runLater(() -> {
          Alert alert = new Alert(AlertType.ERROR);
          alert.setHeaderText(donorParser.getErrorText()
              + "\nPlease notify the developers as this may indicate the data has changed."
              + "\nOffending file: " + selectedFile.getName());
          alert.showAndWait();
          e.printStackTrace();
        });
      }

    });
    new Thread(buildModelText).start();
  }

  /**
   * The {@link FileInputController#FILE_DISPLAY_CLASS} value links to the
   * {@link-plain resources/fileInput.css} style-sheet.
   * 
   * We only want to apply this style when a file has been selected, so we remove this style class
   * from the TextField and only add it back if the {@code file} argument is not null.
   */
  private void updateFileDisplay(TextField fileDisplay, File file) {
    String text = "";
    for (int i = 0; i < fileDisplay.getStyleClass().size(); i++) {
      if (Objects.equals(FILE_DISPLAY_CLASS, fileDisplay.getStyleClass().get(i))) {
        fileDisplay.getStyleClass().remove(i);
        break;
      }
    }

    if (Objects.nonNull(file)) {
      text = file.getName();
      fileDisplay.getStyleClass().add(FILE_DISPLAY_CLASS);
    }

    if (Objects.nonNull(fileDisplay)) {
      fileDisplay.setText(text);
    }
  }
}
