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
package org.pankratzlab.unet.parser.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.NullType;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Specific parsing logic for SureTyper PDFs
 */
public class PdfSureTyperParser {
  private static final String SURE_TYPER = "SureTyper";
  private static final String SUMMARY_START = "SUMMARY";
  private static final String SUMMARY_END = SURE_TYPER;
  private static final String HLA_PREFIX = "HLA";
  private static final String UNKNOWN_ANTIGEN = "-";
  private static final String WHITESPACE_REGEX = "\\s+";
  private static final String TYPING_STOP_TOKEN = SURE_TYPER;
  private static final String TYPING_START_TOKEN = "LABORATORY ASSIGNMENT";
  private static final String GENOTYPE_HEADER = "ALLELES ANTIGEN";
  private static final int DONOR_ID_INDEX = 2;
  private static final String PATIENT_ID_TOKEN = "Patient ID:";

  private static final String BW = "Bw:";
  private static final String HLA_DPB1 = "HLA-DPB1:";
  private static final String HLA_DPA1 = "HLA-DPA1:";
  private static final String HLA_DQB1 = "HLA-DQB1:";
  private static final String HLA_DQA1 = "HLA-DQA1:";
  private static final String HLA_DRB1 = "HLA-DRB1:";
  private static final String HLA_C = "HLA-C:";
  private static final String HLA_B = "HLA-B:";
  private static final String HLA_A = "HLA-A:";
  private static final String HLA_DRB345 = "HLA-DRB345:";
  private static final String HLA_DRB3 = "HLA-DRB3:";
  private static final String HLA_DRB4 = "HLA-DRB4:";
  private static final String HLA_DRB5 = "HLA-DRB5:";
  private static final String HAPLOTYPE_C = "HLA-C";
  private static final String HAPLOTYPE_B = "HLA-B";
  private static final String HAPLOTYPE_DRB1 = "HLA-DRB1";
  private static final String HAPLOTYPE_DQB1 = "HLA-DQB1";
  private static final String HAPLOTYPE_DRB345 = "HLA-DRB345";
  private static ImmutableMap<String, TypeSetter> metadataMap;

  /**
   * Helper method to process the individual type assignment tokens tokens
   */
  public static void parseTypes(ValidationModelBuilder builder, String[] lines) {
    if (metadataMap == null) {
      init();
    }

    Map<String, Multimap<Strand, HLAType>> haplotypeMap = new HashMap<>();
    haplotypeMap.put(HAPLOTYPE_B, ArrayListMultimap.create());
    haplotypeMap.put(HAPLOTYPE_C, ArrayListMultimap.create());
    haplotypeMap.put(HAPLOTYPE_DRB1, ArrayListMultimap.create());
    haplotypeMap.put(HAPLOTYPE_DQB1, ArrayListMultimap.create());
    haplotypeMap.put(HAPLOTYPE_DRB345, ArrayListMultimap.create());
    Map<Strand, BwGroup> bwMap = new HashMap<>();

    // We now process the PDF text line-by-line.
    StringJoiner typeAssignment = new StringJoiner(" ");

    // All the type assignment strings are joined together and then split on whitespace,
    // creating a stream of tokens.
    for (int currentLine = 0; currentLine < lines.length; currentLine++) {
      String line = lines[currentLine].trim();

      if (line.startsWith(PATIENT_ID_TOKEN)) {
        // The patient ID value is at a particular position in the line starting with this token
        builder.donorId(line.split(WHITESPACE_REGEX)[DONOR_ID_INDEX]);
      } else if (line.trim().equals(SUMMARY_START)) {
        parseSummary(lines, typeAssignment, ++currentLine);
      } else if (line.contains(TYPING_START_TOKEN)) {
        // After encountering this token, all following lines contain type assignment data
        currentLine = parseAssignment(lines, typeAssignment, ++currentLine);
      } else if (haplotypeMap.keySet().contains(line)) {
        // This is a genotype section
        if (HAPLOTYPE_DRB345.equals(line)) {
          // Have to parse DRB3, 4 and 5 separately
          Multimap<Strand, HLAType> drb345Map = haplotypeMap.get(line);
          parseHaplotype(lines, currentLine + 1, "DRB3", drb345Map, bwMap);
          parseHaplotype(lines, currentLine + 1, "DRB4", drb345Map, bwMap);
          currentLine = parseHaplotype(lines, ++currentLine, "DRB5", drb345Map, bwMap);
        } else {
          String locus = line.replaceAll("HLA-", "");
          Multimap<Strand, HLAType> locusMap = haplotypeMap.get(line);
          currentLine = parseHaplotype(lines, ++currentLine, locus, locusMap, bwMap);

          if (HAPLOTYPE_B.equals(line)) {
            for (Strand strand : bwMap.keySet()) {
              BwGroup currentGroup = bwMap.get(strand);
              Iterator<HLAType> iterator = locusMap.get(strand).iterator();
              while (iterator.hasNext()) {
                HLAType next = iterator.next();
                BwGroup nextGroup = BwSerotypes.getBwGroup(next);
                if (!Objects.equals(currentGroup, nextGroup)) {
                  iterator.remove();
                }
              }
            }
            System.out.println();
          }
        }
      }
    }

    // Adjust DRB345 for unreported types
    for (Entry<Strand, HLAType> entry : haplotypeMap.get(HAPLOTYPE_DRB1).entries()) {
      if (Objects.isNull(DRAssociations.getDRBLocus(entry.getValue().lowResEquiv()))) {
        // This DRB1 has an unreported DRB345 that needs to be manually registered
        Multimap<Strand, HLAType> drb345Map = haplotypeMap.get(HAPLOTYPE_DRB345);
        Strand unreportedStrand = entry.getKey();
        if (drb345Map.containsKey(unreportedStrand)) {
          // ensure we don't overwrite an existing mapping
          unreportedStrand = unreportedStrand.flip();
        }
        drb345Map.put(unreportedStrand, NullType.UNREPORTED_DRB345);
      }
    }

    builder.bw4(false);
    builder.bw6(false);

    builder.bHaplotype(haplotypeMap.get(HAPLOTYPE_B));
    builder.cHaplotype(haplotypeMap.get(HAPLOTYPE_C));
    builder.dqHaplotype(haplotypeMap.get(HAPLOTYPE_DQB1));
    builder.drHaplotype(haplotypeMap.get(HAPLOTYPE_DRB1));
    builder.dr345Haplotype(haplotypeMap.get(HAPLOTYPE_DRB345));
    builder.bwHaplotype(bwMap);

    BiConsumer<ValidationModelBuilder, String> setter = null;
    String prefix = "";

    for (String token : typeAssignment.toString().split(WHITESPACE_REGEX)) {
      if (metadataMap.containsKey(token)) {
        // When we encounter a section key we update the prefix string and the field setter
        TypeSetter metadata = metadataMap.get(token);
        setter = metadata.getSetter();
        prefix = metadata.getTokenPrefix();
      } else if (setter != null) {
        // Erase the prefix from the current token and set the value on the model builder
        setter.accept(builder, token.replace(prefix, ""));
      }
    }
  }

  /**
   * Helper method to parse the summary section. This is where the selected DRB345 alleles are
   * stored.
   */
  private static int parseSummary(String[] lines, StringJoiner typeAssignment, int currentLine) {
    // go line-by-line, split on whitespace, look for DRB[3/4/5]* tokens and convert to line
    final ImmutableSet<String> validLoci = ImmutableSet.of(HLALocus.DRB3.toString(),
        HLALocus.DRB4.toString(), HLALocus.DRB5.toString());
    boolean homozygous = false;
    for (; currentLine < lines.length; currentLine++) {
      String line = lines[currentLine];
      if (line.contains(SUMMARY_END)) {
        break;
      }
      String[] tokens = line.split(WHITESPACE_REGEX);
      for (String token : tokens) {
        String type = null;
        // Check if this is a non-null DRB3/4/5
        if (validLoci.contains(token)) {
          // For homozygous cases that are reported, the locus appears twice.. once by itself, the
          // other with the allele designation.
          homozygous = true;
        } else if ((token.contains(HLALocus.DRB3 + "*") || token.contains(HLALocus.DRB4 + "*")
            || token.contains(HLALocus.DRB5 + "*")) && !token.endsWith("N")) {
          type = token;
        }
        if (Objects.nonNull(type)) {
          if (type.contains(":")) {
            type = type.substring(0, type.indexOf(":"));
          }
          String typeAssignmentEntry =
              HLA_PREFIX + "-" + type.substring(0, type.indexOf("*")) + ": ";
          typeAssignmentEntry += type;
          if (homozygous) {
            typeAssignmentEntry += (" " + type);
          }
          typeAssignment.add(typeAssignmentEntry);
        }

      }
    }

    return currentLine;

  }

  /**
   * Helper method to parse the possible haplotypes. These are long lists of possible alleles,
   * divided by HLA locus.
   */
  private static int parseHaplotype(String[] lines, int currentLine, String locus,
      Multimap<Strand, HLAType> strandMap, Map<Strand, BwGroup> bwMap) {
    // Sections start with a line containing JUST HLA_A/b/c etc..
    // Strands are marked by first type is always low res (group)
    // Read until we get to a semi-colon

    String line = null;
    int strandIndex = -1;
    // Read until we hit the end of the genotype data
    for (; currentLine < lines.length && strandIndex < Strand.values().length; currentLine++) {
      line = lines[currentLine].trim();

      String[] tokens = line.split(WHITESPACE_REGEX);
      int tokenIndex = 0;

      // Check if we are at a strand break
      if (line.startsWith(SURE_TYPER) || line.contains(GENOTYPE_HEADER)) {
        // These indicate page breaks and have nothing to do with the data
        continue;
      }
      if (line.startsWith(HLA_PREFIX)) {
        // This indicates a new locus is starting
        break;
      }
      if (locus.startsWith("DRB") && line.matches("^DRB[0-9]\\*[0-9]+") && !line.contains(locus)
          && strandIndex >= 0) {
        // DRB345 are all in one section - we do not want to accidentally mix DRB loci. We also
        // check the strand index because we do not want to prematurely end iteration.
        break;
      }

      if (line.matches(locus + "[*w][0-9]+.*")) {
        // This is the first line of allele data. It is possible for it to appear without a
        // GENOTYPE_HEADER if both alleles are on the same page.
        strandIndex++;

        if (!strandMap.isEmpty() && locus.matches("DRB[345]") && line.contains(locus)) {
          strandIndex++;
          // Each DRB345 locus has to be parsed separately. It is possible the map could've been
          // previously parsed, in which case we don't want to start from the first strand.
        }

        // Skip the first token. The first token is the allele group pattern (e.g. B*02)
        tokenIndex++;
      }

      // If we're on a valid strand, parse the tokens to alleles
      for (; tokenIndex < tokens.length
          && (strandIndex >= 0 && strandIndex < Strand.values().length); tokenIndex++) {
        String token = tokens[tokenIndex].trim();

        // Explicitly parse the Bw antigen token
        if (token.matches("B[0-9]+")) {
          bwMap.put(Strand.values()[strandIndex], BwSerotypes.getBwGroup(token));
        }

        if (locus.startsWith(token.replaceAll("[0-9*w]", "")) || token.equals(UNKNOWN_ANTIGEN)) {
          // These cases are either reiterations of the locus (e.g. B*) or antigen equivalents (B75,
          // Cw)
          continue;
        }

        if (!token.contains(":")) {
          // Not an allele specificity
          continue;
        }

        // Sanitize the token string
        token = token.replaceAll("[^0-9:nN-]", "");

        if (token.isEmpty()) {
          // Wasn't actually an allele
          continue;
        }

        HaplotypeUtils.parseAllelesToStrandMap(token, locus, strandIndex, strandMap);
      }

      // Update strand index when we reach the end of an individual allele section
    }

    return --currentLine;
  }

  /**
   * Helper method to parse the laboratory assigned types. These are what will be reported to UNOS.
   */
  private static int parseAssignment(String[] lines, StringJoiner typeAssignment, int currentLine) {
    String line = null;
    // Read until we hit the end of the typing
    for (; currentLine < lines.length
        && !(line = lines[currentLine].trim()).contains(TYPING_STOP_TOKEN); currentLine++) {
      // Building the type assignment lines
      typeAssignment.add(line);
    }
    return --currentLine;
  }

  private static void init() {
    // A PDF is just text with no formal structure. Type text is stored in this PDF in the format:
    // HLA-A: A18 A24 HLA-B: B8 B72 ... etc
    // Therefore to interpret this data we need to link three elements:
    // 1. The token indicating a change in locus
    // 2. The prefix string to each antigen token
    // 3. The appropriate setter method in ValidationModelBuilder
    // NB: this section MUST be parsed completely, even if particular loci are unused, to avoid
    // interfering with other loci
    Builder<String, TypeSetter> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(HLA_A, new TypeSetter("A", ValidationModelBuilder::a));
    setterBuilder.put(HLA_B, new TypeSetter("B", ValidationModelBuilder::b));
    setterBuilder.put(HLA_C, new TypeSetter("Cw", ValidationModelBuilder::c));
    setterBuilder.put(HLA_DRB1, new TypeSetter("DR", ValidationModelBuilder::drb));
    setterBuilder.put(HLA_DQA1, new TypeSetter("DQA1*", ValidationModelBuilder::dqa));
    setterBuilder.put(HLA_DQB1, new TypeSetter("DQ", ValidationModelBuilder::dqb));

    setterBuilder.put(HLA_DRB3, new TypeSetter("DRB3*", ValidationModelBuilder::dr52));
    setterBuilder.put(HLA_DRB4, new TypeSetter("DRB4*", ValidationModelBuilder::dr53));
    setterBuilder.put(HLA_DRB5, new TypeSetter("DRB5*", ValidationModelBuilder::dr51));

    // DR is only low-res at this point and we need high-res
    setterBuilder.put(HLA_DRB345, new TypeSetter("DR", PdfSureTyperParser::noOp));

    // DPA1 is present in the typing data but we do not currently track it in the validation model
    // (not entered in DonorNet)
    setterBuilder.put(HLA_DPA1, new TypeSetter("DPA1*", PdfSureTyperParser::noOp));

    setterBuilder.put(HLA_DPB1, new TypeSetter("DPB1*", ValidationModelBuilder::dpb));

    // Boolean values appear as literal values, indicating true, and are simply absent if false
    setterBuilder.put(BW, new TypeSetter("", PdfSureTyperParser::decodeBw));

    metadataMap = setterBuilder.build();
  }

  /**
   * Helper method to call the appropriate {@link ValidationModelBuilder} method when a Bw value is
   * encountered.
   */
  private static void decodeBw(ValidationModelBuilder builder, String value) {
    switch (value) {
      case "Bw4":
        builder.bw4(true);
        break;
      case "Bw6":
        builder.bw6(true);
        break;
    }
  }

  /**
   * Helper method to call for unsupported loci/tokens
   */
  private static void noOp(ValidationModelBuilder builder, String value) {
    // Nothing to do
  }

}
