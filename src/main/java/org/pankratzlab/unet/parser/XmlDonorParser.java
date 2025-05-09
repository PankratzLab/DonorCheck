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
package org.pankratzlab.unet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidParameterException;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.XmlDonorNetParser;
import org.pankratzlab.unet.parser.util.XmlScore6Parser;
import org.pankratzlab.unet.parser.util.XmlSureTyperParser;

/** {@link DonorFileParser} entry point for XML files */
public class XmlDonorParser extends AbstractDonorFileParser {

  private static final String BODY_TAG = "body";

  private static final String DISPLAY_STRING = "XML";

  private static final String FILE_CHOOSER_HEADER = "Select donor typing XML";
  private static final String INITIAL_NAME = "";
  private static final String EXTENSION_DESC = "DonorNet, SCORE 6, SureTyper";
  private static final String EXTENSION_NAME = "xml";
  private static final String EXTENSION = "*." + EXTENSION_NAME;

  @Override
  public String fileChooserHeader() {
    return FILE_CHOOSER_HEADER;
  }

  @Override
  public String initialName() {
    return INITIAL_NAME;
  }

  @Override
  public String getErrorText() {
    return "Invalid XML file. If this was from DonorNet, try downloading as HTML.";
  }

  @Override
  public String extensionFilter() {
    return EXTENSION;
  }

  @Override
  public String extensionDescription() {
    return EXTENSION_DESC;
  }

  public static String getTypeString() {
    return DISPLAY_STRING;
  }

  @Override
  protected String getDisplayString() {
    return DISPLAY_STRING;
  }

  @Override
  protected String extensionName() {
    return EXTENSION_NAME;
  }

  public static SourceType getSourceType(File file) {
    try (FileInputStream xmlStream = new FileInputStream(file)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      if (FilenameUtils.isExtension(file.getName(), EXTENSION_NAME)) {
        final String rootElement = parsed.getElementsByTag(BODY_TAG).get(0).child(0).tagName();

        // Based on XML contents, pass to specific XML parser
        if (XmlDonorNetParser.ROOT_ELEMENT.equals(rootElement)) {
          return SourceType.DonorNet;
        } else if (XmlScore6Parser.ROOT_ELEMENT.equals(rootElement)) {
          return SourceType.Score6;
        } else if (XmlSureTyperParser.ROOT_ELEMENT.equals(rootElement)) {
          return SourceType.SureTyper;
        }
      } else {
        throw new InvalidParameterException("Unknown File Type: " + file.getName());
      }
    } catch (Throwable e) {
      throw new IllegalStateException("Invalid XML file: " + file, e);
    }
    return null; // Unknown or unsupported file type
  }

  @Override
  protected void doParse(ValidationModelBuilder builder, File file) {
    try (FileInputStream xmlStream = new FileInputStream(file)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      if (FilenameUtils.isExtension(file.getName(), EXTENSION_NAME)) {
        final String rootElement = parsed.getElementsByTag(BODY_TAG).get(0).child(0).tagName();

        // Based on XML contents, pass to specific XML parser
        if (XmlDonorNetParser.ROOT_ELEMENT.equals(rootElement)) {
          builder.sourceType(SourceType.DonorNet);
          XmlDonorNetParser.buildModelFromXML(builder, parsed);
        } else if (XmlScore6Parser.ROOT_ELEMENT.equals(rootElement)) {
          builder.sourceType(SourceType.Score6);
          XmlScore6Parser.buildModelFromXML(builder, parsed);
        } else if (XmlSureTyperParser.ROOT_ELEMENT.equals(rootElement)) {
          builder.sourceType(SourceType.SureTyper);
          XmlSureTyperParser.buildModelFromXML(builder, parsed);
        }
      } else {
        throw new InvalidParameterException("Unknown File Type: " + file.getName());
      }
    } catch (Throwable e) {
      throw new IllegalStateException("Invalid XML file: " + file, e);
    }
  }
}
