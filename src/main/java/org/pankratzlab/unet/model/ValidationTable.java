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
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies.Ethnicity;
import org.pankratzlab.util.JFXPropertyHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
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
 * {@link #getValidationRows()} for accessing a row-dominant view of these models.
 */
public class ValidationTable {

  private final ReadOnlyBooleanWrapper isValidWrapper;
  private final ReadOnlyObjectWrapper<String> firstSourceWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> firstModelWrapper;
  private final ReadOnlyObjectWrapper<String> secondSourceWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> secondModelWrapper;
  private final ReadOnlyListWrapper<ValidationRow<?>> validationRows;
  private final ReadOnlyListWrapper<HaplotypeRow> haplotypeRows;
  private WritableImage validationImage = null;

  public ValidationTable() {
    isValidWrapper = new ReadOnlyBooleanWrapper();
    firstSourceWrapper = new ReadOnlyObjectWrapper<>();
    secondSourceWrapper = new ReadOnlyObjectWrapper<>();
    firstModelWrapper = new ReadOnlyObjectWrapper<>();
    secondModelWrapper = new ReadOnlyObjectWrapper<>();
    validationRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    haplotypeRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

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
  public ReadOnlyListProperty<ValidationRow<?>> getValidationRows() {
    return validationRows.getReadOnlyProperty();
  }

  /**
   * @return The {@link ValidationRow}s for this model, e.g. for display
   */
  public ReadOnlyListProperty<HaplotypeRow> getHaplotypeRows() {
    return haplotypeRows.getReadOnlyProperty();
  }

  /**
   * Helper method to translate the wrapped {@link ValidationModel}s to rows for display.
   */
  private void generateRows() {
    validationRows.clear();
    makeValidationRow(validationRows, "Donor ID", ValidationModel::getDonorId);
    makeValidationRow(validationRows, "A", ValidationModel::getA1);
    makeValidationRow(validationRows, "A", ValidationModel::getA2);

    makeValidationRow(validationRows, "B", ValidationModel::getB1);
    makeValidationRow(validationRows, "B", ValidationModel::getB2);

    makeValidationRow(validationRows, "BW4", ValidationModel::isBw4);
    makeValidationRow(validationRows, "BW6", ValidationModel::isBw6);

    makeValidationRow(validationRows, "C", ValidationModel::getC1);
    makeValidationRow(validationRows, "C", ValidationModel::getC2);

    makeValidationRow(validationRows, "DRB1", ValidationModel::getDRB1);
    makeValidationRow(validationRows, "DRB1", ValidationModel::getDRB2);

    makeValidationRow(validationRows, "DQB1", ValidationModel::getDQB1);
    makeValidationRow(validationRows, "DQB1", ValidationModel::getDQB2);

    makeValidationRow(validationRows, "DQA1", ValidationModel::getDQA1);
    makeValidationRow(validationRows, "DQA1", ValidationModel::getDQA2);

    makeValidationRow(validationRows, "DPB1", ValidationModel::getDPB1);
    makeValidationRow(validationRows, "DPB1", ValidationModel::getDPB2);

    makeValidationRow(validationRows, "DR51", ValidationModel::isDr51);
    makeValidationRow(validationRows, "DR52", ValidationModel::isDr52);
    makeValidationRow(validationRows, "DR53", ValidationModel::isDr53);

    // A Table is valid if all its Rows are valid
    ObservableBooleanValue validBinding = null;
    for (ValidationRow<?> row : validationRows.get()) {
      validBinding = JFXPropertyHelper.andHelper(validBinding, row.isValidProperty());
    }
    isValidWrapper.bind(validBinding);


    haplotypeRows.clear();
    makeHaplotypeRows(haplotypeRows,
        chooseHaplotypeModel(firstModelWrapper.get(), secondModelWrapper.get()));
  }

  /**
   * Use the given model to populate a list of {@link HaplotypeRow}s
   */
  private void makeHaplotypeRows(ReadOnlyListWrapper<HaplotypeRow> rows, ValidationModel model) {
    if (Objects.isNull(model)) {
      return;
    }
    addHaplotypes(rows, ImmutableList.of(model.getBCHaplotypes(), model.getDRDQHaplotypes()));
  }

  /**
   * Add haplotype rows to the given list. Haplotypes are added in Ethnicity order (grouping all
   * haplotypes for a given ethnicity)
   */
  private void addHaplotypes(ReadOnlyListWrapper<HaplotypeRow> rows,
      List<ImmutableMultimap<Ethnicity, Haplotype>> haplotypeEthnicityMaps) {
    for (Ethnicity ethnicity : Ethnicity.values()) {
      haplotypeEthnicityMaps.forEach(ethnicityMap -> {
        ethnicityMap.get(ethnicity).forEach(haplotype -> {
          rows.add(new HaplotypeRow(ethnicity, haplotype));
        });
      });
    } ;
  }

  /**
   * We only report one set of haplotypes, so we have to choose which model to use. We default to
   * the first, but if the first is empty we use the second.
   */
  private ValidationModel chooseHaplotypeModel(ValidationModel model1, ValidationModel model2) {
    if (Objects.isNull(model1)
        || (model1.getBCHaplotypes().isEmpty() && model1.getDRDQHaplotypes().isEmpty())) {
      return model2;
    }
    return model1;
  }

  /**
   * Helper method to create a {@link ValidationRow}
   * 
   * @param rows Destination collection to populate
   * @param rowLabel Description of this row (first column)
   * @param getter Method to use to retrieve this row's value
   */
  private <T> void makeValidationRow(List<ValidationRow<?>> rows, String rowLabel,
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
