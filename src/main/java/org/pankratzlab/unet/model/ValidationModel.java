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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;

/**
 * Backing, immutable model representing a single donor typing.
 *
 * @see ValidationModelBuilder
 */
public class ValidationModel {

  private final String donorId;
  private final String source;
  private final String sourceType;
  private final ImmutableSortedSet<SeroType> aLocus;
  private final ImmutableSortedSet<SeroType> bLocus;
  private final ImmutableSortedSet<SeroType> cLocus;
  private final ImmutableSortedSet<SeroType> drbLocus;
  private final ImmutableSortedSet<SeroType> dqbLocus;
  private final ImmutableSortedSet<SeroType> dqaLocus;
  private final ImmutableSortedSet<SeroType> dpaLocus;
  private final ImmutableSortedSet<HLAType> dpbLocus;
  private final boolean bw4;
  private final boolean bw6;
  private final ImmutableList<HLAType> dr51Locus;
  private final ImmutableList<HLAType> dr52Locus;
  private final ImmutableList<HLAType> dr53Locus;
  private final ImmutableMultimap<RaceGroup, Haplotype> bcHaplotypes;
  private final ImmutableMultimap<RaceGroup, Haplotype> drdqHaplotypes;
  private final ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping;

  public ValidationModel(String donorId, String source, String sourceType, Collection<SeroType> a,
      Collection<SeroType> b, Collection<SeroType> c, Collection<SeroType> drb,
      Collection<SeroType> dqb, Collection<SeroType> dqa, Collection<SeroType> dpa,
      Collection<HLAType> dpb, boolean bw4, boolean bw6, List<HLAType> dr51, List<HLAType> dr52,
      List<HLAType> dr53, Multimap<RaceGroup, Haplotype> bcCwdHaplotypes,
      Multimap<RaceGroup, Haplotype> drdqCwdHaplotypes,
      Map<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping) {
    this.donorId = donorId;
    this.source = source;
    this.sourceType = sourceType;
    aLocus = ImmutableSortedSet.copyOf(a);
    bLocus = ImmutableSortedSet.copyOf(b);
    cLocus = ImmutableSortedSet.copyOf(c);
    drbLocus = ImmutableSortedSet.copyOf(drb);
    dqbLocus = ImmutableSortedSet.copyOf(dqb);
    dqaLocus = ImmutableSortedSet.copyOf(dqa);
    if (Objects.nonNull(dpa)) {
      dpaLocus = ImmutableSortedSet.copyOf(dpa);
    } else {
      dpaLocus = null;
    }
    dpbLocus = ImmutableSortedSet.copyOf(dpb);
    this.bw4 = bw4;
    this.bw6 = bw6;

    dr51Locus = ImmutableList.copyOf(dr51);
    dr52Locus = ImmutableList.copyOf(dr52);
    dr53Locus = ImmutableList.copyOf(dr53);

    bcHaplotypes = ImmutableMultimap.copyOf(bcCwdHaplotypes);
    drdqHaplotypes = ImmutableMultimap.copyOf(drdqCwdHaplotypes);

    this.remapping = ImmutableMap.copyOf(remapping);
  }

  public String getDonorId() {
    return donorId;
  }

  public String getSource() {
    return source;
  }

  public String getSourceType() {
    return sourceType;
  }

  public SeroType getA1() {
    return getFromPair(aLocus, 0);
  }

  public SeroType getA2() {
    return getFromPair(aLocus, 1);
  }

  public SeroType getB1() {
    return getFromPair(bLocus, 0);
  }

  public SeroType getB2() {
    return getFromPair(bLocus, 1);
  }

  public SeroType getC1() {
    return getFromPair(cLocus, 0);
  }

  public SeroType getC2() {
    return getFromPair(cLocus, 1);
  }

  public SeroType getDRB1() {
    return getFromPair(drbLocus, 0);
  }

  public SeroType getDRB2() {
    return getFromPair(drbLocus, 1);
  }

  public SeroType getDQB1() {
    return getFromPair(dqbLocus, 0);
  }

  public SeroType getDQB2() {
    return getFromPair(dqbLocus, 1);
  }

  public SeroType getDQA1() {
    return getFromPair(dqaLocus, 0);
  }

  public SeroType getDQA2() {
    return getFromPair(dqaLocus, 1);
  }

  public SeroType getDPA1() {
    return getFromPair(dpaLocus, 0);
  }

  public SeroType getDPA2() {
    return getFromPair(dpaLocus, 1);
  }

  public HLAType getDPB1() {
    return getFromPair(dpbLocus, 0);
  }

  public HLAType getDPB2() {
    return getFromPair(dpbLocus, 1);
  }

  public String isBw4() {
    return inGroupString(bw4);
  }

  public String isBw6() {
    return inGroupString(bw6);
  }

  public HLAType getDR51_1() {
    return getFromList(dr51Locus, 0);
  }

  public HLAType getDR51_2() {
    return getFromList(dr51Locus, 1);
  }

  public HLAType getDR52_1() {
    return getFromList(dr52Locus, 0);
  }

  public HLAType getDR52_2() {
    return getFromList(dr52Locus, 1);
  }

  public HLAType getDR53_1() {
    return getFromList(dr53Locus, 0);
  }

  public HLAType getDR53_2() {
    return getFromList(dr53Locus, 1);
  }

  public ImmutableMultimap<RaceGroup, Haplotype> getBCHaplotypes() {
    return bcHaplotypes;
  }

  public ImmutableMultimap<RaceGroup, Haplotype> getDRDQHaplotypes() {
    return drdqHaplotypes;
  }

  private String inGroupString(boolean group) {
    return group ? "Positive" : "Negative";
  }

  private <T> T getFromPair(ImmutableSortedSet<T> set, int index) {
    if (Objects.isNull(set) || set.size() <= index) {
      return null;
    }
    return index == 0 ? set.first() : set.last();
  }

  private <T> T getFromList(ImmutableList<T> list, int index) {
    if (list.size() <= index) {
      return null;
    }
    return list.get(index);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((aLocus == null) ? 0 : aLocus.hashCode());
    result = prime * result + ((bLocus == null) ? 0 : bLocus.hashCode());
    result = prime * result + ((bcHaplotypes == null) ? 0 : bcHaplotypes.hashCode());
    result = prime * result + (bw4 ? 1231 : 1237);
    result = prime * result + (bw6 ? 1231 : 1237);
    result = prime * result + ((cLocus == null) ? 0 : cLocus.hashCode());
    result = prime * result + ((donorId == null) ? 0 : donorId.hashCode());
    result = prime * result + ((dpbLocus == null) ? 0 : dpbLocus.hashCode());
    result = prime * result + ((dqaLocus == null) ? 0 : dqaLocus.hashCode());
    result = prime * result + ((dpaLocus == null) ? 0 : dpaLocus.hashCode());
    result = prime * result + ((dqbLocus == null) ? 0 : dqbLocus.hashCode());
    result = prime * result + ((dr51Locus == null) ? 0 : dr51Locus.hashCode());
    result = prime * result + ((dr52Locus == null) ? 0 : dr52Locus.hashCode());
    result = prime * result + ((dr53Locus == null) ? 0 : dr53Locus.hashCode());
    result = prime * result + ((drbLocus == null) ? 0 : drbLocus.hashCode());
    result = prime * result + ((drdqHaplotypes == null) ? 0 : drdqHaplotypes.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ValidationModel other = (ValidationModel) obj;
    if (aLocus == null) {
      if (other.aLocus != null)
        return false;
    } else if (!aLocus.equals(other.aLocus))
      return false;
    if (bLocus == null) {
      if (other.bLocus != null)
        return false;
    } else if (!bLocus.equals(other.bLocus))
      return false;
    if (bcHaplotypes == null) {
      if (other.bcHaplotypes != null)
        return false;
    } else if (!bcHaplotypes.equals(other.bcHaplotypes))
      return false;
    if (bw4 != other.bw4)
      return false;
    if (bw6 != other.bw6)
      return false;
    if (cLocus == null) {
      if (other.cLocus != null)
        return false;
    } else if (!cLocus.equals(other.cLocus))
      return false;
    if (donorId == null) {
      if (other.donorId != null)
        return false;
    } else if (!donorId.equals(other.donorId))
      return false;
    if (dpbLocus == null) {
      if (other.dpbLocus != null)
        return false;
    } else if (!dpbLocus.equals(other.dpbLocus))
      return false;
    if (dpaLocus == null) {
      if (other.dpaLocus != null)
        return false;
    } else if (!dpaLocus.equals(other.dpaLocus))
      return false;
    if (dqaLocus == null) {
      if (other.dqaLocus != null)
        return false;
    } else if (!dqaLocus.equals(other.dqaLocus))
      return false;
    if (dqbLocus == null) {
      if (other.dqbLocus != null)
        return false;
    } else if (!dqbLocus.equals(other.dqbLocus))
      return false;
    if (dr51Locus == null) {
      if (other.dr51Locus != null)
        return false;
    } else if (!dr51Locus.equals(other.dr51Locus))
      return false;
    if (dr52Locus == null) {
      if (other.dr52Locus != null)
        return false;
    } else if (!dr52Locus.equals(other.dr52Locus))
      return false;
    if (dr53Locus == null) {
      if (other.dr53Locus != null)
        return false;
    } else if (!dr53Locus.equals(other.dr53Locus))
      return false;
    if (drbLocus == null) {
      if (other.drbLocus != null)
        return false;
    } else if (!drbLocus.equals(other.drbLocus))
      return false;
    if (drdqHaplotypes == null) {
      if (other.drdqHaplotypes != null)
        return false;
    } else if (!drdqHaplotypes.equals(other.drdqHaplotypes))
      return false;
    if (source == null) {
      if (other.source != null)
        return false;
    } else if (!source.equals(other.source))
      return false;
    return true;
  }

  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner("\n");
    sj.add(getDonorId());
    sj.add(getSource());
    sj.add(getSourceType());
    addPair(sj, getA1(), getA2());
    addPair(sj, getB1(), getB2());
    addPair(sj, getC1(), getC2());
    addPair(sj, getDRB1(), getDRB2());
    addPair(sj, getDQB1(), getDQB2());
    addPair(sj, getDQA1(), getDQA2());
    addPair(sj, getDPA1(), getDPA2());
    addPair(sj, getDPB1(), getDPB2());
    sj.add("Bw4: " + isBw4());
    sj.add("Bw6: " + isBw6());
    addPair(sj, getDR51_1(), getDR51_2(), "DR51");
    addPair(sj, getDR52_1(), getDR52_2(), "DR52");
    addPair(sj, getDR53_1(), getDR53_2(), "DR53");
    addHaplotypes(sj, getBCHaplotypes(), "B-C Haplotype");
    addHaplotypes(sj, getDRDQHaplotypes(), "DR-DQ Haplotype");

    return sj.toString();
  }

  private void addHaplotypes(StringJoiner sj, ImmutableMultimap<RaceGroup, Haplotype> haplotypes,
      String title) {
    sj.add(title);
    for (RaceGroup e : RaceGroup.values()) {
      sj.add("\t" + e.toString());
      List<String> hapStrings =
          haplotypes.get(e).stream().map(Haplotype::toString).sorted().collect(Collectors.toList());
      hapStrings.forEach(s -> sj.add("\t" + s));
    }
  }

  private void addPair(StringJoiner sj, Object s1, Object s2) {
    addPair(sj, s1, s2, "");
  }

  private void addPair(StringJoiner sj, Object s1, Object s2, String prefix) {
    String pair = "";
    if (!prefix.isEmpty()) {
      pair = prefix + ": ";
    }

    if (Objects.isNull(s1) && Objects.isNull(s2)) {
      pair = pair + "-";
    }
    if (Objects.nonNull(s1) && Objects.isNull(s2)) {
      pair = pair + s1.toString();
    }
    if (Objects.isNull(s1) && Objects.nonNull(s2)) {
      pair = pair + s2.toString();
    }
    if (Objects.nonNull(s1) && Objects.nonNull(s2)) {
      pair = pair + s1.toString() + " - " + s2.toString();
    }
    sj.add(pair);
  }

  public boolean wasRemapped(HLALocus locus) {
    return remapping.containsKey(locus);
  }

  public String[] getRemappings(int i) {

    return remapping.entrySet().stream().map(e -> {
      final String collectFrom =
          e.getValue().getLeft().stream().sorted().map(TypePair::getHlaType).map((h) -> {
            return h.specString() + " - " + CommonWellDocumented.getStatus(h);
          }).collect(Collectors.joining(" / "));
      final String collectTo =
          e.getValue().getRight().stream().sorted().map(TypePair::getHlaType).map((h) -> {
            return h.specString() + " - " + CommonWellDocumented.getStatus(h);
          }).collect(Collectors.joining(" / "));
      return "HLA-" + e.getKey().name() + " was remapped from { " + collectFrom + " } to { "
          + collectTo + " } in " + (i == 0 ? "left" : "right") + " model";
    }).sorted().distinct().toArray(String[]::new);
  }
}
