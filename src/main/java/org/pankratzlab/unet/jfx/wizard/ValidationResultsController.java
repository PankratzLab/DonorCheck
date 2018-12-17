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
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import javax.imageio.ImageIO;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.BCHaplotypeRow;
import org.pankratzlab.unet.model.DRDQHaplotypeRow;
import org.pankratzlab.unet.model.HaplotypeRow;
import org.pankratzlab.unet.model.ValidationRow;
import org.pankratzlab.unet.model.ValidationTable;
import com.google.common.collect.ImmutableSet;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

/**
 * Controller for viewing the final results status.
 */
public class ValidationResultsController extends AbstractValidatingWizardController {
  private static final String INVALID_STYLE_CLASS = "invalid-cell";
  private static final String WD_ALLELE_CLASS = "well-documented-allele";
  private static final String UK_ALLELE_CLASS = "unknown-allele";
  private static final String UNKNOWN_HAPLOTYPE_CLASS = "unknown-haplotype";
  private static final Set<String> HAPLOTYPE_CLASSES =
      ImmutableSet.of(WD_ALLELE_CLASS, UK_ALLELE_CLASS, UNKNOWN_HAPLOTYPE_CLASS);

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private ValidatingWizardPane rootPane;

  @FXML
  private MenuItem saveOption;

  @FXML
  private MenuItem printOption;

  @FXML
  private TableView<ValidationRow<?>> resultsTable;

  @FXML
  private TableColumn<ValidationRow<?>, String> rowLabelCol;

  @FXML
  private TableColumn<ValidationRow<?>, String> firstSourceCol;

  @FXML
  private TableColumn<ValidationRow<?>, Boolean> isEqualCol;

  @FXML
  private TableColumn<ValidationRow<?>, String> secondSourceCol;

  @FXML
  private Label resultDisplayText;

  @FXML
  private TableView<BCHaplotypeRow> bcHaplotypeTable;

  @FXML
  private TableColumn<BCHaplotypeRow, String> bcEthnicityColumn;

  @FXML
  private TableColumn<BCHaplotypeRow, String> haplotypeCAlleleColumn;

  @FXML
  private TableColumn<BCHaplotypeRow, String> haplotypeBAlleleColumn;

  @FXML
  private TableColumn<BCHaplotypeRow, String> haplotypeBwColumn;

  @FXML
  private TableView<DRDQHaplotypeRow> drdqHaplotypeTable;

  @FXML
  private TableColumn<DRDQHaplotypeRow, String> drdqEthnicityColumn;

  @FXML
  private TableColumn<DRDQHaplotypeRow, String> haplotypeDRB345AlleleColumn;

  @FXML
  private TableColumn<DRDQHaplotypeRow, String> haplotypeDRB1AlleleColumn;

  @FXML
  private TableColumn<DRDQHaplotypeRow, String> haplotypeDQB1Column;

  /**
   * Write the given {@link ValidationTable#getValidationImage()} to disk
   */
  @FXML
  void saveResults(ActionEvent event) {
    Optional<File> destination = DonorNetUtils.getFile(rootPane, "Save Validation Results",
        getTable().getId() + "_donor_valid", "PNG", ".png", false);

    if (destination.isPresent()) {
      try {
        ImageIO.write(SwingFXUtils.fromFXImage(rootPane.snapshot(null, null), null), "png",
            destination.get());
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText("Saved validation image to: " + destination.get().getName());
        alert.showAndWait();
      } catch (IOException e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setHeaderText("Failed to save results to file: " + destination.get().getName());
        alert.showAndWait();
      }
    }
  }

  @FXML
  void printResults(ActionEvent event) {
    ChoiceDialog<Printer> dialog =
        new ChoiceDialog<>(Printer.getDefaultPrinter(), Printer.getAllPrinters());
    dialog.setHeaderText("Select a printer");
    dialog.setContentText("Choose a printer from available printers");
    dialog.setTitle("Printer Choice");
    Optional<Printer> opt = dialog.showAndWait();
    if (opt.isPresent()) {
      Printer printer = opt.get();
      // start printing ...
      PrinterJob job = PrinterJob.createPrinterJob(printer);
      if (job != null) {
        boolean success = job.printPage(rootPane);
        if (success) {
          job.endJob();
          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setHeaderText("Successfully printed validation results!");
          alert.showAndWait();
        }
      }
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert saveOption != null : "fx:id=\"saveOption\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert printOption != null : "fx:id=\"printOption\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert resultsTable != null : "fx:id=\"resultsTable\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert rowLabelCol != null : "fx:id=\"rowLabelCol\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert firstSourceCol != null : "fx:id=\"firstSourceCol\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert isEqualCol != null : "fx:id=\"isEqualCol\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert secondSourceCol != null : "fx:id=\"secondSourceCol\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert resultDisplayText != null : "fx:id=\"resultDisplayText\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert bcHaplotypeTable != null : "fx:id=\"haplotypeTable\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert bcEthnicityColumn != null : "fx:id=\"ethnicityColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeCAlleleColumn != null : "fx:id=\"haplotypeCAlleleColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeBAlleleColumn != null : "fx:id=\"haplotypeBAlleleColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeBwColumn != null : "fx:id=\"haplotypeBwColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert drdqHaplotypeTable != null : "fx:id=\"drdqHaplotypeTable\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert drdqEthnicityColumn != null : "fx:id=\"drdqEthnicityColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeDRB345AlleleColumn != null : "fx:id=\"haplotypeDRB345AlleleColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeDRB1AlleleColumn != null : "fx:id=\"haplotypeDRB1AlleleColumn\" was not injected: check your FXML file 'ValidationResults.fxml'.";
    assert haplotypeDQB1Column != null : "fx:id=\"haplotypeDQB1Column\" was not injected: check your FXML file 'ValidationResults.fxml'.";

    // Configure validation results table columns
    rowLabelCol.setCellValueFactory(new PropertyValueFactory<>(ValidationRow.ID_PROP));
    firstSourceCol.setCellValueFactory(new PropertyValueFactory<>(ValidationRow.FIRST_COL_PROP));
    isEqualCol.setCellValueFactory(new PropertyValueFactory<>(ValidationRow.IS_VALID_PROP));
    secondSourceCol.setCellValueFactory(new PropertyValueFactory<>(ValidationRow.SECOND_COL_PROP));

    isEqualCol.setCellFactory(new PassFailCellFactory());
    firstSourceCol.setCellFactory(new InvalidColorCellFactory());
    secondSourceCol.setCellFactory(new InvalidColorCellFactory());

    // Configure haplotype table columns
    bcEthnicityColumn
        .setCellValueFactory(new PropertyValueFactory<>(BCHaplotypeRow.ETHNICITY_PROP));
    haplotypeCAlleleColumn
        .setCellValueFactory(new PropertyValueFactory<>(BCHaplotypeRow.C_ALLELE_PROP));
    haplotypeBAlleleColumn
        .setCellValueFactory(new PropertyValueFactory<>(BCHaplotypeRow.B_ALLELE_PROP));
    haplotypeBwColumn.setCellValueFactory(new PropertyValueFactory<>(BCHaplotypeRow.BW_GROUP_PROP));

    haplotypeCAlleleColumn.setCellFactory(new HaplotypeCellFactory<>());
    haplotypeBAlleleColumn.setCellFactory(new HaplotypeCellFactory<>());

    drdqEthnicityColumn
        .setCellValueFactory(new PropertyValueFactory<>(DRDQHaplotypeRow.ETHNICITY_PROP));
    haplotypeDRB345AlleleColumn
        .setCellValueFactory(new PropertyValueFactory<>(DRDQHaplotypeRow.DRB345_PROP));
    haplotypeDRB1AlleleColumn
        .setCellValueFactory(new PropertyValueFactory<>(DRDQHaplotypeRow.DRB1_ALLELE_PROP));
    haplotypeDQB1Column
        .setCellValueFactory(new PropertyValueFactory<>(DRDQHaplotypeRow.DQB1_ALLELE_PROP));

    haplotypeDRB345AlleleColumn.setCellFactory(new HaplotypeCellFactory<>());
    haplotypeDRB1AlleleColumn.setCellFactory(new HaplotypeCellFactory<>());
    haplotypeDQB1Column.setCellFactory(new HaplotypeCellFactory<>());

    // Record an image of the validation state when entering this page
    rootPane.addEventHandler(PageActivatedEvent.PAGE_ACTIVE, e -> performPageSetup());
  }

  @Override
  protected void refreshTable(ValidationTable table) {
    // Can only finish the wizard if the validation is successful
    rootPane.setInvalidBinding(table.isValidProperty().not());
    resultsTable.setItems(table.getValidationRows());
    table.isValidProperty().addListener(e -> updateDisplay(table.isValidProperty().get()));
    firstSourceCol.textProperty().bind(table.firstColSource());
    secondSourceCol.textProperty().bind(table.secondColSource());

    bcHaplotypeTable.setItems(table.getBCHaplotypeRows());
    drdqHaplotypeTable.setItems(table.getDRDQHaplotypeRows());
  }

  /**
   * Perform required actions when the page is being displayed
   */
  private void performPageSetup() {
    // Link the disable property of the Save menu option to that of the finish button
    Button finishButton = (Button) rootPane.lookupButton(ButtonType.FINISH);
    saveOption.disableProperty().bind(finishButton.disabledProperty());
    printOption.disableProperty().bind(finishButton.disabledProperty());
  }

  /**
   * Helper method to update the validation result text
   *
   * @param isValid Whether the validation is successful or not
   */
  private void updateDisplay(Boolean isValid) {
    String displayText = "Validation ";
    String displayStyle = "-fx-text-fill: ";

    if (isValid) {
      displayText += "Successful";
      displayStyle += "limegreen;";
    } else {
      displayText += "Failed";
      displayStyle += "red;";
    }
    resultDisplayText.setText(displayText);
    resultDisplayText.setStyle(displayStyle);
  }

  /**
   * Class for displaying an {@link Image} in a table cell. Used here to display pass/fail icons for
   * each row.
   *
   * @see https://stackoverflow.com/a/44807681/1027800
   */
  private static class PassFailCellFactory implements
      Callback<TableColumn<ValidationRow<?>, Boolean>, TableCell<ValidationRow<?>, Boolean>> {

    private final Image imageTrue = new Image(getClass().getResourceAsStream("/pass.png"));
    private final Image imageFalse = new Image(getClass().getResourceAsStream("/fail.png"));

    @Override
    public TableCell<ValidationRow<?>, Boolean> call(TableColumn<ValidationRow<?>, Boolean> arg0) {

      return new TableCell<ValidationRow<?>, Boolean>() {

        private final ImageView imageView = new ImageView();

        {
          // initialize ImageView + set as graphic
          imageView.setFitWidth(20);
          imageView.setFitHeight(20);
          setGraphic(imageView);
        }

        @Override
        protected void updateItem(Boolean item, boolean empty) {
          if (empty || item == null) {
            // no image for empty cells
            imageView.setImage(null);
          } else {
            // set image for non-empty cell
            imageView.setImage(item ? imageTrue : imageFalse);
          }
        }

      };
    }

  }

  /**
   * Helper class to color cells when the row is invalid
   */
  private static class InvalidColorCellFactory implements
      Callback<TableColumn<ValidationRow<?>, String>, TableCell<ValidationRow<?>, String>> {
    @Override
    public TableCell<ValidationRow<?>, String> call(TableColumn<ValidationRow<?>, String> param) {
      return new TableCell<ValidationRow<?>, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          setText(item);
          ValidationRow<?> row = (ValidationRow<?>) getTableRow().getItem();
          if (Objects.nonNull(row) && Objects.nonNull(row.isValidProperty())
              && !(row.isValidProperty().get())) {
            getStyleClass().add(0, INVALID_STYLE_CLASS);
          } else {
            ObservableList<String> styleList = getStyleClass();
            for (int i = 0; i < styleList.size(); i++) {
              if (styleList.get(i).equals(INVALID_STYLE_CLASS)) {
                styleList.remove(i);
                break;
              }
            }
          }
        }
      };
    }
  }

  /**
   * Helper class to color cells when the row's haplotype is unknown or individual alleles are not
   * common
   */
  private static class HaplotypeCellFactory <T extends HaplotypeRow>
      implements Callback<TableColumn<T, String>, TableCell<T, String>> {
    @Override
    public TableCell<T, String> call(TableColumn<T, String> param) {
      return new TableCell<T, String>() {
        @Override
        protected void updateItem(String alleleText, boolean empty) {
          setText(alleleText);

          ObservableList<String> styleList = getStyleClass();
          for (int i = 0; i < styleList.size(); i++) {
            if (HAPLOTYPE_CLASSES.contains(styleList.get(i))) {
              styleList.remove(i);
              break;
            }
          }

          HaplotypeRow row = (HaplotypeRow) getTableRow().getItem();
          if (Objects.nonNull(row) && Objects.nonNull(row.haplotypeProperty())) {
            switch (CommonWellDocumented.getStatus(HLAType.valueOf(alleleText))) {
              case UNKNOWN:
                getStyleClass().add(0, UK_ALLELE_CLASS);
                break;
              case WELL_DOCUMENTED:
                getStyleClass().add(0, WD_ALLELE_CLASS);
                break;
              case COMMON:
              default:
                break;
            }

            if (Double.compare(0.0, HaplotypeFrequencies.getFrequency(row.ethnicityProperty().get(),
                row.haplotypeProperty().get())) == 0) {
              getStyleClass().add(0, UNKNOWN_HAPLOTYPE_CLASS);
            }

          }
        }
      };
    }
  }
}
