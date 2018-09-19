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
package org.pankratzlab.unet.jfx.wizard;

import java.io.File;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Controller for adding PDF-sourced donor data to the current {@link ValidationTable}
 *
 * @see ValidatingWizardController
 */
public class SelectPDFController extends AbstractFileSelectController {

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
  private static final String EXTENSION_DESC = "SureTyper Report";
  private static final String FILE_CHOOSER_HEADER = "Select PDF Report";
  private static final String WIZARD_PANE_TITLE = "Step 3 of 4";
  private static final String INITIAL_NAME = "myPdf";
  private static final String EXTENSION = "*.pdf";

  private static ImmutableMap<String, XmlMetadata> metadataMap;

  {
    // A PDF is just text with no formal structure. Type text is stored in this PDF in the format:
    // HLA-A: A18 A24 HLA-B: B8 B72 ... etc
    // Therefore to interpret this data we need to link three elements:
    // 1. The token indicating a change in locus
    // 2. The prefix string to each antigen token
    // 3. The appropriate setter method in ValidationModelBuilder
    Builder<String, XmlMetadata> setterBuilder = ImmutableMap.builder();
    setterBuilder.put(HLA_A, new XmlMetadata("A", ValidationModelBuilder::a));
    setterBuilder.put(HLA_B, new XmlMetadata("B", ValidationModelBuilder::b));
    setterBuilder.put(HLA_C, new XmlMetadata("Cw", ValidationModelBuilder::c));
    setterBuilder.put(HLA_DRB1, new XmlMetadata("Cw", ValidationModelBuilder::drb));
    setterBuilder.put(HLA_DQA1, new XmlMetadata("DQA1*", ValidationModelBuilder::dqa));
    setterBuilder.put(HLA_DQB1, new XmlMetadata("DQ", ValidationModelBuilder::dqb));

    // DPA1 is present in the typing data but we do not currently track it in the validation model
    // (not entered in DonorNet)
    setterBuilder.put(HLA_DPA1, new XmlMetadata("DPA1*", SelectPDFController::noOp));

    setterBuilder.put(HLA_DPB1, new XmlMetadata("DPB1*", ValidationModelBuilder::dpb));

    // Boolean values appear as literal values, indicating true, and are simply absent if false
    setterBuilder.put(BW, new XmlMetadata("DQ", SelectPDFController::decodeBoolean));
    setterBuilder.put(HLA_DRB345, new XmlMetadata("", SelectPDFController::decodeBoolean));

    metadataMap = setterBuilder.build();
  }

  /**
   * Helper method to call the appropriate {@link ValidationModelBuilder} method when a boolean
   * value is encountered.
   */
  private static void decodeBoolean(ValidationModelBuilder builder, String value) {
    switch (value) {
      case "Bw4":
        builder.bw4(true);
        break;
      case "Bw6":
        builder.bw6(true);
        break;
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

  @Override
  protected String extensionDesc() {
    return EXTENSION_DESC;
  }

  @Override
  protected String fileChooserHeader() {
    return FILE_CHOOSER_HEADER;
  }

  @Override
  protected String wizardPaneTitle() {
    return WIZARD_PANE_TITLE;
  }

  @Override
  protected String initialName() {
    return INITIAL_NAME;
  }

  @Override
  protected String extension() {
    return EXTENSION;
  }

  @Override
  protected BiConsumer<ValidationTable, ValidationModel> setModel() {
    return ValidationTable::setPdfModel;
  }

  @Override
  protected void parseModel(ValidationModelBuilder builder, File file) {
    try (PDDocument pdf = PDDocument.load(file)) {
      if (!pdf.isEncrypted()) {
        PDFTextStripper tStripper = new PDFTextStripper();
        // Extract all text from the PDF and split it into lines
        String[] lines = tStripper.getText(pdf).split(System.getProperty("line.separator"));

        // We now process the PDF text line-by-line.
        StringJoiner typeAssignment = new StringJoiner(" ");
        boolean inAssignment = false;

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

        // All the type assignment strings are joined together and then split on whitespace,
        // creating a stream of tokens.
        parseTypes(builder, typeAssignment.toString().split("\\s+"));
      }
    } catch (InvalidPasswordException e) {
      throw new UnsupportedOperationException("PDF can not be encrypted");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method to process the individual type assignment tokens tokens
   */
  private void parseTypes(ValidationModelBuilder builder, String[] typeStringTokens) {
    builder.bw4(false);
    builder.bw6(false);
    builder.dr51(false);
    builder.dr52(false);
    builder.dr53(false);

    BiConsumer<ValidationModelBuilder, String> setter = null;
    String prefix = "";

    for (String token : typeStringTokens) {
      if (metadataMap.containsKey(token)) {
        // When we encounter a section key we update the prefix string and the field setter
        XmlMetadata metadata = metadataMap.get(token);
        setter = metadata.setter;
        prefix = metadata.tokenPrefix;
      } else if (setter != null) {
        // Erase the prefix from the current token and set the value on the model builder
        setter.accept(builder, token.replace(prefix, ""));
      }
    }
  }

  /**
   * Helper class to link together related {@link ValidationModelBuilder} setters and token
   * specificity prefixes.
   */
  private static class XmlMetadata {

    private String tokenPrefix;
    private BiConsumer<ValidationModelBuilder, String> setter;

    public XmlMetadata(String tokenPrefix, BiConsumer<ValidationModelBuilder, String> setter) {
      super();
      this.tokenPrefix = tokenPrefix;
      this.setter = setter;
    }
  }
}
