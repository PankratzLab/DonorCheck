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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/** Specific parsing logic for SureTyper PDFs */
public class PdfSureTyperParser {
  private static final String DRB345_REGEX = "DRB[345]";
  private static final String PAGE_END = "page";
  private static final String SUMMARY_START = "SUMMARY";
  private static final String SUMMARY_END = "ALLELES";
  private static final String HLA_PREFIX = "HLA";
  private static final String WHITESPACE_REGEX = "\\s+";

  private static final Set<String> TYPING_STOP_TOKENS =
      ImmutableSet.of("ALLELES", "INTERNAL", "REVIEW", "NOTES");
  // Set of strings to ensure typeAssignment only gets filled with appropriate values
  private static final Set<String> HLA_TOKENS =
      ImmutableSet.of("DRB", "DPA", "DPB", "HLA", "DQA", "DQB");

  private static final String TYPING_START_TOKEN = "LABORATORY ASSIGNMENT";
  private static final String SESSION_HISTORY_TOKEN = "SESSION HISTORY";
  private static final String GENOTYPE_HEADER = "ALLELES ANTIGEN";
  private static final int DONOR_ID_INDEX = 2;
  private static final String UNOS_PATIENT_ID_TOKEN = "Donor UNOS ID:";
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

  /** Helper method to process the individual type assignment tokens tokens */
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
    haplotypeMap.put("HLA-DR", haplotypeMap.get(HAPLOTYPE_DRB1));

    // We now process the PDF text line-by-line.
    StringJoiner typeAssignment = new StringJoiner(" ");
    // All the type assignment strings are joined together and then split on whitespace,
    // creating a stream of tokens.
    String pid = null;
    String upid = null;

    for (int currentLine = 0; currentLine < lines.length; currentLine++) {
      String line = lines[currentLine].trim();
      // If we have parsed to session history we need to break because it contains repeats of key
      // words
      if (line.equals(SESSION_HISTORY_TOKEN)) {
        break;
      } else if (line.contains(PATIENT_ID_TOKEN) || line.contains(UNOS_PATIENT_ID_TOKEN)) {
        if (line.contains(PATIENT_ID_TOKEN)) {
          // The patient ID value is at a particular position in the line starting with this token
          if (line.startsWith(PATIENT_ID_TOKEN)) {
            pid = line.split(WHITESPACE_REGEX)[DONOR_ID_INDEX];
          } else {
            pid = line.split(PATIENT_ID_TOKEN)[1].split(WHITESPACE_REGEX)[1];
          }
        }
        if (line.contains(UNOS_PATIENT_ID_TOKEN)) {
          if (line.startsWith(UNOS_PATIENT_ID_TOKEN)) {
            upid = line.split(WHITESPACE_REGEX)[3];
          } else {
            upid = line.split(UNOS_PATIENT_ID_TOKEN)[1].split(WHITESPACE_REGEX)[1];
          }
        }
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
          parseHaplotype(lines, currentLine + 1, "DRB3", drb345Map);
          parseHaplotype(lines, currentLine + 1, "DRB4", drb345Map);
          currentLine = parseHaplotype(lines, ++currentLine, "DRB5", drb345Map);
        } else {
          String locus = HaplotypeUtils.lineToLocus(line);
          Multimap<Strand, HLAType> locusMap = haplotypeMap.get(line);
          currentLine = parseHaplotype(lines, ++currentLine, locus, locusMap);
        }
      }
    }

    if (upid == null) {
      builder.donorId(pid);
    } else {
      builder.donorId(upid);
    }

    // Adjust DRB345 for unreported types
    for (Entry<Strand, HLAType> entry : haplotypeMap.get(HAPLOTYPE_DRB1).entries()) {
      if (!DRAssociations.getDRBLocus(entry.getValue().lowResEquiv()).isPresent()) {
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
    BiConsumer<ValidationModelBuilder, String> setter = null;
    String prefix = "";
    for (String token : typeAssignment.toString().split(WHITESPACE_REGEX)) {
      if (metadataMap.containsKey(token)) {
        // When we encounter a section key we update the prefix string and the field setter
        TypeSetter metadata = metadataMap.get(token);
        setter = metadata.getSetter();
        prefix = metadata.getTokenPrefix();
      } else if (setter != null && token.matches(".*\\d.*")) {
        // Erase the prefix from the current token and set the value on the model builder
        token = token.replace(prefix, "");
        token = token.replaceAll("[+]", "");
        setter.accept(builder, token);
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
        // Separate type from the rest of the allele
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
   * Helper method to check if any of the given flags are contained in the given line
   *
   * @return true if a flag is found
   */
  private static boolean containsFlag(Set<String> flags, String line) {
    for (String flag : flags) {
      if (line.contains(flag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Helper method to parse the possible haplotypes. These are long lists of possible alleles,
   * divided by HLA locus.
   */
  private static int parseHaplotype(String[] lines, int currentLine, String locus,
      Multimap<Strand, HLAType> strandMap) {
    // Sections start with a line containing JUST HLA_A/b/c etc..
    // Strands are marked by first type is always low res (group)

    String line = null;
    int strandIndex = -1;
    // Read until we hit the end of the genotype data
    for (; currentLine < lines.length && strandIndex < Strand.values().length; currentLine++) {
      line = lines[currentLine].trim();

      String[] tokens = line.split("\\s+|;");
      int tokenIndex = 0;

      // Check if we are at a strand break
      if (line.startsWith(PAGE_END) || line.contains(GENOTYPE_HEADER)) {
        // These indicate page breaks and have nothing to do with the data
        continue;
      }
      if (line.startsWith(HLA_PREFIX)) {
        // This indicates a new locus is starting
        break;
      }

      // DRB345 are unfortunately handled differently than other haplotype sections, and are not
      // consistent between SureTyper versions
      boolean isDRB345Locus = locus.matches(DRB345_REGEX);

      // Check if this is the first line of the allele data we're interested in, which will be
      // marked by a token in the form of:
      // A*01
      // Cw10
      // DRB3
      if (line.matches(locus + "[*w][0-9]+.*") || (isDRB345Locus && tokens[0].equals(locus))) {

        // Now we have to ensure we're parsing the target DRB345 locus
        if (isDRB345Locus) {
          if (!line.contains(locus)) {
            // This is the wrong allele section - could be a DRB1 or other DRB345
            continue;
          } else if (!strandMap.isEmpty()) {
            // We are starting our target locus here, but we have already parsed something (a
            // different DRB345) to the strand map. So we want to start at the next strand index
            strandIndex++;
          }
        }

        strandIndex++;

        // Skip the first token. The first token is the allele group pattern (e.g. B*02)
        tokenIndex++;
      }

      // Here we start parsing lines to alleles, but only if we've confirmed the allele section has
      // started strandIndex > 0)
      for (; tokenIndex < tokens.length
          && (strandIndex >= 0 && strandIndex < Strand.values().length); tokenIndex++) {

        String token = tokens[tokenIndex].replaceAll("\\s+", "");

        // if the dash is at the end it is not a range.
        if (token.endsWith("-")) {
          token = token.replaceAll("-", "");
        }

        if (!token.contains(":")) {
          // Not an allele specificity
          continue;
        }

        // Sanitize the token string
        token = token.replaceAll("[^0-9:nN-]", "");

        if (!token.matches(".*[0-9].*")) {
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
    for (; currentLine < lines.length; currentLine++) {
      line = lines[currentLine].trim();
      if (containsFlag(TYPING_STOP_TOKENS, line)) {
        break;
      } else if (containsFlag(HLA_TOKENS, line)) {
        // Building the type assignment lines
        typeAssignment.add(line);
      }
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
    setterBuilder.put(HLA_DQA1, new TypeSetter("DQA1*", ValidationModelBuilder::dqaSerotype));
    setterBuilder.put(HLA_DQB1, new TypeSetter("DQ", ValidationModelBuilder::dqbSerotype));

    setterBuilder.put(HLA_DRB3, new TypeSetter("DRB3*", ValidationModelBuilder::dr52));
    setterBuilder.put(HLA_DRB4, new TypeSetter("DRB4*", ValidationModelBuilder::dr53));
    setterBuilder.put(HLA_DRB5, new TypeSetter("DRB5*", ValidationModelBuilder::dr51));

    // DR is only low-res at this point and we need high-res
    setterBuilder.put(HLA_DRB345, new TypeSetter("DR", PdfSureTyperParser::noOp));

    // DPA1 is present in the typing data but we do not currently track it in the validation model
    // (not entered in DonorNet)
    setterBuilder.put(HLA_DPA1, new TypeSetter("DPA1*", ValidationModelBuilder::dpaSerotype));

    setterBuilder.put(HLA_DPB1, new TypeSetter("DPB1*", ValidationModelBuilder::dpbSerotype));

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

  /** Helper method to call for unsupported loci/tokens */
  private static void noOp(ValidationModelBuilder builder, String value) {
    // Nothing to do
  }
}
