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
package org.pankratzlab.unet.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.deprecated.hla.SeroLocus;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.AlleleGroups;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.parser.util.BwSerotypes;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import org.pankratzlab.unet.parser.util.DRAssociations;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;

/**
 * Mutable builder class for creating a {@link ValidationModel}.
 *
 * <p>
 * Use the locus-specific setters to build up the donor typing. Each must be called at least once
 * (for homozygous) and at most twice (for heterozygous). Each requires on the specificty string for
 * the given locus (e.g. "24" for A24).
 */
public class ValidationModelBuilder {

  private static final Map<RaceGroup, EthnicityHaplotypeComp> comparators = new EnumMap<>(RaceGroup.class);
  private static final Table<Haplotype, RaceGroup, BigDecimal> frequencyTable = HashBasedTable.create();
  private static final String NEGATIVE_ALLELE = "N-Negative";

  {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      comparators.put(ethnicity, new EthnicityHaplotypeComp(ethnicity));
    }
  }

  private String donorId;
  private String source;
  private String sourceType;
  private Set<SeroType> aLocus;
  private Set<SeroType> bLocus;
  private Set<SeroType> cLocus;
  private Set<SeroType> drbLocus;
  private Set<SeroType> dqbLocus;
  private Set<SeroType> dqaLocus;
  private Set<SeroType> dpaLocus;
  private Set<HLAType> dpbLocus;
  private Boolean bw4;
  private Boolean bw6;
  private List<HLAType> dr51Locus = new ArrayList<>();
  private List<HLAType> dr52Locus = new ArrayList<>();
  private List<HLAType> dr53Locus = new ArrayList<>();
  private Multimap<Strand, HLAType> bHaplotypes = HashMultimap.create();
  private Multimap<Strand, HLAType> cHaplotypes = HashMultimap.create();
  private Multimap<Strand, HLAType> drb1Haplotypes = HashMultimap.create();
  private Multimap<Strand, HLAType> dqb1Haplotypes = HashMultimap.create();
  private Multimap<Strand, HLAType> dpb1Haplotypes = HashMultimap.create();
  private Multimap<Strand, HLAType> dr345Haplotypes = HashMultimap.create();

  /** @param donorId Unique identifying string for this donor */
  public ValidationModelBuilder donorId(String donorId) {
    this.donorId = donorId;
    return this;
  }

  /** @param source Name for the source of this model */
  public ValidationModelBuilder source(String source) {
    this.source = source;
    return this;
  }

  /** @param source Name for the source of this model */
  public ValidationModelBuilder sourceType(String sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  public ValidationModelBuilder a(String aType) {
    if (!aType.matches(".*\\d.*") || aType.equals("98")) {
      return null;
    }
    aLocus = makeIfNull(aLocus);
    addToLocus(aLocus, SeroLocus.A, aType);
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    if (!bType.matches(".*\\d.*") || bType.equals("98")) {
      return null;
    }
    bLocus = makeIfNull(bLocus);
    addToLocus(bLocus, SeroLocus.B, bType);
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    if (!cType.matches(".*\\d.*") || cType.equals("98")) {
      return null;
    }
    cLocus = makeIfNull(cLocus);
    addToLocus(cLocus, SeroLocus.C, cType);
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    drbLocus = makeIfNull(drbLocus);
    if (drbType == null || !drbType.matches(".*\\d.*")) {
      return null;
    }
    if (!Strings.isNullOrEmpty(drbType)
        && Objects.equals(103, Integer.parseInt(drbType.replaceAll(":", "").trim()))) {
      // UNOS explicitly requires DRB1*01:03 to be reported as DRB0103
      drbLocus.add(new SeroType(SeroLocus.DRB, "0103"));
    } else {
      addToLocus(drbLocus, SeroLocus.DRB, drbType);
    }
    return this;
  }

  public ValidationModelBuilder dr51(String dr51) {
    if (isPositive(dr51)) {
      dr51Locus.add(new HLAType(HLALocus.DRB5, dr51));
    }
    return this;
  }

  public ValidationModelBuilder dr52(String dr52) {
    if (isPositive(dr52)) {
      dr52Locus.add(new HLAType(HLALocus.DRB3, dr52));
    }
    return this;
  }

  public ValidationModelBuilder dr53(String dr53) {
    if (isPositive(dr53)) {
      dr53Locus.add(new HLAType(HLALocus.DRB4, dr53));
    }
    return this;
  }

  public ValidationModelBuilder dqb(String dqbType) {
    if (dqbType == null || !dqbType.matches(".*\\d.*")) {
      return null;
    }
    dqbLocus = makeIfNull(dqbLocus);
    addToLocus(dqbLocus, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqa(String dqaType) {
    if (dqaType == null || !dqaType.matches(".*\\d.*")) {
      return null;
    }
    dqaLocus = makeIfNull(dqaLocus);
    addToLocus(dqaLocus, SeroLocus.DQA, dqaType);
    return this;
  }

  public ValidationModelBuilder dpa(String dpaType) {
    if (dpaType == null || !dpaType.matches(".*\\d.*")) {
      return null;
    }
    dpaLocus = makeIfNull(dpaLocus);
    addToLocus(dpaLocus, SeroLocus.DPA, dpaType);
    return this;
  }

  public ValidationModelBuilder dpb(String dpbType) {
    dpbLocus = makeIfNull(dpbLocus);
    // Shorten the allele designation to allele group and specific HLA protein. Further fields can
    // not be entered into UNOS
    if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
      HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
      if (tmpDPB1.spec().size() > 2) {
        tmpDPB1 = new HLAType(HLALocus.DPB1,
                              new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
      }
      dpbLocus.add(tmpDPB1);
    }
    return this;
  }

  public ValidationModelBuilder bw4(boolean bw4) {
    this.bw4 = bw4;
    return this;
  }

  public ValidationModelBuilder bw6(boolean bw6) {
    this.bw6 = bw6;
    return this;
  }

  public ValidationModelBuilder bHaplotype(Multimap<Strand, HLAType> types) {
    bHaplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder cHaplotype(Multimap<Strand, HLAType> types) {
    cHaplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder drHaplotype(Multimap<Strand, HLAType> types) {
    drb1Haplotypes.putAll(types);

    for (Strand strand : types.keySet()) {
      SeroType drbType = types.get(strand).iterator().next().lowResEquiv();
      if (!DRAssociations.getDRBLocus(drbType).isPresent()) {
        dr345Haplotypes.put(strand, NullType.UNREPORTED_DRB345);
      }
    }
    return this;
  }

  public ValidationModelBuilder dqHaplotype(Multimap<Strand, HLAType> types) {
    dqb1Haplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder dpHaplotype(Multimap<Strand, HLAType> types) {
    dpb1Haplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder dr345Haplotype(Multimap<Strand, HLAType> types) {
    for (Strand originalKey : types.keySet()) {
      Strand newKey = originalKey;
      if (dr345Haplotypes.containsKey(newKey)) {
        newKey = newKey.flip();
      }
      dr345Haplotypes.putAll(newKey, types.get(originalKey));
    }
    return this;
  }

  public ImmutableMultimap<Strand, HLAType> getDpbHaplotypes() {
    // FIXME this is a horrible hack that doesn't belong here but is a consequence of tying the
    // parsers to the validation model. We just want a way to extract DPB1 haplotypes for the MACUI
    // controller.
    return ImmutableMultimap.copyOf(dpb1Haplotypes);
  }

  /** @return The immutable {@link ValidationModel} based on the current builder state. */
  public ValidationModel build() {
    // correctDRHomozygosity();
    ensureValidity();

    Multimap<RaceGroup, Haplotype> bcCwdHaplotypes = buildBCHaplotypes(bHaplotypes, cHaplotypes);

    Multimap<RaceGroup, Haplotype> drDqDR345Haplotypes = buildHaplotypes(ImmutableList.of(drb1Haplotypes,
                                                                                          dqb1Haplotypes,
                                                                                          dr345Haplotypes));

    frequencyTable.clear();

    ValidationModel validationModel = new ValidationModel(donorId, source, sourceType, aLocus,
                                                          bLocus, cLocus, drbLocus, dqbLocus,
                                                          dqaLocus, dpaLocus, dpbLocus, bw4, bw6,
                                                          dr51Locus, dr52Locus, dr53Locus,
                                                          bcCwdHaplotypes, drDqDR345Haplotypes);
    return validationModel;
  }

  // /** If the DR assignment is homozygous, ensure the DR51/52/53 assignment is homozygous as well
  // */
  // private void correctDRHomozygosity() {
  // if (drbLocus.size() == 1) {
  // for (List<HLAType> dr : ImmutableList.of(dr51Locus, dr52Locus, dr53Locus)) {
  // if (dr.size() == 1) {
  // dr.add(dr.get(0));
  // }
  // }
  // }
  // }

  /**
   * Helper method to build the B/C haplotypes. Extra filtering is needed based on the Bw groups.
   */
  private Multimap<RaceGroup, Haplotype> buildBCHaplotypes(Multimap<Strand, HLAType> bHaps,
                                                           Multimap<Strand, HLAType> cHaps) {
    if (bw4 && bw6) {
      // One strand is Bw4 and one is Bw6, but we can't know for sure which. So we try both
      Multimap<Strand, HLAType> s4s6 = enforceBws(BwGroup.Bw4, BwGroup.Bw6, bHaps);
      Multimap<Strand, HLAType> s6s4 = enforceBws(BwGroup.Bw6, BwGroup.Bw4, bHaps);
      Multimap<RaceGroup, Haplotype> s4s6Haplotypes = s4s6.isEmpty() ? ImmutableMultimap.of()
                                                                     : buildHaplotypes(ImmutableList.of(s4s6,
                                                                                                        cHaplotypes));
      Multimap<RaceGroup, Haplotype> s6s4Haplotypes = s6s4.isEmpty() ? ImmutableMultimap.of()
                                                                     : buildHaplotypes(ImmutableList.of(s6s4,
                                                                                                        cHaplotypes));

      // Merge the bw4/bw6 sets into a combined Scoring set
      List<ScoredHaplotypes> scoredHaplotypePairs = new ArrayList<>();
      for (RaceGroup raceGroup : RaceGroup.values()) {
        if (s6s4Haplotypes.containsKey(raceGroup)) {
          scoredHaplotypePairs.add(new ScoredHaplotypes(s6s4Haplotypes.get(raceGroup)));
        }

        if (s4s6Haplotypes.containsKey(raceGroup)) {
          scoredHaplotypePairs.add(new ScoredHaplotypes(s4s6Haplotypes.get(raceGroup)));
        }
      }

      // For each ethnicity pick best haplotype pairs from these sets
      Multimap<RaceGroup, Haplotype> haplotypesByEthnicity = HashMultimap.create();

      if (!scoredHaplotypePairs.isEmpty()) {
        for (RaceGroup ethnicity : RaceGroup.values()) {

          // Sort the haplotype pairs to find the most likely pairing for this ethnicity
          ScoredHaplotypes max = Collections.max(scoredHaplotypePairs,
                                                 new EthnicityHaplotypeComp(ethnicity));

          // Record each haplotype in the pair
          for (Haplotype t : max) {
            haplotypesByEthnicity.put(ethnicity, t);
          }
        }
      }
      return haplotypesByEthnicity;
    } else if (bw4) {
      // Both strands bw4
      Multimap<Strand, HLAType> s4s4 = enforceBws(BwGroup.Bw4, BwGroup.Bw4, bHaps);
      return buildHaplotypes(ImmutableList.of(s4s4, cHaplotypes));
    } else if (bw6) {
      // Both strands bw6
      Multimap<Strand, HLAType> s6s6 = enforceBws(BwGroup.Bw6, BwGroup.Bw6, bHaps);
      return buildHaplotypes(ImmutableList.of(s6s6, cHaplotypes));
    }

    return ArrayListMultimap.create();
  }

  /**
   * Helper method to enforce a particular Bw strand alignment for any B alleles in the given
   * multimap
   */
  private Multimap<Strand, HLAType> enforceBws(BwGroup strandOneGroup, BwGroup strandTwoGroup,
                                               Multimap<Strand, HLAType> haplotypes) {
    ListMultimap<Strand, HLAType> enforced = ArrayListMultimap.create();
    for (HLAType t : haplotypes.get(Strand.FIRST)) {
      if (!HLALocus.B.equals(t.locus()) || strandOneGroup.equals(BwSerotypes.getBwGroup(t))
          || BwGroup.Unknown.equals(BwSerotypes.getBwGroup(t))) {
        enforced.put(Strand.FIRST, t);
      }
    }
    for (HLAType t : haplotypes.get(Strand.SECOND)) {
      if (!HLALocus.B.equals(t.locus()) || strandTwoGroup.equals(BwSerotypes.getBwGroup(t))
          || BwGroup.Unknown.equals(BwSerotypes.getBwGroup(t))) {
        enforced.put(Strand.SECOND, t);
      }
    }
    return enforced;
  }

  private boolean isPositive(String dr) {
    return !Objects.equals(dr, NEGATIVE_ALLELE);
  }

  /** @return A table of the highest-probability haplotypes for each ethnicity */
  private Multimap<RaceGroup, Haplotype> buildHaplotypes(List<Multimap<Strand, HLAType>> typesByLocus) {
    Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity = new HashMap<>();
    Multimap<RaceGroup, Haplotype> haplotypesByEthnicity = MultimapBuilder.enumKeys(RaceGroup.class)
                                                                          .arrayListValues()
                                                                          .build();

    List<Multimap<Strand, HLAType>> presentTypesByLocus = typesByLocus.stream()
                                                                      .filter(m -> !m.isEmpty())
                                                                      .collect(Collectors.toList());
    presentTypesByLocus.forEach(this::pruneUnknown);
    presentTypesByLocus.forEach(this::condenseGroups);

    if (!presentTypesByLocus.isEmpty()) {
      // Recursively generate the set of all possible haplotype pairs
      generateHaplotypePairs(maxScorePairsByEthnicity, presentTypesByLocus);
      for (Entry<RaceGroup, ScoredHaplotypes> entry : maxScorePairsByEthnicity.entrySet()) {
        for (Haplotype h : entry.getValue()) {
          haplotypesByEthnicity.put(entry.getKey(), h);
        }
      }
    }

    return haplotypesByEthnicity;
  }

  /**
   * Recursive entry point for generating all possible {@link Haplotype} pairs.
   *
   * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
   * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles for
   *          that strand.
   */
  private void generateHaplotypePairs(Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
                                      List<Multimap<Strand, HLAType>> typesByLocus) {
    // Overview:
    // 1. Recurse through strand 1 sets - for each locus, record the possible complementary options
    // 2. At the terminal strand 1 step, start recursing through the possible strand 2 options
    // 3. At the terminal strand 2 step, create a ScoredHaplotype for the pair

    generateStrandOneHaplotypes(maxScorePairsByEthnicity, typesByLocus, new ArrayList<>(),
                                new ArrayList<>(), 0);
  }

  /**
   * Recursively generate all possible haplotypes for the "first" strand.
   *
   * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
   * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles for
   *          that strand.
   * @param currentHaplotypeAlleles Current alleles of the first haplotype
   * @param strandTwoOptionsByLocus List of allele options, by locus, to populate for complementary
   *          haplotype generation.
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandOneHaplotypes(Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
                                           List<Multimap<Strand, HLAType>> typesByLocus,
                                           List<HLAType> currentHaplotypeAlleles,
                                           List<List<HLAType>> strandTwoOptionsByLocus,
                                           int locusIndex) {
    if (locusIndex == typesByLocus.size()) {
      // Terminal step - we now have one haplotype; recursively generate the second
      Haplotype firstHaplotype = new Haplotype(currentHaplotypeAlleles);
      generateStrandTwoHaplotypes(maxScorePairsByEthnicity, firstHaplotype, strandTwoOptionsByLocus,
                                  new ArrayList<>(), 0);
    } else {
      // Recursive step -
      Multimap<Strand, HLAType> currentLocus = typesByLocus.get(locusIndex);

      // The strand notations are arbitrary. So at each locus we need to consider each possible
      // alignment of the alleles - including whether they are heterozygous or homozygous.
      // However at the first locus we do not need to consider Strand1 + Strand2 AND
      // Strand2 + Strand1, as the resulting haplotype pairs would mirror each other.
      Set<Strand> firstStrandsSet = locusIndex == 0 ? ImmutableSet.of(Strand.FIRST)
                                                    : currentLocus.keySet();

      // Build the combinations of strand1 + strand2 alleles at this locus
      for (Strand strandOne : firstStrandsSet) {
        Collection<HLAType> firstStrandTypes = currentLocus.get(strandOne);
        List<HLAType> secondStrandTypes = ImmutableList.copyOf(currentLocus.get(strandOne.flip()));
        if (secondStrandTypes.isEmpty()) {
          secondStrandTypes = ImmutableList.copyOf(firstStrandTypes);
        }

        // set up the second strand options to iterate over after the first haplotype is built
        setOrAdd(strandTwoOptionsByLocus, secondStrandTypes, locusIndex);

        // Try each possible strand one allele
        for (HLAType currentType : firstStrandTypes) {
          setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);

          // Recurse to the next locus
          generateStrandOneHaplotypes(maxScorePairsByEthnicity, typesByLocus,
                                      currentHaplotypeAlleles, strandTwoOptionsByLocus,
                                      locusIndex + 1);
        }
      }
    }
  }

  /**
   * Recursively generate all possible haplotypes for the "second" strand.
   *
   * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
   * @param firstHaplotype The fixed, complementary haplotype
   * @param strandTwoOptionsByLocus List of allele options, by locus, to use when generating
   *          haplotypes
   * @param currentHaplotypeAlleles Current alleles of the second haplotype
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandTwoHaplotypes(Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
                                           Haplotype firstHaplotype,
                                           List<List<HLAType>> strandTwoOptionsByLocus,
                                           List<HLAType> currentHaplotypeAlleles, int locusIndex) {
    if (locusIndex == strandTwoOptionsByLocus.size()) {
      // Terminal step - we now have two haplotypes so we score them and compare
      ScoredHaplotypes scored = new ScoredHaplotypes(ImmutableList.of(firstHaplotype,
                                                                      new Haplotype(currentHaplotypeAlleles)));

      for (RaceGroup ethnicity : RaceGroup.values()) {
        if (!maxScorePairsByEthnicity.containsKey(ethnicity)
            || comparators.get(ethnicity).compare(maxScorePairsByEthnicity.get(ethnicity),
                                                  scored) < 0) {
          maxScorePairsByEthnicity.put(ethnicity, scored);
        }
      }
    } else {
      // Recursive step - iterate through the possible alleles for this locus, building up the
      // current haplotype
      for (HLAType currentType : strandTwoOptionsByLocus.get(locusIndex)) {
        setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);
        generateStrandTwoHaplotypes(maxScorePairsByEthnicity, firstHaplotype,
                                    strandTwoOptionsByLocus, currentHaplotypeAlleles,
                                    locusIndex + 1);
      }
    }
  }

  /**
   * Stupid helper method to set an item at a specific index, or add the item if the index == the
   * list's size (indicating an append action)
   */
  private static <T> void setOrAdd(List<T> list, T toAdd, int index) {
    if (index == list.size()) {
      list.add(toAdd);
    } else {
      list.set(index, toAdd);
    }
  }

  /** Replace all HLA types with their groups (condensing equivalent alleles) */
  private void condenseGroups(Multimap<Strand, HLAType> typesForStrand) {
    for (Strand strand : typesForStrand.keySet()) {
      Set<HLAType> condensed = new HashSet<>();
      Collection<HLAType> uncondensed = typesForStrand.get(strand);

      for (HLAType hlaType : uncondensed) {
        condensed.add(AlleleGroups.getGGroup(hlaType));
      }

      // NB: making the values empty will break this loop by removing the key from the multimap :D
      typesForStrand.replaceValues(strand, condensed);
    }

    // Homozygous
    if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
      typesForStrand.removeAll(Strand.SECOND);
    }
  }

  /** Filter out {@link Status#UNKNOWN} types and eliminate redundant strands */
  private void pruneUnknown(Multimap<Strand, HLAType> typesForStrand) {
    // Homozygous
    if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
      typesForStrand.removeAll(Strand.SECOND);
    }

    // Sort out our types by CWD status
    for (Strand strand : typesForStrand.keySet()) {
      Multimap<Status, HLAType> typesByStatus = MultimapBuilder.enumKeys(Status.class)
                                                               .hashSetValues().build();
      Collection<HLAType> values = typesForStrand.get(strand);
      values.forEach(t -> typesByStatus.put(CommonWellDocumented.getEquivStatus(t), t));

      Set<HLAType> cwdTypes = new HashSet<>();
      cwdTypes.addAll(typesByStatus.get(Status.COMMON));
      cwdTypes.addAll(typesByStatus.get(Status.WELL_DOCUMENTED));

      // If we have any common or well-documented types, drop all unknown
      if (!cwdTypes.isEmpty()) {
        // NB: making the values empty will break this loop by removing the key from the multimap :D
        typesForStrand.replaceValues(strand, cwdTypes);
      }
    }
  }

  /**
   * @throws IllegalStateException If the model has not been fully populated, or populated
   *           incorrectly.
   */
  private void ensureValidity() throws IllegalStateException {
    // Ensure all fields have been set
    for (Object o : Lists.newArrayList(donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus,
                                       dqaLocus, dpbLocus, bw4, bw6)) {
      if (Objects.isNull(o)) {
        throw new IllegalStateException("ValidationModel incomplete");
      }
    }
    // Ensure all sets have a reasonable number of entries
    for (Set<?> set : ImmutableList.of(aLocus, bLocus, cLocus, drbLocus, dqbLocus, dqaLocus,
                                       dpbLocus)) {
      if (set.isEmpty() || set.size() > 2) {
        throw new IllegalStateException("ValidationModel contains invalid allele count: " + set);
      }
    }
    // Note: haplotype maps are OPTIONAL

    // DPA was more recently added and in an effort not to error out every time DPA is missing from
    // old files we need to throw a warning
    if (Objects.isNull(dpaLocus)) {
      JOptionPane.showMessageDialog(new JFrame(), "DPA is missing from this file", "Dialog",
                                    JOptionPane.WARNING_MESSAGE);
    }

    // Note: Some DRB345 loci may be empty, but should be sorted
    Collections.sort(dr51Locus);
    Collections.sort(dr52Locus);
    Collections.sort(dr53Locus);
  }

  /** Helper method to build a set if it's null */
  private <T> Set<T> makeIfNull(Set<T> locusSet) {
    if (Objects.isNull(locusSet)) {
      locusSet = new LinkedHashSet<>();
    }
    return locusSet;
  }

  /**
   * Helper method to create {@link SeroType}s and add them to a locus's set in a consistent manner
   */
  private void addToLocus(Set<SeroType> locusSet, SeroLocus locus, String typeString) {
    if (typeString.length() > 2) {
      typeString = typeString.substring(0, typeString.length() - 2);
    }
    locusSet.add(new SeroType(locus, typeString));
  }

  /** Helper wrapper class to cache the scores for haplotypes */
  private static class ScoredHaplotypes extends ArrayList<Haplotype> {
    private static final long serialVersionUID = 3780864438450985328L;
    private static final int NO_MISSING_WEIGHT = 10;
    private final Map<RaceGroup, Double> scoresByEthnicity = new HashMap<>();

    private ScoredHaplotypes(Collection<Haplotype> initialHaplotypes) {
      super();
      BigDecimal cwdScore = BigDecimal.ZERO;

      for (Haplotype haplotype : initialHaplotypes) {
        add(haplotype);

        for (HLAType allele : haplotype.getTypes()) {
          switch (CommonWellDocumented.getEquivStatus(allele)) {
            case COMMON:
              // Add 1 points for common alleles
              cwdScore = cwdScore.add(BigDecimal.ONE);
              break;
            case WELL_DOCUMENTED:
              // Add 1/2 point for well-documented alleles
              cwdScore = cwdScore.add(new BigDecimal(0.5));
              break;
            case UNKNOWN:
            default:
              break;
          }
        }
      }

      for (RaceGroup e : RaceGroup.values()) {
        int noMissingCount = 0;
        BigDecimal frequency = new BigDecimal(1.0);
        for (Haplotype haplotype : this) {
          // Add this haplotype to the table
          BigDecimal f = frequencyTable.get(haplotype, e);
          if (Objects.isNull(f)) {
            // Cache the frequency if unseen haplotype
            f = HaplotypeFrequencies.getFrequency(e, haplotype);
            frequencyTable.put(haplotype, e, f);
          }
          if (f.compareTo(BigDecimal.ZERO) > 0) {
            frequency = frequency.multiply(f);
            noMissingCount++;
          }
        }

        BigDecimal weights = cwdScore.add(BigDecimal.valueOf(NO_MISSING_WEIGHT * noMissingCount));

        double s = weights.add(frequency).doubleValue();

        // for homozygous haplotypes we count the score twice
        scoresByEthnicity.put(e, s);
      }
    }

    @Override
    public String toString() {
      return super.toString() + " - " + scoresByEthnicity.toString();
    }

    /**
     * @return A weighted score for this ethnicity, prioritizing haplotypes without missing
     *         frequencies.
     */
    public double getScore(RaceGroup ethnicity) {
      return scoresByEthnicity.get(ethnicity);
    }

    public int compareTo(ScoredHaplotypes o, RaceGroup e) {
      // Prefer larger frequencies for this ethnicity
      int c = Double.compare(getScore(e), o.getScore(e));
      Iterator<Haplotype> myIterator = iterator();
      Iterator<Haplotype> otherIterator = o.iterator();
      // Fall back to the haplotypes themselves
      while (myIterator.hasNext() && otherIterator.hasNext() && c == 0) {
        c = myIterator.next().compareTo(otherIterator.next());
      }
      return c;
    }
  }

  /**
   * {@link Comparator} to sort collections of {@link Haplotype}s based on their frequency and the
   * CWD status of their alleles.
   */
  private static class EthnicityHaplotypeComp implements Comparator<ScoredHaplotypes> {
    private RaceGroup ethnicity;

    private EthnicityHaplotypeComp(RaceGroup e) {
      this.ethnicity = e;
    }

    @Override
    public int compare(ScoredHaplotypes o1, ScoredHaplotypes o2) {

      int result = o1.compareTo(o2, ethnicity);

      if (result == 0) {
        // If the scores are the same, we compare the unique HLATypes between these two
        List<HLAType> t1 = makeList(o1);
        List<HLAType> t2 = makeList(o2);
        removeOverlapAndSort(t1, t2);
        for (int i = 0; i < t1.size() && i < t2.size(); i++) {
          result += t2.get(i).compareTo(t1.get(i));
        }
      }
      return result;
    }

    /**
     * Modify the two input sets to remove any overlap between them, and sort them both before
     * returning.
     */
    private void removeOverlapAndSort(List<HLAType> t1, List<HLAType> t2) {
      Set<HLAType> overlap = new HashSet<>(t1);
      overlap.retainAll(t2);
      t1.removeAll(overlap);
      t2.removeAll(overlap);
      Collections.sort(t1);
      Collections.sort(t2);
    }

    /**
     * Helper method to convert a {@link Haplotype} collection to a sorted list of the alleles
     * contained in that haplotype.
     */
    private List<HLAType> makeList(Collection<Haplotype> haplotypes) {
      List<HLAType> sorted = new ArrayList<>();
      haplotypes.forEach(haplotype -> {
        haplotype.getTypes().forEach(allele -> {
          sorted.add(allele);
        });
      });
      Collections.sort(sorted);
      return sorted;
    }
  }
}
