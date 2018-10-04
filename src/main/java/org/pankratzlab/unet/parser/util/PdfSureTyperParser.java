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

import java.util.StringJoiner;
import java.util.function.BiConsumer;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Specific parsing logic for SureTyper PDFs
 */
public class PdfSureTyperParser {
  private static final String TYPING_STOP_TOKEN = "SureTyper";
  private static final String TYPING_START_TOKEN = "LABORATORY ASSIGNMENT";
  private static final int DONOR_ID_INDEX = 2;
  private static final String PATIENT_ID_TOKEN = "Patient ID:";
  private static final String HLA_DRB345 = "HLA-DRB345:";
  private static final String BW = "Bw:";
  private static final String HLA_DPB1 = "HLA-DPB1:";
  private static final String HLA_DPA1 = "HLA-DPA1:";
  private static final String HLA_DQB1 = "HLA-DQB1:";
  private static final String HLA_DQA1 = "HLA-DQA1:";
  private static final String HLA_DRB1 = "HLA-DRB1:";
  private static final String HLA_C = "HLA-C:";
  private static final String HLA_B = "HLA-B:";
  private static final String HLA_A = "HLA-A:";

  private static ImmutableMap<String, TypeSetter> metadataMap;

  /**
   * Helper method to process the individual type assignment tokens tokens
   */
  public static void parseTypes(ValidationModelBuilder builder, String[] lines) {
    if (metadataMap == null) {
      init();
    }

    // We now process the PDF text line-by-line.
    StringJoiner typeAssignment = new StringJoiner(" ");
    boolean inAssignment = false;

    // All the type assignment strings are joined together and then split on whitespace,
    // creating a stream of tokens.
    for (String line : lines) {
      if (line.startsWith(PATIENT_ID_TOKEN)) {
        // The patient ID value is at a particular position in the line starting with this token
        builder.donorId(line.split("\\s+")[DONOR_ID_INDEX]);
      } else if (line.contains(TYPING_START_TOKEN)) {
        // After encountering this token, all following lines contain type assignment data
        inAssignment = true;
      } else if (inAssignment) {
        if (line.contains(TYPING_STOP_TOKEN)) {
          // Once we hit this token, typing is over
          inAssignment = false;
        } else {
          // Until then, keep building the type assignment
          typeAssignment.add(line);
        }
      }
    }

    builder.bw4(false);
    builder.bw6(false);
    builder.dr51(false);
    builder.dr52(false);
    builder.dr53(false);

    BiConsumer<ValidationModelBuilder, String> setter = null;
    String prefix = "";

    for (String token : typeAssignment.toString().split("\\s+")) {
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

  private static void init() {
    // A PDF is just text with no formal structure. Type text is stored in this PDF in the format:
    // HLA-A: A18 A24 HLA-B: B8 B72 ... etc
    // Therefore to interpret this data we need to link three elements:
    // 1. The token indicating a change in locus
    // 2. The prefix string to each antigen token
    // 3. The appropriate setter method in ValidationModelBuilder
    Builder<String, TypeSetter> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(HLA_A, new TypeSetter("A", ValidationModelBuilder::a));
    setterBuilder.put(HLA_B, new TypeSetter("B", ValidationModelBuilder::b));
    setterBuilder.put(HLA_C, new TypeSetter("Cw", ValidationModelBuilder::c));
    setterBuilder.put(HLA_DRB1, new TypeSetter("DR", ValidationModelBuilder::drb));
    setterBuilder.put(HLA_DQA1, new TypeSetter("DQA1*", ValidationModelBuilder::dqa));
    setterBuilder.put(HLA_DQB1, new TypeSetter("DQ", ValidationModelBuilder::dqb));

    // DPA1 is present in the typing data but we do not currently track it in the validation model
    // (not entered in DonorNet)
    setterBuilder.put(HLA_DPA1, new TypeSetter("DPA1*", PdfSureTyperParser::noOp));

    setterBuilder.put(HLA_DPB1, new TypeSetter("DPB1*", ValidationModelBuilder::dpb));

    // Boolean values appear as literal values, indicating true, and are simply absent if false
    setterBuilder.put(BW, new TypeSetter("", PdfSureTyperParser::decodeBw));
    setterBuilder.put(HLA_DRB345, new TypeSetter("", PdfSureTyperParser::decodeDR));

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
   * Helper method to call the appropriate {@link ValidationModelBuilder} method when a DR value is
   * encountered.
   */
  private static void decodeDR(ValidationModelBuilder builder, String value) {
    switch (value) {
      case "DR51":
        builder.dr51(true);
        break;
      case "DR52":
        builder.dr52(true);
        break;
      case "DR53":
        builder.dr53(true);
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
