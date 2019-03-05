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
package org.pankratzlab.unet.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;

/**
 * Mutable builder class for creating a {@link ValidationModel}.
 * <p>
 * Use the locus-specific setters to build up the donor typing. Each must be called at least once
 * (for homozygous) and at most twice (for heterozygous). Each requires on the specificty string for
 * the given locus (e.g. "24" for A24).
 * </p>
 */
public class ValidationModelBuilder {

  private static final Table<Haplotype, RaceGroup, Double> frequencyTable = HashBasedTable.create();
  private static final String NEGATIVE_ALLELE = "N-Negative";
  private String donorId;
  private String source;
  private Set<SeroType> aLocus;
  private Set<SeroType> bLocus;
  private Set<SeroType> cLocus;
  private Set<SeroType> drbLocus;
  private Set<SeroType> dqbLocus;
  private Set<SeroType> dqaLocus;
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
  private Multimap<Strand, HLAType> dr345Haplotypes = HashMultimap.create();

  /**
   * @param donorId Unique identifying string for this donor
   */
  public ValidationModelBuilder donorId(String donorId) {
    this.donorId = donorId;
    return this;
  }

  /**
   * @param source Name for the source of this model
   */
  public ValidationModelBuilder source(String source) {
    this.source = source;
    return this;
  }

  public ValidationModelBuilder a(String aType) {
    aLocus = makeIfNull(aLocus);
    addToLocus(aLocus, SeroLocus.A, aType);
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    bLocus = makeIfNull(bLocus);
    addToLocus(bLocus, SeroLocus.B, bType);
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    cLocus = makeIfNull(cLocus);
    addToLocus(cLocus, SeroLocus.C, cType);
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    drbLocus = makeIfNull(drbLocus);
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
    dqbLocus = makeIfNull(dqbLocus);
    addToLocus(dqbLocus, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqa(String dqaType) {
    dqaLocus = makeIfNull(dqaLocus);
    addToLocus(dqaLocus, SeroLocus.DQB, dqaType);
    return this;
  }

  public ValidationModelBuilder dpb(String dpbType) {
    dpbLocus = makeIfNull(dpbLocus);
    dpbLocus.add(new HLAType(HLALocus.DPB1, dpbType));
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
      if (Objects.isNull(DRAssociations.getDRBLocus(drbType))) {
        dr345Haplotypes.put(strand, NullType.UNREPORTED_DRB345);
      }
    }
    return this;
  }

  public ValidationModelBuilder dqHaplotype(Multimap<Strand, HLAType> types) {
    dqb1Haplotypes.putAll(types);
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

  /**
   * @return The immutable {@link ValidationModel} based on the current builder state.
   */
  public ValidationModel build() {
    ensureValidity();

    Multimap<RaceGroup, Haplotype> bcCwdHaplotypes = buildBCHaplotypes(bHaplotypes, cHaplotypes);

    Multimap<RaceGroup, Haplotype> drDqDR345Haplotypes =
        buildHaplotypes(ImmutableList.of(drb1Haplotypes, dqb1Haplotypes, dr345Haplotypes));

    frequencyTable.clear();

    ValidationModel validationModel = new ValidationModel(donorId, source, aLocus, bLocus, cLocus,
        drbLocus, dqbLocus, dqaLocus, dpbLocus, bw4, bw6, dr51Locus, dr52Locus, dr53Locus,
        bcCwdHaplotypes, drDqDR345Haplotypes);
    return validationModel;
  }

  /**
   * Helper method to build the B/C haplotypes. Extra filtering is needed based on the Bw groups.
   */
  private Multimap<RaceGroup, Haplotype> buildBCHaplotypes(Multimap<Strand, HLAType> bHaps,
      Multimap<Strand, HLAType> cHaps) {
    if (bw4 && bw6) {
      // One strand is Bw4 and one is Bw6, but we can't know for sure which. So we try both
      Multimap<Strand, HLAType> s4s6 = enforceBws(BwGroup.Bw4, BwGroup.Bw6, bHaps);
      Multimap<Strand, HLAType> s6s4 = enforceBws(BwGroup.Bw4, BwGroup.Bw6, bHaps);
      Multimap<RaceGroup, Haplotype> s4s6Haplotypes =
          buildHaplotypes(ImmutableList.of(s4s6, cHaplotypes));
      Multimap<RaceGroup, Haplotype> s6s4Haplotypes =
          buildHaplotypes(ImmutableList.of(s6s4, cHaplotypes));

      // Merge the bw4/bw6 sets
      List<ScoredHaplotypes> scoredHaplotypePairs = new ArrayList<>();
      for (RaceGroup raceGroup : RaceGroup.values()) {
        scoredHaplotypePairs.add(new ScoredHaplotypes(s6s4Haplotypes.get(raceGroup)));
        scoredHaplotypePairs.add(new ScoredHaplotypes(s4s6Haplotypes.get(raceGroup)));
      }

      // For each ethnicity pick best haplotype pairs from these sets
      Multimap<RaceGroup, Haplotype> haplotypesByEthnicity = HashMultimap.create();

      for (RaceGroup raceGroup : RaceGroup.values()) {
        scoredHaplotypePairs.add(new ScoredHaplotypes(s6s4Haplotypes.get(raceGroup)));
        scoredHaplotypePairs.add(new ScoredHaplotypes(s4s6Haplotypes.get(raceGroup)));
      }
      for (RaceGroup ethnicity : RaceGroup.values()) {

        // Sort the haplotype pairs to find the most likely pairing for this ethnicity
        ScoredHaplotypes max =
            Collections.max(scoredHaplotypePairs, new EthnicityHaplotypeComp(ethnicity));

        for (Haplotype t : max) {
          haplotypesByEthnicity.put(ethnicity, t);
        }
      }
      return haplotypesByEthnicity;
    } else if (bw4)

    {
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
      if (!HLALocus.B.equals(t.locus()) || strandOneGroup.equals(BwSerotypes.getBwGroup(t))) {
        enforced.put(Strand.FIRST, t);
      }
    }
    for (HLAType t : haplotypes.get(Strand.SECOND)) {
      if (!HLALocus.B.equals(t.locus()) || strandTwoGroup.equals(BwSerotypes.getBwGroup(t))) {
        enforced.put(Strand.SECOND, t);
      }
    }
    return enforced;
  }

  private boolean isPositive(String dr) {
    return !Objects.equals(dr, NEGATIVE_ALLELE);
  }

  /**
   * @return A table of the highest-probability haplotypes for each ethnicity
   */
  private Multimap<RaceGroup, Haplotype> buildHaplotypes(
      List<Multimap<Strand, HLAType>> typesByLocus) {
    Multimap<RaceGroup, Haplotype> haplotypesByEthnicity = HashMultimap.create();
    Set<HaplotypeSet> possibleHaplotypePairs = new HashSet<>();

    List<Multimap<Strand, HLAType>> presentTypesByLocus =
        typesByLocus.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
    presentTypesByLocus.forEach(this::pruneUnknown);
    presentTypesByLocus.forEach(this::condenseGroups);

    if (!presentTypesByLocus.isEmpty()) {
      // 1. Recursively generate the set of all possible haplotype pairs
      generateHaplotypePairs(possibleHaplotypePairs, presentTypesByLocus);

      List<ScoredHaplotypes> haplotypePairs =
          possibleHaplotypePairs.stream().map(ScoredHaplotypes::new).collect(Collectors.toList());

      // 2. Then sort once for each ethnicity
      for (RaceGroup ethnicity : RaceGroup.values()) {

        // Sort the haplotype pairs to find the most likely pairing for this ethnicity
        ScoredHaplotypes max =
            Collections.max(haplotypePairs, new EthnicityHaplotypeComp(ethnicity));

        for (Haplotype t : max) {
          haplotypesByEthnicity.put(ethnicity, t);
        }
      }
    }

    return haplotypesByEthnicity;

  }

  /**
   * Recursive entry point for generating all possible {@link Haplotype} pairs.
   *
   * @param possibleHaplotypePairs Collection to populate with possible haplotype pairs
   * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles for
   *        that strand.
   */
  private void generateHaplotypePairs(Set<HaplotypeSet> possibleHaplotypePairs,
      List<Multimap<Strand, HLAType>> typesByLocus) {
    // Overview:
    // 1. Recurse through strand 1 sets - for each locus, record the possible complementary options
    // 2. At the terminal strand 1 step, start recursing through the possible strand 2 options
    // 3. At the terminal strand 2 step, create a ScoredHaplotype for the pair

    generateStrandOneHaplotypes(possibleHaplotypePairs, typesByLocus, new ArrayList<>(),
        new ArrayList<>(), 0);

  }

  /**
   * Recursively generate all possible haplotypes for the "first" strand.
   *
   * @param possibleHaplotypePairs Collection to populate with possible haplotype pairs
   * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles for
   *        that strand.
   * @param currentHaplotypeAlleles Current alleles of the first haplotype
   * @param strandTwoOptionsByLocus List of allele options, by locus, to populate for complementary
   *        haplotype generation.
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandOneHaplotypes(Set<HaplotypeSet> possibleHaplotypePairs,
      List<Multimap<Strand, HLAType>> typesByLocus, List<HLAType> currentHaplotypeAlleles,
      List<List<HLAType>> strandTwoOptionsByLocus, int locusIndex) {
    if (locusIndex == typesByLocus.size()) {
      // Terminal step - we now have one haplotype; recursively generate the second
      Haplotype firstHaplotype = new Haplotype(currentHaplotypeAlleles);
      generateStrandTwoHaplotypes(possibleHaplotypePairs, firstHaplotype, strandTwoOptionsByLocus,
          new ArrayList<>(), 0);
    } else {
      // Recursive step -
      Multimap<Strand, HLAType> currentLocus = typesByLocus.get(locusIndex);
      // The strand notations are arbitrary. So at each locus we need to consider both possibilities
      // But we do not need to flip the first locus as that would create mirrored pairings
      Strand[] strandsToTest = locusIndex == 0 ? new Strand[] {Strand.FIRST} : Strand.values();

      for (Strand strand : strandsToTest) {
        // Whatever strand is picked for this locus, we want to use types of the other strand for
        // the second haplotype
        List<HLAType> secondStrandTypes = ImmutableList.copyOf(currentLocus.get(strand.flip()));

        if (!secondStrandTypes.isEmpty()) {
          // Heterozygous, so we will later need to iterate through the second strand types
          setOrAdd(strandTwoOptionsByLocus, secondStrandTypes, locusIndex);
        }

        // Recurse through each HLA type on the first strand for this locus
        for (HLAType currentType : currentLocus.get(strand)) {
          if (secondStrandTypes.isEmpty()) {
            // Homozygous so the only option at this locus of the other haplotype is this same type
            setOrAdd(strandTwoOptionsByLocus, ImmutableList.of(currentType), locusIndex);
          }
          setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);

          // Recurse
          generateStrandOneHaplotypes(possibleHaplotypePairs, typesByLocus, currentHaplotypeAlleles,
              strandTwoOptionsByLocus, locusIndex + 1);
        }
      }
    }
  }

  /**
   * Recursively generate all possible haplotypes for the "second" strand.
   *
   * @param possibleHaplotypePairs Collection to populate with possible haplotype pairs
   * @param firstHaplotype The fixed, complementary haplotype
   * @param strandTwoOptionsByLocus List of allele options, by locus, to use when generating
   *        haplotypes
   * @param currentHaplotypeAlleles Current alleles of the second haplotype
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandTwoHaplotypes(Set<HaplotypeSet> possibleHaplotypePairs,
      Haplotype firstHaplotype, List<List<HLAType>> strandTwoOptionsByLocus,
      List<HLAType> currentHaplotypeAlleles, int locusIndex) {
    if (locusIndex == strandTwoOptionsByLocus.size()) {
      // Terminal step - we now have two haplotypes so we record the haplotype pair
      Haplotype secondHaplotype = new Haplotype(currentHaplotypeAlleles);
      possibleHaplotypePairs.add(new HaplotypeSet(firstHaplotype, secondHaplotype));
    } else {
      // Recursive step - iterate through the possible alleles for this locus, building up the
      // current haplotype
      for (HLAType currentType : strandTwoOptionsByLocus.get(locusIndex)) {
        setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);
        generateStrandTwoHaplotypes(possibleHaplotypePairs, firstHaplotype, strandTwoOptionsByLocus,
            currentHaplotypeAlleles, locusIndex + 1);
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

  /**
   * Replace all HLA types with their groups (condensing equivalent alleles)
   */
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

  }

  /**
   * Filter out {@link Status#UNKNOWN} types and eliminate redundant strands
   */
  private void pruneUnknown(Multimap<Strand, HLAType> typesForStrand) {
    // Homozygous
    if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
      typesForStrand.removeAll(Strand.SECOND);
    }

    // Sort out our types by CWD status
    for (Strand strand : typesForStrand.keySet()) {
      Multimap<Status, HLAType> typesByStatus =
          MultimapBuilder.enumKeys(Status.class).hashSetValues().build();
      Collection<HLAType> values = typesForStrand.get(strand);
      values.forEach(t -> typesByStatus.put(CommonWellDocumented.getStatus(t), t));

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
   *         incorrectly.
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

    // Note: Some DRB345 loci may be empty, but should be sorted
    Collections.sort(dr51Locus);
    Collections.sort(dr52Locus);
    Collections.sort(dr53Locus);
  }

  private Map<Strand, BwGroup> makeIfNull(Map<Strand, BwGroup> bwMap) {
    if (Objects.isNull(bwMap)) {
      bwMap = new HashMap<>();
    }
    return bwMap;
  }

  /**
   * Helper method to build a set if it's null
   */
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

  /**
   * Helper wrapper class for a set of {@link Haplotype}s
   */
  private static class HaplotypeSet extends HashSet<Haplotype> {
    private static final long serialVersionUID = 7497078613474172335L;

    private HaplotypeSet(Haplotype... initialHaplotypes) {
      super();
      for (Haplotype haplotype : initialHaplotypes) {
        add(haplotype);
      }
    }
  }

  /**
   * Helper wrapper class to cache the scores for haplotypes
   */
  private static class ScoredHaplotypes extends HashSet<Haplotype> {
    private static final long serialVersionUID = 3780864438450985328L;
    private static final int NO_MISSING_WEIGHT = 10;
    private final Map<RaceGroup, Double> scoresByEthnicity = new HashMap<>();

    private ScoredHaplotypes(Collection<Haplotype> initialHaplotypes) {
      super();
      double cwdScore = 0;

      for (Haplotype haplotype : initialHaplotypes) {
        add(haplotype);

        for (HLAType allele : haplotype.getTypes()) {
          switch (CommonWellDocumented.getStatus(allele)) {
            case COMMON:
              // Add 1 points for common alleles
              cwdScore += 1;
              break;
            case WELL_DOCUMENTED:
              // Add 1/2 point for well-documented alleles
              cwdScore += 0.5;
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
          Double f = frequencyTable.get(haplotype, e);
          if (Objects.isNull(f)) {
            // Cache the frequency if unseen haplotype
            f = HaplotypeFrequencies.getFrequency(e, haplotype);
            frequencyTable.put(haplotype, e, f);
          }
          if (Double.compare(f, Double.MIN_VALUE) > 0) {
            frequency = frequency.multiply(new BigDecimal(f));
            noMissingCount++;
          }
        }
        double s = (NO_MISSING_WEIGHT * noMissingCount)
            + new BigDecimal(cwdScore).add(frequency).doubleValue();
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
      return Double.compare(getScore(e), o.getScore(e));
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
