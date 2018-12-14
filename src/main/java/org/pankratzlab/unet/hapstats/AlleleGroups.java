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
package org.pankratzlab.unet.hapstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.pankratzlab.hla.Antigen;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Static utility class for looking up associated allele groupings
 * (http://hla.alleles.org/alleles/g_groups.html, http://hla.alleles.org/alleles/p_groups.html)
 */
public final class AlleleGroups {

  private static final String EQUIV_DELIM = "/";
  private static final String COMMENT_FLAG = "#";
  private static final String ENTRY_DELIM = ";";

  private AlleleGroups() {}

  private static final String P_GROUP_PATH = "/hla_nom_p.txt";
  private static final String G_GROUP_PATH = "/hla_nom_g.txt";
  private static final ImmutableMap<HLAType, HLAType> P_GROUP;
  private static final ImmutableMap<HLAType, HLAType> G_GROUP;

  static {
    P_GROUP = buildGroup(P_GROUP_PATH);
    G_GROUP = buildGroup(G_GROUP_PATH);
  }

  private static ImmutableMap<HLAType, HLAType> buildGroup(String pathToGroupFile) {
    Builder<HLAType, HLAType> builder = ImmutableMap.builder();
    Map<HLAType, HLAType> typeMap = new HashMap<>();
    try (BufferedReader groupFileReader = new BufferedReader(
        new InputStreamReader(AlleleGroups.class.getResourceAsStream(pathToGroupFile)))) {
      String line;
      // Delimiter is ;
      // First entry is locus
      // If no grouping, second entry is specificity
      // If grouping, second entry is '/'-delimited list of equivalent specificities
      // If grouping, third entry is the root allele specificity

      // A*;01:01:01:01/01:01:01:03/01:01:01:04/01:01:01:05/01:01:01:06/01:01:01:07/01:01:01:08/01:01:01:09/01:01:01:10/01:01:01:11/01:01:01:12/01:01:01:13/01:01:01:14/01:01:01:15/01:01:01:16/01:01:01:17/01:01:01:18/01:01:01:19/01:01:01:20/01:01:01:21/01:01:02/01:01:03/01:01:04/01:01:05/01:01:06/01:01:07/01:01:08/01:01:09/01:01:10/01:01:11/01:01:12/01:01:13/01:01:14/01:01:15/01:01:16/01:01:17/01:01:18/01:01:19/01:01:20/01:01:21/01:01:22/01:01:23/01:01:24/01:01:25/01:01:26/01:01:27/01:01:28/01:01:29/01:01:30/01:01:31/01:01:32/01:01:33/01:01:34/01:01:35/01:01:36/01:01:37/01:01:38L/01:01:39/01:01:40/01:01:41/01:01:42/01:01:43/01:01:44/01:01:45/01:01:46/01:01:47/01:01:48/01:01:49/01:01:50/01:01:51/01:01:52/01:01:53/01:01:54/01:01:55/01:01:56/01:01:57/01:01:58/01:01:59/01:01:60/01:01:61/01:01:62/01:01:63/01:01:64/01:01:65/01:01:66/01:01:67/01:01:68/01:01:69/01:01:70/01:01:71/01:01:72/01:01:73/01:01:74/01:01:75/01:01:76/01:01:77/01:01:78/01:01:79/01:01:80/01:01:81/01:01:82/01:01:83/01:01:84/01:01:85/01:01:86/01:01:87/01:01:88/01:01:89/01:01:90/01:01:91/01:01:92/01:01:93/01:01:94/01:01:95/01:01:96/01:32/01:37:01:01/01:37:01:02/01:45/01:81/01:103/01:107/01:109/01:132/01:141/01:142/01:155/01:177/01:212/01:217/01:234/01:237/01:246/01:248Q/01:249/01:251/01:252/01:253/01:261/01:274/01:276/01:277/01:280/01:281;01:01P
      // A*;01:06;
      while ((line = groupFileReader.readLine()) != null) {
        if (line.startsWith(COMMENT_FLAG)) {
          continue;
        }

        String[] entries = line.split(ENTRY_DELIM);
        if (entries.length == 2) {
          continue;
        }
        if (entries.length != 3) {
          throw new IllegalStateException("Unrecognized allele group line: " + line);
        }

        String locus = entries[0];
        try {
          HLALocus.valueOf(locus);
        } catch (IllegalArgumentException e) {
          continue;
        }

        final HLAType rootAllele = getAllele(locus, entries[2]);

        Set<HLAType> equivs = new HashSet<>();

        for (String spec : entries[1].split(EQUIV_DELIM)) {
          equivs.add(getAllele(locus, spec));
        }

        // equivs.forEach(equivAllele -> {
        // builder.put(equivAllele, rootAllele);
        // if (typeMap.put(equivAllele, rootAllele) == null) {
        // System.out.println("");
        // }
        // });
        for (HLAType equivAllele : equivs) {
          builder.put(equivAllele, rootAllele);
          HLAType old = typeMap.put(equivAllele, rootAllele);
          if (old != null) {
            System.out.println("");
          }

        }

      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read allele group file: " + pathToGroupFile);
    }

    return builder.build();
  }

  /**
   * Helper method to convert a locus + specificity string from a group file to an {@link HLAType}
   */
  private static HLAType getAllele(String locus, String specificity) {
    String[] split = specificity.split(Antigen.SPEC_DELIM);
    // The specificities in the group files can be very high-resolution, so we restrict it to two
    // places

    return HLAType.valueOf(locus + split[0] + ":" + split[1].replaceAll("[a-zA-Z]", ""));
  }

  public static HLAType getGroupAllele(String alleleString) {
    HLAType baseType = HLAType.valueOf(alleleString);

    if (alleleString.toLowerCase().endsWith("g")) {
      return getGGroup(baseType);
    }
    if (alleleString.toLowerCase().endsWith("p")) {
      return getPGroup(baseType);
    }
    return baseType;
  }

  /**
   * @param allele
   * @return
   */
  public static HLAType getGGroup(HLAType allele) {
    return getGroupEquiv(G_GROUP, allele);
  }

  /**
   * @param allele
   * @return
   */
  public static HLAType getPGroup(HLAType allele) {
    return getGroupEquiv(P_GROUP, allele);
  }

  /**
   * Helper method to look up an allele in a group map
   */
  private static HLAType getGroupEquiv(ImmutableMap<HLAType, HLAType> groupMap, HLAType allele) {
    HLAType equiv = groupMap.get(allele);
    return equiv == null ? allele : equiv;
  }
}
