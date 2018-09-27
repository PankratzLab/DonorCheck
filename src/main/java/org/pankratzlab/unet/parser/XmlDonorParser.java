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
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class XmlDonorParser extends AbstractDonorFileParser {

  private static final String DISPLAY_STRING = "XML";
  private static final String XML_FALSE = "96";
  private static final String XML_TRUE = "95";
  private static final String DQA_MAP_PATH = "/DqaMap.xml";
  private static final String DPB_MAP_PATH = "/DpbMap.xml";
  private static final String FILE_CHOOSER_HEADER = "Select DonorNet Web Source";
  private static final String INITIAL_NAME = "";
  private static final String EXTENSION_DESC = "DonorNet XML";
  private static final String EXTENSION_NAME = "xml";
  private static final String EXTENSION = "*." + EXTENSION_NAME;
  private static final String XML_ATTR = "value";
  private static final String XML_TAG = "option";
  private static ImmutableMap<String, String> dqaMap;
  private static ImmutableMap<String, String> dpbMap;

  {
    // NB: the DonorNet XML export uses linear numbering (e.g. 1-100) for the DPB and DQA loci.
    // All other loci export their actual specificities. Thus we have to map from the linear
    // numbers to specificities. These files contain mappings for the given locus and need to be
    // updated if the DonorNet pages ever change.
    dpbMap = populateFromFile(DPB_MAP_PATH);

    dqaMap = populateFromFile(DQA_MAP_PATH);
  }

  /**
   * @param donorNetMapPath File containing {@link #XML_TAG} elements, each having a
   *        {@link #XML_ATTR} attribute which needs to be mapped to the corresponding string value
   *        of that tag.
   * @return The mapping defined in the input file
   */
  private static ImmutableMap<String, String> populateFromFile(String donorNetMapPath) {
    Builder<String, String> builder = new Builder<>();

    try (InputStream xmlStream = XmlDonorParser.class.getResourceAsStream(donorNetMapPath)) {

      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      for (Element element : parsed.getElementsByTag(XML_TAG)) {
        builder.put(element.attr(XML_ATTR), element.text());
      }

    } catch (IOException e) {
      throw new IllegalStateException("Invalid Map file: " + donorNetMapPath);
    }

    return builder.build();

  }


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
    return "Invalid DonorNet XML file. Try downloading as HTML.";
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
      if (FilenameUtils.isExtension(file.getName(), EXTENSION_NAME)) {
        buildModelFromXML(builder, parsed);
      } else {
        throw new InvalidParameterException("Unknown File Type: " + file.getName());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Invalid XML file: " + file);
    }
  }


  /**
   * Helper method to translate the parsed XML to a {@link ValidationModel}
   */
  private void buildModelFromXML(ValidationModelBuilder builder, Document doc) {
    Element donorRoot = doc.getElementsByTag("donor_edit_crossmatchhla").get(0);

    // TODO these could be stored in a map of String to Consumer<String> and done in a general way
    getXMLTagVal(donorRoot, "don_id").ifPresent(builder::donorId);
    getXMLTagVal(donorRoot, "a1").ifPresent(builder::a);
    getXMLTagVal(donorRoot, "a2").ifPresent(builder::a);
    getXMLTagVal(donorRoot, "b1").ifPresent(builder::b);
    getXMLTagVal(donorRoot, "b2").ifPresent(builder::b);
    getXMLTagVal(donorRoot, "c1").ifPresent(builder::c);
    getXMLTagVal(donorRoot, "c2").ifPresent(builder::c);
    getXMLTagVal(donorRoot, "dr1").ifPresent(builder::drb);
    getXMLTagVal(donorRoot, "dr2").ifPresent(builder::drb);
    getXMLTagVal(donorRoot, "dq1").ifPresent(builder::dqb);
    getXMLTagVal(donorRoot, "dq2").ifPresent(builder::dqb);
    getXMLTagVal(donorRoot, "dqa1").ifPresent(s -> builder.dqa(decodeValue(dqaMap, s)));
    getXMLTagVal(donorRoot, "dqa2").ifPresent(s -> builder.dqa(decodeValue(dqaMap, s)));
    getXMLTagVal(donorRoot, "dp1").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));
    getXMLTagVal(donorRoot, "dp2").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));

    getXMLTagVal(donorRoot, "bw4").ifPresent(s -> builder.bw4(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "bw6").ifPresent(s -> builder.bw6(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "dr51").ifPresent(s -> builder.dr51(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "dr52").ifPresent(s -> builder.dr52(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "dr53").ifPresent(s -> builder.dr53(decodeXMLBoolean(s)));
  }


  /**
   * Booleans are exported as a linear value which we have to translate to true/false
   */
  private boolean decodeXMLBoolean(String boolCode) {
    if (boolCode.equals(XML_TRUE)) {
      return true;
    } else if (boolCode.equals(XML_FALSE)) {
      return false;
    }
    throw new IllegalArgumentException("Unrecognized boolean code: " + boolCode);
  }


  /**
   * For the loci that are stored as linear values, we have to map those values to the corresponding
   * specificity.
   */
  private String decodeValue(Map<String, String> valueMap, String valueCode) {
    return valueMap.get(valueCode);
  }


  /**
   * Helper method to extract the text of a particular tag.
   * 
   * @param donorBlock Parent {@link Element}
   * @param tag XML Tag of interest
   * @return An {@link Optional} containing either the text value of the tag, or {@code} null if the
   *         tag wasn't present or was empty.
   */
  private Optional<String> getXMLTagVal(Element donorBlock, String tag) {
    Elements elements = donorBlock.getElementsByTag(tag);
    return getText(elements);
  }
}
