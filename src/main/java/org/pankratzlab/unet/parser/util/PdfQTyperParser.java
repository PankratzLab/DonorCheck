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

  private static final String DPB_PREFIX = "DPB1\\\\*";
  private static final String DQ_PREFIX = "DQ";
  private static final String DQA_PREFIX = "DQA1\\\\*";
  private static final String DR_PREFIX = "DR";
  private static final String C_PREFIX = "Cw";
  private static final String B_PREFIX = "B";
  private static final String DPA_PREFIX = "DPA1\\\\*";
  private static final String A_PREFIX = "A";

  private PdfQTyperParser() {
    // no-op
  }

  private static final String TYPING_STOP_TOKEN = "Test details";
  private static final String TYPING_START_TOKEN = "Group Allele tolerance range";
  private static final String PATIENT_ID_TOKEN = "Patient ID";
  private static final String DPB_TYPE_LINE = "DPB1";
  private static final String DPA_TYPE_LINE = "DPA1";
  private static final String DQB_TYPE_LINE = "DQB1";
  private static final String DQA_TYPE_LINE = "DQA1";
  private static final String DRB_TYPE_LINE = "DRB1";
  private static final String C_TYPE_LINE = "C";
  private static final String B_TYPE_LINE = B_PREFIX;
  private static final String A_TYPE_LINE = A_PREFIX;
  private static final String ASSIGNED_TYPE_PREFIX = "HLA-";
  private static final String DR_CLASS_LINE = ASSIGNED_TYPE_PREFIX + "DRB";
  private static final String BW_CLASS_LINE = ASSIGNED_TYPE_PREFIX + "B";


  private static ImmutableMap<String, PdfMetadata> metadataMap;

  private static void init() {
    // FIXME very close to the same as SureTyper, but slightly different
    // To interpret this data we need to link three elements:
    // 1. The token indicating a change in locus
    // 2. The prefix string to each antigen token
    // 3. The appropriate setter method in ValidationModelBuilder
    Builder<String, PdfMetadata> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(A_TYPE_LINE, new PdfMetadata(A_PREFIX, ValidationModelBuilder::a));
    setterBuilder.put(B_TYPE_LINE, new PdfMetadata(B_PREFIX, ValidationModelBuilder::b));
    setterBuilder.put(C_TYPE_LINE, new PdfMetadata(C_PREFIX, ValidationModelBuilder::c));
    setterBuilder.put(DRB_TYPE_LINE, new PdfMetadata(DR_PREFIX, ValidationModelBuilder::drb));
    setterBuilder.put(DQA_TYPE_LINE, new PdfMetadata(DQA_PREFIX, ValidationModelBuilder::dqa));
    setterBuilder.put(DQB_TYPE_LINE, new PdfMetadata(DQ_PREFIX, ValidationModelBuilder::dqb));

    setterBuilder.put(DPA_TYPE_LINE, new PdfMetadata(DPA_PREFIX, PdfQTyperParser::noOp));
    setterBuilder.put(DPB_TYPE_LINE, new PdfMetadata(DPB_PREFIX, ValidationModelBuilder::dpb));

    metadataMap = setterBuilder.build();
  }

  /**
   * Helper method to process the individual type assignment tokens
   * 
   * Group Allele tolerance range: (0) Serological eq. Test details
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
      } else if (line.startsWith(DR_CLASS_LINE) || line.startsWith(BW_CLASS_LINE)) {

        for (; lineNumber < lines.length - 1; lineNumber++) {
          String nextLine = lines[lineNumber + 1];
          if (nextLine.contains(ASSIGNED_TYPE_PREFIX)) {
            // If it's a different assignment so we can stop appending
            break;
          }

          // otherwise join the next line to this
          line += " " + nextLine;
        }
        assignmentLines.add(line);
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
      setFields(builder, metadata, line);
    }
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
    int delimIndex = line.indexOf("*");
    if (delimIndex < 0) {
      return "";
    }

    return line.substring(0, delimIndex);
  }

  /**
   * Set the fields on a builder based on type assignments found in a given line
   */
  private static void setFields(ValidationModelBuilder builder, PdfMetadata metadata, String line) {

    if (line.startsWith(BW_CLASS_LINE)) {
      // NB: B*27:08 is actually Bw6, not Bw4
      if (line.contains("Bw4")
          && (!line.contains("B*27:08") || countOccurrance("Bw4", line) >= 2)) {
        builder.bw4(true);
      }
      if (line.contains("Bw6")) {
        builder.bw6(true);
      }
    } else if (line.startsWith(DR_CLASS_LINE)) {
      if (line.contains("DR51")) {
        builder.dr51(true);
      }
      if (line.contains("DR52")) {
        builder.dr52(true);
      }
      if (line.contains("DR53")) {
        builder.dr53(true);
      }
    } else {
      String spec = getSpecs(metadata.tokenPrefix, line);
      if (!spec.isEmpty()) {
        metadata.setter.accept(builder, spec);
      }
    }
  }

  /**
   * Process a line for the specificities assigned by that line
   *
   * @param typePrefix Leading string on assigned types (ignored)
   * @param line Input assignment text
   * @return A list of all specificities discovered in the given line
   */
  private static String getSpecs(String typePrefix, String line) {
    String spec = "";

    String[] tokens = line.replaceAll(",", "").split("\\s+");

    if (line.contains(DQA_TYPE_LINE) || line.contains(DPB_TYPE_LINE)) {
      // use the first high-res type entered for these loci
      String type = tokens[1];

      // For DQA we have to remove the high-resolution typing
      if (line.contains(DQA_TYPE_LINE)) {
        if (type.contains(":")) {
          type = type.substring(0, type.indexOf(":"));
        }
      }

      spec = type;
    } else {
      // For everything else we have to find the serological equivalent
      for (String token : tokens) {
        if (token.startsWith(typePrefix) && !token.contains("*")) {
          // We just want the first serological equiv
          spec = token;
          break;
        }
      }
    }

    return clean(typePrefix, spec);
  }

  /**
   * @return the base input string striped off the exclusion string + {@link #HETERO_TOKEN}
   */
  private static String clean(String exclusion, String base) {
    // Remove prefix text
    String tmp = base.replaceAll(exclusion, "");

    return tmp;
  }

  private static int countOccurrance(String base, String query) {
    int lastIndex = 0;
    int count = 0;

    while (lastIndex != -1) {

      lastIndex = base.indexOf(query, lastIndex);

      if (lastIndex != -1) {
        count++;
        lastIndex += query.length();
      }
    }
    return count;
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
