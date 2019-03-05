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
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * {@link Application} enabling comparison of donor typing between analysis PDF and DonorNet XML.
 */
public class TypeValidationApp extends Application {

  private static final Set<String> CSS_FILES =
      ImmutableSet.of("/resultTable.css", "/fileInput.css");

  private static final String LANDING_FXML = "/TypeValidationLanding.fxml";

  private static final String APP_TITLE = "DonorNet Validation";

  public static HostServices hostServices;

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Retain the HostServices for later
    TypeValidationApp.hostServices = getHostServices();


    // Set the Title to the Stage
    primaryStage.setTitle(APP_TITLE);
    primaryStage.setResizable(true);

    // Load the FXML for the application landing pane
    Pane typeValidationRoot;
    try (InputStream is = TypeValidationApp.class.getResourceAsStream(LANDING_FXML)) {
      FXMLLoader loader = new FXMLLoader();
      loader.setController(new LandingController());

      typeValidationRoot = loader.load(is);
    } catch (IOException e) {
      throw new IOException("Failed to read main application definition: " + LANDING_FXML, e);
    }

    // Set the Scene to the Stage
    Scene scene = new Scene(typeValidationRoot);
    primaryStage.setScene(scene);

    // Attach style sheets
    for (String styleSheet : CSS_FILES) {
      scene.getStylesheets().add(styleSheet);
    }

    // Display the Stage
    primaryStage.show();
    primaryStage.centerOnScreen();
  }
}
