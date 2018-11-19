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
package org.pankratzlab.unet.parser.util;

import java.util.List;
import org.pankratzlab.hla.HLAType;
import com.google.common.collect.ImmutableSet;

/**
 * Contains static lists of Bw types. B-locus only, from http://hla.alleles.org/antigens/bw46.html
 */
public final class BwSerotypes {
  private BwSerotypes() {

  }

  public static enum BwGroup {
    Bw4, Bw6, Unknown;
  }

  public static final ImmutableSet<String> BW4 =
      ImmutableSet.of("B5", "B5102", "B5103", "B13", "B17", "B27", "B37", "B38", "B44", "B47",
          "B49", "B51", "B52", "B53", "B57", "B58", "B59", "B63", "B77");

  public static final ImmutableSet<String> BW6 = ImmutableSet.of("B7", "B703", "B8", "B14", "B18",
      "B22", "B2708", "B35", "B39", "B3901", "B3902", "B40", "B4005", "B41", "B42", "B45", "B48",
      "B50", "B54", "B55", "B56", "B60", "B61", "B62", "B64", "B65", "B67", "B70", "B71", "B72",
      "B73", "B75", "B76", "B78", "B81", "B82");

  public static final BwGroup getBwGroup(HLAType allele) {
    for (String antigen : ImmutableSet.of(makeAntigen(allele, 2), makeAntigen(allele, 1))) {
      if (BW6.contains(antigen)) {
        return BwGroup.Bw6;
      }
      if (BW4.contains(antigen)) {
        return BwGroup.Bw4;
      }
    }
    return BwGroup.Unknown;
  }

  private static String makeAntigen(HLAType allele, int resolution) {
    List<Integer> spec = allele.spec();
    String antigen = allele.locus().sero().toString();
    for (int i = 0; i < resolution; i++) {
      antigen += spec.get(i);
    }
    return antigen;
  }
}
