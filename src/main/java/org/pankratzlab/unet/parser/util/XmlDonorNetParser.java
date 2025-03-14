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
package org.pankratzlab.unet.parser.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/** Parses downloaded DonorNet XML (un-saved - after saving, only HTML is supported) to a model. */
public class XmlDonorNetParser {
  // If Bw4 or Bw6 tag has either 96 or 6 the B antigen family is deemed to be negative
  private static final ImmutableSet<String> XML_FALSE = ImmutableSet.of("96", "6");
  // If Bw4 or Bw6 tag has either 95 or 5 the B antigen family is deemed to be positive
  private static final ImmutableSet<String> XML_TRUE = ImmutableSet.of("95", "5");
  // Used to identify that input is a DonorNet file
  public static final String ROOT_ELEMENT = "donorupload";
  // Path to resources used for mapping DonorNet DQA, DPB and DRB XML elements
  private static final String DRB_MAP_PATH = "/DrbMap.xml";
  private static final String DQA_MAP_PATH = "/DqaMap.xml";
  private static final String DQB_MAP_PATH = "/DqbMap.xml";
  private static final String DPA_MAP_PATH = "/DpaMap.xml";
  private static final String DPB_MAP_PATH = "/DpbMap.xml";
  private static final String DR51_MAP_PATH = "/DR51Map.xml";
  private static final String DR52_MAP_PATH = "/DR52Map.xml";
  private static final String DR53_MAP_PATH = "/DR53Map.xml";
  private static final String XML_ATTR = "value";
  private static final String XML_TAG = "option";
  private static ImmutableMap<String, String> drbMap;
  private static ImmutableMap<String, String> dqaMap;
  private static ImmutableMap<String, String> dqbMap;
  private static ImmutableMap<String, String> dpaMap;
  private static ImmutableMap<String, String> dpbMap;
  private static ImmutableMap<String, String> dr51Map;
  private static ImmutableMap<String, String> dr52Map;
  private static ImmutableMap<String, String> dr53Map;

  /** Helper method to translate the parsed XML to a {@link ValidationModel} */
  public static void buildModelFromXML(ValidationModelBuilder builder, Document doc) {
    if (Objects.isNull(dqaMap)) {
      init();
    }

    Element donorRoot = doc.getElementsByTag("donor_edit_crossmatchhla").get(0);

    // TODO these could be stored in a map of String to Consumer<String> and done in a general way
    getXMLTagVal(donorRoot, "don_id").ifPresent(builder::donorId);
    getXMLTagVal(donorRoot, "a1").ifPresent(builder::a);
    getXMLTagVal(donorRoot, "a2").ifPresent(builder::a);
    getXMLTagVal(donorRoot, "b1").ifPresent(builder::b);
    getXMLTagVal(donorRoot, "b2").ifPresent(builder::b);
    getXMLTagVal(donorRoot, "c1").ifPresent(builder::c);
    getXMLTagVal(donorRoot, "c2").ifPresent(builder::c);
    getXMLTagVal(donorRoot, "dr1").ifPresent(s -> builder.drb(decodeValue(drbMap, s)));
    getXMLTagVal(donorRoot, "dr2").ifPresent(s -> builder.drb(decodeValue(drbMap, s)));
    getXMLTagVal(donorRoot, "dq1").ifPresent(s -> builder.dqbSerotype(decodeValue(dqbMap, s)));
    getXMLTagVal(donorRoot, "dq2").ifPresent(s -> builder.dqbSerotype(decodeValue(dqbMap, s)));
    getXMLTagVal(donorRoot, "dqa1").ifPresent(s -> builder.dqaSerotype(decodeValue(dqaMap, s)));
    getXMLTagVal(donorRoot, "dqa2").ifPresent(s -> builder.dqaSerotype(decodeValue(dqaMap, s)));
    getXMLTagVal(donorRoot, "dpa1").ifPresent(s -> builder.dpaSerotype(decodeValue(dpaMap, s)));
    getXMLTagVal(donorRoot, "dpa2").ifPresent(s -> builder.dpaSerotype(decodeValue(dpaMap, s)));
    getXMLTagVal(donorRoot, "dp1").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));
    getXMLTagVal(donorRoot, "dp2").ifPresent(s -> builder.dpb(decodeValue(dpbMap, s)));

    getXMLTagVal(donorRoot, "bw4").ifPresent(s -> builder.bw4(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "bw6").ifPresent(s -> builder.bw6(decodeXMLBoolean(s)));
    getXMLTagVal(donorRoot, "dr51").ifPresent(s -> builder.dr51(decodeValue(dr51Map, s)));
    getXMLTagVal(donorRoot, "dr52").ifPresent(s -> builder.dr52(decodeValue(dr52Map, s)));
    getXMLTagVal(donorRoot, "dr53").ifPresent(s -> builder.dr53(decodeValue(dr53Map, s)));
    getXMLTagVal(donorRoot, "dr51_2").ifPresent(s -> builder.dr51(decodeValue(dr51Map, s)));
    getXMLTagVal(donorRoot, "dr52_2").ifPresent(s -> builder.dr52(decodeValue(dr52Map, s)));
    getXMLTagVal(donorRoot, "dr53_2").ifPresent(s -> builder.dr53(decodeValue(dr53Map, s)));
  }

  /** Booleans are exported as a linear value which we have to translate to true/false */
  private static boolean decodeXMLBoolean(String boolCode) {
    if (XML_TRUE.contains(boolCode)) {
      return true;
    } else if (XML_FALSE.contains(boolCode)) {
      return false;
    }
    throw new IllegalArgumentException("Unrecognized boolean code: " + boolCode);
  }

  /**
   * For the loci that are stored as linear values, we have to map those values to the corresponding
   * specificity.
   */
  private static String decodeValue(Map<String, String> valueMap, String valueCode) {
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
  private static Optional<String> getXMLTagVal(Element donorBlock, String tag) {
    Elements elements = donorBlock.getElementsByTag(tag);
    return DonorNetUtils.getText(elements);
  }

  private static void init() {
    // NB: the DonorNet XML export uses linear numbering for the DPB, DQA, and DR51/52/53 loci.
    // This can be found in the .html version of the DonorNet output
    // All other loci export their actual specificities. Thus we have to map from the linear
    // numbers to specificities. These files contain mappings for the given locus and need to be
    // updated if the DonorNet pages ever change.
    drbMap = populateFromFile(DRB_MAP_PATH);
    dpbMap = populateFromFile(DPB_MAP_PATH);
    dpaMap = populateFromFile(DPA_MAP_PATH);
    dqaMap = populateFromFile(DQA_MAP_PATH);
    dqbMap = populateFromFile(DQB_MAP_PATH);
    dr51Map = populateFromFile(DR51_MAP_PATH);
    dr52Map = populateFromFile(DR52_MAP_PATH);
    dr53Map = populateFromFile(DR53_MAP_PATH);
  }

  /**
   * @param donorNetMapPath File containing {@link #XML_TAG} elements, each having a {@link #XML_ATTR}
   *        attribute which needs to be mapped to the corresponding string value of that tag.
   * @return The mapping defined in the input file
   */
  private static ImmutableMap<String, String> populateFromFile(String donorNetMapPath) {
    ImmutableMap.Builder<String, String> builder = new Builder<>();

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
}
