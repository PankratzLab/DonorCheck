package org.pankratzlab.unet.jfx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.LinearFlow;
import org.controlsfx.dialog.WizardPane;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.hla.Info;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.jfx.wizard.ValidationResultsController;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.validation.AlertHelper;
import org.pankratzlab.unet.validation.TEST_RESULT;
import org.pankratzlab.unet.validation.ValidationTestFileSet;
import org.pankratzlab.unet.validation.ValidationTesting;
import org.pankratzlab.unet.validation.XMLRemapProcessor;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import javafx.application.Platform;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.stage.Window;
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
  TableColumn<ValidationTestFileSet, String> testCommentColumn;

  @FXML
  TableColumn<ValidationTestFileSet, Date> lastRunColumn;

  @FXML
  TableColumn<ValidationTestFileSet, Boolean> passingStatusColumn;

  @FXML
  TableColumn<ValidationTestFileSet, TEST_RESULT> lastRunResultColumn;

  @FXML
  TableColumn<ValidationTestFileSet, ObservableSet<SourceType>> testFileTypesColumn;

  @FXML
  TableColumn<ValidationTestFileSet, CommonWellDocumented.SOURCE> ciwdVersionColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> relDnaSerVersion;

  @FXML
  TableColumn<ValidationTestFileSet, String> donorCheckVersion;

  @FXML
  Button closeButton;

  @FXML
  Button removeSelectedButton;

  @FXML
  Button runSelectedButton;

  @FXML
  Button runAllButton;

  @FXML
  Button openDirectoryButton;

  @FXML
  Button openTestButton;

  @FXML
  void initialize() {
    assert testTable != null : "fx:id=\"testTable\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testIDColumn != null : "fx:id=\"testIDColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert lastRunColumn != null : "fx:id=\"lastRunColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert passingStatusColumn != null : "fx:id=\"passingStatusColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testFileTypesColumn != null : "fx:id=\"testFileTypesColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert ciwdVersionColumn != null : "fx:id=\"ciwdVersionColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert relDnaSerVersion != null : "fx:id=\"relDnaSerVersion\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert donorCheckVersion != null : "fx:id=\"donorCheckVersion\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert closeButton != null : "fx:id=\"closeButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert removeSelectedButton != null : "fx:id=\"removeSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runSelectedButton != null : "fx:id=\"runSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runAllButton != null : "fx:id=\"runAllButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";

    testTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    testTable.setEditable(true);

    testIDColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            return p.getValue().id;
          }
        });

    testIDColumn.setOnEditCommit(ev -> {
      String oldId = ev.getOldValue();
      ValidationTestFileSet oldTest = ev.getRowValue();
      ValidationTestFileSet newTest;
      try {
        oldTest.id.set(ev.getNewValue());
        newTest = ValidationTesting.updateTestID(oldId, oldTest);
        testTable.itemsProperty().get().set(testTable.itemsProperty().get().indexOf(oldTest),
            newTest);
        testTable.refresh();
      } catch (IllegalStateException e) {
        AlertHelper.showMessage_ErrorUpdatingTestID(ev.getNewValue(), oldId, e.getCause());
        testTable.refresh();
      }
    });

    testIDColumn.setCellFactory(TextFieldTableCell.forTableColumn());

    testCommentColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            return p.getValue().comment;
          }
        });

    testCommentColumn.setOnEditCommit(ev -> {
      try {
        ev.getRowValue().comment.set(ev.getNewValue());
        ValidationTesting.updateTestProperties(ev.getRowValue());
        testTable.refresh();
      } catch (IllegalStateException e) {
        AlertHelper.showMessage_ErrorUpdatingTestProperties(ev.getRowValue(), e.getCause());
        testTable.refresh();
      }
    });

    testCommentColumn.setCellFactory(TextFieldTableCell.forTableColumn());

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

    lastRunResultColumn.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, TEST_RESULT>, ObservableValue<TEST_RESULT>>() {
          public ObservableValue<TEST_RESULT> call(
              CellDataFeatures<ValidationTestFileSet, TEST_RESULT> p) {
            return p.getValue().lastTestResult;
          }
        });

    lastRunResultColumn.setCellFactory(
        new Callback<TableColumn<ValidationTestFileSet, TEST_RESULT>, TableCell<ValidationTestFileSet, TEST_RESULT>>() {

          @Override
          public TableCell<ValidationTestFileSet, TEST_RESULT> call(
              TableColumn<ValidationTestFileSet, TEST_RESULT> param) {
            return new TableCell<ValidationTestFileSet, TEST_RESULT>() {
              @Override
              protected void updateItem(TEST_RESULT value, boolean empty) {
                super.updateItem(value, empty);
                if (value == null) {
                  setText(null);
                } else {
                  String v = value.name().replace('_', ' ').toLowerCase();
                  v = v.substring(0, 1).toUpperCase() + v.substring(1);
                  setText(v);
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


    testFileTypesColumn.setCellFactory(
        new Callback<TableColumn<ValidationTestFileSet, ObservableSet<SourceType>>, TableCell<ValidationTestFileSet, ObservableSet<SourceType>>>() {

          @Override
          public TableCell<ValidationTestFileSet, ObservableSet<SourceType>> call(
              TableColumn<ValidationTestFileSet, ObservableSet<SourceType>> param) {
            return new TableCell<ValidationTestFileSet, ObservableSet<SourceType>>() {
              @Override
              protected void updateItem(ObservableSet<SourceType> value, boolean empty) {
                super.updateItem(value, empty);
                if (value == null) {
                  setText(null);
                } else {
                  setText(value.stream().sorted().map(SourceType::name)
                      .collect(Collectors.joining(", ")));
                }
              }
            };
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

    donorCheckVersion.setCellValueFactory(
        new Callback<CellDataFeatures<ValidationTestFileSet, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<ValidationTestFileSet, String> p) {
            return p.getValue().donorCheckVersion;
          }
        });

    // Any selection enable/disable
    removeSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    runSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    // Table not empty enable/disable
    runAllButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getItems().isEmpty();
    }, testTable.itemsProperty()));

    // Single selection enable/disable
    openDirectoryButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().size() != 1;
    }, testTable.getSelectionModel().selectedIndexProperty()));

    openTestButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().size() != 1;
    }, testTable.getSelectionModel().selectedIndexProperty()));

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

  @FXML
  void openSelectedTest() {
    final ValidationTestFileSet selectedItem = testTable.getSelectionModel().getSelectedItem();

    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(() -> {
      HaplotypeFrequencies.successfullyInitialized();
    });
    EventHandler<WorkerStateEvent> doValidation = (w) -> {

      // Don't actually run this as an event, though - make it a runnable on the JFX App thread
      Platform.runLater(() -> {
        if (!HaplotypeFrequencies.successfullyInitialized()) {
          Alert alert = new Alert(AlertType.INFORMATION,
              "Haplotype Frequency Tables are not found or are invalid, and thus frequency data will not be displayed.\n\n"
                  + "Would you like to set these tables now?\n\n"
                  + "Note: you can adjust these tables any time from the 'Haplotype' menu",
              ButtonType.YES, ButtonType.NO);
          alert.setTitle("No haplotype frequencies");
          alert.setHeaderText("");
          alert.showAndWait().filter(response -> response == ButtonType.YES)
              .ifPresent(response -> TutorialHelper.chooseFreqTables(null));
        } else if (!Strings.isNullOrEmpty(HaplotypeFrequencies.getMissingTableMessage())) {
          Alert alert =
              new Alert(AlertType.INFORMATION, HaplotypeFrequencies.getMissingTableMessage());
          alert.setTitle("Missing Haplotype Table(s)");
          alert.setHeaderText("");
          alert.showAndWait();
        }

        SOURCE current = CommonWellDocumented.loadPropertyCWDSource();
        String currentRel = HLAProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);

        SOURCE source = selectedItem.cwdSource.get();
        String rel = selectedItem.relDnaSerFile.get();
        boolean changedCWID = false;
        boolean changedRel = false;

        if (current != source || !CommonWellDocumented.isLoaded()) {
          CommonWellDocumented.loadCIWDVersion(source);
          changedCWID = true;
        }
        if (!new File(currentRel).equals(new File(rel))) {
          HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, rel);
          AntigenDictionary.clearCache();
          changedRel = true;
        }

        ValidationTable table = new ValidationTable();

        List<WizardPane> pages = new ArrayList<>();

        String f1 = selectedItem.filePaths.get().get(0);
        String f2 = selectedItem.filePaths.get().get(1);
        ValidationModelBuilder builder1 = new ValidationModelBuilder();
        ValidationModelBuilder builder2 = new ValidationModelBuilder();
        SourceType.parseFile(builder1, new File(f1));
        SourceType.parseFile(builder2, new File(f2));
        builder1.validate(false);
        builder2.validate(false);
        if (selectedItem.remapFile != null && selectedItem.remapFile.get() != null
            && new File(selectedItem.remapFile.get()).exists()) {
          XMLRemapProcessor processor = new XMLRemapProcessor(selectedItem.remapFile.get());
          if (builder1.hasCorrections() && processor.hasRemappings(builder1.getSourceType())) {
            builder1.processCorrections(processor);
          }
          if (builder2.hasCorrections() && processor.hasRemappings(builder2.getSourceType())) {
            builder2.processCorrections(processor);
          }
        }

        try {
          LandingController.makePage(pages, table, LandingController.RESULTS_STEP,
              new ValidationResultsController());
          Wizard.Flow pageFlow = new LinearFlow(pages);

          pages.get(0).getButtonTypes();

          Stage stage = new Stage();
          final Window window = stage.getOwner();
          Wizard validationWizard = new Wizard(window);
          final String string = Info.getVersion();
          validationWizard.setTitle("DonorCheck " + string);
          validationWizard.setFlow(pageFlow);

          table.setFirstModel(builder1.build());
          table.setSecondModel(builder2.build());

          // show wizard and wait for response
          validationWizard.showAndWait();

          if (changedCWID) {
            CommonWellDocumented.loadCIWDVersion(current);
          }
          if (changedRel) {
            HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, currentRel);
            AntigenDictionary.clearCache();
          }
        } catch (IOException e) {
          e.printStackTrace();
          Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e.getMessage(),
              ButtonType.CLOSE);
          alert1.setTitle("Error");
          alert1.setHeaderText("");
          alert1.showAndWait();
        }
      });
    };
    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, doValidation);

    new Thread(runValidationTask).start();
  }

  @FXML
  void openTestDirectory() {
    ValidationTestFileSet selectedItem = testTable.getSelectionModel().getSelectedItem();
    String dir = ValidationTesting.getTestDirectory(selectedItem);
    try {
      Desktop.getDesktop().open(new File(dir));
    } catch (IOException e) {
      e.printStackTrace();
      new Alert(AlertType.ERROR,
          "Error opening directory for test " + selectedItem.id.get() + ":\n" + e.getMessage(),
          ButtonType.CLOSE).showAndWait();
    }
  }

  private void runTasks(List<ValidationTestFileSet> tests) {
    ValidationTesting.runTests(tests);
  }


}
