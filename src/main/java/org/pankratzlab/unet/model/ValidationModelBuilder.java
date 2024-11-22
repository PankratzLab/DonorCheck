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
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.deprecated.hla.SeroLocus;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.hapstats.AlleleGroups;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.remap.CancellationException;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import org.pankratzlab.unet.parser.util.BwSerotypes;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import org.pankratzlab.unet.parser.util.DRAssociations;
import org.pankratzlab.unet.parser.util.RabinKarp;
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
import com.google.common.collect.Sets;
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
  private String filepath;
  private String source;
  private SourceType sourceType;

  // TODO replace per-locus data structures with maps from [Sero|HLA]Locus to data structure
  // (or tables as appropriate)
  /* These are LinkedHashSets, thus ensuring insertion order */
  private Set<SeroType> aLocusCWD;
  private Set<SeroType> bLocusCWD;
  private Set<SeroType> cLocusCWD;
  private Set<SeroType> aLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> bLocusFirst = new LinkedHashSet<>();
  private Set<SeroType> cLocusFirst = new LinkedHashSet<>();

  private Set<HLAType> aLocusCWDTypes;
  private Set<HLAType> bLocusCWDTypes;
  private Set<HLAType> cLocusCWDTypes;
  private Set<HLAType> aLocusFirstTypes;
  private Set<HLAType> bLocusFirstTypes;
  private Set<HLAType> cLocusFirstTypes;

  private Map<HLALocus, AllelePairings> possibleAllelePairings = new HashMap<>();
  private Map<HLALocus, AllelePairings> donorAllelePairings = new HashMap<>();

  private Set<HLALocus> nonCWDLoci = new TreeSet<>();
  private Map<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping = new HashMap<>();

  private Set<SeroType> drbLocus;
  private Set<SeroType> drbLocusNonCWD;

  private Set<SeroType> dqaLocus;
  private Set<SeroType> dqaLocusNonCWD;
  private Set<HLAType> dqaLocusAlleles;
  private Set<HLAType> dqaLocusAllelesNonCWD;

  private Set<SeroType> dqbLocus;
  private Set<SeroType> dqbLocusNonCWD;
  private Set<HLAType> dqbLocusAlleles;
  private Set<HLAType> dqbLocusAllelesNonCWD;

  private Set<SeroType> dpaLocus;
  private Set<SeroType> dpaLocusNonCWD;
  private Set<HLAType> dpaLocusAlleles;
  private Set<HLAType> dpaLocusAllelesNonCWD;

  private Set<SeroType> dpbLocus;
  private Set<HLAType> dpbLocusAlleles;
  private Set<HLAType> dpbLocusAllelesNonCWD;
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
  public ValidationModelBuilder file(String filepath) {
    this.filepath = filepath;
    return this;
  }

  /** @param source Name for the source of this model */
  public ValidationModelBuilder source(String source) {
    this.source = source;
    return this;
  }

  /** @param source Name for the source of this model */
  public ValidationModelBuilder sourceType(SourceType sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  /** @param source Name for the source of this model */
  public SourceType getSourceType() {
    return sourceType;
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

  public ValidationModelBuilder aTypeNonCWD(HLAType aType) {
    aLocusFirstTypes = makeIfNull(aLocusFirstTypes);
    addToLocus(aLocusFirstTypes, HLALocus.A, aType);
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

  public ValidationModelBuilder bTypeNonCWD(HLAType bType) {
    bLocusFirstTypes = makeIfNull(bLocusFirstTypes);
    addToLocus(bLocusFirstTypes, HLALocus.B, bType);
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

  public ValidationModelBuilder cTypeNonCWD(HLAType cType) {
    cLocusFirstTypes = makeIfNull(cLocusFirstTypes);
    addToLocus(cLocusFirstTypes, HLALocus.C, cType);
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

  /**
   * DR51 is a HLA-DR serotype that recognizes the antigens encoded by the minor DR locus HLA-DRB5
   * (cc wikipedia)
   * 
   * @param dr51
   * @return
   */
  public ValidationModelBuilder dr51(String dr51) {
    if (isPositive(dr51)) {
      dr51Locus.add(new HLAType(HLALocus.DRB5, dr51));
    }
    return this;
  }

  /**
   * DR52 is an HLA-DR serotype that recognizes gene products of HLA-DRB3 locus (cc wikipedia)
   * 
   * @param dr52
   * @return
   */
  public ValidationModelBuilder dr52(String dr52) {
    if (isPositive(dr52)) {
      dr52Locus.add(new HLAType(HLALocus.DRB3, dr52));
    }
    return this;
  }

  /**
   * DR53 is an HLA-DR serotype that recognizes gene products of HLA-DRB4 (cc wikipedia)
   * 
   * @param dr53
   * @return
   */
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
    // // Shorten the allele designation to allele group and specific HLA protein. Further fields
    // can
    // // not be entered into UNOS
    // if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
    HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
    // if (tmpDPB1.spec().size() > 2) {
    // tmpDPB1 =
    // new HLAType(HLALocus.DPB1, new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
    // }
    dpbLocusNonCWD.add(tmpDPB1);
    // }
    return this;
  }

  public ValidationModelBuilder dqbSerotype(String dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocus = makeIfNull(dqbLocus);
    addToLocus(dqbLocus, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqaSerotype(String dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocus = makeIfNull(dqaLocus);
    addToLocus(dqaLocus, SeroLocus.DQA, dqaType);
    return this;
  }

  public ValidationModelBuilder dpaSerotype(String dpaType) {
    if (test2(dpaType)) {
      return null;
    }
    dpaLocus = makeIfNull(dpaLocus);
    addToLocus(dpaLocus, SeroLocus.DPA, dpaType);
    return this;
  }

  public ValidationModelBuilder dpb(HLAType dpbType) {
    dpbLocusAlleles = makeIfNull(dpbLocusAlleles);
    dpbLocusAlleles.add(dpbType);
    return this;
  }

  public ValidationModelBuilder dpbSerotype(String dpbType) {
    dpbLocus = makeIfNull(dpbLocus);
    HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
    dpbLocus.add(trim(tmpDPB1));

    return this;
  }

  public ValidationModelBuilder dpbNonCIWD(HLAType dpbType) {
    dpbLocusAllelesNonCWD = makeIfNull(dpbLocusAllelesNonCWD);
    dpbLocusAllelesNonCWD.add(dpbType);
    return this;
  }

  public ValidationModelBuilder dqbType(HLAType dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocusAlleles = makeIfNull(dqbLocusAlleles);
    addToLocus(dqbLocusAlleles, HLALocus.DQB1, dqbType);
    return this;
  }

  public ValidationModelBuilder dqbTypeNonCWD(HLAType dqbType) {
    if (test2(dqbType)) {
      return null;
    }
    dqbLocusAllelesNonCWD = makeIfNull(dqbLocusAllelesNonCWD);
    addToLocus(dqbLocusAllelesNonCWD, HLALocus.DQB1, dqbType);
    return this;
  }

  public ValidationModelBuilder dqaType(HLAType dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocusAlleles = makeIfNull(dqaLocusAlleles);
    addToLocus(dqaLocusAlleles, HLALocus.DQA1, dqaType);
    return this;
  }

  public ValidationModelBuilder dqaTypeNonCWD(HLAType dqaType) {
    if (test2(dqaType)) {
      return null;
    }
    dqaLocusAllelesNonCWD = makeIfNull(dqaLocusAllelesNonCWD);
    addToLocus(dqaLocusAllelesNonCWD, HLALocus.DQA1, dqaType);
    return this;
  }

  public ValidationModelBuilder dpaType(HLAType dpaType) {
    if (test2(dpaType)) {
      return null;
    }
    dpaLocusAlleles = makeIfNull(dpaLocusAlleles);
    addToLocus(dpaLocusAlleles, HLALocus.DPA1, dpaType);
    return this;
  }

  public ValidationModelBuilder dpaTypeNonCWD(HLAType dpaType) {
    if (test2(dpaType)) {
      return null;
    }
    dpaLocusAllelesNonCWD = makeIfNull(dpaLocusAllelesNonCWD);
    addToLocus(dpaLocusAllelesNonCWD, HLALocus.DPA1, dpaType);
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

  public class ValidationResult {

    public final boolean valid;
    public final Optional<String> validationMessage;

    public ValidationResult(boolean valid, Optional<String> validationMessage) {
      this.valid = valid;
      this.validationMessage = validationMessage;
    }

  }

  public ValidationResult validate(boolean showAlertIfInvalid) {
    return ensureValidity(showAlertIfInvalid);
  }

  private boolean test(Set<SeroType> set) {
    return set == null || set.isEmpty();
  }

  private boolean test1(Set<HLAType> set) {
    return set == null || set.isEmpty();
  }

  private Set<SeroType> getFinalTypes(HLALocus locus) {
    Set<SeroType> returnTypes;
    switch (locus) {
      case A:
        returnTypes = test(aLocusFirst) ? aLocusCWD : aLocusFirst;
        break;
      case B:
        returnTypes = test(bLocusFirst) ? bLocusCWD : bLocusFirst;
        break;
      case C:
        returnTypes = test(cLocusFirst) ? cLocusCWD : cLocusFirst;
        break;
      case DRB1:
      case DRB3:
      case DRB4:
      case DRB5:
        returnTypes = test(drbLocusNonCWD) ? drbLocus : drbLocusNonCWD;
        break;
      case DQA1:
        returnTypes = test(dqaLocusNonCWD) ? dqaLocus : dqaLocusNonCWD;
        break;
      case DQB1:
        returnTypes = test(dqbLocusNonCWD) ? dqbLocus : dqbLocusNonCWD;
        break;
      case DPA1:
        returnTypes = test(dpaLocusNonCWD) ? dpaLocus : dpaLocusNonCWD;
        break;
      case DPB1:
      case MICA:
      default:
        return null;
    }
    returnTypes = new LinkedHashSet<>(returnTypes);
    if (remapping.containsKey(locus)) {
      List<SeroType> types = remapping.get(locus).getRight().stream().map(TypePair::getSeroType)
          .collect(Collectors.toList());
      returnTypes.clear();
      returnTypes.addAll(types);
    }
    return returnTypes;
  }

  /** @return The immutable {@link ValidationModel} based on the current builder state. */
  public ValidationModel build() {
    Multimap<RaceGroup, Haplotype> bcCwdHaplotypes = buildBCHaplotypes(bHaplotypes, cHaplotypes);

    Multimap<RaceGroup, Haplotype> drDqDR345Haplotypes =
        buildHaplotypes(ImmutableList.of(drb1Haplotypes, dqb1Haplotypes, dr345Haplotypes));

    frequencyTable.clear();

    System.out.println(dr52Locus);

    ValidationModel validationModel = new ValidationModel(donorId, filepath, source, sourceType,
        getFinalTypes(HLALocus.A), getFinalTypes(HLALocus.B), getFinalTypes(HLALocus.C),
        getFinalTypes(HLALocus.DRB1), getFinalTypes(HLALocus.DQB1), getFinalTypes(HLALocus.DQA1),
        getFinalTypes(HLALocus.DPA1), getFinalDPBTypes(), bw4, bw6, dr51Locus, dr52Locus, dr53Locus,
        bcCwdHaplotypes, drDqDR345Haplotypes, remapping);
    return validationModel;
  }

  // Shorten the allele designation to allele group and specific HLA protein. Further fields can
  // not be entered into UNOS
  private Set<HLAType> getFinalDPBTypes() {
    Set<HLAType> dpbTypes = new HashSet<>();
    Set<HLAType> dpbSource = new LinkedHashSet<>();

    Set<HLAType> returnTypes = new LinkedHashSet<>();
    if (remapping.containsKey(HLALocus.DPB1)) {
      List<HLAType> types = remapping.get(HLALocus.DPB1).getRight().stream()
          .map(TypePair::getHlaType).collect(Collectors.toList());
      returnTypes.clear();
      returnTypes.addAll(types);
    }
    if (!returnTypes.isEmpty()) {
      dpbSource.addAll(returnTypes);
    } else {
      dpbSource = test1(dpbLocusAllelesNonCWD) ? dpbLocusAlleles : dpbLocusAllelesNonCWD;
      Set<SeroType> dpbBackup = test1(dpbLocusNonCWD) ? dpbLocus : trim(dpbLocusNonCWD);
      if (dpbSource == null || dpbSource.isEmpty()) {
        dpbSource = dpbBackup.stream().map(s -> new HLAType(HLALocus.DPB1, s.spec()))
            .collect(ImmutableSet.toImmutableSet());
      }
    }
    for (HLAType dpbType1 : dpbSource) {
      String dpbType = dpbType1.specString();
      if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
        HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
        if (tmpDPB1.spec().size() > 2) {
          tmpDPB1 =
              new HLAType(HLALocus.DPB1, new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
        }
        dpbTypes.add(tmpDPB1);
      }
    }
    return dpbTypes;
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
          int d = Double.compare(CommonWellDocumented.getEquivStatus(h2).getWeight(),
              CommonWellDocumented.getEquivStatus(h1).getWeight());
          if (d != 0)
            return d;
          return h2.compareTo(h1);
        });
        firstStrandTypes.sort(c);
        secondStrandTypes.sort(c);

        double bestCWD;
        int i;

        if (!firstStrandTypes.isEmpty()) {
          bestCWD = CommonWellDocumented.getEquivStatus(firstStrandTypes.get(0)).getWeight();
          i = 1;
          while (i < firstStrandTypes.size() && CommonWellDocumented
              .getEquivStatus(firstStrandTypes.get(i)).getWeight() == bestCWD) {
            i++;
          }
          for (int j = firstStrandTypes.size() - 1; j >= i; j--) {
            firstStrandTypes.remove(j);
          }
        }

        if (!secondStrandTypes.isEmpty()) {
          bestCWD = CommonWellDocumented.getEquivStatus(secondStrandTypes.get(0)).getWeight();
          i = 1;
          while (i < secondStrandTypes.size() && CommonWellDocumented
              .getEquivStatus(secondStrandTypes.get(i)).getWeight() == bestCWD) {
            i++;
          }
          for (int j = secondStrandTypes.size() - 1; j >= i; j--) {
            secondStrandTypes.remove(j);
          }
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
  private ValidationResult ensureValidity(boolean show) {
    // Ensure all fields have been set
    for (Object o : Lists.newArrayList(donorId, source, aLocusCWD, bLocusCWD, cLocusCWD, drbLocus,
        dqbLocus, dqaLocus, getFinalDPBTypes(), bw4, bw6)) {
      if (Objects.isNull(o)) {
        return new ValidationResult(false, Optional.of("ValidationModel incomplete"));
      }
    }
    // Ensure all sets have a reasonable number of entries
    for (Set<?> set : ImmutableList.of(aLocusCWD, bLocusCWD, cLocusCWD, drbLocus, dqbLocus,
        dqaLocus, getFinalDPBTypes())) {
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

    return new ValidationResult(true, Optional.empty());
  }

  public boolean hasCorrections() {
    return !nonCWDLoci.isEmpty();
  }

  public ValidationResult processCorrections(RemapProcessor remapProcessor) {

    boolean cancelled = false;

    for (HLALocus locus : nonCWDLoci) {
      Pair<Set<TypePair>, Set<TypePair>> remapPair = null;

      try {
        remapPair = remapProcessor.processRemapping(locus, this);
      } catch (CancellationException e) {
        cancelled = true;
        break;
      }

      if (remapPair != null) {
        remapping.put(locus, remapPair);
      }
    }

    if (cancelled) {
      return new ValidationResult(false, Optional.empty());
    }

    return new ValidationResult(true, Optional.empty());
  }

  public Set<SeroType> getCWDSeroTypesForLocus(HLALocus locus) {

    switch (locus) {
      case A:
        return aLocusCWD;
      case B:
        return bLocusCWD;
      case C:
        return cLocusCWD;
      case DPA1:
        return dpaLocus;
      case DQA1:
        return dqaLocus;
      case DPB1:
        return dpbLocus;
      case DQB1:
        return dqbLocus;
      case DRB1:
      case DRB3:
      case DRB4:
      case DRB5:
        return drbLocus;
      default:
        return null;
    }

  }

  public Set<SeroType> getAllSeroTypesForLocus(HLALocus locus) {

    switch (locus) {
      case A:
        return aLocusFirst;
      case B:
        return bLocusFirst;
      case C:
        return cLocusFirst;
      case DPA1:
        return dpaLocusNonCWD;
      case DQA1:
        return dqaLocusNonCWD;
      case DPB1:
        return trim(dpbLocusNonCWD);
      case DQB1:
        return dqbLocusNonCWD;
      case DRB1:
      case DRB3:
      case DRB4:
      case DRB5:
        return drbLocusNonCWD;
      default:
        return null;
    }

  }

  private Set<SeroType> trim(Set<HLAType> set) {
    return set.stream().map(this::trim).collect(Collectors.toCollection(HashSet::new));
  }

  private SeroType trim(HLAType hlaType) {
    List<String> seroStr = new ArrayList<>();
    seroStr.add(String.valueOf(hlaType.spec().get(0)));

    if (Objects.equals(HLALocus.DPA1, hlaType.locus())
        || Objects.equals(HLALocus.DPB1, hlaType.locus())) {
      // DPA and DPB report two fields
      seroStr.add(String.valueOf(hlaType.spec().get(1)));
    }

    String[] array = seroStr.toArray(new String[seroStr.size()]);
    return new SeroType(hlaType.locus().sero(), array);
  }

  public Set<HLAType> getCWDTypesForLocus(HLALocus locus) {

    switch (locus) {
      case A:
        return aLocusCWDTypes;
      case B:
        return bLocusCWDTypes;
      case C:
        return cLocusCWDTypes;
      case DPA1:
        return dpaLocusAlleles;
      case DQA1:
        return dqaLocusAlleles;
      case DPB1:
        return dpbLocusAlleles;
      case DQB1:
        return dqbLocusAlleles;
      case DRB1:
      case DRB3:
      case DRB4:
      case DRB5:
      default:
        return null;
    }

  }

  public Set<HLAType> getAllTypesForLocus(HLALocus locus) {
    switch (locus) {
      case A:
        return aLocusFirstTypes;
      case B:
        return bLocusFirstTypes;
      case C:
        return cLocusFirstTypes;
      case DPA1:
        return dpaLocusAllelesNonCWD;
      case DQA1:
        return dqaLocusAllelesNonCWD;
      case DPB1:
        return dpbLocusAllelesNonCWD;
      case DQB1:
        return dqbLocusAllelesNonCWD;
      case DRB1:
      case DRB3:
      case DRB4:
      case DRB5:
      case MICA:
      default:
        return null;
    }
  }

  public AllelePairings getPossibleAllelePairsForLocus(HLALocus locus) {
    return possibleAllelePairings.get(locus);
  }

  public AllelePairings getDonorAllelePairsForLocus(HLALocus locus) {
    return donorAllelePairings.get(locus);
  }

  public AllelePairings getAllelePairs(HLALocus locus) {
    return possibleAllelePairings.get(locus);
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

    public Set<String> getAlleleKeys() {
      return map.keySet();
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

  public Set<HLALocus> getNonCWDLoci() {
    return nonCWDLoci;
  }
}
