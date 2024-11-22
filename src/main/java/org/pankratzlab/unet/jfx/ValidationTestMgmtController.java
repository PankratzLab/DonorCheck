package org.pankratzlab.unet.jfx;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.validation.ValidationTestFileSet;
import org.pankratzlab.unet.validation.ValidationTesting;
import com.google.common.collect.Table;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
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
  TableColumn<ValidationTestFileSet, ObservableSet<SourceType>> testFileTypesColumn;

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

    testTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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

    passingStatusColumn.setCellFactory(
        new Callback<TableColumn<ValidationTestFileSet, Boolean>, TableCell<ValidationTestFileSet, Boolean>>() {

          @Override
          public TableCell<ValidationTestFileSet, Boolean> call(
              TableColumn<ValidationTestFileSet, Boolean> param) {
            return new TableCell<ValidationTestFileSet, Boolean>() {
              @Override
              protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                if (value == null) {
                  setText(null);
                  setStyle("");
                } else {
                  setText(value ? "Passing" : "Failing");
                  setStyle("-fx-background-color: " + (value ? "LimeGreen" : "OrangeRed"));
                }
              }
            };
          }
        });

    testFileTypesColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, ObservableSet<SourceType>>, ObservableValue<ObservableSet<SourceType>>>() {
          public ObservableValue<ObservableSet<SourceType>> call(
              CellDataFeatures<ValidationTestFileSet, ObservableSet<SourceType>> p) {
            return p.getValue().sourceTypes;
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
    ((Stage) closeButton.getScene().getWindow()).close();
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
      Map<ValidationTestFileSet, Boolean> results = ValidationTesting.deleteTests(toRemove);
      Set<ValidationTestFileSet> removed = results.entrySet().stream().filter(e -> e.getValue())
          .map(Map.Entry::getKey).collect(Collectors.toSet());
      Set<ValidationTestFileSet> notRemoved = results.entrySet().stream().filter(e -> !e.getValue())
          .map(Map.Entry::getKey).collect(Collectors.toSet());
      testTable.getItems().removeAll(toRemove);

      if (removed.size() == toRemove.size()) {
        Alert alert1 = new Alert(AlertType.INFORMATION,
            "Successfully removed " + toRemove.size() + " test" + (toRemove.size() > 1 ? "s" : ""),
            ButtonType.CLOSE);
        alert1.setTitle("Successfully Removed Tests");
        alert1.setHeaderText("");
        alert1.showAndWait();
      } else {
        Alert alert1 = new Alert(AlertType.WARNING,
            "Failed to remove " + notRemoved.size() + " test" + (notRemoved.size() > 1 ? "s" : "")
                + "\n\nSuccessfully removed " + removed.size() + " test"
                + (removed.size() > 1 ? "s" : ""),
            ButtonType.CLOSE);
        alert1.setTitle("Failed to Remove Tests");
        alert1.setHeaderText("");
        alert1.showAndWait();
      }

    }

  }

  @FXML
  void runSelected() {
    runTasks(testTable.selectionModelProperty().get().getSelectedItems());
  }

  @FXML
  void runAll() {
    runTasks(testTable.getItems());
  }

  private void runTasks(List<ValidationTestFileSet> tests) {
    // run on background thread

    List<ValidationTestFileSet> sorted = ValidationTesting.sortTests(tests);

    List<Runnable> tasks = sorted.stream().map(t -> new Runnable() {
      @Override
      public void run() {
        ValidationTesting.runTest(t);
      }
    }).collect(Collectors.toList());

    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(tasks);

    EventHandler<WorkerStateEvent> showResults = e -> {
      // TODO do something with results
    };

    runValidationTask.addEventHandler(WorkerStateEvent.ANY, showResults);

    new Thread(runValidationTask).start();
  }


}
