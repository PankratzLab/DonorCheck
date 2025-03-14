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
package org.pankratzlab.unet.parser;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.PdfSureTyperParser;

/** {@link DonorFileParser} entry point for PDF files. */
public class PdfDonorParser extends AbstractDonorFileParser {
  private static final String DISPLAY_STRING = "PDF";
  private static final String FILE_CHOOSER_HEADER = "Select donor typing PDF";
  private static final String INITIAL_NAME = "";
  private static final String EXTENSION_DESC = "LinkSeq";
  private static final String EXTENSION_NAME = "pdf";
  private static final String EXTENSION = "*." + EXTENSION_NAME;
  // private static final String QTYPER = "QTYPE";
  private static final String SURETYPER = "SureTyper";

  static {
    System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
  }

  @Override
  public String extensionFilter() {
    return EXTENSION;
  }

  @Override
  public String extensionDescription() {
    return EXTENSION_DESC;
  }

  @Override
  public String fileChooserHeader() {
    return FILE_CHOOSER_HEADER;
  }

  @Override
  public String initialName() {
    return INITIAL_NAME;
  }

  @Override
  public String getErrorText() {
    return "Could not read donor data from PDF.";
  }

  public static String getTypeString() {
    return DISPLAY_STRING;
  }

  @Override
  protected void doParse(ValidationModelBuilder builder, File file) {
    try (PDDocument pdf = PDDocument.load(file)) {
      if (!pdf.isEncrypted()) {
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setSortByPosition(true);
        // Extract all text from the PDF and split it into lines
        String pdfText = tStripper.getText(pdf);
        String[] pdfLines = pdfText.split(System.getProperty("line.separator"));

        // Pass the text to the appropriate parser
        builder.sourceType(SourceType.SureTyper);
        PdfSureTyperParser.parseTypes(builder, pdfLines);
        // FIXME currently not well supported
        // else if (pdfText.contains(QTYPER)) {
        // PdfQTyperParser.parseTypes(builder, pdfLines);
        // }
      }
    } catch (InvalidPasswordException e) {
      e.printStackTrace();
      throw new UnsupportedOperationException("PDF can not be encrypted");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected String getDisplayString() {
    return DISPLAY_STRING;
  }

  @Override
  protected String extensionName() {
    return EXTENSION_NAME;
  }
}
