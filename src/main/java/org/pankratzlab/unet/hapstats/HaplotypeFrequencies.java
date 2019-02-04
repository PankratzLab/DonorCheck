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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.NullType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Static utility class for accessing haplotype frequencies for B-C and DR-DQ haplotypes. Uses
 * two-field alleles and P/G groups.
 */
public final class HaplotypeFrequencies {
  private static final String DR_DQ_TABLE = "/2013_DRB3-4-5_DRB1_DQB1.csv";
  private static final String BC_TABLE = "/2013_C_B.csv";
  private static final String FREQ_COL_SUFFIX = "_freq";
  private static final Map<Haplotype, HaplotypeFrequency> TABLES;

  private HaplotypeFrequencies() {}

  static {
    // Initialize the haplotype tables
    Builder<Haplotype, HaplotypeFrequency> frequencyMapBuilder = ImmutableMap.builder();
    buildTable(frequencyMapBuilder, BC_TABLE, "C", "B");
    buildTable(frequencyMapBuilder, DR_DQ_TABLE, "DRB3-4-5", "DRB1", "DQB1");

    ImmutableMap<Haplotype, HaplotypeFrequency> table = ImmutableMap.of();
    try {
      table = frequencyMapBuilder.build();
    } catch (Exception e) {
      System.err.println("Error building haplotype frequency table");
      e.printStackTrace();
    }
    TABLES = table;
  }

  /**
   * Helper method to build a haplotype table from a CSV file from NMDP
   */
  private static void buildTable(Builder<Haplotype, HaplotypeFrequency> frequencyTableBuilder,
      String tablePath, String... headers) {

    try (Reader in =
        new InputStreamReader(HaplotypeFrequencies.class.getResourceAsStream(tablePath))) {
      CSVFormat csvFormat = CSVFormat.EXCEL.withFirstRecordAsHeader();
      Iterable<CSVRecord> records = csvFormat.parse(in);

      // Process each haplotype entry
      records.forEach(next -> {
        Set<HLAType> types = new HashSet<>();
        for (String header : headers) {
          types.add(makeType(next, header));
        }
        Haplotype haplotype = new Haplotype(types);
        Multimap<RaceGroup, Double> hapMap = MultimapBuilder.hashKeys().arrayListValues().build();
        // Values are stored as RaceCode frequencies, but we want to condense them to RaceGroups
        for (RaceGroup group : RaceGroup.values()) {
          hapMap.put(group, Double.parseDouble(next.get(group.toString() + FREQ_COL_SUFFIX)));
        }
        frequencyTableBuilder.put(haplotype, new HaplotypeFrequency(hapMap));
      });

    } catch (Exception e) {
      System.err.println("Error generating haplotype frequencies");
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convert a record to a {@link HLAType}
   */
  private static HLAType makeType(CSVRecord record, String specificityHeader) {
    String alleleString = record.get(specificityHeader);
    HLAType groupAllele = AlleleGroups.getGroupAllele(alleleString);
    return truncateFields(groupAllele);
  }

  /**
   * Any null type is treated as {@link NullType#UNREPORTED_DRB345}
   */
  private static HLAType adjustNulls(HLAType testType) {
    if (testType.locus().isDRB345() && testType instanceof NullType) {
      return NullType.UNREPORTED_DRB345;
    }
  
    return testType;
  }

  /**
   * Ensure we never list or look up an allele with more than 2-field specificity
   */
  private static HLAType truncateFields(HLAType testType) {
    List<Integer> truncatedFields = testType.spec().subList(0, 2);
    if (testType instanceof NullType) {
      return new NullType(testType.locus(), truncatedFields);
    }
    return new HLAType(testType.locus(), truncatedFields);
  }

  /**
   * @param ethnicity Target ethnicity
   * @param typeOne First type of target haplotype (order is arbitrary)
   * @param typeTwo Second type of target haplotype (order is arbitrary)
   * @return The population frequency in the specified ethnicity of the haplotype containing these
   *         two types
   * 
   * @see #getFrequency(RaceGroup, Haplotype)
   */
  public static Double getFrequency(RaceGroup ethnicity, HLAType typeOne, HLAType typeTwo) {
    return getFrequency(ethnicity, new Haplotype(typeOne, typeTwo));
  }

  /**
   * @param ethnicity Target ethnicity
   * @param haplotype Target haplotype
   * @return The population frequency in the specified ethnicity of the haplotype containing these
   *         two types
   */
  public static Double getFrequency(RaceGroup ethnicity, Haplotype haplotype) {
    Haplotype equivHaplotype = new Haplotype(haplotype.getTypes().stream()
        .map(AlleleGroups::getGGroup).map(HaplotypeFrequencies::adjustNulls)
        .map(HaplotypeFrequencies::truncateFields).collect(Collectors.toSet()));

    if (!TABLES.containsKey(equivHaplotype)) {
      return 0.0;
    }
    return TABLES.get(equivHaplotype).getFrequencyForEthnicity(ethnicity);
  }

  /**
   * Helper class linking {@link RaceGroup} and frequency values for a particular Haplotype
   */
  private static class HaplotypeFrequency {
    private final ImmutableMap<RaceGroup, Double> frequencyForEthnicity;

    private HaplotypeFrequency(Multimap<RaceGroup, Double> hapMap) {
      Builder<RaceGroup, Double> builder = ImmutableMap.builder();
      for (RaceGroup group : hapMap.keySet()) {
        builder.put(group, hapMap.get(group).stream().mapToDouble(Double::doubleValue).sum());
      }
      this.frequencyForEthnicity = builder.build();
    }

    private Double getFrequencyForEthnicity(RaceGroup e) {
      return frequencyForEthnicity.get(e);
    }

    @Override
    public String toString() {
      return frequencyForEthnicity.toString();
    }
  }
}
