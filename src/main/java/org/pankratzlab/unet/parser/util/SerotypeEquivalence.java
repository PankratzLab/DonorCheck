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
package org.pankratzlab.unet.parser.util;

import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Utility class recording the mapping of genotypes to reported serotypes
 */
public final class SerotypeEquivalence {

  private static final ImmutableMap<HLAType, SeroType> equivalencies;

  static {
    // Build the equivalencies map
    Builder<HLAType, SeroType> builder = ImmutableMap.builder();
    //TODO would be nice to read this from a file instead

    // -- B Locus --
    put(builder, "64", HLALocus.B, "14:01");
    put(builder, "65", HLALocus.B, "14:02");
    put(builder, "14", HLALocus.B, "14:03");
    put(builder, "62", HLALocus.B, "15:01", "15:04", "15:05", "15:06", "15:07", "15:15");
    put(builder, "75", HLALocus.B, "15:02", "15:08");
    put(builder, "72", HLALocus.B, "15:03", "15:46");
    put(builder, "70", HLALocus.B, "15:09");
    put(builder, "71", HLALocus.B, "15:10", "15:18");
    put(builder, "76", HLALocus.B, "15:12", "15:14");
    put(builder, "77", HLALocus.B, "15:13");
    put(builder, "63", HLALocus.B, "15:16", "15:17");
    put(builder, "35", HLALocus.B, "15:22");
    put(builder, "60", HLALocus.B, "40:01", "40:10");
    put(builder, "61", HLALocus.B, "40:02", "40:06", "40:09");
    put(builder, "50", HLALocus.B, "40:05");
    put(builder, "45", HLALocus.B, "44:09");
    put(builder, "50", HLALocus.B, "50:01");
    put(builder, "45", HLALocus.B, "50:02");

    // -- C Locus --
    put(builder, "10", HLALocus.C, "03:02", "03:04");
    put(builder, "9", HLALocus.C, "03:03");
    put(builder, "3", HLALocus.C, "03:05", "03:07");

    // -- DR1 Locus --
    put(builder, "0103", HLALocus.DRB1, "01:03");
    put(builder, "17", HLALocus.DRB1, "03:01", "03:04", "03:05");
    put(builder, "18", HLALocus.DRB1, "03:02", "03:03");

    // -- DQ Locus --
    put(builder, "7", HLALocus.DQB1, "03:01", "03:04", "03:13", "03:19");
    put(builder, "8", HLALocus.DQB1, "03:02", "03:05");
    put(builder, "9", HLALocus.DQB1, "03:03");

    equivalencies = builder.build();
  }

  private static void put(Builder<HLAType, SeroType> builder, String serotypeSpec, HLALocus locus,
      String... hlaSpecs) {
    SeroType s = new SeroType(locus.sero(), serotypeSpec);

    for (String allele : hlaSpecs) {
      HLAType a = new HLAType(locus, allele);
      builder.put(a, s);
    }
  }

  /**
   * @return The {@link SeroType} equivalent for the 2-field input genotype, or {@code null} if no
   *         explicit mapping exists.
   */
  public static SeroType get(HLAType allele) {
    if (allele.spec().size() > 2) {
      // All the equivalencies are based on 2-field alleles
      allele = new HLAType(allele.locus(), allele.spec().subList(0, 2));
    }
    return equivalencies.get(allele);
  }
}
