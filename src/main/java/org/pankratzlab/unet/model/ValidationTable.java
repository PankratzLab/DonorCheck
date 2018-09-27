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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.pankratzlab.util.JFXPropertyHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.scene.image.WritableImage;

/**
 * Backing model for a validation, wrapping multiple {@link ValidationModel}s being compared. Use
 * {@link #getRows()} for accessing a row-dominant view of these models.
 */
public class ValidationTable {

  private final ReadOnlyBooleanWrapper isValidWrapper;
  private final ReadOnlyObjectWrapper<String> firstSourceWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> firstModelWrapper;
  private final ReadOnlyObjectWrapper<String> secondSourceWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> secondModelWrapper;
  private final ReadOnlyListWrapper<ValidationRow<?>> validationRows;
  private WritableImage validationImage = null;

  public ValidationTable() {
    isValidWrapper = new ReadOnlyBooleanWrapper();
    firstSourceWrapper = new ReadOnlyObjectWrapper<>();
    secondSourceWrapper = new ReadOnlyObjectWrapper<>();
    firstModelWrapper = new ReadOnlyObjectWrapper<>();
    secondModelWrapper = new ReadOnlyObjectWrapper<>();
    validationRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    // Each time either model changes we re-generate the rows
    firstModelWrapper.addListener((v, o, n) -> generateRows());
    secondModelWrapper.addListener((v, o, n) -> generateRows());
  }

  /**
   * @return The donor ID for this validation (if available)
   */
  public String getId() {
    if (firstModelWrapper != null) {
      return firstModelWrapper.get().getDonorId();
    }
    if (secondModelWrapper != null) {
      return secondModelWrapper.get().getDonorId();
    }
    return "";
  }

  public ReadOnlyObjectProperty<String> firstColSource() {
    return firstSourceWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<String> secondColSource() {
    return secondSourceWrapper.getReadOnlyProperty();
  }

  /**
   * @return An image of the validation state
   */
  public WritableImage getValidationImage() {
    return validationImage;
  }

  /**
   * @param validationImage An image representation of the validation state
   */
  public void setValidationImage(WritableImage validationImage) {
    this.validationImage = validationImage;
  }

  /**
   * @param model New {@link ValidationModel} for the first column in the table
   */
  public void setFirstModel(ValidationModel model) {
    firstSourceWrapper.set(model.getSource());
    firstModelWrapper.set(model);
  }

  /**
   * @param model New {@link ValidationModel} for the second column in the table
   */
  public void setSecondModel(ValidationModel model) {
    secondSourceWrapper.set(model.getSource());
    secondModelWrapper.set(model);
  }

  /**
   * @return A {@link BooleanProperty} tracking the validity of the complete table
   */
  public ReadOnlyBooleanProperty isValidProperty() {
    return isValidWrapper.getReadOnlyProperty();
  }

  /**
   * @return The {@link ValidationRow}s for this model, e.g. for display
   */
  public ReadOnlyListProperty<ValidationRow<?>> getRows() {
    return validationRows.getReadOnlyProperty();
  }

  /**
   * Helper method to translate the wrapped {@link ValidationModel}s to rows for display.
   */
  private void generateRows() {
    validationRows.clear();
    makeRow(validationRows, "Donor ID", ValidationModel::getDonorId);
    makeRow(validationRows, "A", ValidationModel::getA1);
    makeRow(validationRows, "A", ValidationModel::getA2);

    makeRow(validationRows, "B", ValidationModel::getB1);
    makeRow(validationRows, "B", ValidationModel::getB2);

    makeRow(validationRows, "BW4", ValidationModel::isBw4);
    makeRow(validationRows, "BW6", ValidationModel::isBw6);

    makeRow(validationRows, "C", ValidationModel::getC1);
    makeRow(validationRows, "C", ValidationModel::getC2);

    makeRow(validationRows, "DRB1", ValidationModel::getDRB1);
    makeRow(validationRows, "DRB1", ValidationModel::getDRB2);

    makeRow(validationRows, "DQB1", ValidationModel::getDQB1);
    makeRow(validationRows, "DQB1", ValidationModel::getDQB2);

    makeRow(validationRows, "DQA1", ValidationModel::getDQA1);
    makeRow(validationRows, "DQA1", ValidationModel::getDQA2);

    makeRow(validationRows, "DPB1", ValidationModel::getDPB1);
    makeRow(validationRows, "DPB1", ValidationModel::getDPB2);

    makeRow(validationRows, "DR51", ValidationModel::isDr51);
    makeRow(validationRows, "DR52", ValidationModel::isDr52);
    makeRow(validationRows, "DR53", ValidationModel::isDr53);

    // A Table is valid if all its Rows are valid
    ObservableBooleanValue validBinding = null;
    for (ValidationRow<?> row : validationRows.get()) {
      validBinding = JFXPropertyHelper.andHelper(validBinding, row.isValidProperty());
    }
    isValidWrapper.bind(validBinding);
  }

  /**
   * Helper method to create a {@link ValidationRow}
   * 
   * @param rows Destination collection to populate
   * @param rowLabel Description of this row (first column)
   * @param getter Method to use to retrieve this row's value
   */
  private <T> void makeRow(List<ValidationRow<?>> rows, String rowLabel,
      Function<ValidationModel, T> getter) {
    rows.add(new ValidationRow<T>(rowLabel, getFirstField(getter), getSecondField(getter)));
  }

  /**
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return The value of that field in the second column's model
   */
  private <T> T getSecondField(Function<ValidationModel, T> getter) {
    return getValueFromModel(getter, secondModelWrapper);
  }

  /**
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return The value of that field in the first column's model
   */
  private <T> T getFirstField(Function<ValidationModel, T> getter) {
    return getValueFromModel(getter, firstModelWrapper);
  }

  /**
   * @return The value of the given getter in the given model, or {@code null} if the target model
   *         is null.
   */
  private <T> T getValueFromModel(Function<ValidationModel, T> getter,
      ReadOnlyObjectWrapper<ValidationModel> wrapper) {
    if (Objects.isNull(wrapper.get())) {
      return null;
    }
    return getter.apply(wrapper.get());
  }
}
