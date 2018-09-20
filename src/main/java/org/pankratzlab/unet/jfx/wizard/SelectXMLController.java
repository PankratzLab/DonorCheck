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
package org.pankratzlab.unet.jfx.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Controller for adding XML-sourced donor data to the current {@link ValidationTable}
 *
 * @see ValidatingWizardController
 */
public class SelectXMLController extends AbstractFileSelectController {

  private static final String BOOL_FALSE = "96";
  private static final String BOOL_TRUE = "95";
  private static final String DQA_MAP_PATH = "/DqaMap.xml";
  private static final String DPB_MAP_PATH = "/DpbMap.xml";
  private static final String FILE_CHOOSER_HEADER = "Select DonorNet XML";
  private static final String EXTENSION_DESC = "DonorNet Web Page";
  private static final String WIZARD_PANE_TITLE = "Step 2 of 4";
  private static final String INITIAL_NAME = "";
  private static final String EXTENSION = "*.xml";
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

    try (InputStream xmlStream = SelectXMLController.class.getResourceAsStream(donorNetMapPath)) {

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
  protected String extensionDesc() {
    return EXTENSION_DESC;
  }

  @Override
  protected String fileChooserHeader() {
    return FILE_CHOOSER_HEADER;
  }

  @Override
  protected String wizardPaneTitle() {
    return WIZARD_PANE_TITLE;
  }

  @Override
  protected String initialName() {
    return INITIAL_NAME;
  }

  @Override
  protected String extension() {
    return EXTENSION;
  }

  @Override
  protected BiConsumer<ValidationTable, ValidationModel> setModel() {
    return ValidationTable::setXmlModel;
  }

  @Override
  protected void parseModel(ValidationModelBuilder builder, File file) {
    try (FileInputStream xmlStream = new FileInputStream(file)) {

      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      buildModel(builder, parsed);

    } catch (IOException e) {
      throw new IllegalStateException("Invalid XML file: " + file);
    }
  }

  /**
   * Helper method to translate the parsed XML to a {@link ValidationModel}
   */
  private void buildModel(ValidationModelBuilder builder, Document doc) {
    Element donorRoot = doc.getElementsByTag("donor_edit_crossmatchhla").get(0);

    getTagVal(donorRoot, "don_id").ifPresent(builder::donorId);
    getTagVal(donorRoot, "a1").ifPresent(builder::a);
    getTagVal(donorRoot, "a2").ifPresent(builder::a);
    getTagVal(donorRoot, "b1").ifPresent(builder::b);
    getTagVal(donorRoot, "b2").ifPresent(builder::b);
    getTagVal(donorRoot, "c1").ifPresent(builder::c);
    getTagVal(donorRoot, "c2").ifPresent(builder::c);
    getTagVal(donorRoot, "dr1").ifPresent(builder::drb);
    getTagVal(donorRoot, "dr2").ifPresent(builder::drb);
    getTagVal(donorRoot, "dq1").ifPresent(builder::dqb);
    getTagVal(donorRoot, "dq2").ifPresent(builder::dqb);
    getTagVal(donorRoot, "dqa1").ifPresent(s -> builder.dqa(decodeValue(dqaMap, s)));
    getTagVal(donorRoot, "dqa2").ifPresent(s -> builder.dqa(decodeValue(dqaMap, s)));
    getTagVal(donorRoot, "dp1").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));
    getTagVal(donorRoot, "dp2").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));

    getTagVal(donorRoot, "bw4").ifPresent(s -> builder.bw4(decodeBoolean(s)));
    getTagVal(donorRoot, "bw6").ifPresent(s -> builder.bw6(decodeBoolean(s)));
    getTagVal(donorRoot, "dr51").ifPresent(s -> builder.dr51(decodeBoolean(s)));
    getTagVal(donorRoot, "dr52").ifPresent(s -> builder.dr52(decodeBoolean(s)));
    getTagVal(donorRoot, "dr53").ifPresent(s -> builder.dr53(decodeBoolean(s)));
  }

  /**
   * Booleans are exported as a linear value which we have to translate to true/false
   */
  private boolean decodeBoolean(String boolCode) {
    if (boolCode.equals(BOOL_TRUE)) {
      return true;
    } else if (boolCode.equals(BOOL_FALSE)) {
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
  private Optional<String> getTagVal(Element donorBlock, String tag) {
    Elements elements = donorBlock.getElementsByTag(tag);
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
