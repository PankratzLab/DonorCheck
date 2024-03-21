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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;

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

      // Each line is rather a single allele or a list of alleles followed by the group those
      // alleles belong to
      // Delimiter is ;
      // First element is locus
      // If no grouping, second element is specificity
      // If grouping, second element is '/'-delimited list of equivalent specificities
      // If grouping, third element is the group allele specificity

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
    } catch (Exception e) {
      System.err.println("Failed to read allele group file: " + pathToGroupFile);
      e.printStackTrace();
      throw new IllegalStateException("Failed to read allele group file: " + pathToGroupFile);
    }

    return builder.build();
  }

  /**
   * Helper method to convert a locus + specificity string from a group file to an {@link HLAType}
   */
  private static HLAType getAllele(String locus, String specificity) {
    String alleleString = locus + "*" + specificity.replaceAll("[a-mo-zA-MO-Z]", "");
    if (alleleString.toLowerCase().endsWith("n")) {
      return NullType.valueOf(alleleString);
    }
    return HLAType.valueOf(alleleString);
  }

  /**
   * Accepts allele strings ending in n, g or p and decodes them to the appropriate g or p group
   * 
   * @param alleleString representation of an allele
   * @return the g or p group allele if this allele is a member. Otherwise return allele
   */
  public static HLAType getGroupAllele(String alleleString) {
    HLAType baseType = HLAType.valueOf(alleleString);

    alleleString = alleleString.toLowerCase();

    if (alleleString.endsWith("n")) {
      return getUnknownGroupEquiv(NullType.valueOf(alleleString));
    } else if (alleleString.endsWith("g")) {
      return getGGroup(baseType);
    } else if (alleleString.endsWith("p")) {
      return getPGroup(baseType);
    }

    return baseType;
  }

  private static HLAType getUnknownGroupEquiv(NullType unknown) {
    // Checking if unknown allele is in g group
    HLAType equiv = G_GROUP.get(unknown);
    // If null unknown is not in g group and p group should be checked
    if (Objects.isNull(equiv)) {
      equiv = P_GROUP.get(unknown);
    }
    // If equiv is still null then neither g group or p group
    return Objects.isNull(equiv) ? unknown : equiv;
  }

  /**
   * @param allele key value to search G_Group for
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

  /** Helper method to look up an allele in a group map */
  private static HLAType getGroupEquiv(ImmutableMap<HLAType, HLAType> groupMap, HLAType allele) {
    HLAType equiv = groupMap.get(allele);
    return Objects.isNull(equiv) ? allele : equiv;
  }
}
