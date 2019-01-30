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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.NullType;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Utility class for reporting common/well-documented status of alleles. Uses full field alleles and
 * P/G groups.
 */
public final class CommonWellDocumented {

  private CommonWellDocumented() {};

  private static final String P_TYPE_SUFFIX = "P";
  private static final String G_TYPE_SUFFIX = "G";
  private static final String FREQ_TABLE = "table";
  private static final String FREQ_TABLE_ROW = "tr";
  private static final String FREQ_TABLE_COL = "td";
  private static final String WELL_DOCUMENTED_FLAG = "WD";
  private static final String COMMON_FLAG = "C";
  private static final String ALLELE_FREQ_PATH = "/cwd200.html";
  private static ImmutableMap<HLAType, Status> ALLELE_FREQS;

  public static enum Status {
    COMMON(1.0), WELL_DOCUMENTED(0.5), UNKNOWN(0.0);

    private final double weight;

    private Status(double weight) {
      this.weight = weight;
    }

    public double getWeight() {
      return weight;
    }
  }

  static {

    // -- Read allele frequency map --
    try (InputStream htmlStream = XmlDonorParser.class.getResourceAsStream(ALLELE_FREQ_PATH)) {
      Document parsed = Jsoup.parse(htmlStream, "UTF-8", "http://example.com");
      ImmutableMap.Builder<HLAType, Status> freqMapBuilder = ImmutableMap.builder();
      Multimap<HLAType, Status> cwdMap = HashMultimap.create();

      // Three tables - base, G type and P type
      for (Element frequencyTable : parsed.getElementsByTag(FREQ_TABLE)) {
        Elements rows = frequencyTable.getElementsByTag(FREQ_TABLE_ROW);

        // first row is a header
        for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
          Element row = rows.get(rowNum);
          Elements columns = row.getElementsByTag(FREQ_TABLE_COL);
          String freqKey = columns.get(0).text();
          if (freqKey.endsWith(G_TYPE_SUFFIX) || freqKey.endsWith(P_TYPE_SUFFIX)) {
            // Remove G and P designations
            freqKey = freqKey.substring(0, freqKey.length() - 1);
          }
          Status freqVal = getCwdWeight(columns.get(1).text());
          cwdMap.put(HLAType.valueOf(freqKey), freqVal);
        }
      }
      cwdMap.entries().forEach(e -> {
        freqMapBuilder.put(e);
      });
      ALLELE_FREQS = freqMapBuilder.build();
    } catch (Exception e) {
      System.err.println("Invalid Frequency file: " + ALLELE_FREQ_PATH);
      e.printStackTrace();
      throw new IllegalStateException("Invalid Frequency file: " + ALLELE_FREQ_PATH);
    }
  }

  /**
   * @return A numeric weight whether the input allele is common, well-documented or unknown.
   */
  private static Status getCwdWeight(String cwdText) {
    switch (cwdText) {
      case COMMON_FLAG:
        return Status.COMMON;
      case WELL_DOCUMENTED_FLAG:
        return Status.WELL_DOCUMENTED;
    }
    return Status.UNKNOWN;
  }

  public static Status getStatus(HLAType type) {
    HLAType equivType = AlleleGroups.getGGroup(type);

    if (ALLELE_FREQS.containsKey(equivType)) {
      return ALLELE_FREQS.get(equivType);
    }

    // Try adding :01's to the specificity
    HLAType specModified = equivType;
    while (Objects.nonNull((specModified = growSpec(specModified)))) {
      if (ALLELE_FREQS.containsKey(specModified)) {
        return ALLELE_FREQS.get(specModified);
      }
    }

    // Try removing tailing :01's to the specificity
    specModified = equivType;
    while (Objects.nonNull((specModified = reduceSpec(specModified)))) {
      if (ALLELE_FREQS.containsKey(specModified)) {
        return ALLELE_FREQS.get(specModified);
      }
    }

    return Status.UNKNOWN;
  }

  /**
   * @param equivType Input type to reduce
   * @return The input {@link HLAType} with its tailing "01" field removed, or null if the allele
   *         can not be reduced
   */
  private static HLAType reduceSpec(HLAType equivType) {
    List<Integer> spec = equivType.spec();

    if (spec.size() < 2 || spec.get(spec.size() - 1) != 1) {
      // We can only remove a trailing "01 "specificity, and only if we have 3- or more fields
      return null;
    }

    spec = spec.subList(0, spec.size() - 1);
    return modifiedSpec(equivType, spec);
  }

  /**
   * @param equivType Input type to expand
   * @return The input {@link HLAType} with an additional "01" field, or null if the allele can not
   *         be further expanded
   */
  private static HLAType growSpec(HLAType equivType) {
    List<Integer> spec = new ArrayList<>(equivType.spec());

    if (spec.size() >= 4) {
      // We can only expand 2- and 3-field specificities
      return null;
    }

    spec.add(1);

    return modifiedSpec(equivType, spec);
  }

  /**
   * Helper method to create an updated HLAType
   */
  private static HLAType modifiedSpec(HLAType equivType, List<Integer> spec) {
    if (equivType instanceof NullType) {
      return new NullType(equivType.locus(), spec);
    }
    return new HLAType(equivType.locus(), spec);
  }

  /**
   * @param alleles Set of alleles
   * @return A combined weighting of the input allele set, based on their CWD frequencies
   */
  public static double cwdScore(List<HLAType> alleles) {
    double score = 0;

    for (HLAType type : alleles) {
      Status status = getStatus(type);
      score += status.getWeight();
    }
    return score;
  }


}
