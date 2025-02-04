package org.pankratzlab.unet.jfx;

import java.io.IOException;
import java.io.InputStream;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public final class TutorialHelper {

  static final String XML_TUTORIAL = "/XMLDownloadTutorial.fxml";
  static final String HTML_TUTORIAL = "/HTMLDownloadTutorial.fxml";
  static final String NMDP_DOWNLOAD = "/NMDPDownloadPrompt.fxml";
  static final String REL_SER_DOWNLOAD = "/RelDnaSerDownloadPrompt.fxml";

  public static void chooseFreqTables(ActionEvent event) {
    DownloadNMDPController dc = new DownloadNMDPController();
    showTutorial(NMDP_DOWNLOAD, dc, "Set Frequency Directory");
    if (dc.isDirty()) {
      new Thread(JFXUtilHelper.createProgressTask(() -> {
        HaplotypeFrequencies.doInitialization();
      })).start();
    }
  }

  public static void chooseRelSerLookupFile(ActionEvent event) {
    SerotypeLookupFileController controller = new SerotypeLookupFileController();
    showTutorial(REL_SER_DOWNLOAD, controller, "Set HLA Serotype Lookup File");
    if (controller.isDirty()) {
      new Thread(JFXUtilHelper.createProgressTask(() -> {
        AntigenDictionary.clearCache();
      })).start();
    }
  }

  public static void tutorialHTMLDownload(ActionEvent event) {
    showTutorial(HTML_TUTORIAL, new DownloadTutorialController(), "Donor download instructions");
  }

  public static void tutorialXMLDownload(ActionEvent event) {
    showTutorial(XML_TUTORIAL, new DownloadTutorialController(), "Donor download instructions");
  }

  /** TODO */
  private static void showTutorial(String tutorialFxml, Object controller, String title) {
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(tutorialFxml)) {
      FXMLLoader loader = new FXMLLoader();
      loader.setController(controller);
      // NB: reading the controller from FMXL can cause problems

      Alert alert = new Alert(AlertType.NONE, "", ButtonType.OK);
      alert.getDialogPane().setContent(loader.load(is));
      alert.setTitle(title);
      alert.setHeaderText("");
      alert.showAndWait();
    } catch (IOException e) {
      e.printStackTrace();
      Alert alert = new Alert(AlertType.ERROR, "");
      alert.setHeaderText("Failed to read tutorial page definition: " + tutorialFxml);
      alert.showAndWait();
    }
  }

}
