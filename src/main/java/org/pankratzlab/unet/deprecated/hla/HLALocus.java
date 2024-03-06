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
package org.pankratzlab.unet.deprecated.hla;

import java.util.Objects;

/** {@link Locus} implementation for HLA types */
public enum HLALocus implements Locus<HLALocus> {
  A(SeroLocus.A, TIER_1), B(SeroLocus.B, TIER_1), C(SeroLocus.C, TIER_1), DRB1(SeroLocus.DRB,
      TIER_2), DRB3(SeroLocus.DRB, TIER_2), DRB4(SeroLocus.DRB, TIER_2), DRB5(SeroLocus.DRB,
          TIER_2), DQA1(SeroLocus.DQA, TIER_2), DQB1(SeroLocus.DQB, TIER_2), DPA1(SeroLocus.DPA,
              TIER_2), DPB1(SeroLocus.DPB, TIER_2), MICA(SeroLocus.MICA, TIER_2);

  private final SeroLocus sero;
  private final int tier;

  private HLALocus(SeroLocus sero, int tier) {
    this.sero = sero;
    this.tier = tier;
  }

  /** @return The equivalent {@link SeroLocus} for this {@link HLALocus} */
  public SeroLocus sero() {
    return sero;
  }

  /** @return Tier of this locus */
  public int tier() {
    return tier;
  }

  /** @return {@code true} iff this locus is one of DRB3, DRB4, or DRB5 */
  public boolean isDRB345() {
    return (Objects.equals(DRB3, this) || Objects.equals(DRB4, this) || Objects.equals(DRB5, this));
  }

  /**
   * @param s {@link SeroLocus} of interest
   * @return The first {@link HLALocus} compatible with the given {@link SeroLocus}.
   */
  public static HLALocus getHLAEquivalent(SeroLocus s) {
    for (HLALocus m : values()) {
      if (s.equals(m.sero())) {
        return m;
      }
    }
    throw new IllegalArgumentException("No matching Locus for input: " + s.name());
  }

  /**
   * Helper method to construct a {@link HLALocus} from a string, including the {@link SeroLocus}
   *
   * @param locus Locus or simple locus string value
   * @return First, best matching {@link HLALocus}
   */
  public static HLALocus safeValueOf(String locus) {
    locus = locus.toUpperCase();
    HLALocus hl = null;
    try {
      hl = valueOf(locus);
    } catch (IllegalArgumentException e) {
      // Not a HLALocus.. try SeroLocus and convert
      hl = getHLAEquivalent(SeroLocus.valueOf(locus));
    }
    return hl;
  }
}
