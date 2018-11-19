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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Parses SCORE6 QType format to donor model
 */
public class XmlQTyperParser {

  // -- Type assignment requires assessing allele frequencies, which are read from an HTML doc --


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


      int selectedResult = -1;
      List<String> types = new ArrayList<>();
      List<String> drTypes = new ArrayList<>();

      // Parse the allele pairs in the result section
      for (int result = 0; result < resultCombinations.size(); result++) {
        List<String> resultTypes = null;

        try {
          resultTypes = xmlTypeMap.get(locus).apply(typeSetter.getTokenPrefix(),
              resultCombinations.get(result));
          if (DRB_HEADER.equals(locus)
              && !resultCombinations.get(result).toString().contains("DRB1")) {
            // Non-DRB1 headers in the DR locus are DR5* indicators
            drTypes.addAll(resultTypes);
          } else if (types.isEmpty()) {
            // Record the first result
            types.addAll(resultTypes);
            selectedResult = result;
          } else if (isTestListPreferred(types, resultTypes)) {
            // If an allele pair with a better frequency weight is discovered, switch to that
            types = resultTypes;
            selectedResult = result;
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
      } else if (C_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::cHaplotype);
      } else if (B_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::bHaplotype);
        Map<Strand, BwGroup> bwMap = new HashMap<>();

        // Build the Bw strand map
        for (int strandIdx = 0; strandIdx < types.size(); strandIdx++) {
          bwMap.put(Strand.values()[strandIdx], BwSerotypes.getBwGroup(types.get(strandIdx)));
        }
        builder.bwHaplotype(bwMap);

        // Update the appropriate builder flags
        for (BwGroup bw : bwMap.values()) {
          switch (bw) {
            case Bw4:
              builder.bw4(true);
              break;
            case Bw6:
              builder.bw6(true);
              break;
            default:
              break;
          }
        }
      } else if (DQB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::dqHaplotype);
      }


      if (DRB_HEADER.equals(locus)) {

        // Add haplotypes
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::drHaplotype);

        // Remove the DR51/52/53 types from the type list and set the appropriate flag(s)
        for (String dr : drTypes) {
          switch (dr) {
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
          }
        }

      }

      // Finally, add the types to the model builder
      for (String type : types) {
        typeSetter.getSetter().accept(builder, type.replace(typeSetter.getTokenPrefix(), ""));
      }
    }
  }

  private static void addHaplotypes(ValidationModelBuilder builder, Element resultCombination,
      BiConsumer<ValidationModelBuilder, Multimap<Strand, HLAType>> haplotypeSetter) {
    for (int strandIndex = 1; strandIndex <= Strand.values().length; strandIndex++) {
      Elements results = resultCombination.getElementsByTag(ALLELE_COMBINATION_TAG + strandIndex);
      if (results == null || results.isEmpty()) {
        continue;
      }

      String[] alleleStrings = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);
      Multimap<Strand, HLAType> haplotypeMap = HashMultimap.create();
      String locus = "";
      for (String allele : alleleStrings) {
        if (allele.indexOf(LOCUS_SEPARATOR) > 0) {
          locus = allele.substring(0, allele.indexOf(LOCUS_SEPARATOR));
          allele = allele.substring(allele.indexOf(LOCUS_SEPARATOR) + 1, allele.length());
        }

        if (allele.endsWith("N")) {
          // Skip null types
          continue;
        }

        // Replace suffix flags
        allele.replaceAll("[LSCAQ]", "");

        HaplotypeUtils.parseAllelesToStrandMap(allele, locus, strandIndex - 1, haplotypeMap);
      }

      haplotypeSetter.accept(builder, haplotypeMap);
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
    int diff = Double.compare(CommonWellDocumented.cwdScore(reference),
        CommonWellDocumented.cwdScore(test));

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
