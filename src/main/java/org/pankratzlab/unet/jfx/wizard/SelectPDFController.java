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
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.util.PdfQTyperParser;
import org.pankratzlab.unet.util.PdfSureTyperParser;
import com.google.common.collect.ImmutableMap;

/**
 * Controller for adding PDF-sourced donor data to the current {@link ValidationTable}
 *
 * @see ValidatingWizardController
 */
public class SelectPDFController extends AbstractFileSelectController {

  private static final String EXTENSION_DESC = "Type Report";
  private static final String FILE_CHOOSER_HEADER = "Select PDF Report";
  private static final String WIZARD_PANE_TITLE = "Step 3 of 4";
  private static final String INITIAL_NAME = "";
  private static final String EXTENSION = "*.pdf";
  private static final Map<String, String> EXTENSION_MAP =
      ImmutableMap.of(EXTENSION_DESC, EXTENSION);
  private static final String QTYPER = "QTYPE";
  private static final String SURETYPER = "SureTyper";


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
  protected Map<String, String> extensionMap() {
    return EXTENSION_MAP;
  }

  @Override
  protected BiConsumer<ValidationTable, ValidationModel> setModel() {
    return ValidationTable::setPdfModel;
  }

  @Override
  protected String getErrorText() {
    return "Could not read donor data from PDF.";
  }

  @Override
  protected void parseModel(ValidationModelBuilder builder, File file) {
    try (PDDocument pdf = PDDocument.load(file)) {
      if (!pdf.isEncrypted()) {
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setSortByPosition(true);
        // Extract all text from the PDF and split it into lines
        String pdfText = tStripper.getText(pdf);
        String[] pdfLines = pdfText.split(System.getProperty("line.separator"));
        if (pdfText.contains(SURETYPER)) {
          PdfSureTyperParser.parseTypes(builder, pdfLines);
        } else if (pdfText.contains(QTYPER)) {
          PdfQTyperParser.parseTypes(builder, pdfLines);
        }
      }
    } catch (InvalidPasswordException e) {
      throw new UnsupportedOperationException("PDF can not be encrypted");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}

