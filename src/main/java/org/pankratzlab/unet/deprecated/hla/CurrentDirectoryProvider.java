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
package org.pankratzlab.unet.deprecated.hla;

import java.io.File;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.stage.FileChooser;

/**
 * Static utility class wrapping singletons for application state: working directory and file
 * prefix. Set the {@link #BASE_DIR_PROP_NAME} system property to customize where the last used
 * working directory will be saved, allowing differentiation per-application.
 */
public final class CurrentDirectoryProvider {

  public static final String BASE_DIR_PROP_NAME = "hla.dir.prop";
  private static final String DEFAULT_PROP_NAME = "hla.base.dir";
  private static final String DEFAULT_BASE_DIR = System.getProperty("user.home");
  private static final ReadOnlyObjectWrapper<String> initialFileName =
      new ReadOnlyObjectWrapper<>();

  private CurrentDirectoryProvider() {
    // Prevent instantiation of static utility class
  }

  /**
   * @return A {@link FileChooser} pre-configured with the current initial directory and file name.
   */
  public static FileChooser getFileChooser() {
    FileChooser fc = new FileChooser();

    File baseDir = new File(HLAProperties.get().getProperty(baseDirProp(), DEFAULT_BASE_DIR));
    if (baseDir.exists() && baseDir.isDirectory()) {
      fc.setInitialDirectory(baseDir);
    }
    fc.setInitialFileName(initialFileName.get());

    return fc;
  }

  public static void setBaseDir(File dir) {
    HLAProperties.get().put(baseDirProp(), dir.getAbsolutePath());
  }

  public static void setInitialFileName(String name) {
    initialFileName.set(name);
  }

  private static String baseDirProp() {
    return System.getProperty(BASE_DIR_PROP_NAME, DEFAULT_PROP_NAME);
  }
}
