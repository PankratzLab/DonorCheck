package org.pankratzlab.unet.jfx;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.validation.ValidationTestFileSet;
import org.pankratzlab.unet.validation.ValidationTesting;
import com.google.common.collect.Table;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class ValidationTestMgmtController {

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  TableView<ValidationTestFileSet> testTable;

  @FXML
  TableColumn<ValidationTestFileSet, String> testIDColumn;

  @FXML
  TableColumn<ValidationTestFileSet, Date> lastRunColumn;

  @FXML
  TableColumn<ValidationTestFileSet, Boolean> passingStatusColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> testFileTypesColumn;

  @FXML
  TableColumn<ValidationTestFileSet, CommonWellDocumented.SOURCE> ciwdVersionColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> relDnaSerVersion;

  @FXML
  Button closeButton;

  @FXML
  Button removeSelectedButton;

  @FXML
  Button runSelectedButton;

  @FXML
  Button runAllButton;

  @FXML
  void initialize() {
    assert testTable != null : "fx:id=\"testTable\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testIDColumn != null : "fx:id=\"testIDColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert lastRunColumn != null : "fx:id=\"lastRunColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert passingStatusColumn != null : "fx:id=\"passingStatusColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testFileTypesColumn != null : "fx:id=\"testFileTypesColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert ciwdVersionColumn != null : "fx:id=\"ciwdVersionColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert relDnaSerVersion != null : "fx:id=\"relDnaSerVersion\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert closeButton != null : "fx:id=\"closeButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert removeSelectedButton != null : "fx:id=\"removeSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runSelectedButton != null : "fx:id=\"runSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runAllButton != null : "fx:id=\"runAllButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";

    testIDColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            return p.getValue().id;
          }
        });

    lastRunColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, Date>, ObservableValue<Date>>() {
          public ObservableValue<Date> call(CellDataFeatures<ValidationTestFileSet, Date> p) {
            return p.getValue().lastRunDate;
          }
        });

    passingStatusColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, Boolean>, ObservableValue<Boolean>>() {
          public ObservableValue<Boolean> call(CellDataFeatures<ValidationTestFileSet, Boolean> p) {
            return p.getValue().lastPassingState;
          }
        });

    testFileTypesColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            // TODO convert file paths to file types
            return new SimpleStringProperty();
          }
        });

    ciwdVersionColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, CommonWellDocumented.SOURCE>, ObservableValue<CommonWellDocumented.SOURCE>>() {
          public ObservableValue<CommonWellDocumented.SOURCE> call(
              CellDataFeatures<ValidationTestFileSet, CommonWellDocumented.SOURCE> p) {
            return p.getValue().cwdSource;
          }
        });

    relDnaSerVersion.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            return p.getValue().relDnaSerFile.map(AntigenDictionary::getVersion).orElse("");
          }
        });

    removeSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    runSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    runAllButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getItems().isEmpty();
    }, testTable.itemsProperty()));

  }

  public void setTable(Table<SOURCE, String, List<ValidationTestFileSet>> testData) {
    ObservableList<ValidationTestFileSet> testList = FXCollections.observableArrayList(
        testData.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    testTable.setItems(testList);
  }

  @FXML
  void close() {
    // TODO
  }

  @FXML
  void removeSelected() {
    List<ValidationTestFileSet> toRemove =
        testTable.selectionModelProperty().get().getSelectedItems();

    Alert alert =
        new Alert(AlertType.CONFIRMATION, "Are you sure you'd like to remove " + toRemove.size()
            + " test" + (toRemove.size() > 1 ? "s" : "") + "?", ButtonType.OK, ButtonType.CANCEL);
    alert.setTitle("Remove Tests");
    alert.setHeaderText("");
    Optional<ButtonType> selVal = alert.showAndWait();

    if (selVal.isPresent() && selVal.get() == ButtonType.OK) {
      ValidationTesting.deleteTests(toRemove);
      testTable.getItems().removeAll(toRemove);
    }

  }

  @FXML
  void runSelected() {
    // run on background thread
    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(() -> {
      ValidationTesting.runTests(testTable.selectionModelProperty().get().getSelectedItems());
    });

    EventHandler<WorkerStateEvent> showResults = e -> {
      // TODO do something with results
    };

    runValidationTask.addEventHandler(WorkerStateEvent.ANY, showResults);

    new Thread(runValidationTask).start();
  }

  @FXML
  void runAll() {
    // run on background thread
    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(() -> {
      ValidationTesting.runTests(testTable.getItems());
    });

    EventHandler<WorkerStateEvent> showResults = e -> {
      // TODO do something with results
    };

    runValidationTask.addEventHandler(WorkerStateEvent.ANY, showResults);

    new Thread(runValidationTask).start();
  }

}
