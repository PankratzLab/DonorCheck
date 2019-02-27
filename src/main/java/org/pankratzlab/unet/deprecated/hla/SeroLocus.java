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
package org.pankratzlab.unet.deprecated.hla;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;

/**
 * {@link Locus} implementation for serological types
 */
public enum SeroLocus implements Locus<SeroLocus> {
  A(2, TIER_1), B(2, TIER_1), C(3, TIER_1), DRB(1, TIER_2, "DR"), DQB(1, TIER_2, "DQ"), DQA(1,
      TIER_2), DPB(1, TIER_2, "DP"), DPA(1, TIER_2), MICA(1, -1);

  private final int severity;
  private final int tier;
  private ImmutableList<String> aliases;

  private SeroLocus(int severity, int tier, String... alias) {
    this.severity = severity;
    this.tier = tier;
    aliases = ImmutableList.copyOf(alias);
  }

  /**
   * @return Numeric priority indicating whether antigens on this locus should be preferred as
   *         unacceptable
   */
  public int priority() {
    return severity;
  }

  /**
   * @return All alternative names for this {@link SeroLocus}
   */
  public ImmutableList<String> aliases() {
    return aliases;
  }

  @Override
  public int tier() {
    return tier;
  }

  private static ImmutableList<String> valuesWithAliases;

  static {
    List<String> values =
        Arrays.stream(SeroLocus.values()).map(SeroLocus::name).collect(Collectors.toList());
    for (SeroLocus t : SeroLocus.values()) {
      values.addAll(t.aliases());
    }
    valuesWithAliases = ImmutableList.copyOf(values);
  }

  /**
   * As {@link #valueOf(String)} but will automatically convert {@link HLALocus} strings
   */
  public static SeroLocus safeValueOf(String locus) {
    String lup = locus.toUpperCase();
    SeroLocus sl = null;
    try {
      // Try parsing as SeroLocus directly
      sl = valueOf(lup);
    } catch (IllegalArgumentException e) {
      // Check if the string is an alias
      for (SeroLocus l : SeroLocus.values()) {
        if (l.aliases().contains(lup)) {
          return l;
        }
      }

      // Try parsing as HLA locus and converting
      sl = HLALocus.valueOf(lup).sero();
    }

    return sl;
  }

  /**
   * @return As {@link #values()} but includes {@link SeroLocus#aliases()}
   */
  public static List<String> valuesWithAliases() {
    return valuesWithAliases;
  }
}
