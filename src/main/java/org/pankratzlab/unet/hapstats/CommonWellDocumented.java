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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.parser.XmlDonorParser;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import javafx.scene.control.ChoiceDialog;

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
  private static final String INTERMEDIATE_FLAG = "I";
  private static final String COMMON_FLAG = "C";
  private static final String ALLELE_FREQ_PATH = "/ciwd300.txt";
  private static final String ALLELE_FREQ_PATH_200 = "/cwd200.html";
  private static ImmutableMap<HLAType, Status> ALLELE_FREQS;

  private static LoadingCache<HLAType, Status> doGetStatusCache = CacheBuilder.newBuilder()
                                                                              .build(CacheLoader.from(CommonWellDocumented::doGetStatus));

  public static enum Status {
    COMMON(1.0), INTERMEDIATE(0.5), WELL_DOCUMENTED(0.5), UNKNOWN(0.0);

    private final double weight;

    private Status(double weight) {
      this.weight = weight;
    }

    public double getWeight() {
      return weight;
    }
  }

  private static enum SOURCE {
    CWD_200("CWD 2.0.0 (fast, fewer haplotypes)"),
    CIWD_300("CIWD 3.0.0 (much slower, more haplotypes)");

    SOURCE(String d) {
      displayName = d;
    }

    private final String displayName;

    @Override
    public String toString() {
      return displayName;
    }
  }

  public static void init() {

    doGetStatusCache.invalidateAll();
    // Platform.runLater(() -> {
    ChoiceDialog<SOURCE> cd = new ChoiceDialog<>(SOURCE.CWD_200, SOURCE.values());
    cd.setTitle("Select CWD/CIWD Database");
    cd.setHeaderText("Select a Database from which to load CWD/CIWD data.");
    Optional<SOURCE> result = cd.showAndWait();
    result.ifPresent(r -> {
      try {
        switch (r) {
          case CWD_200:
            loadCWD200();

            break;
          case CIWD_300:

            loadCIWD300();
            break;
          default:
            break;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    // });

  }

  public static void loadCWD200() {
    // -- Read allele frequency map --
    try (InputStream htmlStream = XmlDonorParser.class.getResourceAsStream(ALLELE_FREQ_PATH_200)) {
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
          if (freqKey.endsWith("G") || freqKey.endsWith("P")) {
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

  public static void loadCIWD300() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(XmlDonorParser.class.getResourceAsStream(ALLELE_FREQ_PATH)))) {
      ImmutableMap.Builder<HLAType, Status> freqMapBuilder = ImmutableMap.builder();
      Map<HLAType, Status> cwdMap = new HashMap<>();
      reader.lines().map(s -> s.split("\t", -1)).forEach(s -> {

        // currently we are skipping P and G groups as HLAType doesn't track group
        if (s[0].charAt(s[0].length() - 1) == 'P' || s[0].charAt(s[0].length() - 1) == 'G') {
          return;
        }
        HLAType type = HLAType.valueOf(s[0]);
        Status freqVal = getCwdWeight(s[1]);

        cwdMap.put(type, freqVal);
      });
      cwdMap.entrySet().forEach(e -> {
        freqMapBuilder.put(e);
      });
      ALLELE_FREQS = freqMapBuilder.build();

    } catch (Exception e) {
      System.err.println("Invalid Frequency file: " + ALLELE_FREQ_PATH);
      e.printStackTrace();
      throw new IllegalStateException("Invalid Frequency file: " + ALLELE_FREQ_PATH);
    }
  }

  /** @return A numeric weight whether the input allele is common, well-documented or unknown. */
  private static Status getCwdWeight(String cwdText) {
    switch (cwdText) {
      case COMMON_FLAG:
        return Status.COMMON;
      case INTERMEDIATE_FLAG:
        return Status.INTERMEDIATE;
      case WELL_DOCUMENTED_FLAG:
        return Status.WELL_DOCUMENTED;
    }
    return Status.UNKNOWN;
  }

  /**
   * @param type HLA allele
   * @return Common/Well-documented status of the allele
   */
  public static Status getStatus(HLAType type) {
    return doGetStatusCache.getUnchecked(type);
  }

  /**
   * @param type HLA allele
   * @return Common/Well-documented status of the allele. If the base allele is unknown, we will
   *         check G-group equivalents.
   */
  public static Status getEquivStatus(HLAType type) {
    Status status = doGetStatusCache.getUnchecked(type);
    // if status is unknown attempt getting the status from a common group
    // only checking g group because overlap cough cause failures
    if (Status.UNKNOWN.equals(status)) {
      HLAType equivType = AlleleGroups.getGGroup(type);
      status = doGetStatusCache.getUnchecked(equivType);
    }

    return status;
  }

  public static HLAType getCWDType(HLAType type) {
    if (ALLELE_FREQS.containsKey(type)) {
      return type;
    }
    // adding or removing trailing :01's does not change the allele specificity
    // Try adding :01's to the specificity
    HLAType specModified = type;
    while (Objects.nonNull((specModified = growSpec(specModified)))) {
      if (ALLELE_FREQS.containsKey(specModified)) {
        return specModified;
      }
    }

    // Try removing fourth field, or tailing :01's, to the specificity
    specModified = type;
    while (Objects.nonNull((specModified = reduceSpec(specModified)))) {
      if (ALLELE_FREQS.containsKey(specModified)) {
        return specModified;
      }
    }
    return null;

  }

  private static Status doGetStatus(HLAType type) {
    if (ALLELE_FREQS.containsKey(type)) {
      return ALLELE_FREQS.get(type);
    }
    // adding or removing trailing :01's does not change the allele specificity
    // Try adding :01's to the specificity
    HLAType specModified = type;
    while (Objects.nonNull((specModified = growSpec(specModified)))) {
      if (ALLELE_FREQS.containsKey(specModified)) {
        return ALLELE_FREQS.get(specModified);
      }
    }

    // Try removing fourth field, or tailing :01's, to the specificity
    specModified = type;
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

    if (spec.size() < 2 || (spec.size() < 4 && spec.get(spec.size() - 1) != 1)) {
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

  /** Helper method to create an updated HLAType */
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
