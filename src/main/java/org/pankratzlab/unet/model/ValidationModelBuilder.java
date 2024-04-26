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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.util.TreeSet;
import java.util.function.Function;
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
import org.pankratzlab.unet.parser.util.RabinKarp;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.MultimapBuilder.SortedSetMultimapBuilder;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.StringConverter;

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
  public static final String NOT_ON_CELL_SURFACE = ".+[0-9]+[LSCAQlscaq]$";
  public static final String NOT_EXPRESSED = ".+[0-9]+[Nn]$";

  {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      comparators.put(ethnicity, new EthnicityHaplotypeComp(ethnicity));
    }
  }

  private String donorId;
  private String source;
  private String sourceType;


  // TODO replace per-locus data structures with maps from [Sero|HLA]Locus to data structure
  // (or tables as appropriate)
  /* These are LinkedHashSets, thus ensuring insertion order */
  private Set<SeroType> aLocusCWD;
  private Set<SeroType> bLocusCWD;
  private Set<SeroType> cLocusCWD;
  private Set<HLAType> aLocusCWDTypes;
  private Set<HLAType> bLocusCWDTypes;
  private Set<HLAType> cLocusCWDTypes;
  private Set<SeroType> aLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> bLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> cLocusFirst = new LinkedHashSet<>();

  private Map<HLALocus, AllelePairings> possibleAllelePairings = new HashMap<>();
  private Map<HLALocus, AllelePairings> donorAllelePairings = new HashMap<>();

  private Set<HLALocus> nonCWDLoci = new TreeSet<>();
  private Map<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping = new HashMap<>();

  private Set<SeroType> drbLocus;
  private Set<SeroType> dqaLocus;
  private Set<SeroType> dqbLocus;
  private Set<SeroType> dpaLocus;

  // private Set<HLAType> drbLocusTypes;
  private Set<HLAType> dqaLocusTypes;
  private Set<HLAType> dqbLocusTypes;
  private Set<HLAType> dpaLocusTypes;
  private Set<HLAType> dpbLocusTypes;

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

  public ValidationModelBuilder a(String aSeroType) {
    if (test1(aSeroType)) {
      return null;
    }
    aLocusCWD = makeIfNull(aLocusCWD);
    addToLocus(aLocusCWD, SeroLocus.A, aSeroType);
    return this;
  }

  public ValidationModelBuilder aType(HLAType aType) {
    aLocusCWDTypes = makeIfNull(aLocusCWDTypes);
    addToLocus(aLocusCWDTypes, HLALocus.A, aType);
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

  public ValidationModelBuilder bType(HLAType bType) {
    bLocusCWDTypes = makeIfNull(bLocusCWDTypes);
    addToLocus(bLocusCWDTypes, HLALocus.B, bType);
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

  public ValidationModelBuilder cType(HLAType cType) {
    cLocusCWDTypes = makeIfNull(cLocusCWDTypes);
    addToLocus(cLocusCWDTypes, HLALocus.C, cType);
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

  public ValidationModelBuilder possibleAllelePairings(HLALocus locus, AllelePairings alleles) {
    possibleAllelePairings.put(locus, alleles);
    return this;
  }

  public ValidationModelBuilder donorAllelePairings(HLALocus locus, AllelePairings alleles) {
    donorAllelePairings.put(locus, alleles);
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
    dpbLocusTypes = makeIfNull(dpbLocusTypes);
    // Shorten the allele designation to allele group and specific HLA protein. Further fields can
    // not be entered into UNOS
    if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
      HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
      if (tmpDPB1.spec().size() > 2) {
        tmpDPB1 =
            new HLAType(HLALocus.DPB1, new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
      }
      dpbLocusTypes.add(tmpDPB1);
    }
    return this;
  }

  public ValidationModelBuilder dqbType(HLAType dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocusTypes = makeIfNull(dqbLocusTypes);
    addToLocus(dqbLocusTypes, HLALocus.DQB1, dqbType);
    return this;
  }

  public ValidationModelBuilder dqaType(HLAType dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocusTypes = makeIfNull(dqaLocusTypes);
    addToLocus(dqaLocusTypes, HLALocus.DQA1, dqaType);
    return this;
  }

  public ValidationModelBuilder dpaType(HLAType dpaType) {
    if (test2(dpaType)) {
      return null;
    }
    dpaLocusTypes = makeIfNull(dpaLocusTypes);
    addToLocus(dpaLocusTypes, HLALocus.DPA1, dpaType);
    return this;
  }

  private boolean test2(HLAType dqbType) {
    return dqbType == null;
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

  private final class AlleleStringConverter extends StringConverter<Supplier<TextFlow>> {
    private final PresentableAlleleChoices choices;

    private AlleleStringConverter(PresentableAlleleChoices choices) {
      this.choices = choices;
    }

    @Override
    public String toString(Supplier<TextFlow> object) {
      final String selectedData = choices.getSelectedData(object);
      return selectedData == null ? "" : selectedData;
    }

    @Override
    public Supplier<TextFlow> fromString(String string) {
      return null;
    }
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
        bLocusCWD, cLocusCWD, drbLocus, dqbLocus, dqaLocus, dpaLocus, dpbLocusTypes, bw4, bw6,
        dr51Locus, dr52Locus, dr53Locus, bcCwdHaplotypes, drDqDR345Haplotypes, remapping);
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
        List<HLAType> firstStrandTypes = Lists.newArrayList(currentLocus.get(strandOne));
        List<HLAType> secondStrandTypes = Lists.newArrayList(currentLocus.get(strandOne.flip()));
        if (secondStrandTypes.isEmpty()) {
          secondStrandTypes = Lists.newArrayList(firstStrandTypes);
        }

        // sorts in descending order, notice h2's weight is found first
        Comparator<HLAType> c = ((h1, h2) -> {
          return Double.compare(CommonWellDocumented.getEquivStatus(h2).getWeight(),
              CommonWellDocumented.getEquivStatus(h1).getWeight());
        });
        firstStrandTypes.sort(c);
        secondStrandTypes.sort(c);

        double bestCWD;
        int i;

        bestCWD = CommonWellDocumented.getEquivStatus(firstStrandTypes.get(0)).getWeight();
        i = 1;
        while (i < firstStrandTypes.size() && CommonWellDocumented
            .getEquivStatus(firstStrandTypes.get(i)).getWeight() == bestCWD) {
          i++;
        }
        for (int j = firstStrandTypes.size() - 1; j >= i; j--) {
          firstStrandTypes.remove(j);
        }

        bestCWD = CommonWellDocumented.getEquivStatus(secondStrandTypes.get(0)).getWeight();
        i = 1;
        while (i < secondStrandTypes.size() && CommonWellDocumented
            .getEquivStatus(secondStrandTypes.get(i)).getWeight() == bestCWD) {
          i++;
        }
        for (int j = secondStrandTypes.size() - 1; j >= i; j--) {
          secondStrandTypes.remove(j);
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

      Set<ScoredHaplotypes> maxSet = Sets.newHashSet(maxScorePairsByEthnicity.values());
      // performance hack - shortcut if all the currently tracked max scores are the same
      int sz = maxSet.size();
      if (sz == 1) {
        // doesn't matter which we use, all RaceGroups point to the same ScoredHaplotypes
        if (comparators.get(RaceGroup.AFA).compare(maxSet.iterator().next(), scored) < 0) {
          for (RaceGroup ethnicity : RaceGroup.values()) {
            maxScorePairsByEthnicity.put(ethnicity, scored);
          }
        }
      } else {
        for (RaceGroup ethnicity : RaceGroup.values()) {
          // if the map doesn't have a value yet
          // or if the current value is less than the new value
          // put the value into the map
          if (!maxScorePairsByEthnicity.containsKey(ethnicity) || comparators.get(ethnicity)
              .compare(maxScorePairsByEthnicity.get(ethnicity), scored) < 0) {
            maxScorePairsByEthnicity.put(ethnicity, scored);
          }
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
        dqbLocus, dqaLocus, dpbLocusTypes, bw4, bw6)) {
      if (Objects.isNull(o)) {
        return new ValidationResult(false, Optional.of("ValidationModel incomplete"));
      }
    }
    // Ensure all sets have a reasonable number of entries
    for (Set<?> set : ImmutableList.of(aLocusCWD, bLocusCWD, cLocusCWD, drbLocus, dqbLocus,
        dqaLocus, dpbLocusTypes)) {
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
        Set<HLAType> typesSet;
        AllelePairings allelePairs = possibleAllelePairings.get(locus);
        AllelePairings donorPairs = donorAllelePairings.get(locus);

        switch (locus) {
          case A:
            locusSet = aLocusCWD;
            typesSet = aLocusCWDTypes;
            break;
          case B:
            locusSet = bLocusCWD;
            typesSet = bLocusCWDTypes;
            break;
          case C:
            locusSet = cLocusCWD;
            typesSet = cLocusCWDTypes;
            break;
          case DPA1:
            locusSet = dpaLocus;
            typesSet = dpaLocusTypes;
            break;
          case DQA1:
            locusSet = dqaLocus;
            typesSet = dqaLocusTypes;
            break;
          case DPB1:
            locusSet = null;
            typesSet = dpbLocusTypes;
          case DQB1:
            locusSet = dqbLocus;
            typesSet = dqbLocusTypes;
          default:
            allelePairs = null;
            donorPairs = null;
            locusSet = null;
            typesSet = null;
            break;
        }
        if (allelePairs == null || donorPairs == null) {
          // TODO
          continue;
        }

        Iterator<SeroType> iter = locusSet == null ? null : locusSet.iterator();
        String header = "Assigned allele pair (" + typesSet.stream().map(ht -> {
          return (iter == null ? "" : (iter.next()) + " / ") + ht.specString() + " - "
              + CommonWellDocumented.getEquivStatus(ht);
        }).collect(Collectors.joining(", ")) + ") for HLA-" + locus.name()
            + " locus is not Common / Well-Documented.";

        String text =
            "Please select desired allele pair for this locus. Selecting the first allele will populate valid pairings for the second allele.";

        PresentableAlleleChoices choices = PresentableAlleleChoices.create(allelePairs);

        final List<Supplier<TextFlow>> allChoices = choices.getAllChoices();

        Function<Supplier<TextFlow>, String> tooltipProvider = (tf) -> {
          return Objects.toString(choices.dataMap.get(tf), null);
        };

        StyleableChoiceDialog<Supplier<TextFlow>> cd = new StyleableChoiceDialog<>(
            allChoices.get(0), allChoices, choices.getAllSecondChoices(), choices.dataMap);
        cd.setTitle("Select HLA-" + locus.name() + " Alleles");
        cd.setHeaderText(header);
        cd.setContentText(text);
        cd.setCombo1CellFactory(listView -> new SimpleTableObjectListCell(tooltipProvider));
        cd.setCombo1ButtonCell(new SimpleTableObjectListCell(tooltipProvider));
        cd.setCombo2CellFactory(listView -> new SimpleTableObjectListCell(tooltipProvider));
        cd.setCombo2ButtonCell(new SimpleTableObjectListCell(tooltipProvider));
        cd.setConverter1(new AlleleStringConverter(choices));
        cd.setConverter2(new AlleleStringConverter(choices));

        Optional<Supplier<TextFlow>> result = cd.showAndWait();

        if (!result.isPresent()) {
          return new ValidationResult(false, Optional.empty());
        }

        Supplier<TextFlow> selAllele1 = result.get();
        Supplier<TextFlow> selAllele2 = cd.getSelectedSecondItem();

        String allele1 = choices.getSelectedData(selAllele1);
        String allele2 = choices.getSelectedData(selAllele2);

        HLAType hlaType1 = HLAType.valueOf(allele1);
        HLAType hlaType2 = HLAType.valueOf(allele2);
        SeroType seroType1 = hlaType1.equivSafe();
        SeroType seroType2 = hlaType2.equivSafe();

        final Iterator<HLAType> typeIter = typesSet.iterator();
        HLAType hlaType1Old = typeIter.next();
        HLAType hlaType2Old = typeIter.hasNext() ? typeIter.next() : hlaType1Old;

        final Iterator<SeroType> seroIter = locusSet.iterator();
        SeroType seroType1Old = seroIter.next();
        SeroType seroType2Old = seroIter.hasNext() ? seroIter.next() : seroType1Old;

        locusSet.clear();
        locusSet.add(seroType1);
        locusSet.add(seroType2);
        typesSet.clear();
        typesSet.add(hlaType1);
        typesSet.add(hlaType2);

        boolean a1Match =
            hlaType1.compareTo(hlaType1Old) == 0 || hlaType1.compareTo(hlaType2Old) == 0;
        boolean a2Match =
            hlaType2.compareTo(hlaType1Old) == 0 || hlaType2.compareTo(hlaType2Old) == 0;

        if (!a1Match || !a2Match) {
          ImmutableSortedSet<TypePair> prevSet = ImmutableSortedSet
              .of(new TypePair(hlaType1Old, seroType1Old), new TypePair(hlaType2Old, seroType2Old));

          ImmutableSortedSet<TypePair> newSet = ImmutableSortedSet
              .of(new TypePair(hlaType1, seroType1), new TypePair(hlaType2, seroType2));


          remapping.put(locus, Pair.of(prevSet, newSet));
        }
      }

    }

    return new ValidationResult(true, Optional.empty());
  }

  public abstract static class PresentableDataChoices<T, R> {
    List<T> choices;
    Multimap<T, T> secondChoices;
    BiMap<T, R> dataMap;

    public PresentableDataChoices(List<T> choices, Multimap<T, T> secondChoices,
        BiMap<T, R> presentationToDataMap) {
      this.choices = choices;
      this.secondChoices = secondChoices;
      this.dataMap = presentationToDataMap;
    }

    public List<T> getAllChoices() {
      return choices;
    }

    public Multimap<T, T> getAllSecondChoices() {
      return secondChoices;
    }

    public Collection<T> getSecondChoices(T firstChoice) {
      return secondChoices.get(firstChoice);
    }

    public abstract List<T> getMatchingChoices(String userInput);

    public R getSelectedData(T selected) {
      return dataMap.get(selected);
    }

  }

  public static class PresentableAlleleChoices
      extends PresentableDataChoices<Supplier<TextFlow>, String> {

    public static PresentableAlleleChoices create(AllelePairings allelePairings) {
      List<Supplier<TextFlow>> userChoices = new ArrayList<>();
      ListMultimap<Supplier<TextFlow>, Supplier<TextFlow>> secondChoices =
          SortedSetMultimapBuilder.hashKeys().arrayListValues().build();
      BiMap<Supplier<TextFlow>, String> presentationToDataMap = HashBiMap.create();

      userChoices.add(() -> new TextFlow());

      List<String> alleleKeys = new ArrayList<>(allelePairings.map.keySet());
      sortAlleleStrings(alleleKeys);

      List<String> choiceList = condenseIntoGroups(allelePairings, alleleKeys, true);

      for (String allele : choiceList) {
        Supplier<TextFlow> supp = () -> getText(allele);

        userChoices.add(supp);
        presentationToDataMap.put(supp, allele);
      }

      for (Supplier<TextFlow> choice : userChoices) {
        String data = presentationToDataMap.get(choice);
        if (data == null) {
          continue;
        }
        String pairingAllele = data.contains("-") ? data.split("-")[0] : data;

        List<String> pairings = new ArrayList<>(allelePairings.getValidPairings(pairingAllele));
        sortAlleleStrings(pairings);

        List<String> secondChoicePairings = condenseIntoGroups(allelePairings, pairings, false);

        for (String pairing : secondChoicePairings) {
          Supplier<TextFlow> presentationView = presentationToDataMap.inverse().get(pairing);
          if (presentationView == null) {
            presentationView = () -> getText(pairing);
            presentationToDataMap.put(presentationView, pairing);
          }
          secondChoices.put(choice, presentationView);
        }
      }

      return new PresentableAlleleChoices(userChoices, secondChoices, presentationToDataMap,
          allelePairings);
    }

    private static List<String> condenseIntoGroups(AllelePairings allelePairings,
        List<String> alleleKeys, boolean checkPairings) {
      List<List<String>> subsets = new ArrayList<>();
      List<String> subset = new ArrayList<>();

      HLAType prev3FieldType = null;

      // all allele strings should be in the correct order now
      // now let's condense into subsets of the same allele types, with
      // separate subsets for N/n and LSCAQ/lscaq alleles
      for (String allele : alleleKeys) {
        if (allele.matches(NOT_EXPRESSED) || allele.matches(NOT_ON_CELL_SURFACE)) {
          // make sure to add the currently-being-built subset first!
          if (subset.size() > 0) {
            subsets.add(subset);
            subset = new ArrayList<>();
          }
          // add null or lscaq alleles as single entries
          subsets.add(Lists.newArrayList(allele));
          // clear out known three-field type
          prev3FieldType = null;
          continue;
        }

        HLAType hType = HLAType.valueOf(allele);
        if (hType.resolution() <= 2) {
          // make sure to add the currently-being-built subset first!
          if (subset.size() > 0) {
            subsets.add(subset);
            subset = new ArrayList<>();
          }
          // add two-field alleles as single entries
          subsets.add(Lists.newArrayList(allele));
          // clear out known three-field type
          prev3FieldType = null;
          continue;
        }

        // we know resolution is >= 3

        HLAType curr3FieldType = (hType.resolution() == 3) ? hType
            : new HLAType(hType.locus(), hType.spec().get(0), hType.spec().get(1),
                hType.spec().get(2));
        if (prev3FieldType != null && prev3FieldType.compareTo(curr3FieldType) != 0) {
          // new three field type
          subsets.add(subset);
          subset = new ArrayList<>();
        }
        // same or new three-field type, either way:
        // update known three-field type, and
        // add four-field type to subset
        prev3FieldType = curr3FieldType;
        subset.add(allele);

      }
      if (subset.size() > 0) {
        subsets.add(subset);
      }

      List<String> choiceList = new ArrayList<>();

      // now we need to process the subset lists
      // single element subsets can be added directly
      // -- this preserves null/not-expressed subsets
      // multiple element subsets need to be further
      // separated into subsets: all alleles in a subset must
      // map to the same serotype and the same second choice alleles

      for (List<String> sub : subsets) {
        if (sub.size() == 1) {
          choiceList.add(sub.get(0));
          continue;
        }

        List<List<String>> subSubsets = new ArrayList<>();
        List<String> subSubset = new ArrayList<>();

        SeroType prevSero = null;
        Set<String> prevPairings = null;

        for (String subAllele : sub) {
          HLAType type = HLAType.valueOf(subAllele);
          SeroType newSero = type.equivSafe();
          HashSet<String> newPairings =
              checkPairings ? Sets.newHashSet(allelePairings.getValidPairings(subAllele))
                  : new HashSet<>();
          if (prevSero == null) {
            prevSero = newSero;
            prevPairings = newPairings;
          } else if (prevSero.compareTo(newSero) != 0) {
            if (subSubset.size() > 0) {
              subSubsets.add(subSubset);
              subSubset = new ArrayList<>();
            }
            prevSero = newSero;
            prevPairings = newPairings;
          } else if (!prevPairings.containsAll(newPairings)
              || !newPairings.containsAll(prevPairings)) {
            if (subSubset.size() > 0) {
              subSubsets.add(subSubset);
              subSubset = new ArrayList<>();
            }
            prevSero = newSero;
            prevPairings = newPairings;
          }
          subSubset.add(subAllele);
        }
        if (subSubset.size() > 0) {
          subSubsets.add(subSubset);
          subSubset = new ArrayList<>();
        }

        for (List<String> subS : subSubsets) {
          if (subS.size() == 1) {
            choiceList.add(subS.get(0));
          } else {
            choiceList.add(subS.get(0) + "-" + subS.get(subS.size() - 1));
          }
        }

      }
      return choiceList;
    }

    private static void sortAlleleStrings(List<String> alleleKeys) {
      alleleKeys.sort((s1, s2) -> {
        boolean check1N = s1.matches(NOT_EXPRESSED);
        boolean check1C = s1.matches(NOT_ON_CELL_SURFACE);
        boolean check2N = s2.matches(NOT_EXPRESSED);
        boolean check2C = s2.matches(NOT_ON_CELL_SURFACE);
        boolean check1 = check1N || check1C;
        boolean check2 = check2N || check2C;
        char s1C = s1.charAt(s1.length() - 1);
        char s2C = s2.charAt(s2.length() - 1);
        String s1Hs = check1 ? s1.substring(0, s1.length() - 1) : s1;
        String s2Hs = check2 ? s2.substring(0, s2.length() - 1) : s2;

        HLAType h1 = HLAType.valueOf(s1Hs);
        HLAType h2 = HLAType.valueOf(s2Hs);

        int comp;
        if ((comp = h1.compareTo(h2)) != 0)
          return comp;

        if (check1 && !check2) {
          // first element ends with special character, meaning second element should come first
          return 1;
        } else if (check2 && !check1) {
          // second element ends with special character, meaning first element should come first
          return -1;
        }

        // both end with a special character
        if (check1N && check2N) {
          // both null and same HLAType - these are the same allele
          return 0;
        } else if (check1N && !check2N) {
          // first element is null, second is lscaq, second comes first
          return 1;
        } else if (!check1N && check2N) {
          // first element is lscaq, second is null, first comes first
          return -1;
        }


        // this block could probably be condensed based on knowing null status from previous checks,
        // but it's easier to just duplicate the logic for now...

        // both end with a special character
        if (check1C && check2C) {
          // both null and same HLAType - these are the same allele
          return 0;
        } else if (check1C && !check2C) {
          // first element is null, second is lscaq, second comes first
          return 1;
        } else if (!check1C && check2C) {
          // first element is lscaq, second is null, first comes first
          return -1;
        }

        // TODO dunno what's going on here... probably should do something special?
        return 0;
      });
    }

    private final AllelePairings allelePairs;

    private PresentableAlleleChoices(List<Supplier<TextFlow>> choices,
        Multimap<Supplier<TextFlow>, Supplier<TextFlow>> secondChoices,
        BiMap<Supplier<TextFlow>, String> presentationToDataMap, AllelePairings allelePairs) {
      super(choices, secondChoices, presentationToDataMap);
      this.allelePairs = allelePairs;
    }

    @Override
    public List<Supplier<TextFlow>> getMatchingChoices(String userInput) {
      return allelePairs.getMatchingAlleles(userInput).stream().map(s -> dataMap.inverse().get(s))
          .filter(Predicates.notNull()).collect(Collectors.toList());
    }

  }

  private static TextFlow getText(String allele) {
    List<Text> textNodes = new ArrayList<>();
    if (allele.contains("-")) {
      String[] a = allele.split("-");
      addTextNodes(textNodes, a[0]);
      textNodes.add(new Text("-"));
      addTextNodes(textNodes, a[1]);
    } else {
      addTextNodes(textNodes, allele);
    }

    Text[] nodes = textNodes.toArray(new Text[textNodes.size()]);
    TextFlow tf = new TextFlow(nodes);
    return tf;
  }

  private static void addTextNodes(List<Text> textNodes, final String allele) {
    HLAType alleleType = HLAType.valueOf(allele);
    HLAType cwdType1 = CommonWellDocumented.getCWDType(alleleType);
    Status status1 = CommonWellDocumented.getStatus(alleleType);

    textNodes.add(new Text(alleleType.locus().name() + "*"));

    String specString = alleleType.specString();
    boolean match = allele.matches(NOT_EXPRESSED) || allele.matches(NOT_ON_CELL_SURFACE);

    if (status1 != Status.UNKNOWN) {

      if (cwdType1.specString().length() < specString.length()) {
        Text t1 = new Text(cwdType1.specString());
        t1.setStyle("-fx-font-weight:bold;");
        textNodes.add(t1);
        textNodes.add(new Text(specString.substring(cwdType1.specString().length())));
      } else {
        Text t1 = new Text(specString);
        t1.setStyle("-fx-font-weight:bold;");
        textNodes.add(t1);
      }

      if (match) {
        textNodes.add(new Text("" + allele.charAt(allele.length() - 1)));
      }

    } else {
      textNodes
          .add(new Text(specString + (match ? ("" + allele.charAt(allele.length() - 1)) : "")));
    }

    textNodes.add(
        new Text(" (" + alleleType.locus().name() + alleleType.equivSafe().specString() + ")"));
  }

  private static class SimpleTableObjectListCell extends ListCell<Supplier<TextFlow>> {

    // private Function<Supplier<TextFlow>, Supplier<TextFlow>> tooltipProvider;
    private Function<Supplier<TextFlow>, String> tooltipProvider;
    private Tooltip tooltip = new Tooltip();

    public SimpleTableObjectListCell(Function<Supplier<TextFlow>, String> tooltipProvider) {
      // Function<Supplier<TextFlow>, Supplier<TextFlow>> tooltipProvider) {
      this.tooltipProvider = tooltipProvider;

      // hack for adjusting tooltip delay / etc
      // from https://stackoverflow.com/a/43291239/875496
      // TODO FIXME change when JavaFX9+ is available
      try {
        Class<?> clazz = tooltip.getClass().getDeclaredClasses()[0];
        Constructor<?> constructor = clazz.getDeclaredConstructor(Duration.class, Duration.class,
            Duration.class, boolean.class);
        constructor.setAccessible(true);
        Object tooltipBehavior = constructor.newInstance(new Duration(50), // open
            new Duration(500000), // visible
            new Duration(100), // close
            false);
        Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
        fieldBehavior.setAccessible(true);
        fieldBehavior.set(tooltip, tooltipBehavior);
      } catch (Throwable t) {
        t.printStackTrace();
      }

      tooltip.setWrapText(true);
      tooltip.setMaxWidth(600);

      /*
       * NB: TODO FIXME setting style here because I couldn't figure out how to locate it in an
       * actual CSS style sheet -- 04-18-2024, b.cole
       */
      tooltip.setStyle(
          "-fx-background: rgba(230,230,230); -fx-text-fill: black; -fx-background-color: rgba(230,230,230,0.95); -fx-background-radius: 5px; -fx-background-insets: 0; -fx-padding: 0.667em 0.75em 0.667em 0.75em; -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.5) , 10, 0.0 , 0 , 3 ); -fx-font-size: 0.95em;");
    }

    @Override
    public void updateItem(Supplier<TextFlow> item, boolean empty) {
      try {
        super.updateItem(item, empty);
        if (item != null && !empty) {
          setGraphic(item.get());

          String tf = tooltipProvider.apply(item);
          // Supplier<TextFlow> tf = tooltipProvider.apply(item);
          if (tf == null || tf.length() < 150) {
            // TODO don't show tooltips if item isn't too long!
            setTooltip(null);
          } else {
            // System.out.println("tool:: " + tf);
            tooltip.setText(tf);
            // final TextFlow value = tf.get();
            // value.setMaxWidth(tooltip.getMaxWidth());
            // tooltip.setGraphic(value);
            setTooltip(tooltip);
          }
        } else {
          setGraphic(null);
          setTooltip(null);
        }
      } catch (Throwable t) {
        t.printStackTrace();
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
   * Helper method to create {@link HLAType}s and add them to a locus's set in a consistent manner
   */
  private void addToLocus(Set<HLAType> locusSet, HLALocus locus, String typeString) {
    locusSet.add(new HLAType(locus, typeString));
  }

  /**
   * Helper method to create {@link HLAType}s and add them to a locus's set in a consistent manner
   */
  private void addToLocus(Set<HLAType> locusSet, HLALocus locus, HLAType typeString) {
    // TODO ensure locus and typeString.locus are the same
    locusSet.add(typeString);
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

  public static class AllelePairings {

    private Multimap<String, String> map = HashMultimap.create();

    /**
     * @param a1
     * @param a2
     */
    public void addPairing(String a1, String a2) {
      map.put(a1, a2);
      map.put(a2, a1);
    }

    public Collection<String> getValidPairings(String allele) {
      return map.get(allele);
    }

    public boolean isValidPairing(String a1, String a2) {
      return map.containsEntry(a1, a2);
    }

    public Collection<String> getMatchingAlleles(String pattern) {
      final RabinKarp rabinKarp = new RabinKarp(pattern);
      return map.keySet().stream().filter(s -> rabinKarp.search(s) < s.length())
          .collect(Collectors.toSet());
    }

  }

  public static class AlleleGroupPairings {

    private List<Set<String>> groupings1 = new ArrayList<>();
    private List<Set<String>> groupings2 = new ArrayList<>();
    private Map<String, Set<Set<String>>> alleleToGroupsMap = new HashMap<>();
    private Map<String, Set<Integer>> alleleToGroupIndexMap = new HashMap<>();
    private Multimap<String, String> map = HashMultimap.create();

    public List<Set<String>> getAllGroups() {
      List<Set<String>> list = new ArrayList<>(groupings1);
      list.addAll(groupings2);
      return list;
    }

    /**
     * AlleleList groupings/entries
     * 
     * Alleles in alleleList1 entries will only have one set, but alleles in alleleList2 entries
     * will have multiple sets
     */
    public void addAllele1Grouping(Collection<String> alleles) {
      processAlleles(alleles, groupings1);
    }

    /**
     * AlleleList groupings/entries
     * 
     * Alleles in alleleList1 entries will only have one set, but alleles in alleleList2 entries
     * will have multiple sets
     */
    public void addAllele2Grouping(Collection<String> alleles) {
      processAlleles(alleles, groupings2);
    }

    private void processAlleles(Collection<String> alleles, List<Set<String>> groupings) {
      // create group
      Set<String> group = Sets.newTreeSet(alleles);

      // if already present, return
      if (groupings.contains(group))
        return;

      // add to list of groups
      groupings.add(group);

      // compute index of group in list
      int ind = groupings.size() - 1;

      for (String allele : alleles) {
        // connect each allele to the group it's in
        Set<Set<String>> s = alleleToGroupsMap.get(allele);
        if (s == null) {
          alleleToGroupsMap.put(allele, s = new HashSet<>());
        }
        s.add(group);

        // retain the index of the group also for quick access (may not end up needing this)
        Set<Integer> i = alleleToGroupIndexMap.get(allele);
        if (i == null) {
          alleleToGroupIndexMap.put(allele, i = new HashSet<>());
        }
        i.add(ind);
      }
    }

    /**
     * For each allele(a1) in AlleleList1: For each allele(a2) in AlleleList2: addPairing(a1, a2)
     * 
     * @param a1
     * @param a2
     */
    public void addPairing(String a1, String a2) {
      map.put(a1, a2);
      map.put(a2, a1);
    }

    public boolean isValidGroup(Collection<String> alleles) {
      if (alleles == null || alleles.isEmpty())
        return false;

      for (Set<String> group : groupings1) {
        if (group.containsAll(alleles))
          return true;
      }
      for (Set<String> group : groupings2) {
        if (group.containsAll(alleles))
          return true;
      }
      return false;
    }

    public List<Set<String>> getGroups(Collection<String> alleles) {
      List<Set<String>> found = new ArrayList<>();
      for (Set<String> group : groupings1) {
        if (group.containsAll(alleles))
          found.add(group);
      }
      for (Set<String> group : groupings2) {
        if (group.containsAll(alleles))
          found.add(group);
      }
      return found;
    }

    public Collection<String> getValidPairings(String allele) {
      return map.get(allele);
    }

    public List<Set<String>> getValidPairGroups(Set<String> alleles) {
      if (!isValidGroup(alleles))
        return new ArrayList<>();

      Iterator<String> iter = alleles.iterator();
      Set<String> allPairings = new HashSet<>(getValidPairings(iter.next()));
      while (iter.hasNext() && !allPairings.isEmpty()) {
        Set<String> currPairs = new HashSet<>(allPairings);
        Set<String> nextPairs = new HashSet<>(getValidPairings(iter.next()));
        allPairings.clear();
        Sets.intersection(currPairs, nextPairs).copyInto(allPairings);
      }

      if (allPairings.isEmpty())
        return new ArrayList<>();

      List<Set<String>> validGroupPairings = new ArrayList<>();
      for (Set<String> g : groupings1) {
        if (allPairings.containsAll(g))
          validGroupPairings.add(g);
      }
      for (Set<String> g : groupings2) {
        if (allPairings.containsAll(g))
          validGroupPairings.add(g);
      }

      return validGroupPairings;
    }

    public Collection<String> getMatchingAlleles(String pattern) {
      final RabinKarp rabinKarp = new RabinKarp(pattern);
      return map.keySet().stream().filter(s -> rabinKarp.search(s) < s.length())
          .collect(Collectors.toSet());
    }

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

      // TODO - document walk-through
      scoresByEthnicity
          // calculate the score for each ethnicity
          .putAll(Arrays.stream(RaceGroup.values()).collect(Collectors.toMap(e -> e, e -> {

            int noMissingCount = 0;

            // starting from 1
            BigDecimal frequency = new BigDecimal(1.0);

            for (Haplotype haplotype : this) {
              BigDecimal f = frequencyTable.get(haplotype, e);

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
