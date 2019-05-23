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

import java.util.ArrayList;
import java.util.List;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.model.Strand;
import com.google.common.collect.Multimap;

/** Static utility class for haplotype operations */
public final class HaplotypeUtils {
  private HaplotypeUtils() {}

  public static final String RANGE_TOKEN = "-";

  /**
   * Helper method to take allele/allele range strings and populate a strand multimap
   *
   * @param specString Sanitized specificity string, which may or may not represent a range of
   *     alleles (indicated by {@link #RANGE_TOKEN})
   * @param locus {@link HLALocus} string representation
   * @param strandIndex {@link Strand} index
   * @param haplotypeMap Map to populate
   */
  public static void parseAllelesToStrandMap(
      String specString, String locus, int strandIndex, Multimap<Strand, HLAType> haplotypeMap) {

    if (specString.contains(RANGE_TOKEN)) {
      // Convert ranges to individual alleles
      HLAType firstType = new HLAType(HLALocus.valueOf(locus), specString.split(RANGE_TOKEN)[0]);
      HLAType lastType = new HLAType(HLALocus.valueOf(locus), specString.split(RANGE_TOKEN)[1]);
      List<Integer> newSpec = new ArrayList<>(lastType.spec());
      for (int rangeIndex = firstType.spec().get(1);
          rangeIndex <= lastType.spec().get(1);
          rangeIndex++) {
        newSpec.set(1, rangeIndex);
        haplotypeMap.put(Strand.values()[strandIndex], new HLAType(firstType.locus(), newSpec));
      }
    } else {
      HLALocus parsedLocus = HLALocus.valueOf(locus);
      HLAType rawType = new HLAType(parsedLocus, specString.replaceAll("N", ""));
      HLAType finalType;
      if (specString.endsWith("N")) {
        finalType = new NullType(parsedLocus, rawType.spec());
      } else {
        finalType = new HLAType(parsedLocus, rawType.spec());
      }
      haplotypeMap.put(Strand.values()[strandIndex], finalType);
    }
  }
}
