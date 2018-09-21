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
package org.pankratzlab.unet.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Static utility class for decoding QTyper PDF output
 */
public final class PdfQTyperParser {

  private PdfQTyperParser() {
    // no-op
  }

  private static final String TYPING_STOP_TOKEN =
      "This typing result was assigned manually by the user";
  private static final String TYPING_START_TOKEN = "Laboratory assignment";
  private static final String PATIENT_ID_TOKEN = "Patient ID";
  private static final String HLA_DPB1 = "HLA-DPB1";
  private static final String HLA_DPA1 = "HLA-DPA1";
  private static final String HLA_DQB1 = "HLA-DQB1";
  private static final String HLA_DQA1 = "HLA-DQA1";
  private static final String HLA_DRB1 = "HLA-DRB";
  private static final String HLA_C = "HLA-C";
  private static final String HLA_B = "HLA-B";
  private static final String HLA_A = "HLA-A";
  private static final String HETERO_TOKEN = ";";


  private static ImmutableMap<String, PdfMetadata> metadataMap;

  /**
   * Helper method to process the individual type assignment tokens
   */
  public static void parseTypes(ValidationModelBuilder builder, String[] lines) {
    if (metadataMap == null) {
      init();
    }

    // Process the PDF line-by-line
    List<String> assignmentLines = new ArrayList<>();
    boolean inAssignment = false;

    // Assignment lines are extracted. Sometimes alleles can span multiple lines, in which case we
    // try to unify the lines to a single string.
    for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
      String line = lines[lineNumber];
      if (line.startsWith(PATIENT_ID_TOKEN)) {
        // ID is actually on the following line
        String idLine = lines[++lineNumber];
        builder.donorId(idLine.split("\\s+")[0]);
      } else if (line.contains(TYPING_START_TOKEN)) {
        // After encountering this token, all following lines contain type assignment data
        inAssignment = true;
      } else if (inAssignment) {
        if (line.contains(TYPING_STOP_TOKEN)) {
          // Once we hit this token, typing is over
          inAssignment = false;
        } else {
          // Ensure this is ACTUALLY a typing line
          if (isAssignmentLine(line)) {

            // Then check if the following lines are related to this line.
            for (; lineNumber < lines.length - 1; lineNumber++) {
              String nextLine = lines[lineNumber + 1];
              if (isAssignmentLine(nextLine) || nextLine.contains(TYPING_STOP_TOKEN)) {
                // If it's a different assignment or a stop signal, we can stop appending
                break;
              }

              // otherwise join the next line to this
              line += " " + nextLine;
            }

            // Add the built up line to our collection of lines
            assignmentLines.add(line);
          }

        }
      }
    }

    builder.bw4(false);
    builder.bw6(false);
    builder.dr51(false);
    builder.dr52(false);
    builder.dr53(false);

    // Update the builder based on the PDF assignment data
    for (String line : assignmentLines) {
      String lineKey = getKey(line);
      PdfMetadata metadata = metadataMap.get(lineKey);
      setFields(builder, metadata.setter, metadata.tokenPrefix, line);
    }
  }

  private static void init() {
    // FIXME very close to the same as SureTyper, but slightly different
    // To interpret this data we need to link three elements:
    // 1. The token indicating a change in locus
    // 2. The prefix string to each antigen token
    // 3. The appropriate setter method in ValidationModelBuilder
    Builder<String, PdfMetadata> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(HLA_A, new PdfMetadata("A", ValidationModelBuilder::a));
    setterBuilder.put(HLA_B, new PdfMetadata("B", ValidationModelBuilder::b));
    setterBuilder.put(HLA_C, new PdfMetadata("Cw", ValidationModelBuilder::c));
    setterBuilder.put(HLA_DRB1, new PdfMetadata("DR", ValidationModelBuilder::drb));
    setterBuilder.put(HLA_DQA1, new PdfMetadata("DQA1*", ValidationModelBuilder::dqa));
    setterBuilder.put(HLA_DQB1, new PdfMetadata("DQ", ValidationModelBuilder::dqb));

    setterBuilder.put(HLA_DPA1, new PdfMetadata("DPA1*", PdfQTyperParser::noOp));
    setterBuilder.put(HLA_DPB1, new PdfMetadata("DPB1*", ValidationModelBuilder::dpb));

    metadataMap = setterBuilder.build();
  }

  /**
   * @return True if the given line starts an assignment for a locus
   */
  private static boolean isAssignmentLine(String line) {
    return metadataMap.containsKey(getKey(line));
  }

  /**
   * @return The string used as a key to the metadataMap
   */
  private static String getKey(String line) {
    String[] split = line.split("\\s+");
    if (split.length == 0) {
      return "";
    }

    return split[0];
  }

  /**
   * Process a line for the specificities assigned by that line
   *
   * @param typePrefix Leading string on assigned types (ignored)
   * @param line Input assignment text
   * @return A list of all specificities discovered in the given line
   */
  private static List<String> getSpecs(String typePrefix, String line) {
    List<String> specList = new ArrayList<>();

    String[] tokens = line.split("\\s+");

    if (line.contains(HLA_DQA1) || line.contains(HLA_DPB1)) {
      // Only high-res types entered for these loci
      specList.add(clean(typePrefix, tokens[1]));

      if (tokens.length >= 3) {
        specList.add(clean(typePrefix, tokens[2]));
      }

      // For DQA we have to remove the high-resolution typing
      if (line.contains(HLA_DQA1)) {
        List<String> tmpList = new ArrayList<>();
        for (String s : specList) {
          if (s.contains(":")) {
            s = s.substring(0, s.indexOf(":"));
          }
          tmpList.add(s);
        }
        specList = tmpList;
      }
    } else if (line.contains(HETERO_TOKEN)) {
      // Other loci have a serological equivalent section, heterozygotes separated by a ;
      specList.add(clean(typePrefix, tokens[3]));
      specList.add(clean(typePrefix, tokens[4]));
    } else {
      specList.add(clean(typePrefix, tokens[2]));
    }

    return specList;
  }

  /**
   * @return the base input string striped off the exclusion string + {@link #HETERO_TOKEN}
   */
  private static String clean(String exclusion, String base) {
    // Remove heterozygous token
    String tmp = base.replaceAll(HETERO_TOKEN, "");

    // Remove prefix text
    tmp = tmp.replaceAll(exclusion, "");

    return tmp;
  }

  /**
   * Set the fields on a builder based on type assignments found in a given line
   */
  private static void setFields(ValidationModelBuilder builder,
      BiConsumer<ValidationModelBuilder, String> setter, String exclusion, String line) {
    for (String spec : getSpecs(exclusion, line)) {
      setter.accept(builder, spec);
    }
    if (line.contains("Bw4")) {
      builder.bw4(true);
    }
    if (line.contains("Bw6")) {
      builder.bw6(true);
    }
    if (line.contains("DR51")) {
      builder.dr51(true);
    }
    if (line.contains("DR52")) {
      builder.dr52(true);
    }
    if (line.contains("DR53")) {
      builder.dr53(true);
    }
  }

  /**
   * Helper method to call for unsupported loci/tokens
   */
  private static void noOp(ValidationModelBuilder builder, String value) {
    // Nothing to do
  }

  /**
   * Helper class to link together related {@link ValidationModelBuilder} setters and token
   * specificity prefixes.
   */
  private static class PdfMetadata {

    private String tokenPrefix;
    private BiConsumer<ValidationModelBuilder, String> setter;

    public PdfMetadata(String tokenPrefix, BiConsumer<ValidationModelBuilder, String> setter) {
      super();
      this.tokenPrefix = tokenPrefix;
      this.setter = setter;
    }
  }
}
