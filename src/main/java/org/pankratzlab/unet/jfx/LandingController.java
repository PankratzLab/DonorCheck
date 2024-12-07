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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.LinearFlow;
import org.controlsfx.dialog.WizardPane;
import org.pankratzlab.unet.deprecated.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.jfx.macui.MACUIController;
import org.pankratzlab.unet.jfx.wizard.FileInputController;
import org.pankratzlab.unet.jfx.wizard.ValidatingWizardController;
import org.pankratzlab.unet.jfx.wizard.ValidationResultsController;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.validation.ValidationTestFileSet;
import org.pankratzlab.unet.validation.ValidationTesting;
import org.pankratzlab.unet.validation.ValidationTesting.TestLoadingResults;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Controller instance for the main user page. Validation wizards can be launched from here. */
public class LandingController {

  private static final String WEBSITE_DONORCHECK_GITHUB =
      "https://github.com/PankratzLab/DonorCheck";
  private static final String WEBSITE_COMP_PATH =
      "https://med.umn.edu/pathology/research/computational-pathology";
  static final String DONORCHECK_VERSION = "DONORCHECK_VERSION";
  private static final String UNET_BASE_DIR_PROP = "unet.base.dir";
  private static final String MACUI_ENTRY = "/MACUIConversionPanel.fxml";

  private static final String INPUT_STEP = "/FileInput.fxml";
  static final String RESULTS_STEP = "/ValidationResults.fxml";
  private static final String TESTING_MGMT = "/ValidationTestMgmt.fxml";

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private BorderPane rootPane;

  @FXML
  private Label versionLabel;

  @FXML
  private Label menuVersionLabel;

  private String version;

  SeparatorMenuItem i;
  CustomMenuItem v;
  MenuItem v1;

  @FXML
  void fileQuitAction(ActionEvent event) {
    Platform.exit();
  }

  @FXML
  void chooseFreqTables(ActionEvent event) {
    TutorialHelper.chooseFreqTables(event);
  }

  @FXML
  void chooseRelSerLookupFile(ActionEvent event) {
    TutorialHelper.chooseRelSerLookupFile(event);
  }

  @FXML
  void chooseCIWDSource(ActionEvent event) {
    // initialize CWD data for this file
    CommonWellDocumented.init();
  }

  @FXML
  void tutorialHTMLDownload(ActionEvent event) {
    TutorialHelper.tutorialHTMLDownload(event);
  }

  @FXML
  void manageTestingFiles(ActionEvent event) {
    Task<TestLoadingResults> manageValidationTask = JFXUtilHelper.createProgressTask(() -> {
      // first, load the test data
      try {
        TestLoadingResults validationDirectory = ValidationTesting.loadValidationDirectory();

        return validationDirectory;
      } catch (Exception e1) {
        e1.printStackTrace();
        Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e1.getMessage(),
            ButtonType.CLOSE);
        alert1.setTitle("Error");
        alert1.setHeaderText("");
        alert1.showAndWait();
        throw new IllegalStateException("Error loading test data: " + e1.getMessage());
      }
    });

    EventHandler<WorkerStateEvent> doValidation = e -> {
      Platform.runLater(() -> {
        TestLoadingResults testLoad;
        try {
          testLoad = manageValidationTask.get();
        } catch (Exception e1) {
          e1.printStackTrace();
          Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e1.getMessage(),
              ButtonType.CLOSE);
          alert1.setTitle("Error");
          alert1.setHeaderText("");
          alert1.showAndWait();
          return;
        }

        if (testLoad.invalidTests.size() > 0) {
          Alert alert1 = new Alert(AlertType.ERROR);
          alert1.getButtonTypes().add(ButtonType.CLOSE);

          String content = "Please remove the listed tests manually from this directory: \n\n"
              + ValidationTesting.VALIDATION_DIRECTORY + "\n";
          for (String invalidTest : testLoad.invalidTests) {
            content += "\n" + invalidTest;
          }
          TextArea textArea = new TextArea(content);
          textArea.setEditable(false);
          textArea.setWrapText(true);

          alert1.setTitle("Error Loading Tests");
          alert1.setHeaderText("Failed to Load " + testLoad.invalidTests.size() + " Tests");
          alert1.getDialogPane().setContent(textArea);
          alert1.setResizable(true);
          alert1.showAndWait();
        }

        Table<SOURCE, String, List<ValidationTestFileSet>> testData = testLoad.testSets;

        ValidationTestMgmtController controller = new ValidationTestMgmtController();

        FXMLLoader loader = new FXMLLoader(LandingController.class.getResource(TESTING_MGMT));
        loader.setController(controller);

        Scene newScene;
        try {
          newScene = new Scene(loader.load());
          Stage inputStage = new Stage();
          inputStage.initOwner(rootPane.getScene().getWindow());
          inputStage.initModality(Modality.APPLICATION_MODAL);
          inputStage.setTitle("Manage Testing Files");
          inputStage.setResizable(true);
          inputStage.setScene(newScene);
          controller.setTable(testData);
          inputStage.showAndWait();
        } catch (IOException e1) {
          e1.printStackTrace();
          Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e1.getMessage(),
              ButtonType.CLOSE);
          alert1.setTitle("Error");
          alert1.setHeaderText("");
          alert1.showAndWait();
        }
      });
    };

    manageValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, doValidation);


    new Thread(manageValidationTask).start();
  }

  @FXML
  void runTesting(ActionEvent event) {
    Task<List<ValidationTestFileSet>> loadTestsTask = JFXUtilHelper.createProgressTask(() -> {
      TestLoadingResults tests = ValidationTesting.loadValidationDirectory();

      List<ValidationTestFileSet> allTests = new ArrayList<>();
      for (String relFile : tests.testSets.columnKeySet()) {
        for (SOURCE cwd : tests.testSets.column(relFile).keySet()) {
          allTests.addAll(tests.testSets.get(cwd, relFile));
        }
      }

      allTests = ValidationTesting.sortTests(allTests);

      return allTests;
    });

    EventHandler<WorkerStateEvent> doValidation = e -> {
      List<ValidationTestFileSet> allTests;
      try {
        allTests = loadTestsTask.get();
      } catch (InterruptedException | ExecutionException e1) {
        e1.printStackTrace();
        Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e1.getMessage(),
            ButtonType.CLOSE);
        alert1.setTitle("Error");
        alert1.setHeaderText("");
        alert1.showAndWait();
        return;
      }

      ValidationTesting.runTests(allTests);
    };

    loadTestsTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, doValidation);

    new Thread(loadTestsTask).start();
  }

  @FXML
  void tutorialXMLDownload(ActionEvent event) {
    TutorialHelper.tutorialXMLDownload(event);
  }

  @FXML
  void macConversionScore6(ActionEvent event) {
    CommonWellDocumented.initFromProperty();

    MACUIController controller = new MACUIController();
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(MACUI_ENTRY)) {
      FXMLLoader loader = new FXMLLoader();
      loader.setController(controller);

      Alert alert = new Alert(AlertType.NONE, "", ButtonType.OK);
      alert.getDialogPane().setContent(loader.load(is));
      alert.setTitle("Select Donor Score6 analysis file");
      alert.setHeaderText("");
      alert.showAndWait();
    } catch (IOException e) {
      e.printStackTrace();
      Alert alert = new Alert(AlertType.ERROR, "");
      alert.setHeaderText("Failed to read page definition: " + MACUI_ENTRY);
      alert.showAndWait();
    }
  }

  @FXML
  void openWebsiteCompPath(ActionEvent event) {
    TypeValidationApp.hostServices.showDocument(WEBSITE_COMP_PATH);
  }

  @FXML
  void openWebsiteGitHub(ActionEvent event) {
    TypeValidationApp.hostServices.showDocument(WEBSITE_DONORCHECK_GITHUB);
  }

  @FXML
  void runValidation(ActionEvent event) throws IOException {
    // The way DonorCheck is set up, "validation" is run in two parts

    // The first is the actual task we're running, which is to check/initialize the haplotype freqs
    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(() -> {
      HaplotypeFrequencies.successfullyInitialized();
    });

    // Then we set up the actual file validation as an event that triggers
    // in response to the success of the haplotypes initialization task above
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
              .ifPresent(response -> chooseFreqTables(event));
        } else if (!Strings.isNullOrEmpty(HaplotypeFrequencies.getMissingTableMessage())) {
          Alert alert =
              new Alert(AlertType.INFORMATION, HaplotypeFrequencies.getMissingTableMessage());
          alert.setTitle("Missing Haplotype Table(s)");
          alert.setHeaderText("");
          alert.showAndWait();
        }

        CommonWellDocumented.initFromProperty();

        ValidationTable table = new ValidationTable();
        final Scene scene = ((Node) event.getSource()).getScene();

        final Window window = scene.getWindow();
        Wizard validationWizard = new Wizard(window);
        validationWizard.setTitle("DonorCheck " + (version.isEmpty() ? "" : version));

        List<WizardPane> pages = new ArrayList<>();
        try {
          makePage(pages, table, INPUT_STEP, new FileInputController());
          makePage(pages, table, RESULTS_STEP, new ValidationResultsController());
        } catch (IOException e) {
          e.printStackTrace();
          throw new IllegalStateException(e);
        }

        pages.get(0).getButtonTypes();

        Wizard.Flow pageFlow = new LinearFlow(pages);

        validationWizard.setFlow(pageFlow);

        // show wizard and wait for response
        validationWizard.showAndWait();
      });
    };

    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, doValidation);

    new Thread(runValidationTask).start();
  }

  /**
   * @param pages List to populate
   * @param table Backing {@link ValidationTable} for this wizard
   * @param pageFXML FXML to read
   * @param controller Controller instance to attach to this page
   * @throws IOException If errors during FXML reading
   */
  public static void makePage(List<WizardPane> pages, @Nullable ValidationTable table,
      String pageFXML, Object controller) throws IOException {
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(pageFXML)) {
      FXMLLoader loader = new FXMLLoader();
      // NB: reading the controller from FMXL can cause problems
      loader.setController(controller);

      pages.add(loader.load(is));

      // If this controller needs a table, link it
      if (controller instanceof ValidatingWizardController && Objects.nonNull(table)) {
        ((ValidatingWizardController) controller).setTable(table);
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new IOException("Failed to read wizard page definition: " + pageFXML, e);
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'TypeValidationLanding.fxml'.";

    final Properties properties = new Properties();
    try {
      properties
          .load(LandingController.class.getClassLoader().getResourceAsStream("project.properties"));
      final String value = "Version: " + (version = properties.getProperty("version")) + " ";
      versionLabel.setText(value);
      menuVersionLabel.setText("DonorCheck " + value);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    System.setProperty(DONORCHECK_VERSION, version);
    System.setProperty(CurrentDirectoryProvider.BASE_DIR_PROP_NAME, UNET_BASE_DIR_PROP);
  }
}
