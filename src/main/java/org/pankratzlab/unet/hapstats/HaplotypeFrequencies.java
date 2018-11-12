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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Static utility class for accessing haplotype frequencies for B-C and DR-DQ haplotypes
 */
public final class HaplotypeFrequencies {
  private static final Map<Haplotype, HaplotypeFrequency> TABLES;

  private HaplotypeFrequencies() {}

  static {
    // Initialize the haplotype tables
    Builder<Haplotype, HaplotypeFrequency> frequencyMapBuilder = ImmutableMap.builder();
    buildTable(frequencyMapBuilder, "C", "B", "/NMDP_CB.csv");
    buildTable(frequencyMapBuilder, "DRB1", "DQB1", "/NMDP_DRB1DQB1.csv");

    TABLES = frequencyMapBuilder.build();
  }

  /**
   * Helper method to build a haplotype table from a CSV file from NMDP
   */
  private static void buildTable(Builder<Haplotype, HaplotypeFrequency> frequencyTableBuilder,
      String firstHeader, String secondHeader, String tablePath) {

    try (Reader in =
        new InputStreamReader(HaplotypeFrequencies.class.getResourceAsStream(tablePath))) {
      CSVFormat csvFormat = CSVFormat.EXCEL.withFirstRecordAsHeader();
      Iterable<CSVRecord> records = csvFormat.parse(in);

      HLALocus firstLocus = HLALocus.valueOf(firstHeader);
      HLALocus secondLocus = HLALocus.valueOf(secondHeader);

      // Process each haplotype entry
      records.forEach(next -> {
        Haplotype haplotype = new Haplotype(makeType(firstLocus, next, firstHeader),
            makeType(secondLocus, next, secondHeader));
        Map<Ethnicity, Double> hapMap = new HashMap<>();
        for (Ethnicity e : Ethnicity.values()) {
          hapMap.put(e, Double.parseDouble(next.get(e)));
        }
        frequencyTableBuilder.put(haplotype, new HaplotypeFrequency(hapMap));
      });

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convert a record to a {@link HLAType}
   */
  private static HLAType makeType(HLALocus locus, CSVRecord record, String specificityHeader) {
    String specificity = record.get(specificityHeader).replaceAll("[a-zA-Z]", "");
    // All specificities are 4 characters, to be split into two groups of two.
    return new HLAType(locus, specificity.substring(0, 2), specificity.substring(2));
  }

  /**
   * @param ethnicity Target ethnicity
   * @param typeOne First type of target haplotype (order is arbitrary)
   * @param typeTwo Second type of target haplotype (order is arbitrary)
   * @return The population frequency in the specified ethnicity of the haplotype containing these
   *         two types
   * 
   * @see #getFrequency(Ethnicity, Haplotype)
   */
  public static Double getFrequency(Ethnicity ethnicity, HLAType typeOne, HLAType typeTwo) {
    return getFrequency(ethnicity, new Haplotype(typeOne, typeTwo));
  }

  /**
   * @param ethnicity Target ethnicity
   * @param haplotype Target haplotype
   * @return The population frequency in the specified ethnicity of the haplotype containing these
   *         two types
   */
  public static Double getFrequency(Ethnicity ethnicity, Haplotype haplotype) {
    if (!TABLES.containsKey(haplotype)) {
      return 0.0;
    }
    return TABLES.get(haplotype).getFrequencyForEthnicity(ethnicity);
  }


  /**
   * Enum of ethnicities known in Haplotype CSVs
   */
  public static enum Ethnicity {
    EUR_freq, AFA_freq, API_freq, HIS_freq;

    private static final String SEPARATOR = "_";

    public String displayString() {
      return name().substring(0, name().indexOf(SEPARATOR));
    }
  }

  /**
   * Helper class linking {@link Ethnicity} and frequency values for a particular Haplotype
   */
  private static class HaplotypeFrequency {
    private final ImmutableMap<Ethnicity, Double> frequencyForEthnicity;

    private HaplotypeFrequency(Map<Ethnicity, Double> frequencyByEth) {
      this.frequencyForEthnicity = ImmutableMap.copyOf(frequencyByEth);
    }

    private Double getFrequencyForEthnicity(Ethnicity e) {
      return frequencyForEthnicity.get(e);
    }

    @Override
    public String toString() {
      return frequencyForEthnicity.toString();
    }
  }
}
