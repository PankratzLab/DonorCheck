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
package org.pankratzlab.unet.jfx;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.annotation.Nullable;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.LinearFlow;
import org.controlsfx.dialog.WizardPane;
import org.pankratzlab.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.jfx.wizard.DownloadTutorialController;
import org.pankratzlab.unet.jfx.wizard.FileInputController;
import org.pankratzlab.unet.jfx.wizard.ValidatingWizardController;
import org.pankratzlab.unet.jfx.wizard.ValidationResultsController;
import org.pankratzlab.unet.model.ValidationTable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

/**
 * Controller instance for the main user page. Validation wizards can be launched from here.
 */
public class LandingController {

  private static final String UNET_BASE_DIR_PROP = "unet.base.dir";
  private static final String XML_TUTORIAL = "/XMLDownloadTutorial.fxml";
  private static final String HTML_TUTORIAL = "/HTMLDownloadTutorial.fxml";

  private static final String INPUT_STEP = "/FileInput.fxml";
  private static final String RESULTS_STEP = "/ValidationResults.fxml";

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private BorderPane rootPane;

  @FXML
  void fileQuitAction(ActionEvent event) {
    Platform.exit();
  }

  @FXML
  void tutorialHTMLDownload(ActionEvent event) {
    showTutorial(HTML_TUTORIAL, new DownloadTutorialController());
  }

  @FXML
  void tutorialXMLDownload(ActionEvent event) {
    showTutorial(XML_TUTORIAL, new DownloadTutorialController());
  }

  @FXML
  void runValidation(ActionEvent event) throws IOException {
    ValidationTable table = new ValidationTable();
    Wizard validationWizard = new Wizard(((Node) event.getSource()).getScene().getWindow());
    validationWizard.setTitle("Donor Validation Wizard");

    List<WizardPane> pages = new ArrayList<>();
    makePage(pages, table, INPUT_STEP, new FileInputController());
    makePage(pages, table, RESULTS_STEP, new ValidationResultsController());

    pages.get(0).getButtonTypes();

    validationWizard.setFlow(new LinearFlow(pages));

    // show wizard and wait for response
    validationWizard.showAndWait();
  }


  /**
   * TODO
   */
  private void showTutorial(String tutorialFxml, Object controller) {
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(tutorialFxml)) {
      FXMLLoader loader = new FXMLLoader();
      // NB: reading the controller from FMXL can cause problems


      Alert alert = new Alert(AlertType.NONE, "", ButtonType.OK);
      alert.getDialogPane().setContent(loader.load(is));
      alert.setTitle("DonorNet Tutorial");
      alert.setHeaderText("");
      alert.showAndWait();
    } catch (IOException e) {
      Alert alert = new Alert(AlertType.ERROR, "");
      alert.setHeaderText("Failed to read tutorial page definition: " + tutorialFxml);
      alert.showAndWait();
    }

  }

  /**
   * @param pages List to populate
   * @param table Backing {@link ValidationTable} for this wizard
   * @param pageFXML FXML to read
   * @param controller Controller instance to attach to this page
   * @throws IOException If errors during FXML reading
   */
  private void makePage(List<WizardPane> pages, @Nullable ValidationTable table, String pageFXML,
      Object controller) throws IOException {
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
      throw new IOException("Failed to read wizard page definition: " + pageFXML, e);
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'TypeValidationLanding.fxml'.";
    System.setProperty(CurrentDirectoryProvider.BASE_DIR_PROP_NAME, UNET_BASE_DIR_PROP);
  }

}
