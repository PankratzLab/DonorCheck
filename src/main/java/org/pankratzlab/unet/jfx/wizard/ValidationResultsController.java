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

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import org.pankratzlab.unet.model.ValidationRow;
import org.pankratzlab.unet.model.ValidationTable;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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

  private EventHandler<Event> handler;

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private ValidatingWizardPane rootPane;

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
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert resultsTable != null : "fx:id=\"resultsTable\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert rowLabelCol != null : "fx:id=\"rowLabelCol\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert firstSourceCol != null : "fx:id=\"firstSourceCol\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert isEqualCol != null : "fx:id=\"isEqualCol\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert secondSourceCol != null : "fx:id=\"secondSourceCol\" was not injected: check your FXML file 'StepFourResults.fxml'.";
    assert resultDisplayText != null : "fx:id=\"resultDisplayText\" was not injected: check your FXML file 'StepFourResults.fxml'.";

    rowLabelCol.setCellValueFactory(new PropertyValueFactory<>("id"));

    firstSourceCol.setCellValueFactory(new PropertyValueFactory<>("firstCol"));

    isEqualCol.setCellValueFactory(new PropertyValueFactory<>("isValid"));

    secondSourceCol.setCellValueFactory(new PropertyValueFactory<>("secondCol"));

    isEqualCol.setCellFactory(new PassFailCellFactory());
    firstSourceCol.setCellFactory(new InvalidColorCellFactory());
    secondSourceCol.setCellFactory(new InvalidColorCellFactory());

    // Record an image of the validation state when entering this page
    rootPane.addEventHandler(PageActivatedEvent.PAGE_ACTIVE, e -> addFinishHandler());
  }

  @Override
  protected void refreshTable(ValidationTable table) {
    // Can only finish the wizard if the validation is successful
    rootPane.setInvalidBinding(table.isValidProperty().not());
    resultsTable.setItems(table.getRows());
    table.isValidProperty().addListener(e -> updateDisplay(table.isValidProperty().get()));
    firstSourceCol.textProperty().bind(table.firstColSource());
    secondSourceCol.textProperty().bind(table.secondColSource());
  }

  /**
   * Add an event handler to save the validation image when the Finish button is clicked
   */
  private void addFinishHandler() {
    if (handler == null) {
      handler = (e) -> getTable().setValidationImage(rootPane.snapshot(null, null));
      Button finishButton = (Button) rootPane.lookupButton(ButtonType.FINISH);
      finishButton.addEventHandler(Event.ANY, handler);
    }
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
}
