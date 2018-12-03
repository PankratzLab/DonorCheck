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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroLocus;
import org.pankratzlab.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies.Ethnicity;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
  private Set<HLAType> dr51Locus;
  private Set<HLAType> dr52Locus;
  private Set<HLAType> dr53Locus;
  private Multimap<Strand, HLAType> bHaplotypes;
  private Map<Strand, BwGroup> bwHaplotypes;
  private Multimap<Strand, HLAType> cHaplotypes;
  private Multimap<Strand, HLAType> drHaplotypes;
  private Multimap<Strand, HLAType> dqHaplotypes;

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
    addToLocus(aLocus, SeroLocus.DQB, aType);
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    bLocus = makeIfNull(bLocus);
    addToLocus(bLocus, SeroLocus.DQB, bType);
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    cLocus = makeIfNull(cLocus);
    addToLocus(cLocus, SeroLocus.DQB, cType);
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    drbLocus = makeIfNull(drbLocus);
    addToLocus(drbLocus, SeroLocus.DRB, drbType);
    return this;
  }

  public ValidationModelBuilder dr51(String dr51) {
    dr51Locus = makeIfNull(dr51Locus);
    dr51Locus.add(new HLAType(HLALocus.DRB5, dr51));
    return this;
  }

  public ValidationModelBuilder dr52(String dr52) {
    dr52Locus = makeIfNull(dr52Locus);
    dr52Locus.add(new HLAType(HLALocus.DRB3, dr52));
    return this;
  }

  public ValidationModelBuilder dr53(String dr53) {
    dr53Locus = makeIfNull(dr53Locus);
    dr53Locus.add(new HLAType(HLALocus.DRB4, dr53));
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
    bHaplotypes = makeIfNull(bHaplotypes);
    bHaplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder bwHaplotype(Map<Strand, BwGroup> bwMap) {
    bwHaplotypes = makeIfNull(bwHaplotypes);
    bwHaplotypes.putAll(bwMap);
    return this;
  }

  public ValidationModelBuilder cHaplotype(Multimap<Strand, HLAType> types) {
    cHaplotypes = makeIfNull(cHaplotypes);
    cHaplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder drHaplotype(Multimap<Strand, HLAType> types) {
    drHaplotypes = makeIfNull(drHaplotypes);
    drHaplotypes.putAll(types);
    return this;
  }

  public ValidationModelBuilder dqHaplotype(Multimap<Strand, HLAType> types) {
    dqHaplotypes = makeIfNull(dqHaplotypes);
    dqHaplotypes.putAll(types);
    return this;
  }

  /**
   * @return The immutable {@link ValidationModel} based on the current builder state.
   */
  public ValidationModel build() {
    ensureValidity();
    Multimap<Ethnicity, Haplotype> bcCwdHaplotypes =
        buildCwdHaplotypes(makeIfNull(bHaplotypes), makeIfNull(cHaplotypes));
    Multimap<Ethnicity, Haplotype> drdqCwdHaplotypes =
        buildCwdHaplotypes(makeIfNull(drHaplotypes), makeIfNull(dqHaplotypes));
    Map<HLAType, BwGroup> bwAlleleMap = makeBwAlleleMap();
    return new ValidationModel(donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus,
        dqaLocus, dpbLocus, bw4, bw6, dr51Locus, dr52Locus, dr53Locus, bcCwdHaplotypes, drdqCwdHaplotypes,
        bwAlleleMap);
  }

  /**
   * @return Map all the possible alleles to their {@link BwGroup} (based on {@link Strand})
   */
  private Map<HLAType, BwGroup> makeBwAlleleMap() {
    Map<HLAType, BwGroup> bwMap = new HashMap<>();
    if (Objects.nonNull(bHaplotypes) && Objects.nonNull(bwHaplotypes)) {
      for (Strand strand : bHaplotypes.keySet()) {
        BwGroup bw = bwHaplotypes.get(strand);
        for (HLAType allele : bHaplotypes.get(strand)) {
          bwMap.put(allele, bw);
        }
      }
    }
    return bwMap;
  }

  /**
   * @return A table of the highest-probability haplotypes for each ethnicity
   */
  private Multimap<Ethnicity, Haplotype> buildCwdHaplotypes(Multimap<Strand, HLAType> locusOneTypes,
      Multimap<Strand, HLAType> locusTwoTypes) {
    Multimap<Ethnicity, Haplotype> haplotypesByEthnicity = HashMultimap.create();

    if (!(locusOneTypes.isEmpty() && locusTwoTypes.isEmpty())) {

      prune(locusOneTypes);
      prune(locusTwoTypes);

      // for each ethnicity
      for (Ethnicity ethnicity : Ethnicity.values()) {
        Table<Strand, Strand, Set<Haplotype>> optionsByStrand = HashBasedTable.create();

        // Iterate over each strand of each locus
        for (Strand strandLocusOne : Strand.values()) {
          for (Strand strandLocusTwo : Strand.values()) {
            Set<Haplotype> hapSet = new HashSet<>();

            // Add all possible haplotypes for these strands to a TreeSet
            for (HLAType t1 : locusOneTypes.get(strandLocusOne)) {
              for (HLAType t2 : locusTwoTypes.get(strandLocusTwo)) {
                hapSet.add(new Haplotype(t1, t2));
              }
            }

            // Add the winning haplotype to the haploTable
            optionsByStrand.put(strandLocusOne, strandLocusTwo, hapSet);
          }
        }

        TreeSet<Collection<Haplotype>> haplotypeSets =
            new TreeSet<>(new EthnicityHaplotypeComp(ethnicity));

        // Compute the most likely strand combinations
        // Each strand value must be used once and only once in both row and column indices
        addHaplotypes(haplotypeSets, optionsByStrand.get(Strand.FIRST, Strand.FIRST),
            optionsByStrand.get(Strand.SECOND, Strand.SECOND));

        addHaplotypes(haplotypeSets, optionsByStrand.get(Strand.FIRST, Strand.SECOND),
            optionsByStrand.get(Strand.SECOND, Strand.FIRST));

        // Whichever Haplotype pairing of haplotypes had the highest probability gets recorded for
        // this ethnicity
        for (Haplotype t : haplotypeSets.first()) {
          haplotypesByEthnicity.put(ethnicity, t);
        }
      }
    }

    return haplotypesByEthnicity;
  }

  /**
   * Add all combinations of haplotypes in set 1 and set 2 to the base set
   */
  private void addHaplotypes(Set<Collection<Haplotype>> baseSet, Set<Haplotype> setOne,
      Set<Haplotype> setTwo) {
    setOne.forEach(h1 -> {
      setTwo.forEach(h2 -> {
        baseSet.add(ImmutableSet.of(h1, h2));
      });
    });
  }

  /**
   * Filter out {@link Status#UNKNOWN} types and eliminate redundant strands
   */
  private void prune(Multimap<Strand, HLAType> typesForStrand) {
    if (typesForStrand.isEmpty()) {
      return;
    }

    // Homozygous
    if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
      typesForStrand.removeAll(Strand.SECOND);
    }

    // Remove UNKNOWN types
    Iterator<HLAType> iterator = typesForStrand.values().iterator();
    while (iterator.hasNext()) {
      HLAType type = iterator.next();
      if (Objects.equals(Status.UNKNOWN, CommonWellDocumented.getStatus(type))) {
        iterator.remove();
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
        dqaLocus, dpbLocus, bw4, bw6, dr51Locus, dr52Locus, dr53Locus)) {
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
  }

  /**
   * Helper method to build a multimap if it's null
   */
  private Multimap<Strand, HLAType> makeIfNull(Multimap<Strand, HLAType> hapMap) {
    if (hapMap == null) {
      hapMap = HashMultimap.create();
    }
    return hapMap;
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
   * {@link Comparator} to sort collections of {@link Haplotype}s based on their frequency and the
   * CWD status of their alleles.
   */
  private static class EthnicityHaplotypeComp implements Comparator<Collection<Haplotype>> {
    private static final int NO_MISSING_WEIGHT = 10;
    private Ethnicity ethnicity;

    private EthnicityHaplotypeComp(Ethnicity e) {
      this.ethnicity = e;
    }

    @Override
    public int compare(Collection<Haplotype> o1, Collection<Haplotype> o2) {
      double p1 = getScore(ethnicity, o1);
      double p2 = getScore(ethnicity, o2);

      // Prefer larger frequencies for this ethnicity
      int result = Double.compare(p2, p1);

      if (result == 0) {
        // If the scores are the same, we do compare the unique HLATypes between these two
        List<HLAType> t1 = makeList(o1);
        List<HLAType> t2 = makeList(o2);
        removeOverlapAndSort(t1, t2);
        for (int i = 0; i < t1.size(); i++) {
          result += t1.get(i).compareTo(t2.get(i));
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
      t1.retainAll(t2);
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

    /**
     * @return The combined frequency of the given haplotypes for the specified ethnicity
     * 
     * @see #getScore(Ethnicity, Haplotype)
     */
    public static double getScore(Ethnicity ethnicity, Collection<Haplotype> haplotypes) {
      double noMissingBonus = haplotypes.size();
      double cwdBonus = 2 * haplotypes.size();

      double frequency = 1.0;
      for (Haplotype type : haplotypes) {
        double f = HaplotypeFrequencies.getFrequency(ethnicity, type);

        if (f > 0) {
          frequency *= f;
        } else {
          // penalize mising haplotypes
          noMissingBonus--;
        }

        for (HLAType allele : type.getTypes()) {
          switch (CommonWellDocumented.getStatus(allele)) {
            case UNKNOWN:
              // Remove one point for unknown alleles
              cwdBonus--;
              break;
            case WELL_DOCUMENTED:
              // Remove half a point for well-documented alleles
              cwdBonus -= 0.5;
              break;
            case COMMON:
            default:
              break;
          }
        }
      }

      // The bonuses ensure that haplotypes with no missing frequencies are prioritized, while
      // allowing comparison between unknown and well-documented alleles/haplotypes.
      return (NO_MISSING_WEIGHT * noMissingBonus) + cwdBonus + frequency;
    }
  }
}
