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
package org.pankratzlab.unet.jfx.wizard;

import java.net.URL;
import java.util.ResourceBundle;

import org.pankratzlab.unet.model.ValidationTable;

import javafx.fxml.FXML;

/**
 * Abstract superclass for {@link ValidatingWizardController}s. Takes care of tracking the {@link
 * ValidationTable}.
 */
public abstract class AbstractValidatingWizardController implements ValidatingWizardController {

  @FXML private ResourceBundle resources;

  @FXML private URL location;

  @FXML private ValidatingWizardPane rootPane;

  private ValidationTable validationTable;

  @FXML
  void initialize() {
    assert rootPane != null
        : "fx:id=\"rootPane\" was not injected: check your FXML file 'StepThreeInputPDF.fxml'.";
  }

  @Override
  public void setTable(ValidationTable table) {
    validationTable = table;

    refreshTable(table);
  }

  /** @return The currently set validation table */
  protected ValidationTable getTable() {
    return validationTable;
  }

  protected ValidatingWizardPane rootPane() {
    return rootPane;
  }

  /**
   * Optional callback to override if a controller needs to perform any additional functionality
   * when the validation table changes.
   */
  protected void refreshTable(ValidationTable table) {}
}
