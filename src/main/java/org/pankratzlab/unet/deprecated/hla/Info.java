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
import java.io.IOException;
import java.util.Properties;
import org.pankratzlab.unet.jfx.LandingController;

/**
 * Container class for information that needs to be available to both the HLA application and
 * updater.
 */
public class Info {
  public static final String MANIFEST_MAIN = "Main-Class";
  public static final String DONOR_CHECK_HOME = System.getProperty("user.home") + File.separator + ".donor_check" + File.separator;
  /** Filename of the last requested version */
  public static final String VERSION_TO_LOAD = DONOR_CHECK_HOME + ".version.load.ser";

  /** List of all available versions */
  public static final String VERSIONS_KNOWN_LIST = DONOR_CHECK_HOME + ".version.avail.ser";

  public static final String REMOTE_URL = "http://www.genvisis.org/";
  public static final String REMOTE_VERSION_FILE = REMOTE_URL + "hla_releases.txt";

  public static final String LOCAL_VERSIONS_DIR = DONOR_CHECK_HOME + ".hlaversions" + File.separator;

  private static String version = null;

  public static String getVersion() {
    if (version == null) {
      try {
        final Properties properties = new Properties();
        properties.load(LandingController.class.getClassLoader().getResourceAsStream("project.properties"));
        version = properties.getProperty("version");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return version;
  }
}
