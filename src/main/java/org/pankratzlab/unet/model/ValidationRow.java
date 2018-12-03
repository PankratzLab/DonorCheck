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
public abstract class ValidationRow<T> {
  public static final String SECOND_COL_PROP = "secondCol";
  public static final String IS_VALID_PROP = "isValid";
  public static final String FIRST_COL_PROP = "firstCol";
  public static final String ID_PROP = "id";

  private final ReadOnlyStringWrapper rowLabelWRapper;
  private final ReadOnlyObjectWrapper<T> firstColWrapper;
  private final ReadOnlyObjectWrapper<T> secondColWrapper;
  private final ReadOnlyStringWrapper firstColStringWrapper;
  private final ReadOnlyStringWrapper secondColStringWrapper;
  private final ReadOnlyBooleanWrapper isValidWrapper;

  public ValidationRow(String rowLabel, T firstCol, T secondCol) {
    this.rowLabelWRapper = new ReadOnlyStringWrapper(rowLabel);
    firstColWrapper = new ReadOnlyObjectWrapper<>(firstCol);
    secondColWrapper = new ReadOnlyObjectWrapper<>(secondCol);
    firstColStringWrapper = new ReadOnlyStringWrapper();

    // Ultimately we want to display a String representation of each column value. This string
    // unfortunately depends on the underlying data type.
    firstColStringWrapper.bind(
        Bindings.createStringBinding(() -> getDisplayString(firstColWrapper.get()), firstColWrapper));
    secondColStringWrapper = new ReadOnlyStringWrapper();
    secondColStringWrapper.bind(
        Bindings.createStringBinding(() -> getDisplayString(secondColWrapper.get()), secondColWrapper));

    isValidWrapper = new ReadOnlyBooleanWrapper();
    isValidWrapper.bind(Bindings.createBooleanBinding(this::isValid, firstColWrapper, secondColWrapper));
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
  public ReadOnlyStringProperty firstColProperty() {
    return firstColStringWrapper.getReadOnlyProperty();
  }

  /**
   * @return {@link StringProperty} for the PDF value of this row
   */
  public ReadOnlyStringProperty secondColProperty() {
    return secondColStringWrapper.getReadOnlyProperty();
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
  protected  abstract String getDisplayString(T toDisplay) throws IllegalStateException;

  /**
   * @return true iff the XML and PDF column values are the same
   */
  private boolean isValid() {
    return Objects.equals(firstColWrapper.get(), secondColWrapper.get());
  }

  public static interface RowBuilder<T> {
    ValidationRow<T> makeRow(String s, T col1, T col2);
  }
}
