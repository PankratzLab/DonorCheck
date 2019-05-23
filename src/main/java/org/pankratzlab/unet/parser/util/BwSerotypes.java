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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * Contains static lists of Bw types. B-locus only, from http://hla.alleles.org/antigens/bw46.html
 */
public final class BwSerotypes {
  private static final char COMMENT = '#';

  private BwSerotypes() {}

  public static enum BwGroup {
    Bw4("Bw4"),
    Bw6("Bw6"),
    Unknown("No entry");

    private final String groupString;

    private BwGroup(String groupString) {
      this.groupString = groupString;
    }

    @Override
    public String toString() {
      return groupString;
    }
  }

  private static final String BW4_ANTIGEN_PATH = "/bw4Antigens.csv";
  private static final String BW6_ANTIGEN_PATH = "/bw6Antigens.csv";
  private static final String BW4_ALLELE_PATH = "/bw4Alleles.csv";
  private static final String BW6_ALLELE_PATH = "/bw6Alleles.csv";

  private static final ImmutableSet<String> BW4;
  private static final ImmutableSet<String> BW6;
  private static final ImmutableMap<HLAType, BwGroup> ALLELE_MAP;

  // -- Initialize BW groups --
  static {
    BW4 = readAntigens(BW4_ANTIGEN_PATH);
    BW6 = readAntigens(BW6_ANTIGEN_PATH);
    ImmutableMap.Builder<HLAType, BwGroup> builder = ImmutableMap.builder();
    mapAlleles(builder, BwGroup.Bw4, BW4_ALLELE_PATH);
    mapAlleles(builder, BwGroup.Bw6, BW6_ALLELE_PATH);
    ALLELE_MAP = builder.build();
  }

  /** Map all antigens in a csv file to the specified {@link BwGroup} */
  private static ImmutableSet<String> readAntigens(String antigenCsvPath) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    try (CSVParser parser =
        new CSVParser(
            new BufferedReader(
                new InputStreamReader(BwSerotypes.class.getResourceAsStream(antigenCsvPath))),
            CSVFormat.DEFAULT.withCommentMarker(COMMENT))) {
      for (CSVRecord record : parser) {
        // Each element of a record is an antigen of the group for this file
        builder.addAll(record);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return ImmutableSet.of();
    }
    return builder.build();
  }

  /** Map all alleles in a csv file to the specified {@link BwGroup} */
  private static void mapAlleles(
      Builder<HLAType, BwGroup> builder, BwGroup bwGroup, String alleleCsvPath) {
    try (CSVParser parser =
        new CSVParser(
            new BufferedReader(
                new InputStreamReader(BwSerotypes.class.getResourceAsStream(alleleCsvPath))),
            CSVFormat.DEFAULT.withCommentMarker(COMMENT))) {
      for (CSVRecord record : parser) {
        // Each element of a record is the specificity of a B allele for the group of this file
        for (String specificity : record) {
          builder.put(new HLAType(HLALocus.B, specificity), bwGroup);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @return the {@link BwGroup} matching the given antigen string
   * @see #getBwGroup(SeroType)
   */
  public static final BwGroup getBwGroup(String antigen) {
    if (BW6.contains(antigen)) {
      return BwGroup.Bw6;
    }
    if (BW4.contains(antigen)) {
      return BwGroup.Bw4;
    }
    return BwGroup.Unknown;
  }

  /**
   * @return the {@link BwGroup} matching the given {@link SeroType}
   * @see #getBwGroup(String)
   */
  public static final BwGroup getBwGroup(SeroType antigen) {
    return getBwGroup(antigen.toString());
  }

  /**
   * @return the {@link BwGroup} of the given {@link HLAType}
   * @see #getBwGroup(String)
   */
  public static BwGroup getBwGroup(HLAType allele) {
    // Try the 2-field specificity
    HLAType twoField = new HLAType(allele.locus(), allele.spec().subList(0, 2));

    BwGroup group = BwGroup.Unknown;
    if (ALLELE_MAP.containsKey(twoField)) {
      // Check the known whitelist
      group = ALLELE_MAP.get(twoField);
    } else if (AntigenDictionary.isValid(twoField)) {
      // Check the antigen dictionary
      group = getBwGroup(AntigenDictionary.lookup(twoField).iterator().next());
    } else {
      // Try the naive two-field specificity
      group = getBwGroup(new SeroType(twoField.locus().sero(), twoField.spec()));
    }

    if (Objects.equals(BwGroup.Unknown, group)) {
      // Try the single-field specificity
      group = getBwGroup(new SeroType(allele.locus().sero(), allele.spec().subList(0, 1)));
    }

    return group;
  }
}
