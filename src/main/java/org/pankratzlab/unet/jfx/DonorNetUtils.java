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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jsoup.select.Elements;
import org.pankratzlab.hla.CurrentDirectoryProvider;
import com.google.common.collect.ImmutableMap;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/**
 * Utility class with shared methods for use with the DonorNet application
 */
public final class DonorNetUtils {

  private DonorNetUtils() {
    // prevent instantiation of static utility class
  }

  /**
   * Convenience method which unwraps an input {@link ActionEvent}
   *
   * @see #getFile(Node, String, String, String, String, boolean)
   */
  public static Optional<File> getFile(@Nullable ActionEvent event, String title,
      String initialName, String extensionDescription, String extension, boolean open) {
    Node source = null;
    if (Objects.nonNull(event)) {
      source = (Node) event.getSource();
    }
    return getFile(source, title, initialName, extensionDescription, extension, open);
  }

  /**
   * Convenience method for single extension file choosers
   *
   * @see #getFile(Node, String, String, Map, boolean)
   */
  public static Optional<File> getFile(Node node, String title, String initialName,
      String extensionDescription, String extension, boolean open) {
    return getFile(node, title, initialName, ImmutableMap.of(extensionDescription, extension),
        open);
  }

  /**
   * Helper method to get a file from a user in a consistent way
   *
   * @param event Source {@link ActionEvent}, e.g. if called from a button
   * @param title File chooser title
   * @param initialName Initial file name
   * @param extensionMap Mapping of extension descriptions to filter values
   * @param open Whether to show the open or save dialog
   * @return An {@link Optional} wrapper around the file selected by the user
   */
  public static Optional<File> getFile(Node node, String title, String initialName,
      Map<String, String> extensionMap, boolean open) {
    CurrentDirectoryProvider.setInitialFileName(initialName);
    FileChooser fileChooser = CurrentDirectoryProvider.getFileChooser();
    fileChooser.setTitle(title);
    for (Entry<String, String> descriptionToFilter : extensionMap.entrySet()) {
      fileChooser.getExtensionFilters()
          .add(new ExtensionFilter(descriptionToFilter.getKey(), descriptionToFilter.getValue()));
    }
    Window owner = null;
    if (Objects.nonNull(node)) {
      owner = node.getScene().getWindow();
    }
    File selectedFile =
        open ? fileChooser.showOpenDialog(owner) : fileChooser.showSaveDialog(owner);

    return Optional.ofNullable(selectedFile);
  }

  /**
   * Helper method to extract the text value of an element, with a null value if the element is not
   * present or empty
   */
  public static Optional<String> getText(Elements elements) {
    String val = null;
    if (!elements.isEmpty()) {
      String text = elements.get(0).text();
      if (!text.isEmpty()) {
        val = text;
      }
    }

    return Optional.ofNullable(val);
  }
}
