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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.logging.log4j.util.TriConsumer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.AllelePairings;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/** Parses SCORE6 QType format to donor model */
public class XmlScore6Parser {

  // -- Type assignment requires assessing allele frequencies, which are read from an HTML doc --
  private static final String LOCUS_SEPARATOR = "*";
  private static final String RESULT_SEPARATOR = ",";

  // - Specific allele values -
  private static final String UNDEFINED_TYPE = "Undefined";
  private static final String UNDEFINED_TYPE2 = "undefined";
  private static final Set<String> UNDEFINED_TOKENS =
      ImmutableSet.of(UNDEFINED_TYPE, UNDEFINED_TYPE2, "-");
  private static final String NULL_TYPE = "Null";

  // -- XML Tags required for parsing --
  public static final String ROOT_ELEMENT = "batchsubmission";
  private static final String PATIENT_ID_TAG = "patientId";
  private static final String ALLELE_RESULTS_TAG = "alleleResults";
  private static final String RESULT_COMBINATION_TAG = "resultCombination";
  private static final String SERO_COMBINATION_TAG = "serologicalCombination";
  private static final String ALLELE_COMBINATIONS_TAG = "alleleCombinations";
  private static final String ALLELE_COMBINATION_TAG = "alleleCombination";
  private static final String LOCUS_TAG = "locus";
  private static final String SINGLE_LOCUS_TAG = "typedLocus";
  private static final String LOCI_LIST_TAG = "typedLoci";

  // -- Section headers --
  private static final String DRB_HEADER = "HLA-DRB";
  private static final String DQA_HEADER = "HLA-DQA1";
  private static final String DPB_HEADER = "HLA-DPB1";
  private static final String DPA_HEADER = "HLA-DPA1";
  private static final String DQB_HEADER = "HLA-DQB1";
  private static final String C_HEADER = "HLA-C";
  private static final String B_HEADER = "HLA-B";
  private static final String A_HEADER = "HLA-A";

  // -- Delimiter for parent hierarchies --
  private static final String PARENT_TYPE_SEPARATOR = "/";

  // -- These came into use after nomenclature had switched to molecular --
  private static final Set<String> DISREGARD_SERO = ImmutableSet.of("DQA1", "DPA1", "DPB1");

  // -- Map to which value to use for each locus
  private static ImmutableMap<String, BiConsumer<ValidationModelBuilder, String>> metadataMap;
  private static ImmutableMap<String, BiConsumer<ValidationModelBuilder, HLAType>> metadataTypeMap;
  private static ImmutableMap<String, BiConsumer<ValidationModelBuilder, String>> metadataNonCWDMap;
  private static ImmutableMap<String, BiConsumer<ValidationModelBuilder, HLAType>> metadataTypeNonCWDMap;
  private static ImmutableMap<String, TriConsumer<ValidationModelBuilder, HLALocus, AllelePairings>> possibleAlleleRecordingMap;
  private static ImmutableMap<String, TriConsumer<ValidationModelBuilder, HLALocus, AllelePairings>> donorAlleleRecordingMap;
  private static ImmutableMap<HLALocus, BiConsumer<ValidationModelBuilder, String>> drbMap;

  // Map of Locus values to setter + type prefix
  private static ImmutableMap<String, Function<ResultCombination, String>> specStringGeneratorMap;
  private static ImmutableMap<String, Function<HLAType, String>> specStringGeneratorMap1;
  private static ImmutableMap<String, BiFunction<SeroType, HLAType, String>> specStringGeneratorMap2;


  private static void init() {
    // -- Build mapping between loci sections, serotype prefixes and validation model setters --
    Builder<String, BiConsumer<ValidationModelBuilder, String>> setterBuilder =
        ImmutableMap.builder();
    setterBuilder.put(A_HEADER, ValidationModelBuilder::a);
    setterBuilder.put(B_HEADER, ValidationModelBuilder::b);
    setterBuilder.put(C_HEADER, ValidationModelBuilder::c);
    setterBuilder.put(DQB_HEADER, ValidationModelBuilder::dqbSerotype);

    // Reported as allele types
    setterBuilder.put(DPB_HEADER, ValidationModelBuilder::dpbSerotype);
    setterBuilder.put(DQA_HEADER, ValidationModelBuilder::dqaSerotype);
    setterBuilder.put(DPA_HEADER, ValidationModelBuilder::dpaSerotype);

    // DR52/53/54 appears as a serological combination
    setterBuilder.put(DRB_HEADER, ValidationModelBuilder::drb);

    // NB: not in DonorNet
    // setterBuilder.put("HLA-DPA1", null);

    metadataMap = setterBuilder.build();

    // -- Build mapping bemetadataTypeMaptween loci sections, serotype prefixes and validation model
    // setters --
    Builder<String, BiConsumer<ValidationModelBuilder, HLAType>> setterBuilderTypes =
        ImmutableMap.builder();
    setterBuilderTypes.put(A_HEADER, ValidationModelBuilder::aType);
    setterBuilderTypes.put(B_HEADER, ValidationModelBuilder::bType);
    setterBuilderTypes.put(C_HEADER, ValidationModelBuilder::cType);
    setterBuilderTypes.put(DQB_HEADER, ValidationModelBuilder::dqbType);

    // Reported as allele types
    // DPB is already being tracked as HLATypes
    setterBuilderTypes.put(DPB_HEADER, ValidationModelBuilder::dpb);
    setterBuilderTypes.put(DQA_HEADER, ValidationModelBuilder::dqaType);
    setterBuilderTypes.put(DPA_HEADER, ValidationModelBuilder::dpaType);

    // DR52/53/54 appears as a serological combination
    // setterBuilderTypes.put(DRB_HEADER, ValidationModelBuilder::drbType);

    // NB: not in DonorNet
    // setterBuilder.put("HLA-DPA1", null);

    metadataTypeMap = setterBuilderTypes.build();

    // -- Build mapping bemetadataTypeMaptween loci sections, serotype prefixes and validation model
    // setters --
    Builder<String, BiConsumer<ValidationModelBuilder, HLAType>> setterBuilderTypesNonCWD =
        ImmutableMap.builder();
    setterBuilderTypesNonCWD.put(A_HEADER, ValidationModelBuilder::aTypeNonCWD);
    setterBuilderTypesNonCWD.put(B_HEADER, ValidationModelBuilder::bTypeNonCWD);
    setterBuilderTypesNonCWD.put(C_HEADER, ValidationModelBuilder::cTypeNonCWD);
    setterBuilderTypesNonCWD.put(DQB_HEADER, ValidationModelBuilder::dqbTypeNonCWD);

    // Reported as allele types
    // DPB is already being tracked as HLATypes
    setterBuilderTypesNonCWD.put(DPB_HEADER, ValidationModelBuilder::dpbNonCIWD);
    setterBuilderTypesNonCWD.put(DQA_HEADER, ValidationModelBuilder::dqaTypeNonCWD);
    setterBuilderTypesNonCWD.put(DPA_HEADER, ValidationModelBuilder::dpaTypeNonCWD);

    // DR52/53/54 appears as a serological combination
    // setterBuilderTypes.put(DRB_HEADER, ValidationModelBuilder::drbType);

    // NB: not in DonorNet
    // setterBuilder.put("HLA-DPA1", null);

    metadataTypeNonCWDMap = setterBuilderTypesNonCWD.build();

    // -- Build mapping between loci sections, serotype prefixes and validation model setters --
    Builder<String, BiConsumer<ValidationModelBuilder, String>> setterBuilderNonCWD =
        ImmutableMap.builder();
    setterBuilderNonCWD.put(A_HEADER, ValidationModelBuilder::aNonCWD);
    setterBuilderNonCWD.put(B_HEADER, ValidationModelBuilder::bNonCWD);
    setterBuilderNonCWD.put(C_HEADER, ValidationModelBuilder::cNonCWD);
    setterBuilderNonCWD.put(DQB_HEADER, ValidationModelBuilder::dqbNonCWD);

    // Reported as allele types
    setterBuilderNonCWD.put(DPB_HEADER, ValidationModelBuilder::dpbNonCWD);
    setterBuilderNonCWD.put(DQA_HEADER, ValidationModelBuilder::dqaNonCWD);
    setterBuilderNonCWD.put(DPA_HEADER, ValidationModelBuilder::dpaNonCWD);

    // DR52/53/54 appears as a serological combination
    setterBuilderNonCWD.put(DRB_HEADER, ValidationModelBuilder::drbNonCWD);

    metadataNonCWDMap = setterBuilderNonCWD.build();

    Builder<String, TriConsumer<ValidationModelBuilder, HLALocus, AllelePairings>> setterBuilderAlleles =
        ImmutableMap.builder();
    setterBuilderAlleles.put(A_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(B_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(C_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(DQA_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(DQB_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(DPA_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(DPB_HEADER, ValidationModelBuilder::possibleAllelePairings);
    setterBuilderAlleles.put(DRB_HEADER, ValidationModelBuilder::possibleAllelePairings);

    possibleAlleleRecordingMap = setterBuilderAlleles.build();

    Builder<String, TriConsumer<ValidationModelBuilder, HLALocus, AllelePairings>> setterBuilderDonorAlleles =
        ImmutableMap.builder();
    setterBuilderDonorAlleles.put(A_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(B_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(C_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(DQA_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(DQB_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(DPA_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(DPB_HEADER, ValidationModelBuilder::donorAllelePairings);
    setterBuilderDonorAlleles.put(DRB_HEADER, ValidationModelBuilder::donorAllelePairings);

    donorAlleleRecordingMap = setterBuilderDonorAlleles.build();

    // -- Build mapping specifically for DRB3/4/5 alleles. These are also under the DRB header but
    // parsed differently --
    Builder<HLALocus, BiConsumer<ValidationModelBuilder, String>> drbBuilder =
        ImmutableMap.builder();
    drbBuilder.put(HLALocus.DRB3, ValidationModelBuilder::dr52);
    drbBuilder.put(HLALocus.DRB4, ValidationModelBuilder::dr53);
    drbBuilder.put(HLALocus.DRB5, ValidationModelBuilder::dr51);
    drbMap = drbBuilder.build();

    // -- Build mapping between loci and serological/allele parsing
    Builder<String, Function<ResultCombination, String>> specGeneratorBuilder =
        ImmutableMap.builder();
    Builder<String, Function<HLAType, String>> specGenerator1Builder = ImmutableMap.builder();
    Builder<String, BiFunction<SeroType, HLAType, String>> specGenerator2Builder =
        ImmutableMap.builder();
    specGeneratorBuilder.put(A_HEADER, XmlScore6Parser::getAlleleSpec);
    specGenerator1Builder.put(A_HEADER, XmlScore6Parser::getAlleleSpec);
    specGeneratorBuilder.put(B_HEADER, XmlScore6Parser::getAlleleLookup);
    specGenerator2Builder.put(B_HEADER, XmlScore6Parser::getAlleleLookup);
    specGeneratorBuilder.put(C_HEADER, XmlScore6Parser::getAlleleLookup);
    specGenerator2Builder.put(C_HEADER, XmlScore6Parser::getAlleleLookup);
    specGeneratorBuilder.put(DRB_HEADER, XmlScore6Parser::getAlleleLookup);
    specGenerator2Builder.put(DRB_HEADER, XmlScore6Parser::getAlleleLookup);
    specGeneratorBuilder.put(DQB_HEADER, XmlScore6Parser::getAlleleLookup);
    specGenerator2Builder.put(DQB_HEADER, XmlScore6Parser::getAlleleLookup);
    specGeneratorBuilder.put(DPB_HEADER, XmlScore6Parser::getAlleleSpec);
    specGenerator1Builder.put(DPB_HEADER, XmlScore6Parser::getAlleleSpec);
    specGeneratorBuilder.put(DQA_HEADER, XmlScore6Parser::getAlleleSpec);
    specGenerator1Builder.put(DQA_HEADER, XmlScore6Parser::getAlleleSpec);
    specGeneratorBuilder.put(DPA_HEADER, XmlScore6Parser::getAlleleSpec);
    specGenerator1Builder.put(DPA_HEADER, XmlScore6Parser::getAlleleSpec);

    // DR52/53/54 appears as a serological combination
    specStringGeneratorMap = specGeneratorBuilder.build();
    specStringGeneratorMap1 = specGenerator1Builder.build();
    specStringGeneratorMap2 = specGenerator2Builder.build();
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

  /** Add a locus's block to the builder */
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

      // each locus has one allele combination block which contains possible allele pairs
      Elements alleleCombinations = typedLocus.getElementsByTag(ALLELE_COMBINATIONS_TAG).get(0)
          .getElementsByTag(RESULT_COMBINATION_TAG);

      Elements pairs = typedLocus.getElementsByTag("resultPairs").get(0)
          .getElementsByTag(RESULT_COMBINATION_TAG);


      int selectedResultIndex = -1;
      int selectedDRB345Index = -1;
      List<ResultCombination> firstResultPairs = new ArrayList<>();
      List<ResultCombination> ciwdResultPairs = new ArrayList<>();
      List<ResultCombination> drb345Pairs = new ArrayList<>();
      List<ResultCombination> drb345PairsFirst = new ArrayList<>();

      HLALocus hlaLocus = null;

      AllelePairings allPossibleAllelePairs = new AllelePairings();
      AllelePairings donorAllelePairs = new AllelePairings();

      // first, iterate through allele list element pairs and parse possible allele pairings
      for (int i = 0; i < pairs.size(); i++) {
        Element currPair = pairs.get(i);
        Elements a1Alleles =
            currPair.getElementsByTag("alleleList1").get(0).getElementsByTag("allele");
        Elements a2Alleles =
            currPair.getElementsByTag("alleleList2").get(0).getElementsByTag("allele");

        for (Element a1E : a1Alleles) {
          String a1A = a1E.text();
          for (Element a2E : a2Alleles) {
            String a2A = a2E.text();
            allPossibleAllelePairs.addPairing(a1A, a2A);
          }
        }
      }

      // next, parse alleleCombinations elements for donor's possible allele pairs
      // these are different from the alleleResults elements
      /*
       * Class-1 Loci (A/B/C) have one set of possible allele pairs in the alleleResults element
       * 
       * Class-2 Loci (DR/DP/DQ) can have multiple sets of possible allele pairs in the
       * alleleResults element
       * 
       * TODO clarify how/why these are different
       */

      for (int currentResult = 0; currentResult < alleleCombinations.size(); currentResult++) {
        Element currentCombination = alleleCombinations.get(currentResult);
        HLALocus locusType = null;

        // The DRB345 combinations are grouped into a single "DRB" header with DRB1
        boolean isDRB345 =
            DRB_HEADER.equals(locus) && !currentCombination.toString().contains("DRB1");

        if (isDRB345) {
          // We can't derive the locus from the header for DRB
          locusType = parseDRBCombination(currentCombination.toString());
          if (locusType == null) {
            // NB: this situation is only known to arise when an individual is homozygous with
            // unexpressed DRB345s
            // In this scenario, we can fall back to looking at the DRB1 alleles to figure out what
            // we should find.
            locusType = deduceDRB345Locus(ciwdResultPairs);
          }
        } else {
          locusType = HLALocus.safeValueOf(locus.substring(locus.indexOf("-") + 1));
        }

        // Each combination is an allele/antigen pair
        parseAlleleCombinations(currentCombination, locusType, donorAllelePairs);
      }

      // Parse the allele lists in the result section
      // the first allele in each list goes into the "firstResultPairs" list
      // the first **CIWD allele** goes into the "actualResultPairs" list

      /*
       * if we're parsing a Class-2 locus, we find the "best" pair to use for the
       * "actualResultPairs" list, and then use the first pairing from that pair for the
       * "firstResultPairs" list
       */
      for (int currentResult = 0; currentResult < resultCombinations.size(); currentResult++) {
        Element currentCombination = resultCombinations.get(currentResult);
        HLALocus locusType = null;

        // The DRB345 combinations are grouped into a single "DRB" header with DRB1
        boolean isDRB345 =
            DRB_HEADER.equals(locus) && !currentCombination.toString().contains("DRB1");

        if (isDRB345) {
          // We can't derive the locus from the header for DRB
          locusType = parseDRBCombination(currentCombination.toString());
          if (locusType == null) {
            // NB: this situation is only known to arise when an individual is homozygous with
            // unexpressed DRB345s
            // In this scenario, we can fall back to looking at the DRB1 alleles to figure out what
            // we should find.
            locusType = deduceDRB345Locus(ciwdResultPairs);
          }
        } else {
          locusType = HLALocus.safeValueOf(locus.substring(locus.indexOf("-") + 1));
        }

        if (hlaLocus == null) {
          hlaLocus = locusType;
        } else if (hlaLocus.compareTo(locusType) != 0) {
          /*
           * This seems to only happen when parsing the DRB3/4/5 loci, as the original locus is DRB1
           */
        }

        // Each combination is an allele/antigen pair
        List<ResultCombination> comboCIWD =
            parseResultCombinations(currentCombination, locusType, true);
        List<ResultCombination> comboFirst =
            parseResultCombinations(currentCombination, locusType, false);

        List<ResultCombination> toUpdateCIWD = ciwdResultPairs;
        List<ResultCombination> toUpdateFirst = firstResultPairs;

        if (isDRB345) {
          toUpdateCIWD = drb345Pairs;
          toUpdateFirst = drb345PairsFirst;

          // Ensure we only check DRB345 combinations that are consistent with the DRB1 result
          Multiset<HLALocus> countDRB1 = countDRB1(ciwdResultPairs);
          Multiset<HLALocus> countDRB345 = countDRB345(comboCIWD);
          if (countDRB345.size() > countDRB1.size() || !countDRB1.containsAll(countDRB345)) {
            continue;
          }
        }

        // Check if the current combination is better than the last best combination
        if (!toUpdateCIWD.isEmpty() && isTestListPreferred(toUpdateCIWD, comboCIWD)) {
          // if so, clear both CIWD/First lists
          toUpdateCIWD.clear();
          toUpdateFirst.clear();
        }

        if (toUpdateCIWD.isEmpty()) {
          // Record this set
          toUpdateCIWD.addAll(comboCIWD);
          if (!toUpdateFirst.isEmpty()) {
            toUpdateFirst.clear();
          }
          toUpdateFirst.addAll(comboFirst);

          if (!isDRB345) {
            // The selected result is the selected non-DRB345 index
            selectedResultIndex = currentResult;
          } else {
            selectedDRB345Index = currentResult;
          }
        }
      }

      // -- Locus-specific processing of the individual types --
      if (!Objects.isNull(drb345PairsFirst) && !drb345PairsFirst.isEmpty()) {
        // Set the DRB345 types
        for (ResultCombination drbType : drb345PairsFirst) {
          HLAType drbAllele = drbType.getAlleleCombination();
          if (drbMap.containsKey(drbAllele.locus())) {
            String drbString = String.valueOf(drbAllele.spec().get(0));
            drbMap.get(drbAllele.locus()).accept(builder, drbString);
          }
        }
      }

      // Parse haplotypes
      if (C_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResultIndex),
            identityLocusMap(HLALocus.C), ValidationModelBuilder::cHaplotype);
      } else if (DQB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResultIndex),
            identityLocusMap(HLALocus.DQB1), ValidationModelBuilder::dqHaplotype);
      } else if (DRB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResultIndex),
            identityLocusMap(HLALocus.DRB1), ValidationModelBuilder::drHaplotype);
        if (selectedDRB345Index > 0) {
          addHaplotypes(builder, resultCombinations.get(selectedDRB345Index),
              drb345Map(ciwdResultPairs), ValidationModelBuilder::dr345Haplotype);
        } else {
          builder.dr345Haplotype(ArrayListMultimap.create());
        }
      } else if (B_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResultIndex),
            identityLocusMap(HLALocus.B), ValidationModelBuilder::bHaplotype);

        for (int strandIdx = 0; strandIdx < ciwdResultPairs.size(); strandIdx++) {
          // B*47:03 is considered BW6 despite B47 antigen being considered BW4
          if (ciwdResultPairs.get(strandIdx).alleleCombination.toString().contains("B*47:03")) {
            builder.bw6(true);
            break;
          }
          // Update the appropriate builder flags
          BwGroup bw =
              BwSerotypes.getBwGroup(ciwdResultPairs.get(strandIdx).getAntigenCombination());
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
      } else if (DPB_HEADER.equals(locus)) {
        addHaplotypes(builder, resultCombinations.get(selectedResultIndex),
            identityLocusMap(HLALocus.DPB1), ValidationModelBuilder::dpHaplotype);
      }

      // Finally, add the types to the model builder
      // if the first two fields of the firstResultPairs alleles are the same
      // as the first two fields of the actualResultPairs alleles, we don't
      // need to flag this locus as non-CIWD
      Set<HLAType> ciwdTwoField = new HashSet<>();
      Set<HLAType> firstTwoField = new HashSet<>();

      Set<String> ciwdTypes = new HashSet<>();
      Set<String> firstTypes = new HashSet<>();

      for (int i = 0; i < ciwdResultPairs.size(); i++) {
        HLAType ciwdAllele = ciwdResultPairs.get(i).alleleCombination;
        if (ciwdAllele.resolution() > 2) {
          ciwdAllele =
              new HLAType(ciwdAllele.locus(), ciwdAllele.spec().get(0), ciwdAllele.spec().get(1));
        } else if (ciwdAllele.resolution() == 1) {
          // TODO
        }
        ciwdTwoField.add(ciwdAllele);
        ciwdTypes.add(convertToSerotype(locus, ciwdAllele));
      }

      for (int i = 0; i < firstResultPairs.size(); i++) {
        HLAType firstAllele = firstResultPairs.get(i).alleleCombination;
        if (firstAllele.resolution() > 2) {
          firstAllele = new HLAType(firstAllele.locus(), firstAllele.spec().get(0),
              firstAllele.spec().get(1));
        } else if (firstAllele.resolution() == 1) {
          // TODO
        }
        firstTwoField.add(firstAllele);
        firstTypes.add(convertToSerotype(locus, firstAllele));
      }

      boolean hasUnknown = firstResultPairs.stream().map(ResultCombination::getAlleleCombination)
          .map(CommonWellDocumented::getStatus).filter(c -> c == Status.UNKNOWN).count() > 0;

      if (hasUnknown) {
        if (ValidationModelBuilder.REPORT_SERO.contains(hlaLocus)) {
          if (!ciwdTypes.containsAll(firstTypes)) {
            builder.locusIsNonCIWD(ciwdResultPairs.get(0).alleleCombination);
          }
        } else {
          if (!ciwdTwoField.containsAll(firstTwoField)) {
            builder.locusIsNonCIWD(ciwdResultPairs.get(0).alleleCombination);
          }
        }
      }

      for (int i = 0; i < ciwdResultPairs.size(); i++) {
        final HLAType alleleCombination = ciwdResultPairs.get(i).alleleCombination;
        final HLAType alleleCombination2 = firstResultPairs.get(i).alleleCombination;

        String specStringActual = specStringGeneratorMap.get(locus).apply(ciwdResultPairs.get(i));
        String specStringFirst = specStringGeneratorMap.get(locus).apply(firstResultPairs.get(i));

        metadataMap.get(locus).accept(builder, specStringActual);

        if (metadataNonCWDMap.containsKey(locus)) {
          metadataNonCWDMap.get(locus).accept(builder, specStringFirst);
        }
        if (metadataTypeMap.containsKey(locus)) {
          metadataTypeMap.get(locus).accept(builder, alleleCombination);
        }
        if (metadataTypeNonCWDMap.containsKey(locus)) {
          metadataTypeNonCWDMap.get(locus).accept(builder, alleleCombination2);
        }
      }

      // record alleles for reassignment if necessary
      if (possibleAlleleRecordingMap.containsKey(locus)) {
        if (hlaLocus == null) {
          // TODO FIXME do something other than print to system out
          // System.out.println("Couldn't set alleles - no locus parsed from " + locus);
        } else {
          possibleAlleleRecordingMap.get(locus).accept(builder, hlaLocus, allPossibleAllelePairs);
          donorAlleleRecordingMap.get(locus).accept(builder, hlaLocus, donorAllelePairs);
        }
      }

    }
  }

  private static String convertToSerotype(String locus, HLAType h1) {
    String sero1;
    if (specStringGeneratorMap1.containsKey(locus)) {
      sero1 = specStringGeneratorMap1.get(locus).apply(h1);
    } else if (specStringGeneratorMap2.containsKey(locus)) {
      sero1 = specStringGeneratorMap2.get(locus).apply(SerotypeEquivalence.get(h1), h1);
    } else {
      sero1 = h1.equivSafe().specString();
    }
    return sero1;
  }

  /** Helper method to do a reverse lookup of sorts from DRB1 alleles to a DRB345 locus. */
  private static HLALocus deduceDRB345Locus(List<ResultCombination> drb1Combinations) {
    Set<HLALocus> loci = new HashSet<>();
    for (ResultCombination combination : drb1Combinations) {
      DRAssociations.getDRBLocus(combination.getAntigenCombination()).ifPresent(v -> loci.add(v));
    }
    if (loci.size() == 1) {
      return loci.iterator().next();
    }
    throw new IllegalStateException(
        "DRB345 alleles found without locus; interrogation of DRB1 alleles found " + loci.size()
            + " distinct DRB345 loci");
  }

  private static Map<Strand, HLALocus> drb345Map(List<ResultCombination> resultPairs) {
    Builder<Strand, HLALocus> mapBuilder = ImmutableMap.builder();
    int strandIdx = 0;
    for (int combinationIndex = 0; combinationIndex < resultPairs.size(); combinationIndex++) {
      Optional<HLALocus> locus =
          DRAssociations.getDRBLocus(resultPairs.get(combinationIndex).getAntigenCombination());
      if (locus.isPresent()) {
        mapBuilder.put(Strand.values()[strandIdx++], locus.get());
      }
    }
    return mapBuilder.build();
  }

  private static Map<Strand, HLALocus> identityLocusMap(HLALocus locus) {
    Builder<Strand, HLALocus> mapBuilder = ImmutableMap.builder();
    for (Strand strand : Strand.values()) {
      mapBuilder.put(strand, locus);
    }
    return mapBuilder.build();
  }

  /**
   * Helper method to read an xml combination and check which DRB* locus is contained within.
   *
   * @return The DRB locus discovered, or null if it is not found
   */
  private static HLALocus parseDRBCombination(String xml) {
    Set<HLALocus> drbLoci =
        ImmutableSet.of(HLALocus.DRB1, HLALocus.DRB3, HLALocus.DRB4, HLALocus.DRB5);

    for (HLALocus locus : drbLoci) {
      if (xml.contains(locus.toString() + "*")) {
        return locus;
      }
    }
    return null;
  }

  /**
   * @return The counts of each DRB345 equivalent locus in the given list of DRB1 combinations, per
   *         the mapping defined in {@link DRAssociations#getDRBLocus(SeroType)}
   */
  private static Multiset<HLALocus> countDRB1(List<ResultCombination> resultPairs) {
    Multiset<HLALocus> drCounts = EnumMultiset.create(HLALocus.class);

    resultPairs.forEach(result -> {
      DRAssociations.getDRBLocus(result.getAntigenCombination()).ifPresent(v -> drCounts.add(v));
    });

    return drCounts;
  }

  /** @return The counts of each DRB345 locus in the given list of DRB345 combinations. */
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

  private static void parseAlleleCombinations(Element resultCombination, HLALocus locus,
      AllelePairings allelePairs) {

    List<String> a1Types =
        getAllValidTypes(resultCombination.getElementsByTag("alleleList1"), locus);
    List<String> a2Types =
        getAllValidTypes(resultCombination.getElementsByTag("alleleList2"), locus);

    for (String a1 : a1Types) {
      for (String a2 : a2Types) {
        // TODO parse allele ranges (dashed-combos)
        allelePairs.addPairing(a1, a2);
      }
    }
  }

  /** Parse the allele + antigen pairs from a result combination */
  private static List<ResultCombination> parseResultCombinations(Element currentCombination,
      HLALocus locus, boolean requireCWD) {
    List<ResultCombination> combinations = new ArrayList<>();
    for (int combination = 1; combination <= 4; combination++) {
      ResultCombination nextResult = null;
      try {
        nextResult = parseCombination(currentCombination, combination, locus,
            ALLELE_COMBINATION_TAG, requireCWD ? XmlScore6Parser::getFirstValidCWDType
                : XmlScore6Parser::getFirstValidType);
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }
      if (Objects.nonNull(nextResult)) {
        combinations.add(nextResult);
      }
    }
    return combinations;
  }

  /** Parse a particular allele + antigen pair from a single result combination */
  private static ResultCombination parseCombination(Element resultCombinations,
      int combinationIndex, HLALocus locus, String alleleComboTag,
      BiFunction<Elements, HLALocus, String> typeFunction) {

    String alleleString = typeFunction
        .apply(resultCombinations.getElementsByTag(alleleComboTag + combinationIndex), locus);
    String antigenString1 = typeFunction
        .apply(resultCombinations.getElementsByTag(SERO_COMBINATION_TAG + combinationIndex), null);
    String antigenString =
        parseSerotype(resultCombinations.getElementsByTag(SERO_COMBINATION_TAG + combinationIndex));

    if (!hasCombination(resultCombinations, combinationIndex, alleleComboTag)
        && !DISREGARD_SERO.contains(locus.toString())) {
      return null;
    }
    if (alleleString != null && alleleString.matches(ValidationModelBuilder.NOT_EXPRESSED)) {
      return null;
    }
    HLAType allele = null;
    SeroType antigen = null;

    if (alleleString != null) {

      // Ensure this is a recognized allele
      try {
        allele = HLAType.valueOf(alleleString);
        // This type is valid
      } catch (IllegalArgumentException e) {
        // These types are invalid
      }

      if (Objects.nonNull(antigenString) || DISREGARD_SERO.contains(locus.toString())) {
        if (NULL_TYPE.equals(antigenString) && !DISREGARD_SERO.contains(locus.toString())) {
          // This combination isn't expressed
          return null;
        }
        if (UNDEFINED_TOKENS.contains(antigenString) || DISREGARD_SERO.contains(locus.toString())) {
          // No serological equivalent defined so we just take the first position of the allele spec
          antigen = new SeroType(allele.locus().sero(), allele.spec().get(0));
        } else {
          antigen = SeroType.valueOf(antigenString);
        }
      }
      return new ResultCombination(antigen, allele);
    }
    return null;
  }

  private static String parseSerotype(Elements results) {
    String typeText = null;
    if (!results.isEmpty() && results.get(0).hasText()) {
      String type = null;
      String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

      for (int result =
          0; (Strings.isNullOrEmpty(type) || UNDEFINED_TOKENS.contains(type) || isNullType(type))
              && result < resultTypes.length; result++) {
        String tmp = resultTypes[result];
        if (tmp.isEmpty()) {
          // Sometimes a leading comma can create empty strings - skip these
          continue;
        }
        if (isNullType(tmp) && Objects.nonNull(type)) {
          // Null type overrides undefined, but not other types
          continue;
        } else if (UNDEFINED_TOKENS.contains(tmp) && Objects.nonNull(type) && !isNullType(type)) {
          // Undefined type overrides null, but not any other type
          continue;
        }
        if (Objects.nonNull(tmp)) {
          type = tmp;
        }
      }
      typeText = type;
    }
    return typeText;
  }

  /**
   * Read all possible alleles in a result combination. These will be used to compute the most
   * probable haplotypes.
   */
  private static void addHaplotypes(ValidationModelBuilder builder, Element resultCombination,
      Map<Strand, HLALocus> locusMap,
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
        if (allele.isEmpty()) {
          continue;
        }

        if (allele.indexOf(LOCUS_SEPARATOR) > 0) {
          locus = allele.substring(0, allele.indexOf(LOCUS_SEPARATOR));
          allele = allele.substring(allele.indexOf(LOCUS_SEPARATOR) + 1, allele.length());
        }
        if (locus.isEmpty()) {
          locus = locusMap.get(Strand.values()[strandIndex - 1]).toString();
        }

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
    // If the lists are not equal size return the larger list
    if (reference.size() != test.size()) {
      return reference.size() < test.size();
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
  private static int compareSpec(List<HLAType> reference, List<HLAType> test) {
    if (!Objects.equals(reference.size(), test.size())) {
      String refString =
          reference.stream().map(HLAType::toString).collect(Collectors.joining(", "));
      String testString = test.stream().map(HLAType::toString).collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "Found allele combinations with different sizes: " + refString + " - " + testString);
    }

    List<Integer> refCounts = countSpec(reference);
    List<Integer> testCounts = countSpec(test);

    int diff = 0;

    // Compare the lists until we find different types
    for (int i = 0; i < refCounts.size() && i < testCounts.size() && diff == 0; i++) {
      diff = refCounts.get(i).compareTo(testCounts.get(i));
    }

    return diff;
  }

  /**
   * Helper method to combine the specificities of each allele in the given list. e.g. given alleles
   * 01:01:01, 02:02:02, the return list output would be 03, 03, 03.
   */
  private static List<Integer> countSpec(List<HLAType> test) {
    List<Integer> scoredSpecs = new ArrayList<>();

    for (HLAType allele : test) {
      List<Integer> spec = allele.spec();
      for (int i = 0; i < spec.size(); i++) {
        if (i < scoredSpecs.size()) {
          int score = scoredSpecs.get(i) + spec.get(i);
          scoredSpecs.set(i, score);
        } else {
          scoredSpecs.add(spec.get(i));
        }
      }
    }

    return scoredSpecs;
  }

  /** @return true iff the combination XML block contains a result combination of the given index */
  private static boolean hasCombination(Element resultCombinations, int combinationIndex,
      String alleleComboTag) {
    for (String combinationTag : ImmutableSet.of(alleleComboTag, SERO_COMBINATION_TAG)) {
      Elements results = resultCombinations.getElementsByTag(combinationTag + combinationIndex);
      if (results.isEmpty() || !results.get(0).hasText()) {
        return false;
      }
    }
    return true;
  }

  private static List<String> getAllValidTypes(Elements results, HLALocus locus) {

    List<String> types = new ArrayList<>();

    if (!results.isEmpty() && results.get(0).hasText()) {
      String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

      for (int result = 0; result < resultTypes.length; result++) {
        String tmp = resultTypes[result];

        // null string, or allele ends with 'N'
        if ((Strings.isNullOrEmpty(tmp) || isNullType(tmp)))
          continue;

        // undefined allele
        if (UNDEFINED_TOKENS.contains(tmp))
          continue;

        // This is an allele which is unlikely to be expressed, per
        // http://hla.alleles.org/nomenclature/naming.html
        if (tmp.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE))
          continue;

        // Sometimes the inheritance structure of the types is listed. We only want the first
        // (most specific) type, with the best CWD status
        if (tmp.contains(PARENT_TYPE_SEPARATOR)) {
          tmp = tmp.substring(0, tmp.indexOf(PARENT_TYPE_SEPARATOR));
        }

        if (Objects.nonNull(locus)) {
          if (!tmp.contains("*")) {
            tmp = locus.toString() + "*" + tmp;
          }
        }

        types.add(tmp);
      }

    }

    return types;
  }

  /**
   * @return {@code null} if there is no type at this position (homozygous or single DRB345);
   *         {@link #NULL_TYPE} if the allele is present but not expressed; {@link #UNDEFINED_TYPE}
   *         if the allele is expressed but has no serological equivalent.
   */
  private static String getFirstValidType(Elements results, HLALocus locus) {
    String typeText = null;
    if (!results.isEmpty() && results.get(0).hasText()) {
      String type = null;
      String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

      for (int result = 0; (Strings.isNullOrEmpty(type)) && result < resultTypes.length; result++) {
        String tmp = resultTypes[result];
        if (tmp.isEmpty()) {
          // Sometimes a leading comma can create empty strings - skip these
          continue;
        }

        int sep = -1;
        if ((sep = tmp.indexOf("-")) != -1) {
          tmp = tmp.substring(0, sep);
        }

        // // Null and Undefined types don't take precedence over any other values
        // if (isNullType(tmp) && Objects.nonNull(type)) {
        // // Null type overrides undefined, but not other types
        // continue;
        // } else if (UNDEFINED_TOKENS.contains(tmp) && Objects.nonNull(type) && !isNullType(type))
        // {
        // // Undefined type overrides null, but not any other type
        // continue;
        // } else if (tmp.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE)) {
        // // This is an allele which is unlikely to be expressed, per
        // // http://hla.alleles.org/nomenclature/naming.html
        // continue;
        // }
        if (Objects.nonNull(tmp)) {
          // Sometimes the inheritance structure of the types is listed. We only want the first
          // (most specific) type.
          if (tmp.contains(PARENT_TYPE_SEPARATOR)) {
            tmp = tmp.substring(0, tmp.indexOf(PARENT_TYPE_SEPARATOR));
          }
          if (Objects.nonNull(locus)) {
            if (!tmp.contains("*")) {
              tmp = locus.toString() + "*" + tmp;
            }
          }
        }
        type = tmp;
      }
      typeText = type;
    }
    return typeText;
  }

  /**
   * @return {@code null} if there is no type at this position (homozygous or single DRB345);
   *         {@link #NULL_TYPE} if the allele is present but not expressed; {@link #UNDEFINED_TYPE}
   *         if the allele is expressed but has no serological equivalent.
   */
  private static String getFirstValidCWDType(Elements results, HLALocus locus) {
    Status bestStatus = Objects.isNull(locus) ? Status.COMMON : null;
    String typeText = null;
    if (!results.isEmpty() && results.get(0).hasText()) {
      String type = null;
      String[] resultTypes = results.get(0).text().replaceAll("\\s+", "").split(RESULT_SEPARATOR);

      for (int result = 0; (Strings.isNullOrEmpty(type) || UNDEFINED_TYPE.equals(type)
          || isNullType(type) || !Objects.equals(Status.COMMON, bestStatus))
          && result < resultTypes.length; result++) {
        String tmp = resultTypes[result];
        if (tmp.isEmpty()) {
          // Sometimes a leading comma can create empty strings - skip these
          continue;
        }

        int sep = -1;
        if ((sep = tmp.indexOf("-")) != -1) {
          tmp = tmp.substring(0, sep);
        }

        // Null and Undefined types don't take precedence over any other values
        if (isNullType(tmp) && Objects.nonNull(type)) {
          // Null type overrides undefined, but not other types
          continue;
        } else if (UNDEFINED_TOKENS.contains(tmp) && Objects.nonNull(type) && !isNullType(type)) {
          // Undefined type overrides null, but not any other type
          continue;
        } else if (tmp.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE)) {
          // This is an allele which is unlikely to be expressed, per
          // http://hla.alleles.org/nomenclature/naming.html
          continue;
        }
        if (Objects.nonNull(tmp)) {
          // Sometimes the inheritance structure of the types is listed. We only want the first
          // (most specific) type, with the best CWD status
          if (tmp.contains(PARENT_TYPE_SEPARATOR)) {
            tmp = tmp.substring(0, tmp.indexOf(PARENT_TYPE_SEPARATOR));
          }
          if (Objects.nonNull(locus)) {
            if (!tmp.contains("*")) {
              tmp = locus.toString() + "*" + tmp;
            }
            // Make sure that this allele has a better CWD status than the last
            Status currentStatus = CommonWellDocumented.getStatus(HLAType.valueOf(tmp));
            if (Objects.nonNull(bestStatus)
                && bestStatus.getWeight() >= currentStatus.getWeight()) {
              continue;
            }
            bestStatus = currentStatus;
          }
        }
        type = tmp;
      }
      typeText = type;
    }
    return typeText;
  }

  /** @return True if the given allele string indicates a type that is not expressed */
  private static boolean isNullType(String allele) {
    return NULL_TYPE.equals(allele) || allele.matches(ValidationModelBuilder.NOT_EXPRESSED);
  }

  /** @return The specificity of alleles with special lookup rules */
  private static String getAlleleLookup(ResultCombination combination) {
    SeroType st = SerotypeEquivalence.get(combination.getAlleleCombination());
    return getAlleleLookup(st, combination.alleleCombination);
  }

  /** @return The specificity of alleles with special lookup rules */
  private static String getAlleleLookup(SeroType st, HLAType allele) {
    if (st != null) {
      return st.specString();
    }

    // TODO why not return combination.antigenCombination?
    // Not a special case allele - use standard rules
    return allele.equivSafe().specString();
  }

  /** @return The {@link HLAType} specificity of an allele + antigen pairing */
  private static String getAlleleSpec(ResultCombination combination) {
    // Standard operating procedure is to just report the first field of the allele
    HLAType hlaType = combination.getAlleleCombination();

    return getAlleleSpec(hlaType);
  }

  /** @return The {@link HLAType} specificity of an allele + antigen pairing */
  private static String getAlleleSpec(HLAType hlaType) {
    // Standard operating procedure is to just report the first field of the allele

    if (hlaType.locus() == HLALocus.DPA1 || hlaType.locus() == HLALocus.DPB1) {
      StringJoiner sj = new StringJoiner(":");
      sj.add(String.valueOf(hlaType.spec().get(0)));

      if (Objects.equals(HLALocus.DPA1, hlaType.locus())
          || Objects.equals(HLALocus.DPB1, hlaType.locus())) {
        // DPA and DPB report two fields
        HLAType t = hlaType;
        if (hlaType.resolution() == 1) {
          t = HLAType.growSpec(t);
        }
        sj.add(String.valueOf(t.spec().get(1)));
      }

      return sj.toString();
    }

    return hlaType.equivSafe().specString();
  }

  /**
   * Helper class to store paired allele + antigen pairs. Each result combination maps to one of
   * these pairs, and we may need both for determining which combinations to pick (e.g. we select
   * the most likely combination based on allele frequency, but report the serotype)
   */
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
        // TODO FIXME do something other than print to system out
        // System.out.println();
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
