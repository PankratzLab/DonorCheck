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
package org.pankratzlab.unet.deprecated.hla;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.jfx.TypeValidationApp;
import org.pankratzlab.unet.parser.DonorFileParser;
import com.google.common.net.UrlEscapers;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Utility class for any logging actions. NB: this currently conflates "logging" with "messaging" or
 * notifying the user. The intent is to replace this class with functions from Spring/log4j. So it
 * is primarily used to track where logging is performed.
 */
public final class LoggingPlaceholder {

  private LoggingPlaceholder() {
    // Prevent instantiation of static utility class
  }

  /**
   * Reports the error with a default title: {@code "Unexpected Error"}
   *
   * @see #reportError(Exception, String)
   */
  public static void reportError(Exception exc) {
    reportError(exc, "Unexpected Error");
  }

  /**
   * Notify the user that an error has occurred (non-Exception related)
   *
   * @param error Error dialog title
   */
  public static void reportError(String error) {
    report(error, AlertType.ERROR);
  }

  public static void report(String error, AlertType alertType) {
    Platform.runLater(() -> {
      Alert alert = new Alert(alertType);
      alert.setHeaderText(error);
      alert.showAndWait();
    });
  }

  /**
   * Shows a dialog to the user indicating an error has occurred. Includes instructions for what to
   * send to the developers when reporting this error.
   *
   * @param exc Exception to build a stack trace from
   * @param title Title to use on the dialog shown to the user
   */
  public static void reportError(Throwable exc, String title) {
    Platform.runLater(() -> {
      StringBuilder sb = new StringBuilder(
          "Please send the following to the developers:\n" + "- A screenshot of this error\n"
              + "- The input files (or MRN) that caused this error\n\n");
      sb.append(exc.getClass());
      sb.append(": ");
      sb.append(exc.getMessage());
      sb.append("\n");
      sb.append(Arrays.stream(exc.getStackTrace()).map(StackTraceElement::toString)
          .collect(Collectors.joining("\n")));
      TextArea errorText = new TextArea(sb.toString());
      errorText.setWrapText(true);
      JFXUtilHelper.makeContentOnlyAlert(AlertType.ERROR, title, errorText).showAndWait();
    });
  }

  public static void alertError(DonorFileParser donorParser, File selectedFile, Throwable e) {
    Alert alert = new Alert(AlertType.ERROR);

    Text errorText = new Text(
        donorParser.getErrorText() + "\nOffending file: " + selectedFile.getName() + "\n\nPlease ");
    Hyperlink emailLink = new Hyperlink("notify the developers");
    emailLink.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent t) {
        try {
          final String mailtoLink = constructMailtoLink(donorParser, selectedFile, e);
          System.out.println(mailtoLink);
          TypeValidationApp.hostServices.showDocument(mailtoLink);
        } catch (Throwable t1) {
          t1.printStackTrace();
        }
      }
    });

    Text errorText2 = new Text(" as this may indicate the data has changed."
        + "\nIf possible, please include the source file when notifying the developers and the"
        + "\nprogram version and exception information below:");

    TextFlow headerFlow = new TextFlow(errorText, emailLink, errorText2);
    headerFlow.setPadding(new Insets(10));

    TextArea ta = new TextArea();
    ta.setEditable(false);
    String trace = getExceptionStackTraceString(e);
    String version = Info.getVersion();
    String text = "DonorCheck Version: " + version + "\n\n" + trace;
    ta.setText(text);
    ScrollPane sp = new ScrollPane(ta);
    sp.setFitToWidth(true);
    alert.getDialogPane().setHeader(headerFlow);
    alert.getDialogPane().setContent(sp);


    alert.showAndWait();
    e.printStackTrace();
  }

  public static String getExceptionStackTraceString(Throwable e) {
    Throwable rootCause = ExceptionUtils.getRootCause(e);
    if (rootCause == null) {
      rootCause = e;
      while (rootCause.getCause() != null) {
        rootCause = rootCause.getCause();
      }
    }
    String trace = ExceptionUtils.getStackTrace(rootCause);
    return trace;
  }

  public static String constructMailtoLink(DonorFileParser donorParser, File selectedFile,
      Throwable e) {
    StringBuilder sb = new StringBuilder("mailto:donor_check@umn.edu?subject=Error%20Report&body=");
    StringBuilder sb1 = new StringBuilder();
    sb1.append("[[[ PLEASE ATTACH SOURCE FILE IF POSSIBLE ]]]\n\n");
    sb1.append("DonorCheck Version: ").append(Info.getVersion()).append("\n\n");
    sb1.append("Exception information:\n");
    sb1.append(getExceptionStackTraceString(e));
    sb.append(UrlEscapers.urlFragmentEscaper().escape(sb1.toString()));
    return sb.toString();
  }
}
