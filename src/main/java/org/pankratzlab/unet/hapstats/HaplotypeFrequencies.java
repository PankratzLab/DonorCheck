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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Static utility class for accessing haplotype frequencies for B-C and DR-DQ haplotypes. Uses
 * two-field alleles and P/G groups.
 */
public final class HaplotypeFrequencies {

  private static final String UNREPORTED_DRB345 = "DRBX*NNNN";
  public static final double UNKNOWN_HAP_CUTOFF = 0.00001;
  private static final String FREQ_COL_SUFFIX = "_freq";

  public static final String NMDP_CB_PROP = "hla.nmdp.haplotype.bc";
  public static final String NMDP_DRDQ_PROP = "hla.nmdp.haplotype.drdq";

  private static Map<Haplotype, HaplotypeFrequency> TABLES;
  private static Boolean initialized = null;
  private static String missingTableMsg;

  private HaplotypeFrequencies() {}

  /**
   * @return True if {@link #doInitialization()} succeeded.
   */
  public static boolean successfullyInitialized() {
    if (Objects.isNull(initialized)) {
      initialized = doInitialization();
    }
    return initialized;
  }

  /**
   * Update the haplotype frequency tables.
   * <p>
   * NOTE: This should always be run off the JFX application thread
   * </p>
   *
   * @return true if at least one haplotype is read successfully.
   */
  public static boolean doInitialization() {
    Builder<Haplotype, HaplotypeFrequency> frequencyMapBuilder = ImmutableMap.builder();
    StringJoiner noTable = new StringJoiner("\n");

    String bcTablePath = HLAProperties.get().getProperty(NMDP_CB_PROP);
    File bcTableFile;
    if (!Strings.isNullOrEmpty(bcTablePath) && (bcTableFile = new File(bcTablePath)).exists()) {
      buildTable(frequencyMapBuilder, bcTableFile, "C", "B");
    } else {
      noTable.add("CB");
    }

    String drdqTablePath = HLAProperties.get().getProperty(NMDP_DRDQ_PROP);
    File drdqTableFile;
    if (!Strings.isNullOrEmpty(drdqTablePath)
        && (drdqTableFile = new File(drdqTablePath)).exists()) {
      buildTable(frequencyMapBuilder, drdqTableFile, "DRB3-4-5", "DRB1", "DQB1");
    } else {
      noTable.add("DRB345-DRB1-DQB1");
    }

    ImmutableMap<Haplotype, HaplotypeFrequency> table = ImmutableMap.of();
    try {
      table = frequencyMapBuilder.build();
    } catch (Exception e) {
      System.err.println("Error building haplotype frequency table");
      e.printStackTrace();
    }
    TABLES = table;

    missingTableMsg = "";
    if (!noTable.toString().isEmpty()) {
      missingTableMsg =
          "The following frequency table(s) are missing. Corresponding haplotype frequencies will not be used.\n"
              + noTable.toString() + "\n\nYou can edit the table paths via the 'Haplotypes' menu.";
    }

    initialized = !TABLES.isEmpty();
    return initialized;
  }

  /**
   * @return A description of any tables that failed to load in the last {@link #doInitialization()}
   *         call. Empty if no missing tables.
   */
  public static String getMissingTableMessage() {
    return missingTableMsg;
  }

  /**
   * Helper method to build a haplotype table from a CSV file from NMDP
   */
  private static void buildTable(Builder<Haplotype, HaplotypeFrequency> frequencyTableBuilder,
      File frequencyFile, String... loci) {

    try (InputStream is = new FileInputStream(frequencyFile);
        HSSFWorkbook workbook = new HSSFWorkbook(is)) {

      for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
        Iterator<Row> rows = workbook.getSheetAt(0).rowIterator();
        Map<String, Integer> alleleIndexMap = new HashMap<>();
        Map<RaceGroup, Integer> ethnicityMap = new EnumMap<>(RaceGroup.class);
        Row headerRow = rows.next();
        for (Cell headerCell : headerRow) {
          int colIndex = headerCell.getColumnIndex();
          String cellVal = headerCell.toString();
          for (String l : loci) {
            if (cellVal.equals(l)) {
              alleleIndexMap.put(l, colIndex);
            }
          }
          for (RaceGroup group : RaceGroup.values()) {
            if (cellVal.equals(group.toString() + FREQ_COL_SUFFIX)) {
              ethnicityMap.put(group, colIndex);
            }
          }
        }
        // TODO could validate the header extraction by ensuring all loci and ethnicities were
        // found

        while (rows.hasNext()) {
          Row row = rows.next();

          // Process each haplotype entry
          Set<HLAType> types = new HashSet<>();
          for (String header : loci) {
            Cell cell = row.getCell(alleleIndexMap.get(header));
            types.add(makeType(cell.toString()));
          }
          Haplotype haplotype = new Haplotype(types);
          Multimap<RaceGroup, Double> hapMap = MultimapBuilder.hashKeys().arrayListValues().build();
          // Values are stored as RaceCode frequencies, but we want to condense them to RaceGroups
          for (RaceGroup group : RaceGroup.values()) {
            Cell cell = row.getCell(ethnicityMap.get(group));
            double frequency = cell.getNumericCellValue();
            if (Double.compare(UNKNOWN_HAP_CUTOFF, frequency) > 0) {
              frequency = Double.MIN_VALUE;
            }
            hapMap.put(group, frequency);
          }
          frequencyTableBuilder.put(haplotype, new HaplotypeFrequency(hapMap));
        }
      }
    } catch (IOException e) {
      System.err.println("Error generating haplotype frequencies");
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Convert a record to a {@link HLAType}
   */
  private static HLAType makeType(String alleleString) {
    if (alleleString.equals(UNREPORTED_DRB345)) {
      alleleString = NullType.UNREPORTED_DRB345.toString();
    }
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
    Double freq = 0.0;
    Haplotype equivHaplotype = new Haplotype(haplotype.getTypes().stream()
        .map(AlleleGroups::getGGroup).map(HaplotypeFrequencies::adjustNulls)
        .map(HaplotypeFrequencies::truncateFields).collect(Collectors.toSet()));
    if (TABLES.containsKey(equivHaplotype)) {
      freq = TABLES.get(equivHaplotype).getFrequencyForEthnicity(ethnicity);
    }
    return freq;
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
