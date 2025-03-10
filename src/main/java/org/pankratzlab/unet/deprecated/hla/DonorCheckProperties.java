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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;

/**
 * Helper class for managing persistent properties from a central location. Use {@link #get()} to
 * get the application-specific property values for the HLA suite. These will automatically be
 * updated on app shutdown.
 */
public final class DonorCheckProperties {
  private static final String PROP_FILE = Info.DONOR_CHECK_HOME + "hla.properties";

  public static final String FIRST_TYPE = "FIRST_TYPE";
  public static final String FIRST_TYPE_DEFAULT = PdfDonorParser.getTypeString();

  public static final String SECOND_TYPE = "SECOND_TYPE";
  public static final String SECOND_TYPE_DEFAULT = XmlDonorParser.getTypeString();

  public static final String USE_ALLELE_CALL = "USE_ALLELE_CALL";
  public static final String USE_ALLELE_CALL_DEFAULT = "true";

  public static final String SURETYPER_ALLOW_INVALID_DQA_ALLELES = "ALLOW_INVALID_SURETYPER_ALLELES";
  public static final String SURETYPER_ALLOW_INVALID_DQA_ALLELES_DEFAULT = "true";

  public static final String FAIL_OR_DISCARD_IF_AC_INVALID = "FAIL_OR_DISCARD_IF_AC_INVALID";
  public static final String AC_INVALID_FAIL = "Fail";
  public static final String AC_INVALID_DISCARD = "Discard";
  public static final String FAIL_OR_DISCARD_IF_AC_INVALID_DEFAULT = AC_INVALID_FAIL;

  private static Properties hlaProps;

  public static Properties get() {
    if (hlaProps == null) {
      loadProps();
    }
    return hlaProps;
  }

  public static String getOrDefault(String key) {
    get();
    switch (key) {
      case FIRST_TYPE:
        return hlaProps.getProperty(FIRST_TYPE, FIRST_TYPE_DEFAULT);
      case SECOND_TYPE:
        return hlaProps.getProperty(SECOND_TYPE, SECOND_TYPE_DEFAULT);
      case USE_ALLELE_CALL:
        return hlaProps.getProperty(USE_ALLELE_CALL, USE_ALLELE_CALL_DEFAULT);
      case SURETYPER_ALLOW_INVALID_DQA_ALLELES:
        return hlaProps.getProperty(SURETYPER_ALLOW_INVALID_DQA_ALLELES, SURETYPER_ALLOW_INVALID_DQA_ALLELES_DEFAULT);
      case FAIL_OR_DISCARD_IF_AC_INVALID:
        return hlaProps.getProperty(FAIL_OR_DISCARD_IF_AC_INVALID, FAIL_OR_DISCARD_IF_AC_INVALID_DEFAULT);
      default:
        return null;
    }
  }

  /**
   * If the properties have not already been loaded, load them and register a hook to write them to
   * disk on shutdown.
   */
  private static synchronized void loadProps() {
    if (hlaProps == null) {
      Properties props = new Properties();
      if (new File(PROP_FILE).exists()) {
        try (FileInputStream in = new FileInputStream(PROP_FILE)) {
          props.load(in);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      // Ensure the properties are updated on app close
      Runtime.getRuntime().addShutdownHook(new Thread(() -> writeProps()));
      hlaProps = props;
    }
  }

  /** Update the properties file on disk */
  private static void writeProps() {
    try (FileOutputStream out = new FileOutputStream(PROP_FILE)) {
      hlaProps.store(out, "---HLA properties---");
      out.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
