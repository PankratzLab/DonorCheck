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
package org.pankratzlab.unet.hapstats;

import java.util.Collection;
import java.util.SortedSet;
import java.util.StringJoiner;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * A {@link Haplotype} is a collection of types at different loci which co-occur together.
 */
public class Haplotype {

  private final ImmutableSortedSet<HLAType> types;

  public Haplotype(Collection<HLAType> types) {
    if (types.size() < 2) {
      throw new IllegalStateException("Invalid haplotype: " + types.toString());
    }
    this.types = ImmutableSortedSet.copyOf(types);
  }

  public Haplotype(HLAType... types) {
    this(ImmutableSet.copyOf(types));
  }

  /**
   * @return The {@link HLAType}s linked by this haplotype
   */
  public SortedSet<HLAType> getTypes() {
    return types;
  }

  /**
   * @return A short representation string
   */
  public String toShortString() {
    StringJoiner sj = new StringJoiner(" + ");
    types.forEach(t -> sj.add(t.toString()));
    return sj.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((types == null) ? 0 : types.hashCode());
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
    Haplotype other = (Haplotype) obj;
    if (types == null) {
      if (other.types != null)
        return false;
    } else if (!types.equals(other.types))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Haplotype [types=" + types + "]";
  }
}
