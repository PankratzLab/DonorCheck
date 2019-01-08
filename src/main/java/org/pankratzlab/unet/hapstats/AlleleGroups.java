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

        String locus = entries[0].replace("*", "");
        try {
          HLALocus.valueOf(locus);
        } catch (IllegalArgumentException e) {
          System.err.println("AlleleGroups; not a recognized locus: " + locus);
          continue;
        }

        final HLAType rootAllele = getAllele(locus, entries[2]);

        Set<HLAType> equivs = new HashSet<>();

        for (String spec : entries[1].split(EQUIV_DELIM)) {
          equivs.add(getAllele(locus, spec));
        }

        for (HLAType equivAllele : equivs) {
          builder.put(equivAllele, rootAllele);
          typeMap.put(equivAllele, rootAllele);

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
    return HLAType.valueOf(locus + specificity);
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
    if (equiv == null) {
      equiv = allele;
    }
    equiv = new HLAType(equiv.locus(), equiv.spec().subList(0, 2));
    return equiv;
  }
}
