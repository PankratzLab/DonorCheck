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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.LinearFlow;
import org.controlsfx.dialog.WizardPane;
import org.pankratzlab.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.jfx.wizard.DownloadXMLController;
import org.pankratzlab.unet.jfx.wizard.SelectPDFController;
import org.pankratzlab.unet.jfx.wizard.SelectXMLController;
import org.pankratzlab.unet.jfx.wizard.ValidatingWizardController;
import org.pankratzlab.unet.jfx.wizard.ValidationResultsController;
import org.pankratzlab.unet.model.ValidationTable;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
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
  void runValidation(ActionEvent event) throws IOException {
    ValidationTable table = new ValidationTable();
    Wizard validationWizard = new Wizard(((Node) event.getSource()).getScene().getWindow());

    List<WizardPane> pages = new ArrayList<>();
    makePage(pages, table, "/StepOneDownloadXML.fxml", new DownloadXMLController());
    makePage(pages, table, "/StepTwoInputXML.fxml", new SelectXMLController());
    makePage(pages, table, "/StepThreeInputPDF.fxml", new SelectPDFController());
    makePage(pages, table, "/StepFourResults.fxml", new ValidationResultsController());

    validationWizard.setFlow(new LinearFlow(pages));

    // show wizard and wait for response
    validationWizard.showAndWait().ifPresent(result -> {
      if (result == ButtonType.FINISH) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText("Would you like to save the validation results?");
        alert.showAndWait().filter(response -> response == ButtonType.OK)
            .ifPresent(response -> saveResult(table));
      }
    });
  }

  private void saveResult(ValidationTable table) {
    Optional<File> destination = DonorNetUtils.getFile(rootPane, "Save Validation Results",
        table.getId() + "_donor_valid", "PNG", ".png", false);

    if (destination.isPresent()) {
      try {
        ImageIO.write(SwingFXUtils.fromFXImage(table.getValidationImage(), null), "png",
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

  /**
   * @param pages List to populate
   * @param table Backing {@link ValidationTable} for this wizard
   * @param pageFXML FXML to read
   * @param controller Controller instance to attach to this page
   * @throws IOException If errors during FXML reading
   */
  private void makePage(List<WizardPane> pages, ValidationTable table, String pageFXML,
      Object controller) throws IOException {
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(pageFXML)) {
      FXMLLoader loader = new FXMLLoader();
      // NB: reading the controller from FMXL can cause problems
      loader.setController(controller);

      pages.add(loader.load(is));

      // If this controller needs a table, link it
      if (controller instanceof ValidatingWizardController) {
        ((ValidatingWizardController) controller).setTable(table);
      }
    } catch (IOException e) {
      throw new IOException("Failed to read wizard page definition: " + pageFXML, e);
    }
  }

  @FXML
  void initialize() {
    assert rootPane != null : "fx:id=\"rootPane\" was not injected: check your FXML file 'TypeValidationLanding.fxml'.";

    CurrentDirectoryProvider.setBaseDir(new File(System.getProperty("user.home")));
  }

}
