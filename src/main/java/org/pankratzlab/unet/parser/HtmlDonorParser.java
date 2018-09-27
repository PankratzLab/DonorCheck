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
import java.util.function.BiConsumer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;

public class HtmlDonorParser extends AbstractDonorFileParser {

  private static final String DISPLAY_STRING = "HTML";
  private static final String FILE_CHOOSER_HEADER = "Select DonorEdit HTML";
  private static final String INITIAL_NAME = "DonorEdit";
  private static final String EXTENSION_DESC = "DonorEdit HTML";
  private static final String EXTENSION_NAME = "html";
  private static final String EXTENSION = "*." + EXTENSION_NAME;
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
  public BiConsumer<ValidationTable, ValidationModel> setModel() {
    return ValidationTable::setFirstModel;
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
      buildModelFromHTML(builder, parsed);
    } catch (IOException e) {
      throw new IllegalStateException("Invalid HTML file: " + file);
    }
  }

  /**
   * Helper method to translate the parsed HTML to a {@link ValidationModel}
   */
  private void buildModelFromHTML(ValidationModelBuilder builder, Document parsed) {

    Element idElement = parsed.getElementsByAttributeValue("id", "hdonid").get(0);
    builder.donorId(idElement.val());

    // TODO these could be stored in a map of String to Consumer<String> and done in a general way
    getHTMLTypeValue(parsed, "A1").ifPresent(builder::a);
    getHTMLTypeValue(parsed, "A2").ifPresent(builder::a);
    getHTMLTypeValue(parsed, "B1").ifPresent(builder::b);
    getHTMLTypeValue(parsed, "B2").ifPresent(builder::b);
    getHTMLTypeValue(parsed, "C1").ifPresent(builder::c);
    getHTMLTypeValue(parsed, "C2").ifPresent(builder::c);
    getHTMLTypeValue(parsed, "DR1").ifPresent(builder::drb);
    getHTMLTypeValue(parsed, "DR2").ifPresent(builder::drb);
    getHTMLTypeValue(parsed, "DQB1").ifPresent(builder::dqb);
    getHTMLTypeValue(parsed, "DQB2").ifPresent(builder::dqb);
    getHTMLTypeValue(parsed, "DQA1").ifPresent(builder::dqa);
    getHTMLTypeValue(parsed, "DQA2").ifPresent(builder::dqa);
    getHTMLTypeValue(parsed, "DPB1").ifPresent(builder::dpb);
    getHTMLTypeValue(parsed, "DPB2").ifPresent(builder::dpb);

    getHTMLTypeValue(parsed, "BW4").ifPresent(s -> builder.bw4(decodeHTMLBoolean(s)));
    getHTMLTypeValue(parsed, "BW6").ifPresent(s -> builder.bw6(decodeHTMLBoolean(s)));
    getHTMLTypeValue(parsed, "DR51").ifPresent(s -> builder.dr51(decodeHTMLBoolean(s)));
    getHTMLTypeValue(parsed, "DR52").ifPresent(s -> builder.dr52(decodeHTMLBoolean(s)));
    getHTMLTypeValue(parsed, "DR53").ifPresent(s -> builder.dr53(decodeHTMLBoolean(s)));

  }

  /**
   * Booleans are exported as a linear value which we have to translate to true/false
   */
  private boolean decodeHTMLBoolean(String boolString) {
    return !boolString.contains(HTML_NEGATIVE);
  }

  /**
   * Helper method to extract the text of a particular tag.
   * 
   * @param parsed Parent {@link Document}
   * @param typeString The typing to look up (e.g. A1)
   * @return The value of the given tag
   */
  private Optional<String> getHTMLTypeValue(Document parsed, String typeString) {
    // All tags with type info start and end with these
    final String prefix = "ddl";
    final String suffix = "_label";

    return getText(parsed.getAllElements().get(0).getElementsByAttributeValue("id",
        prefix + typeString + suffix));
  }

}
