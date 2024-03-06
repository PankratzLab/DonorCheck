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
import java.util.Arrays;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;
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
import org.pankratzlab.unet.jfx.StyleableChoiceDialog;
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
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Table;

import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Mutable builder class for creating a {@link ValidationModel}.
 *
 * <p>
 * Use the locus-specific setters to build up the donor typing. Each must be called at least once
 * (for homozygous) and at most twice (for heterozygous). Each requires on the specificty string for
 * the given locus (e.g. "24" for A24).
 */
public class ValidationModelBuilder {

  private static final Map<RaceGroup, EthnicityHaplotypeComp> comparators =
      new EnumMap<>(RaceGroup.class);
  private static final Table<Haplotype, RaceGroup, BigDecimal> frequencyTable =
      HashBasedTable.create();
  private static final String NEGATIVE_ALLELE = "N-Negative";

  {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      comparators.put(ethnicity, new EthnicityHaplotypeComp(ethnicity));
    }
  }

  private String donorId;
  private String source;
  private String sourceType;

  /* These are LinkedHashSets, thus ensuring insertion order */
  private Set<SeroType> aLocusCWD;
  private Set<SeroType> bLocusCWD;
  private Set<SeroType> cLocusCWD;
  private Set<SeroType> aLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> bLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> cLocusFirst = new LinkedHashSet<>();

  private List<Pair<TypePair, TypePair>> aAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> bAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> cAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> dqaAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> dqbAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> dpaAlleles = new ArrayList<>();
  private List<Pair<TypePair, TypePair>> dpbAlleles = new ArrayList<>();

  private Set<HLALocus> nonCWDLoci = new HashSet<>();
  private Map<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping = new HashMap<>();

  private Set<SeroType> drbLocus;
  private Set<SeroType> dqaLocus;
  private Set<SeroType> dqbLocus;
  private Set<SeroType> dpaLocus;
  private Set<HLAType> dpbLocus;

  private Set<SeroType> drbLocusNonCWD;
  private Set<SeroType> dqaLocusNonCWD;
  private Set<SeroType> dqbLocusNonCWD;
  private Set<SeroType> dpaLocusNonCWD;
  private Set<HLAType> dpbLocusNonCWD;

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
    if (test1(aType)) {
      return null;
    }
    aLocusCWD = makeIfNull(aLocusCWD);
    addToLocus(aLocusCWD, SeroLocus.A, aType);
    return this;
  }

  public ValidationModelBuilder aNonCWD(String aType) {
    if (test1(aType)) {
      return null;
    }
    aLocusFirst = makeIfNull(aLocusFirst);
    addToLocus(aLocusFirst, SeroLocus.A, aType);
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    if (test1(bType)) {
      return null;
    }
    bLocusCWD = makeIfNull(bLocusCWD);
    addToLocus(bLocusCWD, SeroLocus.B, bType);
    return this;
  }

  public ValidationModelBuilder bNonCWD(String bType) {
    if (test1(bType)) {
      return null;
    }
    bLocusFirst = makeIfNull(bLocusFirst);
    addToLocus(bLocusFirst, SeroLocus.B, bType);
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    if (test1(cType)) {
      return null;
    }
    cLocusCWD = makeIfNull(cLocusCWD);
    addToLocus(cLocusCWD, SeroLocus.C, cType);
    return this;
  }

  public ValidationModelBuilder cNonCWD(String cType) {
    if (test1(cType)) {
      return null;
    }
    cLocusFirst = makeIfNull(cLocusFirst);
    addToLocus(cLocusFirst, SeroLocus.C, cType);
    return this;
  }

  private boolean test1(String type) {
    return !type.matches(".*\\d.*") || type.equals("98");
  }

  public ValidationModelBuilder aAllele(Pair<TypePair, TypePair> alleles) {
    aAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder bAllele(Pair<TypePair, TypePair> alleles) {
    bAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder cAllele(Pair<TypePair, TypePair> alleles) {
    cAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder dqaAllele(Pair<TypePair, TypePair> alleles) {
    dqaAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder dqbAllele(Pair<TypePair, TypePair> alleles) {
    dqbAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder dpaAllele(Pair<TypePair, TypePair> alleles) {
    dpaAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder dpbAllele(Pair<TypePair, TypePair> alleles) {
    dpbAlleles.add(alleles);
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    drbLocus = makeIfNull(drbLocus);
    if (test2(drbType)) {
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

  public ValidationModelBuilder drbNonCWD(String drbType) {
    drbLocusNonCWD = makeIfNull(drbLocusNonCWD);
    if (test2(drbType)) {
      return null;
    }
    if (!Strings.isNullOrEmpty(drbType)
        && Objects.equals(103, Integer.parseInt(drbType.replaceAll(":", "").trim()))) {
      // UNOS explicitly requires DRB1*01:03 to be reported as DRB0103
      drbLocusNonCWD.add(new SeroType(SeroLocus.DRB, "0103"));
    } else {
      addToLocus(drbLocusNonCWD, SeroLocus.DRB, drbType);
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

  public ValidationModelBuilder dqbNonCWD(String dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocusNonCWD = makeIfNull(dqbLocusNonCWD);
    addToLocus(dqbLocusNonCWD, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqaNonCWD(String dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocusNonCWD = makeIfNull(dqaLocusNonCWD);
    addToLocus(dqaLocusNonCWD, SeroLocus.DQA, dqaType);
    return this;
  }

  public ValidationModelBuilder dpaNonCWD(String dpaType) {
    if (test2(dpaType)) {
      return null;
    }
    dpaLocusNonCWD = makeIfNull(dpaLocusNonCWD);
    addToLocus(dpaLocusNonCWD, SeroLocus.DPA, dpaType);
    return this;
  }

  public ValidationModelBuilder dpbNonCWD(String dpbType) {
    dpbLocusNonCWD = makeIfNull(dpbLocusNonCWD);
    // Shorten the allele designation to allele group and specific HLA protein. Further fields can
    // not be entered into UNOS
    if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
      HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
      if (tmpDPB1.spec().size() > 2) {
        tmpDPB1 =
            new HLAType(HLALocus.DPB1, new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
      }
      dpbLocusNonCWD.add(tmpDPB1);
    }
    return this;
  }

  public ValidationModelBuilder dqb(String dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocus = makeIfNull(dqbLocus);
    addToLocus(dqbLocus, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqa(String dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocus = makeIfNull(dqaLocus);
    addToLocus(dqaLocus, SeroLocus.DQA, dqaType);
    return this;
  }

  public ValidationModelBuilder dpa(String dpaType) {
    if (test2(dpaType)) {
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
        tmpDPB1 =
            new HLAType(HLALocus.DPB1, new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
      }
      dpbLocus.add(tmpDPB1);
    }
    return this;
  }

  private boolean test2(String dqbType) {
    return dqbType == null || !dqbType.matches(".*\\d.*");
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

  public void locusIsNonCIWD(HLAType locus) {
    nonCWDLoci.add(locus.locus());
  }

  public class ValidationResult {

    public final boolean valid;
    public final Optional<String> validationMessage;

    public ValidationResult(boolean valid, Optional<String> validationMessage) {
      this.valid = valid;
      this.validationMessage = validationMessage;
    }

  }

  public ValidationResult validate() {
    return ensureValidity();
  }

  /** @return The immutable {@link ValidationModel} based on the current builder state. */
  public ValidationModel build() {
    Multimap<RaceGroup, Haplotype> bcCwdHaplotypes = buildBCHaplotypes(bHaplotypes, cHaplotypes);

    Multimap<RaceGroup, Haplotype> drDqDR345Haplotypes =
        buildHaplotypes(ImmutableList.of(drb1Haplotypes, dqb1Haplotypes, dr345Haplotypes));

    frequencyTable.clear();

    ValidationModel validationModel = new ValidationModel(donorId, source, sourceType, aLocusCWD,
        bLocusCWD, cLocusCWD, drbLocus, dqbLocus, dqaLocus, dpaLocus, dpbLocus, bw4, bw6, dr51Locus,
        dr52Locus, dr53Locus, bcCwdHaplotypes, drDqDR345Haplotypes, remapping);
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
      Multimap<Strand, HLAType> s6s4 = enforceBws(BwGroup.Bw6, BwGroup.Bw4, bHaps);
      Multimap<RaceGroup, Haplotype> s4s6Haplotypes = s4s6.isEmpty() ? ImmutableMultimap.of()
          : buildHaplotypes(ImmutableList.of(s4s6, cHaplotypes));
      Multimap<RaceGroup, Haplotype> s6s4Haplotypes = s6s4.isEmpty() ? ImmutableMultimap.of()
          : buildHaplotypes(ImmutableList.of(s6s4, cHaplotypes));

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
          ScoredHaplotypes max =
              Collections.max(scoredHaplotypePairs, new EthnicityHaplotypeComp(ethnicity));

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
  private Multimap<RaceGroup, Haplotype> buildHaplotypes(
      List<Multimap<Strand, HLAType>> typesByLocus) {
    Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity = new HashMap<>();
    Multimap<RaceGroup, Haplotype> haplotypesByEthnicity =
        MultimapBuilder.enumKeys(RaceGroup.class).arrayListValues().build();

    List<Multimap<Strand, HLAType>> presentTypesByLocus =
        typesByLocus.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
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
   *        that strand.
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
   *        that strand.
   * @param currentHaplotypeAlleles Current alleles of the first haplotype
   * @param strandTwoOptionsByLocus List of allele options, by locus, to populate for complementary
   *        haplotype generation.
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandOneHaplotypes(
      Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
      List<Multimap<Strand, HLAType>> typesByLocus, List<HLAType> currentHaplotypeAlleles,
      List<List<HLAType>> strandTwoOptionsByLocus, int locusIndex) {
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
      Set<Strand> firstStrandsSet =
          locusIndex == 0 ? ImmutableSet.of(Strand.FIRST) : currentLocus.keySet();

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
              currentHaplotypeAlleles, strandTwoOptionsByLocus, locusIndex + 1);
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
   *        haplotypes
   * @param currentHaplotypeAlleles Current alleles of the second haplotype
   * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
   */
  private void generateStrandTwoHaplotypes(
      Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity, Haplotype firstHaplotype,
      List<List<HLAType>> strandTwoOptionsByLocus, List<HLAType> currentHaplotypeAlleles,
      int locusIndex) {
    if (locusIndex == strandTwoOptionsByLocus.size()) {
      // Terminal step - we now have two haplotypes so we score them and compare
      final Haplotype e2 = new Haplotype(currentHaplotypeAlleles);
      ScoredHaplotypes scored = new ScoredHaplotypes(ImmutableList.of(firstHaplotype, e2));

      for (RaceGroup ethnicity : RaceGroup.values()) {
        if (!maxScorePairsByEthnicity.containsKey(ethnicity) || comparators.get(ethnicity)
            .compare(maxScorePairsByEthnicity.get(ethnicity), scored) < 0) {
          maxScorePairsByEthnicity.put(ethnicity, scored);
        }
      }
    } else {
      // Recursive step - iterate through the possible alleles for this locus, building up the
      // current haplotype
      for (HLAType currentType : strandTwoOptionsByLocus.get(locusIndex)) {
        setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);
        generateStrandTwoHaplotypes(maxScorePairsByEthnicity, firstHaplotype,
            strandTwoOptionsByLocus, currentHaplotypeAlleles, locusIndex + 1);
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
      Multimap<Status, HLAType> typesByStatus =
          MultimapBuilder.enumKeys(Status.class).hashSetValues().build();
      Collection<HLAType> values = typesForStrand.get(strand);
      values.forEach(t -> typesByStatus.put(CommonWellDocumented.getEquivStatus(t), t));

      Set<HLAType> cwdTypes = new HashSet<>();
      for (Status s : Status.values()) {
        if (s != Status.UNKNOWN) {
          cwdTypes.addAll(typesByStatus.get(s));
        }
      }

      // If we have any common or well-documented types, drop all unknown
      if (!cwdTypes.isEmpty()) {
        // NB: making the values empty will break this loop by removing the key from the multimap :D
        typesForStrand.replaceValues(strand, cwdTypes);
      }
    }
  }

  /**
   * @return Optional<String> Value is present if the model has not been fully populated, or
   *         populated incorrectly.
   */
  private ValidationResult ensureValidity() {
    // Ensure all fields have been set
    for (Object o : Lists.newArrayList(donorId, source, aLocusCWD, bLocusCWD, cLocusCWD, drbLocus,
        dqbLocus, dqaLocus, dpbLocus, bw4, bw6)) {
      if (Objects.isNull(o)) {
        return new ValidationResult(false, Optional.of("ValidationModel incomplete"));
      }
    }
    // Ensure all sets have a reasonable number of entries
    for (Set<?> set : ImmutableList.of(aLocusCWD, bLocusCWD, cLocusCWD, drbLocus, dqbLocus,
        dqaLocus, dpbLocus)) {
      if (set.isEmpty() || set.size() > 2) {
        return new ValidationResult(false,
            Optional.of("ValidationModel contains invalid allele count: " + set));
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

    if (!nonCWDLoci.isEmpty()) {
      for (HLALocus locus : nonCWDLoci) {

        Set<SeroType> locusSet;
        List<Pair<TypePair, TypePair>> alleleList;

        switch (locus) {
          case A:
            locusSet = aLocusCWD;
            alleleList = aAlleles;
            break;
          case B:
            locusSet = bLocusCWD;
            alleleList = bAlleles;
            break;
          case C:
            locusSet = cLocusCWD;
            alleleList = cAlleles;
            break;
          default:
            alleleList = null;
            locusSet = null;
            break;
        }
        if (alleleList == null) {
          // TODO
          continue;
        }

        String header = "Assigned allele pair ("
            + locusSet.stream().map(SeroType::specString).collect(Collectors.joining(", "))
            + ") for HLA-" + locus.name() + " locus is not Common / Well-Documented.";

        String text = "Please select desired allele pair for this locus:";

        List<Supplier<TextFlow>> choices = new ArrayList<>();
        Map<Supplier<TextFlow>, Pair<TypePair, TypePair>> map = new HashMap<>();
        for (int i = 0; i < alleleList.size(); i++) {
          Pair<TypePair, TypePair> p = alleleList.get(i);
          int i1 = i;
          Supplier<TextFlow> tf = () -> getText(p, i1 == 0);
          choices.add(tf);
          map.put(tf, p);
        }

        StyleableChoiceDialog<Supplier<TextFlow>> cd =
            new StyleableChoiceDialog<>(choices.get(0), choices);
        cd.setTitle("Select HLA-" + locus.name() + " Alleles");
        cd.setHeaderText(header);
        cd.setContentText(text);
        cd.setComboCellFactory(listView -> new SimpleTableObjectListCell());
        cd.setComboButtonCell(new SimpleTableObjectListCell());
        Optional<Supplier<TextFlow>> result = cd.showAndWait();

        if (!result.isPresent()) {
          return new ValidationResult(false, Optional.empty());
        }

        final TypePair left = map.get(result.get()).getLeft();
        final SeroType seroType1 = left.getSeroType();
        final TypePair right = map.get(result.get()).getRight();
        final SeroType seroType2 = right.getSeroType();
        final HLAType hlaType1 = left.getHlaType();

        // if the user selected a different allele choice, track the remapping
        if (!choices.get(0).equals(result.get())) {
          remapping.put(locus,
              Pair.of(
                  ImmutableSortedSet.of(alleleList.get(0).getLeft(),
                      (alleleList.get(0).getRight() == null ? alleleList.get(0).getLeft()
                          : alleleList.get(0).getRight())),
                  ImmutableSortedSet.of(
                      hlaType1.equals(alleleList.get(0).getLeft().getHlaType()) ? left : right,
                      hlaType1.equals(alleleList.get(0).getLeft().getHlaType()) ? right : left)));

          locusSet.clear();
          locusSet.add(seroType1);
          locusSet.add(seroType2);
        }

      }

    }

    return new ValidationResult(true, Optional.empty());
  }

  private static TextFlow getText(Pair<TypePair, TypePair> data, boolean cwd) {
    List<Text> textNodes = new ArrayList<>();

    TypePair left;
    TypePair right;

    int c = data.getLeft().seroType.compareTo(data.getRight().seroType);
    if (c == 0) {
      c = data.getRight().hlaType.compareTo(data.getLeft().hlaType);
    }

    left = c == 0 ? data.getLeft() : (c > 0 ? data.getRight() : data.getLeft());
    right = c == 0 ? data.getRight() : (c > 0 ? data.getLeft() : data.getRight());

    addTextNodes(textNodes, left, cwd);

    textNodes.add(new Text(", "));

    addTextNodes(textNodes, right, cwd);

    Text[] nodes = textNodes.toArray(new Text[textNodes.size()]);
    TextFlow tf = new TextFlow(nodes);
    return tf;
  }

  private static void addTextNodes(List<Text> textNodes, final TypePair typePair, boolean cwd) {
    HLAType cwdType1 = CommonWellDocumented.getCWDType(typePair.getHlaType());
    Status status1 = CommonWellDocumented.getStatus(typePair.getHlaType());

    String a1Start = typePair.seroType.specString() + " [";
    String a1End = " - " + status1 + "]";

    if (status1 != Status.UNKNOWN) {
      textNodes.add(new Text(a1Start));

      if (cwdType1.specString().length() < typePair.hlaType.specString().length()) {
        Text t1 = new Text(cwdType1.specString());
        t1.setStyle("-fx-font-weight:bold;");
        textNodes.add(t1);
        textNodes
            .add(new Text(typePair.hlaType.specString().substring(cwdType1.specString().length())));
      } else {
        Text t1 = new Text(typePair.hlaType.specString());
        t1.setStyle("-fx-font-weight:bold;");
        textNodes.add(t1);
      }
      textNodes.add(new Text(a1End));

    } else {
      textNodes.add(new Text(a1Start + typePair.hlaType.specString() + a1End));
    }
  }

  private static class SimpleTableObjectListCell extends ListCell<Supplier<TextFlow>> {

    @Override
    public void updateItem(Supplier<TextFlow> item, boolean empty) {
      super.updateItem(item, empty);
      if (item != null) {
        setGraphic(item.get());
      } else {
        setGraphic(null);
      }
    }

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

  public static class TypePair implements Comparable<TypePair> {
    private final HLAType hlaType;
    private final SeroType seroType;

    public TypePair(HLAType hlaType, SeroType seroType) {
      this.hlaType = hlaType;
      this.seroType = seroType;
    }

    /**
     * @return the hlaType
     */
    public HLAType getHlaType() {
      return hlaType;
    }

    /**
     * @return the seroType
     */
    public SeroType getSeroType() {
      return seroType;
    }

    @Override
    public String toString() {
      return seroType.specString() + " [" + hlaType.specString() + " - "
          + CommonWellDocumented.getStatus(getHlaType()) + "]";
    }

    @Override
    public int compareTo(TypePair o) {
      int c = seroType.compareTo(o.seroType);
      if (c != 0)
        return c;
      return hlaType.compareTo(o.hlaType);
    }

  }

  /** Helper wrapper class to cache the scores for haplotypes */
  private static class ScoredHaplotypes extends ArrayList<Haplotype> {
    private static final long serialVersionUID = 3780864438450985328L;
    private static final int NO_MISSING_WEIGHT = 10;
    private final Map<RaceGroup, Double> scoresByEthnicity = new HashMap<>();

    private ScoredHaplotypes(Collection<Haplotype> initialHaplotypes) {
      super();
      BigDecimal cwdScore1 = BigDecimal.ZERO;

      for (Haplotype haplotype : initialHaplotypes) {
        add(haplotype);

        for (RaceGroup e : RaceGroup.values()) {
          BigDecimal f;
          if (!frequencyTable.contains(haplotype, e)) {
            f = HaplotypeFrequencies.getFrequency(e, haplotype);
            frequencyTable.put(haplotype, e, f);
          }
        }

        for (HLAType allele : haplotype.getTypes()) {
          cwdScore1 = cwdScore1
              .add(new BigDecimal(CommonWellDocumented.getEquivStatus(allele).getWeight()));
        }
      }
      BigDecimal cwdScore = cwdScore1;

      scoresByEthnicity
          .putAll(Arrays.stream(RaceGroup.values()).collect(Collectors.toMap(e -> e, e -> {
            int noMissingCount = 0;
            BigDecimal frequency = new BigDecimal(1.0);
            for (Haplotype haplotype : this) {
              // Add this haplotype to the table
              BigDecimal f;

              f = frequencyTable.get(haplotype, e);

              if (f.compareTo(BigDecimal.ZERO) > 0) {
                frequency = frequency.multiply(f);
                noMissingCount++;
              }
            }

            BigDecimal weights =
                cwdScore.add(BigDecimal.valueOf(NO_MISSING_WEIGHT * noMissingCount));

            double s = weights.add(frequency).doubleValue();

            return s;
          })));
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
