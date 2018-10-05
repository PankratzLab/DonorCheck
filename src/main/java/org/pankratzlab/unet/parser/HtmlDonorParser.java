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
package org.pankratzlab.unet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;

/**
 * {@link DonorFileParser} for HTML files. Currently this is restricted to DonorNet html
 */
public class HtmlDonorParser extends AbstractDonorFileParser {

  private static final String SELECTED_VALUE = "selected";
  private static final String SELECTED_ATTRIBUTE = SELECTED_VALUE;
  private static final String DONOR_ATTRIBUTE = "hdonid";
  private static final String HTML_TYPE_ATTR = "id";
  private static final String HTML_SUFFIX = "_label";
  private static final String HTML_PREFIX = "ddl";
  private static final String DISPLAY_STRING = "HTML";
  private static final String FILE_CHOOSER_HEADER = "Select DonorEdit HTML";
  private static final String INITIAL_NAME = "DonorEdit";
  private static final String EXTENSION_DESC = "Donor HTML";
  private static final String EXTENSION_NAME = "html";
  private static final String EXTENSION = "DonorEdit." + EXTENSION_NAME;
  private static final String HTML_NEGATIVE = "Negative";


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
    return "Invalid DonorEdit.html file. Try downloading as XML.";
  }

  @Override
  public String extensionFilter() {
    return EXTENSION;
  }

  @Override
  public String extensionDescription() {
    return EXTENSION_DESC;
  }

  @Override
  protected String getDisplayString() {
    return DISPLAY_STRING;
  }

  @Override
  protected String extensionName() {
    return EXTENSION_NAME;
  }

  @Override
  protected void doParse(ValidationModelBuilder builder, File file) {
    try (FileInputStream xmlStream = new FileInputStream(file)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");

      // Test for saved or unsaved HTML state
      // No selected elements means this has been saved previously
      Elements selectedElements = parsed.getElementsByAttribute(SELECTED_ATTRIBUTE);

      if (selectedElements.isEmpty()) {
        buildModelFromHTML(builder, parsed, this::getSavedHTMLType);
      } else {
        buildModelFromHTML(builder, parsed, this::getUnsavedHTMLType);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Invalid HTML file: " + file);
    }
  }

  /**
   * Helper method to translate the parsed HTML to a {@link ValidationModel}
   */
  private void buildModelFromHTML(ValidationModelBuilder builder, Document parsed,
      BiFunction<Document, String, Optional<String>> typeParser) {

    Element idElement = parsed.getElementsByAttributeValue(HTML_TYPE_ATTR, DONOR_ATTRIBUTE).get(0);
    builder.donorId(idElement.val());

    // TODO these could be stored in a map of String to Consumer<String> and done in a general way
    typeParser.apply(parsed, "A1").ifPresent(builder::a);
    typeParser.apply(parsed, "A2").ifPresent(builder::a);
    typeParser.apply(parsed, "B1").ifPresent(builder::b);
    typeParser.apply(parsed, "B2").ifPresent(builder::b);
    typeParser.apply(parsed, "C1").ifPresent(builder::c);
    typeParser.apply(parsed, "C2").ifPresent(builder::c);
    typeParser.apply(parsed, "DR1").ifPresent(builder::drb);
    typeParser.apply(parsed, "DR2").ifPresent(builder::drb);
    typeParser.apply(parsed, "DQB1").ifPresent(builder::dqb);
    typeParser.apply(parsed, "DQB2").ifPresent(builder::dqb);
    typeParser.apply(parsed, "DQA1").ifPresent(builder::dqa);
    typeParser.apply(parsed, "DQA2").ifPresent(builder::dqa);
    typeParser.apply(parsed, "DPB1").ifPresent(builder::dpb);
    typeParser.apply(parsed, "DPB2").ifPresent(builder::dpb);

    typeParser.apply(parsed, "BW4").ifPresent(s -> builder.bw4(decodeHTMLBoolean(s)));
    typeParser.apply(parsed, "BW6").ifPresent(s -> builder.bw6(decodeHTMLBoolean(s)));
    typeParser.apply(parsed, "DR51").ifPresent(s -> builder.dr51(decodeHTMLBoolean(s)));
    typeParser.apply(parsed, "DR52").ifPresent(s -> builder.dr52(decodeHTMLBoolean(s)));
    typeParser.apply(parsed, "DR53").ifPresent(s -> builder.dr53(decodeHTMLBoolean(s)));

  }

  /**
   * Booleans are exported as a linear value which we have to translate to true/false
   */
  private boolean decodeHTMLBoolean(String boolString) {
    return !boolString.contains(HTML_NEGATIVE);
  }

  /**
   * Helper method to extract the type text from HTML for a donor still editable
   * 
   * @param parsed Parent {@link Document}
   * @param typeString The typing to look up (e.g. A1)
   * @return The value of the given type
   */
  private Optional<String> getUnsavedHTMLType(Document parsed, String typeString) {
    Element typeRow =
        parsed.getElementsByAttributeValue(HTML_TYPE_ATTR, HTML_PREFIX + typeString).get(0);
    Elements selectedOption =
        typeRow.getElementsByAttributeValue(SELECTED_ATTRIBUTE, SELECTED_VALUE);
    return DonorNetUtils.getText(selectedOption);
  }

  /**
   * Helper method to extract the type text from HTML for a previously saved donor
   * 
   * @param parsed Parent {@link Document}
   * @param typeString The typing to look up (e.g. A1)
   * @return The value of the given type
   */
  private Optional<String> getSavedHTMLType(Document parsed, String typeString) {
    // All tags with type info start and end with these

    return DonorNetUtils.getText(parsed.getAllElements().get(0)
        .getElementsByAttributeValue(HTML_TYPE_ATTR, HTML_PREFIX + typeString + HTML_SUFFIX));
  }

}
