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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

/** Utility class recording the mapping of genotypes to reported serotypes */
public final class SerotypeEquivalence {

  private static final String CAREDX_FILE = "ExpertWhoSerology_20240110_3.54.xml";
  private static final ImmutableMap<HLAType, SeroType> careDxEquivalencies;
  private static final ImmutableMap<HLAType, SeroType> manualEquivalencies;

  private static final Set<String> INVALID =
      Sets.newHashSet("-", "Undefined", "Null", "NotExpressed", "Blank");

  static {
    careDxEquivalencies = buildLookupFromCareDxXMLFile();
    manualEquivalencies = buildManualOverrideLookup();

    // List<String> test = Lists.newArrayList("B*15:15", "B*15:15:01", "B*15:15:01:01");
    // Set<HLAType> hset = new HashSet<>();
    // for (String t : test) {
    // HLAType h = HLAType.valueOf(t);
    // hset.add(h);
    // System.out.println(h.toString() + "\t" + h.equivSafe());
    // }
    //
    // test =
    // Lists.newArrayList("B*15:08", "B*15:08:01", "B*15:08:01:01", "B*15:08:01:02", "B*15:08:02");
    // hset = new HashSet<>();
    // for (String t : test) {
    // HLAType h = HLAType.valueOf(t);
    // hset.add(h);
    // System.out.println(h.toString() + "\t" + h.equivSafe());
    // }

    // for (HLAType h : manualEquivalencies.keySet()) {
    // SeroType newS = careDxEquivalencies.get(h);
    // SeroType oldS = manualEquivalencies.get(h);
    // SeroType autoS = h.equivSafe();
    // SeroType autoLS = h.lowResEquiv();
    // if (h.locus() == HLALocus.B && h.spec().get(0) == 15 && h.spec().get(1) == 46) {
    // System.out.println(h + "\t" + oldS + "\t" + autoLS + "\t" + autoS);
    // }
    // if (newS == null) {
    // System.out.println(h + "\t" + oldS + "\t" + autoLS + "\t" + autoS);
    // } else if (oldS.compareTo(newS) != 0) {
    // System.out.println(
    // "----------- " + h + ": was " + oldS + " || now is " + newS + " (auto: " + autoS + ")");
    // }
    // }

  }

  private static ImmutableMap<HLAType, SeroType> buildLookupFromCareDxXMLFile() {
    Builder<HLAType, SeroType> builder = ImmutableMap.builder();

    try (InputStream xmlStream =
        SerotypeEquivalence.class.getClassLoader().getResourceAsStream(CAREDX_FILE)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");

      Elements elementsByTag = parsed.getElementsByTag("allele");
      for (Element e : elementsByTag) {
        try {
          processAllele(builder, e);
        } catch (NullPointerException e1) {
          System.err.println("Couldn't find allele name: " + e.toString());
          continue;
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Invalid XML file: " + CAREDX_FILE);
    }

    return builder.build();
  }

  private static void processAllele(Builder<HLAType, SeroType> builder, Element e) {
    Element elementById = e.getElementsByTag("allelename").get(0);
    if (elementById == null) {
      elementById = e.getElementsByTag("alleleName").get(0);
    }
    if (elementById == null) {
      System.err.println("Couldn't find allele name: " + e.toString());
      return;
    }
    final String alleleText = elementById.text();
    HLAType allele = HLAType.valueOf(alleleText);
    Elements expertElements = e.getElementsByTag("expert");
    String careDxSeroText = expertElements.isEmpty() ? null : expertElements.get(0).text();
    Elements whoElements = e.getElementsByTag("WHO");
    String whoSeroText = whoElements.isEmpty() ? null : whoElements.get(0).text();
    boolean inval1 = careDxSeroText == null || INVALID.stream().map(String::toLowerCase)
        .filter(s -> careDxSeroText.toLowerCase().contains(s)).count() > 0;
    boolean inval2 = whoSeroText == null || INVALID.stream().map(String::toLowerCase)
        .filter(s -> whoSeroText.toLowerCase().contains(s)).count() > 0;
    if (inval1 && inval2) {
      return;
    }

    try {
      HLALocus locus = HLALocus.valueOf("" + careDxSeroText.charAt(0));
      put(builder, careDxSeroText.substring(1), locus, allele.specString());
    } catch (IllegalArgumentException e1) {
      try {
        HLALocus locus = HLALocus.valueOf("" + whoSeroText.charAt(0));
        put(builder, whoSeroText.substring(1), locus, allele.specString());
      } catch (IllegalArgumentException e2) {
        return;
      }
    }
  }

  private static ImmutableMap<HLAType, SeroType> buildManualOverrideLookup() {
    // Build the equivalencies map
    Builder<HLAType, SeroType> builder = ImmutableMap.builder();
    // TODO would be nice to read this from a file instead

    // -- B Locus --
    put(builder, "64", HLALocus.B, "14:01");
    put(builder, "65", HLALocus.B, "14:02");
    put(builder, "14", HLALocus.B, "14:03");
    put(builder, "62", HLALocus.B, "15:01", "15:04", "15:05", "15:06", "15:07", "15:15", "15:24");
    put(builder, "75", HLALocus.B, "15:02", "15:08");
    put(builder, "72", HLALocus.B, "15:46", "15:03");
    put(builder, "70", HLALocus.B, "15:09");
    put(builder, "71", HLALocus.B, "15:10", "15:18");
    put(builder, "76", HLALocus.B, "15:12", "15:14");
    put(builder, "77", HLALocus.B, "15:13");
    put(builder, "63", HLALocus.B, "15:16", "15:17");
    put(builder, "35", HLALocus.B, "15:22");
    put(builder, "60", HLALocus.B, "40:01", "40:10");
    put(builder, "61", HLALocus.B, "40:02", "40:06", "40:09", "40:20");
    put(builder, "50", HLALocus.B, "40:05");
    put(builder, "45", HLALocus.B, "44:09");
    put(builder, "50", HLALocus.B, "50:01");
    put(builder, "45", HLALocus.B, "50:02");

    // -- C Locus --
    put(builder, "10", HLALocus.C, "03:02", "03:04");
    put(builder, "9", HLALocus.C, "03:03");
    put(builder, "3", HLALocus.C, "03:05", "03:07");

    // -- DR1 Locus --
    put(builder, "0103", HLALocus.DRB1, "01:03");
    put(builder, "17", HLALocus.DRB1, "03:01", "03:04", "03:05");
    put(builder, "18", HLALocus.DRB1, "03:02", "03:03");

    // -- DQ Locus --
    put(builder, "7", HLALocus.DQB1, "03:01", "03:04", "03:13", "03:19");
    put(builder, "8", HLALocus.DQB1, "03:02", "03:05");
    put(builder, "9", HLALocus.DQB1, "03:03");

    return builder.build();
  }

  // put method for creating the reference table used in the get method.
  private static void put(Builder<HLAType, SeroType> builder, String serotypeSpec, HLALocus locus,
      String... hlaSpecs) {
    SeroType s = new SeroType(locus.sero(), serotypeSpec);
    // create a one to one table for mapping alleles to serotype
    for (String allele : hlaSpecs) {
      HLAType a = new HLAType(locus, allele);
      builder.put(a, s);
    }
  }

  /**
   * @return The {@link SeroType} equivalent for the 2-field input genotype, or {@code null} if no
   *         explicit mapping exists.
   */
  public static SeroType get(HLAType allele) {
    HLAType manualAllele = allele;
    if (allele.spec().size() > 2) {
      // All the manual equivalencies are based on 2-field alleles
      manualAllele = new HLAType(allele.locus(), allele.spec().subList(0, 2));
    }
    if (allele.resolution() == 1) {
      manualAllele = HLAType.growSpec(allele);
    }
    if (manualEquivalencies.containsKey(manualAllele)) {
      return manualEquivalencies.get(manualAllele);
    }
    if (careDxEquivalencies.containsKey(allele)) {
      return careDxEquivalencies.get(allele);
    }
    if (careDxEquivalencies.containsKey(manualAllele)) {
      return careDxEquivalencies.get(manualAllele);
    }
    return null;
  }
}
