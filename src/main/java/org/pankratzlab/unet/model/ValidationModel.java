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

import java.util.Collection;
import java.util.Map;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroType;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies.Ethnicity;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
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
  private final ImmutableSortedSet<SeroType> aLocus;
  private final ImmutableSortedSet<SeroType> bLocus;
  private final ImmutableSortedSet<SeroType> cLocus;
  private final ImmutableSortedSet<SeroType> drbLocus;
  private final ImmutableSortedSet<SeroType> dqbLocus;
  private final ImmutableSortedSet<SeroType> dqaLocus;
  private final ImmutableSortedSet<HLAType> dpbLocus;
  private final boolean bw4;
  private final boolean bw6;
  private final ImmutableSortedSet<HLAType> dr51Locus;
  private final ImmutableSortedSet<HLAType> dr52Locus;
  private final ImmutableSortedSet<HLAType> dr53Locus;
  private final ImmutableMultimap<Ethnicity, Haplotype> bcHaplotypes;
  private final ImmutableMultimap<Ethnicity, Haplotype> drdqHaplotypes;
  private final ImmutableMap<HLAType, BwGroup> bwMap;

  public ValidationModel(String donorId, String source, Collection<SeroType> a,
      Collection<SeroType> b, Collection<SeroType> c, Collection<SeroType> drb,
      Collection<SeroType> dqb, Collection<SeroType> dqa, Collection<HLAType> dpb, boolean bw4,
      boolean bw6, Collection<HLAType> dr51, Collection<HLAType> dr52, Collection<HLAType> dr53,
      Multimap<Ethnicity, Haplotype> bcCwdHaplotypes,
      Multimap<Ethnicity, Haplotype> drdqCwdHaplotypes, Map<HLAType, BwGroup> bwAlleleMap) {
    this.donorId = donorId;
    this.source = source;
    aLocus = ImmutableSortedSet.copyOf(a);
    bLocus = ImmutableSortedSet.copyOf(b);
    cLocus = ImmutableSortedSet.copyOf(c);
    drbLocus = ImmutableSortedSet.copyOf(drb);
    dqbLocus = ImmutableSortedSet.copyOf(dqb);
    dqaLocus = ImmutableSortedSet.copyOf(dqa);
    dpbLocus = ImmutableSortedSet.copyOf(dpb);
    this.bw4 = bw4;
    this.bw6 = bw6;
    dr51Locus = ImmutableSortedSet.copyOf(dr51);
    dr52Locus = ImmutableSortedSet.copyOf(dr52);
    dr53Locus = ImmutableSortedSet.copyOf(dr53);
    bcHaplotypes = ImmutableMultimap.copyOf(bcCwdHaplotypes);
    drdqHaplotypes = ImmutableMultimap.copyOf(drdqCwdHaplotypes);
    bwMap = ImmutableMap.copyOf(bwAlleleMap);
  }

  public String getDonorId() {
    return donorId;
  }

  public String getSource() {
    return source;
  }

  public SeroType getA1() {
    return getFromList(aLocus, 0);
  }

  public SeroType getA2() {
    return getFromList(aLocus, 1);
  }

  public SeroType getB1() {
    return getFromList(bLocus, 0);
  }

  public SeroType getB2() {
    return getFromList(bLocus, 1);
  }

  public SeroType getC1() {
    return getFromList(cLocus, 0);
  }

  public SeroType getC2() {
    return getFromList(cLocus, 1);
  }

  public SeroType getDRB1() {
    return getFromList(drbLocus, 0);
  }

  public SeroType getDRB2() {
    return getFromList(drbLocus, 1);
  }

  public SeroType getDQB1() {
    return getFromList(dqbLocus, 0);
  }

  public SeroType getDQB2() {
    return getFromList(dqbLocus, 1);
  }

  public SeroType getDQA1() {
    return getFromList(dqaLocus, 0);
  }

  public SeroType getDQA2() {
    return getFromList(dqaLocus, 1);
  }

  public HLAType getDPB1() {
    return getFromList(dpbLocus, 0);
  }

  public HLAType getDPB2() {
    return getFromList(dpbLocus, 1);
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

  public ImmutableMultimap<Ethnicity, Haplotype> getBCHaplotypes() {
    return bcHaplotypes;
  }

  public ImmutableMultimap<Ethnicity, Haplotype> getDRDQHaplotypes() {
    return drdqHaplotypes;
  }

  public ImmutableMap<HLAType, BwGroup> getBwMap() {
    return bwMap;
  }

  private String inGroupString(boolean group) {
    return group ? "Positive" : "Negative";
  }

  private <T> T getFromList(ImmutableSortedSet<T> set, int index) {
    if (set.size() <= index) {
      return null;
    }
    return index == 0 ? set.first() : set.last();
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
    result = prime * result + ((bwMap == null) ? 0 : bwMap.hashCode());
    result = prime * result + ((cLocus == null) ? 0 : cLocus.hashCode());
    result = prime * result + ((donorId == null) ? 0 : donorId.hashCode());
    result = prime * result + ((dpbLocus == null) ? 0 : dpbLocus.hashCode());
    result = prime * result + ((dqaLocus == null) ? 0 : dqaLocus.hashCode());
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
    if (bwMap == null) {
      if (other.bwMap != null)
        return false;
    } else if (!bwMap.equals(other.bwMap))
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

}
