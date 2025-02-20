package org.pankratzlab.unet.jfx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.LinearFlow;
import org.controlsfx.dialog.WizardPane;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.DonorCheckProperties;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
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
import org.pankratzlab.unet.validation.TEST_EXPECTATION;
import org.pankratzlab.unet.validation.TEST_RESULT;
import org.pankratzlab.unet.validation.ValidationTestFileSet;
import org.pankratzlab.unet.validation.ValidationTesting;
import org.pankratzlab.unet.validation.XMLRemapProcessor;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

public class BatchTestMgmtController {

  private static final String PASS = "Pass";
  private static final String EXPECTED_FAILURE = "Expected failure";
  private static final String UNEXPECTED_PASS = "Unexpected pass";
  private static final String UNEXPECTED_FAILURE = "Unexpected failure";
  private static final String UNEXPECTED_ERROR = "Unexpected error";
  private static final String EXPECTED_ERROR = "Expected error";

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private VBox rootPane;

  @FXML
  TableView<ValidationTestFileSet> testTable;

  @FXML
  TableColumn<ValidationTestFileSet, String> testIDColumn;

  @FXML
  TableColumn<ValidationTestFileSet, ObservableSet<SourceType>> testFileTypesColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> manualEditsColumn;

  @FXML
  TableColumn<ValidationTestFileSet, CommonWellDocumented.SOURCE> ciwdVersionColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> relDnaSerVersion;

  @FXML
  TableColumn<ValidationTestFileSet, TEST_EXPECTATION> expectingPassFailColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> lastRunResultColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> testCommentColumn;

  @FXML
  TableColumn<ValidationTestFileSet, String> lastRunDetailsColumn; // date and version of last run

  @FXML
  Button openDirectoryButton;

  @FXML
  Button openTestButton;

  @FXML
  Button removeSelectedButton;

  @FXML
  Button runSelectedButton;

  @FXML
  Button runAllButton;

  private <K, V> TableCell<K, V> configureTableCell(TableCell<K, V> cell) {
    cell.setAlignment(Pos.CENTER);
    return cell;
  }

  private <V> Callback<TableColumn<ValidationTestFileSet, String>, TableCell<ValidationTestFileSet, String>> getEditableCellFactory() {
    Callback<TableColumn<ValidationTestFileSet, String>, TableCell<ValidationTestFileSet, String>> c1 =
        list -> configureTableCell(new TextFieldTableCell<ValidationTestFileSet, String>(new DefaultStringConverter()));
    return c1;
  }

  private <T> Callback<TableColumn<ValidationTestFileSet, T>, TableCell<ValidationTestFileSet, T>> getComboBoxCellFactory(
      @SuppressWarnings("unchecked") T... values) {
    return list -> configureTableCell(new ComboBoxTableCell<>(values));
  }

  private final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss");

  @FXML
  void initialize() {
    assert ciwdVersionColumn != null : "fx:id=\"ciwdVersionColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert expectingPassFailColumn != null : "fx:id=\"expectingPassFailColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert lastRunDetailsColumn != null : "fx:id=\"lastRunDetailsColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert lastRunResultColumn != null : "fx:id=\"lastRunResultColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert manualEditsColumn != null : "fx:id=\"manualEditsColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert openDirectoryButton != null : "fx:id=\"openDirectoryButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert openTestButton != null : "fx:id=\"openTestButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert relDnaSerVersion != null : "fx:id=\"relDnaSerVersion\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert removeSelectedButton != null : "fx:id=\"removeSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runAllButton != null : "fx:id=\"runAllButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert runSelectedButton != null : "fx:id=\"runSelectedButton\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testCommentColumn != null : "fx:id=\"testCommentColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testFileTypesColumn != null : "fx:id=\"testFileTypesColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testIDColumn != null : "fx:id=\"testIDColumn\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";
    assert testTable != null : "fx:id=\"testTable\" was not injected: check your FXML file 'ValidationTestMgmt.fxml'.";


    testTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    testTable.setEditable(true);

    testIDColumn.setCellValueFactory(p -> {
      return p.getValue().id;
    });

    testIDColumn.setOnEditCommit(ev -> {
      String oldId = ev.getOldValue();
      ValidationTestFileSet oldTest = ev.getRowValue();
      ValidationTestFileSet newTest;
      try {
        oldTest.id.set(ev.getNewValue());
        newTest = ValidationTesting.updateTestID(oldId, oldTest);
        testTable.itemsProperty().get().set(testTable.itemsProperty().get().indexOf(oldTest), newTest);
        testTable.refresh();
      } catch (IllegalStateException e) {
        AlertHelper.showMessage_ErrorUpdatingTestID(ev.getNewValue(), oldId, e.getCause());
        testTable.refresh();
      }
    });

    testIDColumn.setCellFactory(getEditableCellFactory());

    testCommentColumn.setCellValueFactory(cdf -> {
      return cdf.getValue().comment;
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

    testCommentColumn.setCellFactory(getEditableCellFactory());

    expectingPassFailColumn.setCellFactory(getComboBoxCellFactory(TEST_EXPECTATION.values()));

    expectingPassFailColumn.setCellValueFactory(p -> {
      return p.getValue().expectedResult;
    });

    expectingPassFailColumn.setOnEditCommit(ev -> {
      try {
        ev.getRowValue().expectedResult.set(ev.getNewValue());
        ValidationTesting.updateTestProperties(ev.getRowValue());
        testTable.refresh();
      } catch (Throwable e) {
        AlertHelper.showMessage_ErrorUpdatingTestProperties(ev.getRowValue(), e.getCause());
        testTable.refresh();
      }
    });

    lastRunDetailsColumn.setCellFactory(getDefaultTextTableCellFactory());

    lastRunDetailsColumn.setCellValueFactory(p -> {
      ValidationTestFileSet value = p.getValue();
      return Bindings.createStringBinding(() -> {
        String ver = value.donorCheckVersion.getValueSafe();
        String date = value.lastRunDate.getValue() == null ? "---" : format.format(value.lastRunDate.getValue());
        return date + (ver.isBlank() ? "" : " (" + ver + ")");
      }, value.donorCheckVersion, value.lastRunDate);
    });

    lastRunResultColumn.setCellValueFactory(p -> {
      return Bindings.createStringBinding(() -> {
        TEST_RESULT testResult = p.getValue().lastTestResult.get();
        if (testResult == null) {
          return "---";
        }

        String v = "";
        switch (p.getValue().expectedResult.get()) {
          case Error:
            // expecting an error
            if (testResult != TEST_RESULT.TEST_SUCCESS && testResult != TEST_RESULT.TEST_FAILURE) {
              // got an error
              v = EXPECTED_ERROR;
            } else {
              if (testResult == TEST_RESULT.TEST_SUCCESS) {
                // didn't get an error
                v = UNEXPECTED_PASS;
              } else if (testResult == TEST_RESULT.TEST_FAILURE) {
                // didn't get an error
                v = UNEXPECTED_FAILURE;
              }
            }
            break;
          case Pass:
            if (testResult == TEST_RESULT.TEST_SUCCESS) {
              v = PASS;
            } else if (testResult == TEST_RESULT.TEST_FAILURE) {
              v = UNEXPECTED_FAILURE;
            } else {
              v = UNEXPECTED_ERROR + " (" + convert(testResult.name()) + ")";
            }
            break;
          case Fail:
            if (testResult == TEST_RESULT.TEST_SUCCESS) {
              v = UNEXPECTED_PASS;
            } else if (testResult == TEST_RESULT.TEST_FAILURE) {
              v = EXPECTED_FAILURE;
            } else {
              v = UNEXPECTED_ERROR + " (" + convert(testResult.name()) + ")";
            }
            break;
        };
        return v;
      }, p.getValue().expectedResult, p.getValue().lastTestResult);
    });

    lastRunResultColumn.setCellFactory(param -> {
      return configureTableCell(new TableCell<ValidationTestFileSet, String>() {
        @Override
        protected void updateItem(String value, boolean empty) {
          super.updateItem(value, empty);
          if (value == null) {
            setText(null);
            setStyle("");
          } else {
            String color = null;
            if (value.startsWith(PASS) || value.startsWith(EXPECTED_FAILURE) || value.startsWith(EXPECTED_ERROR)) {
              color = "LimeGreen";
            } else if (value.startsWith(UNEXPECTED_FAILURE)) {
              color = "Orange";
            } else if (value.startsWith(UNEXPECTED_PASS)) {
              color = "Lime";
            } else if (value.startsWith(UNEXPECTED_ERROR)) {
              color = "OrangeRed";
            }
            if (color != null) {
              setStyle("-fx-background-color: " + color + "; -fx-text-fill: black");
            } else {
              setStyle("");
            }
            setText(value);
          }
        }
      });
    });

    testFileTypesColumn.setCellValueFactory(p -> {
      return p.getValue().sourceTypes;
    });

    testFileTypesColumn.setCellFactory(param -> {
      return configureTableCell(new TableCell<ValidationTestFileSet, ObservableSet<SourceType>>() {
        @Override
        protected void updateItem(ObservableSet<SourceType> value, boolean empty) {
          super.updateItem(value, empty);
          if (value == null) {
            setText(null);
          } else {
            setText(value.stream().sorted().map(SourceType::getDisplayName).collect(Collectors.joining(", ")));
          }
        }
      });
    });

    manualEditsColumn.setCellValueFactory(p -> {
      ReadOnlyStringProperty valueSafe = p.getValue().remapFile;
      if (valueSafe != null && valueSafe.get() != null && !valueSafe.get().isBlank() && new File(valueSafe.get()).exists()) {
        return new SimpleStringProperty(p.getValue().remappedLoci.get().stream().map(HLALocus::name).collect(Collectors.joining(", ")));
      } else {
        return new SimpleStringProperty("");
      }
    });

    manualEditsColumn.setCellFactory(getDefaultTextTableCellFactory());

    ciwdVersionColumn.setCellValueFactory(p -> {
      return p.getValue().cwdSource;
    });

    ciwdVersionColumn.setCellFactory(param -> configureTableCell(new TableCell<ValidationTestFileSet, SOURCE>() {
      @Override
      protected void updateItem(SOURCE value, boolean empty) {
        super.updateItem(value, empty);
        setText(value == null ? "" : value.getVersion());
      }
    }));

    relDnaSerVersion.setCellValueFactory(p -> {
      return p.getValue().relDnaSerFile.map(os -> {
        String v = "Unknown";
        v = AntigenDictionary.getVersion(os);
        if (v == null) {
          v = "File missing (" + os + ")";
        }
        return v;
      }).orElse("");
    });

    relDnaSerVersion.setCellFactory(getDefaultTextTableCellFactory());

    removeSelectedButton.textProperty().bind(Bindings.createStringBinding(() -> {
      int t = testTable.getSelectionModel().getSelectedIndices().size();
      if (t == 0) {
        return "Remove selected tests";
      }
      return "Remove " + t + " selected test" + (t > 1 ? "s" : "");
    }, testTable.getSelectionModel().selectedIndexProperty()));

    // Any selection enable/disable
    removeSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    runSelectedButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return testTable.getSelectionModel().getSelectedIndices().isEmpty();
    }, testTable.getSelectionModel().selectedIndexProperty()));

    runSelectedButton.textProperty().bind(Bindings.createStringBinding(() -> {
      int t = testTable.getSelectionModel().getSelectedIndices().size();
      if (t == 0) {
        return "Run selected tests";
      }
      return "Run " + t + " selected test" + (t > 1 ? "s" : "");
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

  public <T> Callback<TableColumn<ValidationTestFileSet, T>, TableCell<ValidationTestFileSet, T>> getDefaultTextTableCellFactory() {
    return param -> configureTableCell(new TableCell<ValidationTestFileSet, T>() {
      @Override
      protected void updateItem(T value, boolean empty) {
        super.updateItem(value, empty);
        setText(Objects.toString(value).trim());
      }
    });
  }

  private String convert(String s) {
    String v = s.replace('_', ' ').toLowerCase();
    v = v.substring(0, 1).toUpperCase() + v.substring(1);
    return v;
  }

  public void setTable(Table<SOURCE, String, List<ValidationTestFileSet>> testData) {
    ObservableList<ValidationTestFileSet> testList =
        FXCollections.observableArrayList(testData.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    testTable.setItems(testList);
  }

  @FXML
  void removeSelected() {
    List<ValidationTestFileSet> toRemove = testTable.selectionModelProperty().get().getSelectedItems();

    Alert alert = new Alert(AlertType.CONFIRMATION,
        "Are you sure you'd like to remove " + toRemove.size() + " test" + (toRemove.size() > 1 ? "s" : "") + "?", ButtonType.OK, ButtonType.CANCEL);
    alert.setTitle("Remove tests");
    alert.setHeaderText("");
    Optional<ButtonType> selVal = alert.showAndWait();

    if (selVal.isPresent() && selVal.get() == ButtonType.OK) {
      Map<ValidationTestFileSet, Boolean> results = ValidationTesting.deleteTests(toRemove);
      Set<ValidationTestFileSet> removed = results.entrySet().stream().filter(e -> e.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
      Set<ValidationTestFileSet> notRemoved =
          results.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
      testTable.getItems().removeAll(toRemove);

      if (removed.size() == toRemove.size()) {
        Alert alert1 = new Alert(AlertType.INFORMATION, "Successfully removed " + toRemove.size() + " test" + (toRemove.size() > 1 ? "s" : ""),
            ButtonType.CLOSE);
        alert1.setTitle("Successfully removed tests");
        alert1.setHeaderText("");
        alert1.showAndWait();
      } else {
        Alert alert1 = new Alert(AlertType.WARNING, "Failed to remove " + notRemoved.size() + " test" + (notRemoved.size() > 1 ? "s" : "")
            + "\n\nSuccessfully removed " + removed.size() + " test" + (removed.size() > 1 ? "s" : ""), ButtonType.CLOSE);
        alert1.setTitle("Failed to remove tests");
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
        if (!HaplotypeFrequencies.successfullyInitialized().get()) {
          Alert alert = new Alert(AlertType.INFORMATION,
              "Haplotype Frequency Tables are not found or are invalid, and thus frequency data will not be displayed.\n\n"
                  + "Would you like to set these tables now?\n\n" + "Note: you can adjust these tables any time from the 'Haplotype' menu",
              ButtonType.YES, ButtonType.NO);
          alert.setTitle("No haplotype frequencies");
          alert.setHeaderText("");
          alert.showAndWait().filter(response -> response == ButtonType.YES).ifPresent(response -> TutorialHelper.chooseFreqTables(null));
        } else if (!Strings.isNullOrEmpty(HaplotypeFrequencies.getMissingTableMessage())) {
          Alert alert = new Alert(AlertType.INFORMATION, HaplotypeFrequencies.getMissingTableMessage());
          alert.setTitle("Missing haplotype frequency table(s)");
          alert.setHeaderText("");
          alert.showAndWait();
        }

        SOURCE current = CommonWellDocumented.loadPropertyCWDSource();
        String currentRel = DonorCheckProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);

        SOURCE source = selectedItem.cwdSource.get();
        String rel = selectedItem.relDnaSerFile.get();
        boolean changedCWID = false;
        boolean changedRel = false;

        if (current != source || !CommonWellDocumented.isLoaded()) {
          CommonWellDocumented.loadCIWDVersion(source);
          changedCWID = true;
        }
        if (!new File(currentRel).equals(new File(rel))) {
          DonorCheckProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, rel);
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
        if (selectedItem.remapFile != null && selectedItem.remapFile.get() != null && new File(selectedItem.remapFile.get()).exists()) {
          XMLRemapProcessor processor = new XMLRemapProcessor(selectedItem.remapFile.get());
          if (builder1.hasCorrections() && processor.hasRemappings(builder1.getSourceType())) {
            builder1.processCorrections(processor);
          }
          if (builder2.hasCorrections() && processor.hasRemappings(builder2.getSourceType())) {
            builder2.processCorrections(processor);
          }
        }

        try {
          LandingController.makePage(pages, table, LandingController.RESULTS_STEP, new ValidationResultsController());
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
            DonorCheckProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, currentRel);
            AntigenDictionary.clearCache();
          }
        } catch (IOException e) {
          e.printStackTrace();
          Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e.getMessage(), ButtonType.CLOSE);
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
      new Alert(AlertType.ERROR, "Error opening directory for test " + selectedItem.id.get() + ":\n" + e.getMessage(), ButtonType.CLOSE)
          .showAndWait();
    }
  }

  private void runTasks(List<ValidationTestFileSet> tests) {
    ValidationTesting.runTests(rootPane, tests);
  }
}
