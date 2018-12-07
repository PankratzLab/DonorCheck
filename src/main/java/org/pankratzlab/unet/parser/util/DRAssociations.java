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

import java.util.Objects;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.SeroType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public final class DRAssociations {
  private DRAssociations() {};

  // NONE : DRB1* 01, 08, 10
  //
  // DRB3 - 52 : DRB1* 03, 11, 12, 13, 14
  //
  // DRB4 - 53 : DRB1* 04, 07, 09
  //
  // DRB5 - 51 : DRB1* 15, 16
  private static ImmutableMap<SeroType, HLALocus> DR_MAP;

  private static void init() {
    ImmutableMap.Builder<SeroType, HLALocus> builder = ImmutableMap.builder();

    addMapping(builder, HLALocus.DRB3, "03", "11", "12", "13", "14", "17", "18");
    addMapping(builder, HLALocus.DRB4, "04", "07", "09");
    addMapping(builder, HLALocus.DRB5, "15", "16");

    DR_MAP = builder.build();
  }

  private static void addMapping(Builder<SeroType, HLALocus> builder, HLALocus drbLocus,
      String... drFields) {

    for (String dr : drFields) {
      builder.put(SeroType.valueOf("DR" + dr), drbLocus);
    }
  }

  private static ImmutableMap<SeroType, HLALocus> drMap() {
    if (Objects.isNull(DR_MAP)) {
      init();
    }
    return DR_MAP;
  }

  public static HLALocus getDRBLocus(SeroType drType) {
    if (Objects.nonNull(drType) && drMap().containsKey(drType)) {
      return drMap().get(drType);
    }

    return null;
  }

}
