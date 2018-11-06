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
import org.pankratzlab.hla.type.HLAType;
import org.pankratzlab.hla.type.SeroType;
import com.google.common.collect.ImmutableSortedSet;

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
  private final boolean dr51;
  private final boolean dr52;
  private final boolean dr53;

  public ValidationModel(String donorId, String source, Collection<SeroType> a,
      Collection<SeroType> b, Collection<SeroType> c, Collection<SeroType> drb,
      Collection<SeroType> dqb, Collection<SeroType> dqa, Collection<HLAType> dpb, boolean bw4,
      boolean bw6, boolean dr51, boolean dr52, boolean dr53) {
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
    this.dr51 = dr51;
    this.dr52 = dr52;
    this.dr53 = dr53;
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

  public String isDr51() {
    return inGroupString(dr51);
  }

  public String isDr52() {
    return inGroupString(dr52);
  }

  public String isDr53() {
    return inGroupString(dr53);
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
    result = prime * result + (bw4 ? 1231 : 1237);
    result = prime * result + (bw6 ? 1231 : 1237);
    result = prime * result + ((cLocus == null) ? 0 : cLocus.hashCode());
    result = prime * result + ((donorId == null) ? 0 : donorId.hashCode());
    result = prime * result + ((dpbLocus == null) ? 0 : dpbLocus.hashCode());
    result = prime * result + ((dqaLocus == null) ? 0 : dqaLocus.hashCode());
    result = prime * result + ((dqbLocus == null) ? 0 : dqbLocus.hashCode());
    result = prime * result + (dr51 ? 1231 : 1237);
    result = prime * result + (dr52 ? 1231 : 1237);
    result = prime * result + (dr53 ? 1231 : 1237);
    result = prime * result + ((drbLocus == null) ? 0 : drbLocus.hashCode());
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
    if (dr51 != other.dr51)
      return false;
    if (dr52 != other.dr52)
      return false;
    if (dr53 != other.dr53)
      return false;
    if (drbLocus == null) {
      if (other.drbLocus != null)
        return false;
    } else if (!drbLocus.equals(other.drbLocus))
      return false;
    return true;
  }

}
