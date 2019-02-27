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
package org.pankratzlab.unet.deprecated.jfx;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Static utility class providing common JavaFX funcitons
 */
public final class JFXUtilHelper {

  private JFXUtilHelper() {
    // Prevent instantiation of static utility class
  }

  /**
   * Create an {@link Alert} with no header or graphic.
   */
  public static Alert makeContentOnlyAlert(AlertType alertType, String title, Node content,
      ButtonType... buttons) {
    // create pop-up
    Alert alert = new Alert(alertType, "", buttons);
    alert.setTitle(title);
    alert.setHeaderText(null); // removes header pane
    alert.setGraphic(null); // removes confirmation icon
    alert.getDialogPane().setContent(content);

    return alert;
  }

  /**
   * @return A modal stage pre-built with a progress indicator
   */
  public static Stage createProgressStage() {
    ProgressIndicator pi = new ProgressIndicator();
    VBox root = new VBox(pi);
    pi.setPrefWidth(Screen.getPrimary().getBounds().getWidth() / 10);
    pi.setPrefHeight(Screen.getPrimary().getBounds().getHeight() / 10);
    root.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-border-thickness: 2;");
    Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
    loadingStage.initModality(Modality.APPLICATION_MODAL);
    Scene loadingScene = new Scene(root, Color.TRANSPARENT);
    loadingStage.setScene(loadingScene);
    loadingStage.centerOnScreen();
    return loadingStage;
  }
}
