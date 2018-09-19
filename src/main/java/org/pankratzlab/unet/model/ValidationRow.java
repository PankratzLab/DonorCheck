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
package org.pankratzlab.unet.model;

import java.util.Objects;
import javax.jms.IllegalStateException;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

/**
 * Represents a single row in a {@link ValidationTable}. This includes the following columns:
 * <table>
 * <tr>
 * <td>row_name</td>
 * <td>xml_value</td>
 * <td>equality_state</td>
 * <td>pdf_value</td>
 * </tr>
 * </table>
 */
public class ValidationRow<T> {

  private final ReadOnlyStringWrapper rowLabelWRapper;
  private final ReadOnlyObjectWrapper<T> xmlColWrapper;
  private final ReadOnlyObjectWrapper<T> pdfColWrapper;
  private final ReadOnlyStringWrapper xmlColStringWrapper;
  private final ReadOnlyStringWrapper pdfColStringWrapper;
  private final ReadOnlyBooleanWrapper isValidWrapper;

  public ValidationRow(String rowLabel, T xmlColVal, T pdfColVal) {
    this.rowLabelWRapper = new ReadOnlyStringWrapper(rowLabel);
    xmlColWrapper = new ReadOnlyObjectWrapper<>(xmlColVal);
    pdfColWrapper = new ReadOnlyObjectWrapper<>(pdfColVal);
    xmlColStringWrapper = new ReadOnlyStringWrapper();

    // Ultimately we want to display a String representation of each column value. This string
    // unfortunately depends on the underlying data type.
    xmlColStringWrapper.bind(
        Bindings.createStringBinding(() -> getDisplayString(xmlColWrapper.get()), xmlColWrapper));
    pdfColStringWrapper = new ReadOnlyStringWrapper();
    pdfColStringWrapper.bind(
        Bindings.createStringBinding(() -> getDisplayString(pdfColWrapper.get()), pdfColWrapper));

    isValidWrapper = new ReadOnlyBooleanWrapper();
    isValidWrapper.bind(Bindings.createBooleanBinding(this::isValid, xmlColWrapper, pdfColWrapper));
  }

  /**
   * @return {@link StringProperty} for the identifying name of this row
   */
  public ReadOnlyStringProperty idProperty() {
    return rowLabelWRapper.getReadOnlyProperty();
  }

  /**
   * @return {@link StringProperty} for the XML value of this row
   */
  public ReadOnlyStringProperty xmlProperty() {
    return xmlColStringWrapper.getReadOnlyProperty();
  }

  /**
   * @return {@link StringProperty} for the PDF value of this row
   */
  public ReadOnlyStringProperty pdfProperty() {
    return pdfColStringWrapper.getReadOnlyProperty();
  }

  /**
   * @return {@link BooleanProperty} indicating if this row is valid or not
   */
  public ReadOnlyBooleanProperty isValidProperty() {
    return isValidWrapper.getReadOnlyProperty();
  }

  /**
   * @param toDisplay Type to convert to string
   * @return The String representation of the given type
   * @throws IllegalStateException if the type is not one of supported values
   */
  private String getDisplayString(T toDisplay) throws IllegalStateException {
    // TODO each ValidationRow could take the method to use here as a constructor argument?
    if (Objects.isNull(toDisplay)) {
      return "";
    }
    if (toDisplay instanceof String) {
      return (String) toDisplay;
    }
    if (toDisplay instanceof SeroType) {
      return ((SeroType) toDisplay).specString();
    }
    if (toDisplay instanceof HLAType) {
      return ((HLAType) toDisplay).specString();
    }
    if (toDisplay instanceof Boolean) {
      return String.valueOf((Boolean) toDisplay);
    }

    throw new IllegalStateException("Invalid ValidationRow value: " + toDisplay);
  }

  /**
   * @return true iff the XML and PDF column values are the same
   */
  private boolean isValid() {
    return Objects.equals(xmlColWrapper.get(), pdfColWrapper.get());
  }
}
