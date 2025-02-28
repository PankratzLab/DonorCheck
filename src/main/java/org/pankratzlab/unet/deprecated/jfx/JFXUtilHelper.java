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
package org.pankratzlab.unet.deprecated.jfx;

import java.util.List;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Static utility class providing common JavaFX functions */
public final class JFXUtilHelper {

  public static final double PROGRESS_POPUP_SIZE = 100;

  private JFXUtilHelper() {
    // Prevent instantiation of static utility class
  }

  /** Create an {@link Alert} with no header or graphic. */
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

  private static class ProgressStage {
    public final Stage stage;
    public final ProgressIndicator pi;

    private ProgressStage(Stage stage, ProgressIndicator pi) {
      this.stage = stage;
      this.pi = pi;
    }
  }

  /** @return A modal stage pre-built with a progress indicator */
  private static ProgressStage createDeterminiteProgressStage() {
    ProgressIndicator pi = new ProgressIndicator(0);
    VBox vbox = new VBox(pi);
    VBox.setVgrow(pi, Priority.ALWAYS);
    vbox.setMinSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    vbox.setPrefSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    vbox.setMaxSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    pi.setMaxSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    vbox.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-border-thickness: 2;");
    BorderPane root = new BorderPane(vbox);
    Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
    loadingStage.initModality(Modality.APPLICATION_MODAL);
    Scene loadingScene = new Scene(root, Color.TRANSPARENT);
    loadingStage.setScene(loadingScene);
    loadingStage.centerOnScreen();
    return new ProgressStage(loadingStage, pi);
  }


  /** @return A modal stage pre-built with a progress indicator */
  private static Stage createProgressStage() {
    ProgressIndicator pi = new ProgressIndicator();
    VBox root = new VBox(pi);
    VBox.setVgrow(pi, Priority.ALWAYS);
    pi.setPrefSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    pi.setMaxSize(PROGRESS_POPUP_SIZE, PROGRESS_POPUP_SIZE);
    root.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-border-thickness: 2;");
    Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
    loadingStage.initModality(Modality.APPLICATION_MODAL);
    Scene loadingScene = new Scene(root, Color.TRANSPARENT);
    loadingStage.setScene(loadingScene);
    loadingStage.centerOnScreen();
    return loadingStage;
  }

  /**
   * Helper method to add hooks to close the given {@link Stage} when the given {@link Task}
   * completes.
   */
  private static <T> void addCloseHooks(Stage stage, Task<T> task) {
    EventHandler<WorkerStateEvent> closeStage = (w) -> {
      Platform.runLater(() -> {
        stage.close();
      });
    };
    task.setOnCancelled(closeStage);
    task.setOnFailed(closeStage);
    task.setOnSucceeded(closeStage);
  }

  /**
   * Helper method to combine {@link #createProgressStage()} and
   * {@link #addCloseHooks(Stage, Task)}, generating a {@link Task} that creates and shows a
   * progress dialog while running a {@link Runnable} and then closes the progress graphic when
   * finished.
   */
  public static Task<Void> createProgressTask(List<Runnable> runnables) {
    ProgressStage progressStage = createDeterminiteProgressStage();
    Task<Void> progressTask = new Task<Void>() {

      @Override
      protected Void call() throws Exception {
        Platform.runLater(() -> progressStage.stage.show());
        for (Runnable runnable : runnables) {
          runnable.run();
          updateProgress(runnables.indexOf(runnable) + 1, runnables.size());
        }
        return null;
      }
    };
    progressStage.pi.progressProperty().bind(progressTask.progressProperty());
    addCloseHooks(progressStage.stage, progressTask);

    return progressTask;
  }

  /**
   * Helper method to combine {@link #createProgressStage()} and
   * {@link #addCloseHooks(Stage, Task)}, generating a {@link Task} that creates and shows a
   * progress dialog while running a {@link Runnable} and then closes the progress graphic when
   * finished.
   */
  public static Task<Void> createProgressTask(Runnable runnable) {
    Stage progressStage = createProgressStage();
    Task<Void> progressTask = new Task<Void>() {

      @Override
      protected Void call() throws Exception {
        Platform.runLater(() -> progressStage.show());
        runnable.run();
        return null;
      }
    };
    addCloseHooks(progressStage, progressTask);

    return progressTask;
  }

  /**
   * Helper method to combine {@link #createProgressStage()} and
   * {@link #addCloseHooks(Stage, Task)}, generating a {@link Task} that creates and shows a
   * progress dialog while running a {@link Runnable} and then closes the progress graphic when
   * finished.
   */
  public static <T> Task<T> createProgressTask(Callable<T> runnable) {
    Stage progressStage = createProgressStage();
    Task<T> progressTask = new Task<T>() {

      @Override
      protected T call() throws Exception {
        Platform.runLater(() -> progressStage.show());
        return runnable.call();
      }
    };
    addCloseHooks(progressStage, progressTask);

    return progressTask;
  }
}
