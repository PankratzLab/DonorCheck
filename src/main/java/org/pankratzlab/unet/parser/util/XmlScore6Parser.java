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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.base.Strings;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * Parses SCORE6 QType format to donor model
 */
public class XmlScore6Parser {

  // -- Type assignment requires assessing allele frequencies, which are read from an HTML doc --


  private static final String LOCUS_SEPARATOR = "*";
  private static final String RESULT_SEPARATOR = ",";

  // - Specific allele values -
  private static final String UNDEFINED_TYPE = "Undefined";
  private static final Set<String> UNDEFINED_TOKENS = ImmutableSet.of(UNDEFINED_TYPE, "-");
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

  private static ImmutableMap<String, BiConsumer<ValidationModelBuilder, String>> metadataMap;
  private static ImmutableMap<HLALocus, BiConsumer<ValidationModelBuilder, String>> drbMap;

  // Map of Locus values to setter + type prefix
  private static ImmutableMap<String, Function<ResultCombination, String>> specStringGeneratorMap;

  private static void init() {
    // -- Build mapping between loci sections, serotype prefixes and validation model setters --
    Builder<String, BiConsumer<ValidationModelBuilder, String>> setterBuilder =
        ImmutableMap.builder();
    setterBuilder.put(A_HEADER, ValidationModelBuilder::a);
    setterBuilder.put(B_HEADER, ValidationModelBuilder::b);
    setterBuilder.put(C_HEADER, ValidationModelBuilder::c);
    setterBuilder.put(DQB_HEADER, ValidationModelBuilder::dqb);

    // Reported as allele types
    setterBuilder.put(DPB_HEADER, ValidationModelBuilder::dpb);
    setterBuilder.put(DQA_HEADER, ValidationModelBuilder::dqa);

    // DR52/53/54 appears as a serological combination
    setterBuilder.put(DRB_HEADER, ValidationModelBuilder::drb);

    // -- Build mapping specifically for DRB3/4/5 alleles. These are also under the DRB header but
    // parsed differently --
    Builder<HLALocus, BiConsumer<ValidationModelBuilder, String>> drbBuilder =
        ImmutableMap.builder();
    drbBuilder.put(HLALocus.DRB3, ValidationModelBuilder::dr52);
    drbBuilder.put(HLALocus.DRB4, ValidationModelBuilder::dr53);
    drbBuilder.put(HLALocus.DRB5, ValidationModelBuilder::dr51);
    drbMap = drbBuilder.build();

    // NB: not in DonorNet
    // setterBuilder.put("HLA-DPA1", null);

    metadataMap = setterBuilder.build();


    // -- Build mapping between loci and serological/allele parsing
    Builder<String, Function<ResultCombination, String>> specGeneratorBuilder =
        ImmutableMap.builder();
    specGeneratorBuilder.put(A_HEADER, XmlScore6Parser::getSeroSpec);
    specGeneratorBuilder.put(B_HEADER, XmlScore6Parser::getSeroSpec);
    specGeneratorBuilder.put(C_HEADER, XmlScore6Parser::getSeroSpec);
    specGeneratorBuilder.put(DRB_HEADER, XmlScore6Parser::getSeroSpec);
    specGeneratorBuilder.put(DQB_HEADER, XmlScore6Parser::getSeroSpec);
    specGeneratorBuilder.put(DPB_HEADER, XmlScore6Parser::getAlleleSpec);
    specGeneratorBuilder.put(DQA_HEADER, XmlScore6Parser::getDQASpec);

    // DR52/53/54 appears as a serological combination
    specStringGeneratorMap = specGeneratorBuilder.build();
  }


  public static void buildModelFromXML(ValidationModelBuilder builder, Document doc) {
    if (Objects.isNull(metadataMap)) {
      init();
    }

    // These fields are not present if false. If present they are true
    builder.bw4(false);
    builder.bw6(false);

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
      if (!metadataMap.containsKey(locus)) {
        return;
      }

      // Each locus has one allele results block which contains potential allele pairs
      Elements resultCombinations = typedLocus.getElementsByTag(ALLELE_RESULTS_TAG).get(0)
          .getElementsByTag(RESULT_COMBINATION_TAG);

      int selectedResult = -1;
      List<ResultCombination> resultPairs = new ArrayList<>();
      List<ResultCombination> drb345Pairs = new ArrayList<>();

      // Parse the allele pairs in the result section
      for (int currentResult = 0; currentResult < resultCombinations.size(); currentResult++) {
        Element currentCombination = resultCombinations.get(currentResult);

        List<ResultCombination> combinations = parseResultCombinations(currentCombination);

        List<ResultCombination> toUpdate = resultPairs;

        boolean isDRB345 =
            DRB_HEADER.equals(locus) && !currentCombination.toString().contains("DRB1");

        if (isDRB345) {
          toUpdate = drb345Pairs;

          // Ensure we only check DRB345 combinations that are consistent with the DRB1 result
          Multiset<HLALocus> countDRB1 = countDRB1(resultPairs);
          Multiset<HLALocus> countDRB345 = countDRB345(combinations);
          if (countDRB345.size() > countDRB1.size() || !countDRB1.containsAll(countDRB345)) {
            continue;
          }
        }

        // Check if the new set is preferred to the old set
        if (!toUpdate.isEmpty() && isTestListPreferred(toUpdate, combinations)) {
          toUpdate.clear();
        }

        if (toUpdate.isEmpty()) {
          // Record this set
          toUpdate.addAll(combinations);

          if (!isDRB345) {
            // The selected result is the selected non-DRB345 index
            selectedResult = currentResult;
          }
        }
      }

      // -- Locus-specific processing of the individual types --
      if (!Objects.isNull(drb345Pairs) && !drb345Pairs.isEmpty()) {
        for (ResultCombination drbType : drb345Pairs) {
          HLAType drbAllele = drbType.getAlleleCombination();
          if (drbMap.containsKey(drbAllele.locus())) {
            String drbString = String.valueOf(drbAllele.spec().get(0));
            drbMap.get(drbAllele.locus()).accept(builder, drbString);
          }
        }
      }

      if (C_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::cHaplotype);
      } else if (DQB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::dqHaplotype);
      } else if (DRB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::drHaplotype);
      } else if (B_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResult),
            ValidationModelBuilder::bHaplotype);
        Map<Strand, BwGroup> bwMap = new HashMap<>();

        // Build the Bw strand map
        for (int strandIdx = 0; strandIdx < resultPairs.size(); strandIdx++) {
          bwMap.put(Strand.values()[strandIdx],
              BwSerotypes.getBwGroup(resultPairs.get(strandIdx).getAntigenCombination()));
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
      }

      // Finally, add the types to the model builder
      for (ResultCombination combination : resultPairs) {
        String specString = specStringGeneratorMap.get(locus).apply(combination);
        metadataMap.get(locus).accept(builder, specString);
      }
    }

  }

  private static Multiset<HLALocus> countDRB1(List<ResultCombination> resultPairs) {
    Multiset<HLALocus> drCounts = EnumMultiset.create(HLALocus.class);

    resultPairs.forEach(result -> {
      HLALocus drbLocus = DRAssociations.getDRBLocus(result.getAntigenCombination());

      if (drbLocus != null) {
        drCounts.add(drbLocus);
      }
    });

    return drCounts;
  }

  private static Multiset<HLALocus> countDRB345(List<ResultCombination> resultPairs) {
    Multiset<HLALocus> drCounts = EnumMultiset.create(HLALocus.class);

    resultPairs.forEach(result -> {
      HLALocus drbLocus = result.getAlleleCombination().locus();

      if (drbLocus != null) {
        drCounts.add(drbLocus);
      }
    });

    return drCounts;
  }


  private static List<ResultCombination> parseResultCombinations(Element currentCombination) {
    List<ResultCombination> combinations = new ArrayList<>();
    for (int combination = 1; combination <= 4; combination++) {
      ResultCombination nextResult = null;
      try {
        nextResult = parseCombination(currentCombination, combination);
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }
      if (Objects.nonNull(nextResult)) {
        combinations.add(nextResult);
      }
    }
    return combinations;
  }

  /**
  
   */
  private static ResultCombination parseCombination(Element resultCombinations,
      int combinationIndex) {
    if (!hasCombination(resultCombinations, combinationIndex)) {
      return null;
    }
    HLAType allele = null;
    SeroType antigen = null;

    String alleleString = getFirstValidType(
        resultCombinations.getElementsByTag(ALLELE_COMBINATION_TAG + combinationIndex));
    String antigenString = getFirstValidType(
        resultCombinations.getElementsByTag(SERO_COMBINATION_TAG + combinationIndex));

    // Ensure these are recognized types
    try {
      allele = HLAType.valueOfStrict(alleleString);
      // This type is valid
    } catch (IllegalArgumentException e) {
      // These types are invalid
    }

    if (Objects.nonNull(antigenString)) {
      if (NULL_TYPE.equals(antigenString)) {
        // This type isn't expressed
        return null;
      }
      if (UNDEFINED_TOKENS.contains(antigenString)) {
        // No serological equivalent defined so we just take the first position of the allele spec
        antigen = new SeroType(allele.locus().sero(), allele.spec().get(0));
      } else {
        antigen = SeroType.valueOf(antigenString);
      }
    }

    return new ResultCombination(antigen, allele);
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
  private static boolean isTestListPreferred(List<ResultCombination> referenceCombinations,
      List<ResultCombination> testCombinations) {
    if (Objects.isNull(testCombinations) || testCombinations.isEmpty()) {
      return false;
    }

    List<HLAType> reference = referenceCombinations.stream()
        .map(ResultCombination::getAlleleCombination).collect(Collectors.toList());
    List<HLAType> test = testCombinations.stream().map(ResultCombination::getAlleleCombination)
        .collect(Collectors.toList());

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
  private static int compareSpec(List<HLAType> reference, List<HLAType> test) {
    if (!Objects.equals(reference.size(), test.size())) {
      String refString =
          reference.stream().map(HLAType::toString).collect(Collectors.joining(", "));
      String testString = test.stream().map(HLAType::toString).collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "Found allele combinations with different sizes: " + refString + " - " + testString);
    }

    int diff = 0;

    // Compare the lists until we find different types
    for (int i = 0; i < reference.size() && diff == 0; i++) {
      HLAType refType = reference.get(i);
      HLAType testType = test.get(i);

      diff = refType.compareTo(testType);
    }

    return diff;
  }


  private static boolean hasCombination(Element resultCombinations, int combinationIndex) {
    for (String combinationTag : ImmutableSet.of(ALLELE_COMBINATION_TAG, SERO_COMBINATION_TAG)) {
      Elements results = resultCombinations.getElementsByTag(combinationTag + combinationIndex);
      if (results.isEmpty() || !results.get(0).hasText()) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return {@code null} if there is no type at this position (homozygous or single DRB345);
   *         {@link #NULL_TYPE} if the allele is present but not expressed; {@link #UNDEFINED_TYPE}
   *         if the allele is expressed but has no serological equivalent.
   */
  private static String getFirstValidType(Elements results) {
    String typeText = null;
    if (!results.isEmpty() && results.get(0).hasText()) {
      String type = null;
      String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

      for (int result = 0; (Strings.isNullOrEmpty(type) || UNDEFINED_TYPE.equals(type))
          && result < resultTypes.length; result++) {
        String tmp = resultTypes[result];
        // Null and Undefined types don't take precedence over any other values
        if (NULL_TYPE.equals(tmp) && Objects.nonNull(type)) {
          // Null type overrides undefined, but not other types
          continue;
        } else if (UNDEFINED_TOKENS.contains(tmp) && Objects.nonNull(type)
            && !NULL_TYPE.equals(type)) {
          // Undefined type overrides null, but not any other type
          continue;
        }
        type = tmp;
      }

      if (Objects.nonNull(type)) {
        // Sometimes the inheritance structure of the types is listed. We only want the first (most
        // specific) type.
        if (type.contains(PARENT_TYPE_SEPARATOR)) {
          type = type.substring(0, type.indexOf(PARENT_TYPE_SEPARATOR));
        }

        typeText = type;
      }
    }
    return typeText;
  }

  private static String getSeroSpec(ResultCombination combination) {
    return combination.getAntigenCombination().specString();
  }

  private static String getAlleleSpec(ResultCombination combination) {
    HLAType allele = combination.getAlleleCombination();
    // Cap the allele fields to two
    allele = new HLAType(allele.locus(), allele.spec().subList(0, 2));
    return allele.specString();
  }

  private static String getDQASpec(ResultCombination combination) {
    // if DQA we have to remove the higher-position spec
    return String.valueOf(combination.getAlleleCombination().spec().get(0));
  }

  private static class ResultCombination {
    private final SeroType antigenCombination;
    private final HLAType alleleCombination;

    public ResultCombination(@Nonnull SeroType antigenCombination,
        @Nonnull HLAType alleleCombination) {
      super();
      try {
        Objects.requireNonNull(antigenCombination);
        Objects.requireNonNull(alleleCombination);
      } catch (NullPointerException e) {
        System.out.println();
      }
      this.alleleCombination = alleleCombination;
      this.antigenCombination = antigenCombination;
    }

    @Override
    public String toString() {
      return "ResultCombination [antigenCombination=" + antigenCombination + ", alleleCombination="
          + alleleCombination + "]";
    }

    public SeroType getAntigenCombination() {
      return antigenCombination;
    }

    public HLAType getAlleleCombination() {
      return alleleCombination;
    }
  }
}
