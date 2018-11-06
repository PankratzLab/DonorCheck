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
package org.pankratzlab.unet.parser.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.hla.type.HLAType;
import org.pankratzlab.hla.type.SeroType;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

/**
 * Parses SCORE6 QType format to donor model
 */
public class XmlQTyperParser {

  // -- Type assignment requires assessing allele frequencies, which are read from an HTML doc --
  private static final String P_TYPE_SUFFIX = "P";
  private static final String G_TYPE_SUFFIX = "G";
  private static final String FREQ_TABLE = "table";
  private static final String FREQ_TABLE_ROW = "tr";
  private static final String FREQ_TABLE_COL = "td";
  private static final String WELL_DOCUMENTED_FLAG = "WD";
  private static final String COMMON_FLAG = "C";
  private static final String ALLELE_FREQ_PATH = "/cwd200.html";
  private static ImmutableMultimap<String, Double> ALLELE_FREQS;

  private static final String SPEC_SEPARATOR = ":";
  private static final String LOCUS_SEPARATOR = "*";
  private static final String RESULT_SEPARATOR = ",";

  // - Specific allele values -
  private static final Set<String> UNDEFINED_TOKENS = ImmutableSet.of("-");
  private static final String UNDEFINED_TYPE = "Undefined";
  private static final String NULL_TYPE = "Null";

  // -- XML Tags required for parsing --
  public static final String ROOT_ELEMENT = "batchsubmission";
  private static final String PATIENT_ID_TAG = "patientId";
  private static final String ALLELE_RESULTS_TAG = "alleleResults";
  private static final String RESULT_COMBINATION_TAG = "resultCombination";
  private static final String SERO_COMBINATION_TAG = "serologicalCombination";
  private static final String ALLELE_COMBINATION_TAG = "alleleCombination";
  private static final String LOCUS_TAG = "locus";
  private static final String SINGLE_LOCUS_TAG = "typedLocus";
  private static final String LOCI_LIST_TAG = "typedLoci";

  private static final String DRB_HEADER = "HLA-DRB";
  private static final String DQA_HEADER = "HLA-DQA1";
  private static final String DPB_HEADER = "HLA-DPB1";
  private static final String DQB_HEADER = "HLA-DQB1";
  private static final String C_HEADER = "HLA-C";
  private static final String B_HEADER = "HLA-B";
  private static final String A_HEADER = "HLA-A";

  // -- BW4 and BW6 status is not embedded in the XML, so we need to explicitly do a look up --

  private static final ImmutableSet<String> BW4 =
      ImmutableSet.of("B5", "B5102", "B5103", "B13", "B17", "B27", "B37", "B38", "B44", "B47",
          "B49", "B51", "B52", "B53", "B57", "B58", "B59", "B63", "B77");

  private static final ImmutableSet<String> BW6 = ImmutableSet.of("B7", "B703", "B8", "B14", "B18",
      "B22", "B2708", "B35", "B39", "B3901", "B3902", "B40", "B4005", "B41", "B42", "B45", "B46",
      "B48", "B50", "B54", "B55", "B56", "B60", "B61", "B62", "B64", "B65", "B67", "B70", "B71",
      "B72", "B73", "B75", "B76", "B78", "B81", "B82");
  private static final String PARENT_TYPE_SEPARATOR = "/";

  // Map of Locus values to setter + type prefix
  private static ImmutableMap<String, TypeSetter> metadataMap;

  private static ImmutableMap<String, BiFunction<String, Element, List<String>>> xmlTypeMap;

  private static void init() {
    // -- Build mapping between loci sections, serotype prefixes and validation model setters --
    Builder<String, TypeSetter> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(A_HEADER, new TypeSetter("A", ValidationModelBuilder::a));
    setterBuilder.put(B_HEADER, new TypeSetter("B", ValidationModelBuilder::b));
    setterBuilder.put(C_HEADER, new TypeSetter("Cw", ValidationModelBuilder::c));
    setterBuilder.put(DQB_HEADER, new TypeSetter("DQ", ValidationModelBuilder::dqb));

    // Reported as allele types
    setterBuilder.put(DPB_HEADER, new TypeSetter("DPB1*", ValidationModelBuilder::dpb));
    setterBuilder.put(DQA_HEADER, new TypeSetter("DQA1*", ValidationModelBuilder::dqa));

    // DR52/53/54 appears as a serological combination
    setterBuilder.put(DRB_HEADER, new TypeSetter("DR", ValidationModelBuilder::drb));

    // NB: not in DonorNet
    // setterBuilder.put("HLA-DPA1", null);

    metadataMap = setterBuilder.build();

    // -- Build mapping between loci and serological/allele parsing
    Builder<String, BiFunction<String, Element, List<String>>> xmlBuilder = ImmutableMap.builder();

    xmlBuilder.put(A_HEADER, XmlQTyperParser::parseSerological);
    xmlBuilder.put(B_HEADER, XmlQTyperParser::parseSerological);
    xmlBuilder.put(C_HEADER, XmlQTyperParser::parseSerological);
    xmlBuilder.put(DRB_HEADER, XmlQTyperParser::parseSerological);
    xmlBuilder.put(DQB_HEADER, XmlQTyperParser::parseSerological);

    // Reported as allele types
    xmlBuilder.put(DPB_HEADER, XmlQTyperParser::parseAlleles);
    xmlBuilder.put(DQA_HEADER, XmlQTyperParser::parseAlleles);

    // DR52/53/54 appears as a serological combination
    xmlTypeMap = xmlBuilder.build();

    // -- Read allele frequency map --
    try (InputStream htmlStream = XmlDonorParser.class.getResourceAsStream(ALLELE_FREQ_PATH)) {
      Document parsed = Jsoup.parse(htmlStream, "UTF-8", "http://example.com");
      ImmutableListMultimap.Builder<String, Double> freqMapBuilder =
          new ImmutableListMultimap.Builder<>();

      // Three tables - base, G type and P type
      for (Element frequencyTable : parsed.getElementsByTag(FREQ_TABLE)) {
        Elements rows = frequencyTable.getElementsByTag(FREQ_TABLE_ROW);

        // first row is a header
        for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
          Element row = rows.get(rowNum);
          Elements columns = row.getElementsByTag(FREQ_TABLE_COL);
          String freqKey = columns.get(0).text();
          if (freqKey.endsWith(G_TYPE_SUFFIX) || freqKey.endsWith(P_TYPE_SUFFIX)) {
            // Remove G and P designations
            freqKey = freqKey.substring(0, freqKey.length() - 1);
          }
          Double freqVal = getCwdWeight(columns.get(1).text());
          freqMapBuilder.put(freqKey, freqVal);
        }
      }
      ALLELE_FREQS = freqMapBuilder.build();
    } catch (IOException e) {
      throw new IllegalStateException("Invalid Frequency file: " + ALLELE_FREQ_PATH);
    }
  }

  /**
   * @return A numeric weight whether the input allele is common, well-documented or unknown.
   */
  private static Double getCwdWeight(String cwdText) {
    switch (cwdText) {
      case COMMON_FLAG:
        return 1.0;
      case WELL_DOCUMENTED_FLAG:
        return 0.5;
    }
    return 0.0;
  }

  public static void buildModelFromXML(ValidationModelBuilder builder, Document doc) {
    if (metadataMap == null) {
      init();
    }

    // These fields are not present if false. If present they are true
    builder.bw4(false);
    builder.bw6(false);
    builder.dr51(false);
    builder.dr52(false);
    builder.dr53(false);

    DonorNetUtils.getText(doc.getElementsByTag(PATIENT_ID_TAG))
        .ifPresent(s -> builder.donorId(s.toUpperCase()));


    Element element = doc.getElementsByTag(LOCI_LIST_TAG).get(0);
    Elements elementsByTag = element.getElementsByTag(SINGLE_LOCUS_TAG);
    for (Element e : elementsByTag) {
      processLocus(builder, e);
    }
  }

  /**
   * Add a locus's block to the builder
   */
  private static void processLocus(ValidationModelBuilder builder, Element typedLocus) {
    Optional<String> tag = DonorNetUtils.getText(typedLocus.getElementsByTag(LOCUS_TAG));
    if (tag.isPresent()) {
      String locus = tag.get();
      if (!xmlTypeMap.containsKey(locus) || !metadataMap.containsKey(locus)) {
        return;
      }

      TypeSetter typeSetter = metadataMap.get(locus);

      // Each locus has one allele results block which contains potential allele pairs
      Elements resultCombinations = typedLocus.getElementsByTag(ALLELE_RESULTS_TAG).get(0)
          .getElementsByTag(RESULT_COMBINATION_TAG);

      List<String> types = new ArrayList<>();

      // Parse the allele pairs in the result section
      for (int result = 0; result < resultCombinations.size(); result++) {
        List<String> resultTypes = null;
        try {
          resultTypes = xmlTypeMap.get(locus).apply(typeSetter.getTokenPrefix(),
              resultCombinations.get(result));
          if (types.isEmpty() || DRB_HEADER.equals(locus)) {
            // DRB contains the DR5* designations in the same results block as the actual types
            // Otherwise, the first valid result is added
            types.addAll(resultTypes);
          } else if (isTestListPreferred(types, resultTypes)) {
            // If an allele pair with a better frequency weight is discovered, switch to that
            types = resultTypes;
          }

        } catch (Exception e) {
        }
      }

      // -- Locus-specific processing of the individual types --

      if (DQA_HEADER.equals(locus)) {
        // if DQA we have to remove the higher-position spec
        List<String> tmp = new ArrayList<>();
        for (String type : types) {
          if (type.contains(SPEC_SEPARATOR)) {
            type = type.substring(0, type.indexOf(SPEC_SEPARATOR));
          }
          tmp.add(type);
        }
        types = tmp;
      }

      if (DRB_HEADER.equals(locus)) {
        // Remove the DR51/52/53 types from the type list and set the appropriate flag(s)
        List<String> tmp = new ArrayList<>();
        for (String type : types) {
          switch (type) {
            case "DR51":
              builder.dr51(true);
              break;
            case "DR52":
              builder.dr52(true);
              break;
            case "DR53":
              builder.dr53(true);
              break;
            default:
              tmp.add(type);
          }
        }

        types = tmp;
      }


      for (String type : types) {
        // If this is a BW4 or BW6 type, set the appropriate flag
        if (BW4.contains(type)) {
          builder.bw4(true);
        }
        if (BW6.contains(type)) {
          builder.bw6(true);
        }
      }

      // Finally, add the types to the model builder
      for (String type : types) {
        typeSetter.getSetter().accept(builder, type.replace(typeSetter.getTokenPrefix(), ""));
      }
    }
  }

  /**
   * @param reference Base list
   * @param test List to compare to base
   * @return true if the test list contains a more frequent allele pairing than the reference set.
   *         If the frequency is the same, the specificity values will be compared - preferring
   *         lower values.
   */
  private static boolean isTestListPreferred(List<String> reference, List<String> test) {
    if (Objects.isNull(test) || test.isEmpty()) {
      return false;
    }

    // Test if test has higher frequency alleles
    int diff = Double.compare(freqScore(reference), freqScore(test));

    if (diff == 0) {
      // Test if test contains alleles with smaller spec
      // When comparing the specificities, a smaller value is preferred
      return compareSpec(reference, test) > 0;
    }

    // When comparing the frequency score, a larger value is preferred
    return diff < 0;
  }

  /**
   * @return As {@link Comparable#compareTo} when comparing each allele in the given lists, based
   *         purely on specificity numbers.
   */
  private static int compareSpec(List<String> reference, List<String> test) {
    if (!Objects.equals(reference.size(), test.size())) {
      String refString = reference.stream().collect(Collectors.joining(", "));
      String testString = test.stream().collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "Found allele combinations with different sizes: " + refString + " - " + testString);
    }

    int diff = 0;

    // Compare the lists until we find different types
    for (int i = 0; i < reference.size() && diff == 0; i++) {
      HLAType refType = HLAType.valueOf(reference.get(i));
      HLAType testType = HLAType.valueOf(test.get(i));

      diff = refType.compareTo(testType);
    }

    return diff;
  }


  /**
   * @param alleles Set of alleles
   * @return A combined weighting of the input allele set, based on their CWD frequencies
   */
  private static double freqScore(List<String> alleles) {
    double score = 0;

    for (String allele : alleles) {
      if (ALLELE_FREQS.containsKey(allele)) {
        OptionalDouble average = ALLELE_FREQS.get(allele).stream().mapToDouble(a -> a).average();
        if (average.isPresent()) {
          score += average.getAsDouble();
        }
      }
    }
    return score;
  }

  /**
   * Parser for allele combinations from an allele-based loci
   *
   * @return The list of alleles discovered for this combination
   */
  private static List<String> parseAlleles(String locus, Element combination) {
    List<String> firstTypes = getFirstValidTypes(combination, ALLELE_COMBINATION_TAG);
    for (String type : firstTypes) {
      // Ensure these are recognized types
      HLAType.valueOfStrict(type);
    }
    return firstTypes;
  }

  /**
   * Parser for allele combinations from an serological loci
   *
   * @return The list of alleles discovered for this combination
   */
  private static List<String> parseSerological(String locus, Element combination) {
    List<String> types = getFirstValidTypes(combination, SERO_COMBINATION_TAG);
    List<Integer> undefined = new ArrayList<>();

    // Check if there are undefined types in the result list
    for (int i = 0; i < types.size(); i++) {
      String type = types.get(i);
      if (UNDEFINED_TYPE.equals(type)) {
        undefined.add(i);
      }
    }

    // If so, we need to look up the corresponding allele type for the undefined serotypes
    if (!undefined.isEmpty()) {
      List<String> alleles = parseAlleles(locus, combination);
      for (Integer index : undefined) {
        String allele = alleles.get(index);
        // We have to convert the locus to the serological version
        if (allele.indexOf(LOCUS_SEPARATOR) > 0) {
          allele = allele.substring(allele.indexOf(LOCUS_SEPARATOR) + 1, allele.length());
        }
        // We also have to remove the high-resolution typing
        if (allele.contains(SPEC_SEPARATOR)) {
          allele = locus + allele.substring(0, allele.indexOf(SPEC_SEPARATOR));
        }
        types.set(index, allele);
      }
    }

    // Finally, we ensure these are recognized types
    for (String type : types) {
      SeroType.valueOf(type);
    }

    return types;
  }

  /**
   * Parses the types from an allele combination block. Each block contains 1 or more individual
   * combinations (alleles). Each allele may have multiple results.
   */
  private static List<String> getFirstValidTypes(Element combinations, String tagPrefix) {
    List<String> typeText = new ArrayList<>();
    for (int i = 1; i < 5; i++) {
      Elements results = combinations.getElementsByTag(tagPrefix + i);
      if (!results.isEmpty() && results.get(0).hasText()) {
        String type = null;
        String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

        for (int result = 0; (Strings.isNullOrEmpty(type) || UNDEFINED_TYPE.equals(type))
            && result < resultTypes.length; result++) {
          String tmp = resultTypes[result];
          // The undefined tokens are just skipped
          // Null types don't take precedence over any other values
          if (UNDEFINED_TOKENS.contains(tmp) || (NULL_TYPE.equals(tmp) && Objects.nonNull(type))) {
            continue;
          }
          type = tmp;
        }

        // Sometimes the inheritance structure of the types is listed. We only want the first (most
        // specific) type.
        if (type.contains(PARENT_TYPE_SEPARATOR)) {
          type = type.substring(0, type.indexOf(PARENT_TYPE_SEPARATOR));
        }

        // If the only result is {@link #NULL_TYPE}, the allele is skipped (homozygous).
        if (!NULL_TYPE.equals(type)) {
          typeText.add(type);
        }
      }
    }
    return typeText;
  }
}
