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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.unet.hapstats.HaplotypeFreqTable.Haplotype;
import com.google.common.collect.ImmutableSet;

/**
 * Static utility class for accessing haplotype frequencies for B-C and DR-DQ haplotypes
 */
public final class HaplotypeFrequency {
  private static final Map<Set<HLALocus>, HaplotypeFreqTable> TABLES = new HashMap<>();

  private HaplotypeFrequency() {}

  static {
    buildTable("C", "B", "/NMDP_CB.csv");
    buildTable("DRB1", "DQB1", "/NMDP_DRB1DQB1.csv");
  }

  private static void buildTable(String firstHeader, String secondHeader, String tablePath) {

    try (Reader in =
        new InputStreamReader(HaplotypeFrequency.class.getResourceAsStream(tablePath))) {
      CSVFormat csvFormat = CSVFormat.EXCEL.withFirstRecordAsHeader();
      Iterable<CSVRecord> records = csvFormat.parse(in);

      HaplotypeFreqTable freqTable = new HaplotypeFreqTable();

      HLALocus firstLocus = HLALocus.valueOf(firstHeader);
      HLALocus secondLocus = HLALocus.valueOf(secondHeader);

      // Process each haplotype entry
      records.forEach(next -> {
        HLAType typeOne = makeType(firstLocus, next, firstHeader);
        HLAType typeTwo = makeType(secondLocus, next, secondHeader);
        Map<Ethnicity, Double> hapMap = new HashMap<>();
        for (Ethnicity e : Ethnicity.values()) {
          hapMap.put(e, Double.parseDouble(next.get(e)));
        }
        freqTable.put(typeOne, typeTwo, new Haplotype(hapMap));
      });

      TABLES.put(ImmutableSet.of(firstLocus, secondLocus), freqTable);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static HLAType makeType(HLALocus locus, CSVRecord record, String specificityHeader) {
    String specificity = record.get(specificityHeader).replaceAll("[a-zA-Z]", "");
    return new HLAType(locus, specificity);
  }

  public static enum Ethnicity {
    EUR_freq(), AFA_freq, API_freq, HIS_freq;

    private static final String SEPARATOR = "_";

    public String displayString() {
      return name().substring(0, name().indexOf(SEPARATOR));
    }
  }
}
