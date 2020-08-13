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
package org.pankratzlab.unet.model;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.pankratzlab.unet.deprecated.jfx.JFXPropertyHelper;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.ValidationRow.RowBuilder;

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
  private final ReadOnlyListWrapper<BCHaplotypeRow> bcHaplotypeRows;
  private final ReadOnlyListWrapper<DRDQHaplotypeRow> drdqHaplotypeRows;
  private final ReadOnlyObjectWrapper<String> csvValues;
  private WritableImage validationImage = null;

  public ValidationTable() {
    isValidWrapper = new ReadOnlyBooleanWrapper();
    firstSourceWrapper = new ReadOnlyObjectWrapper<>();
    secondSourceWrapper = new ReadOnlyObjectWrapper<>();
    firstModelWrapper = new ReadOnlyObjectWrapper<>();
    secondModelWrapper = new ReadOnlyObjectWrapper<>();
    validationRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    bcHaplotypeRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    drdqHaplotypeRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    csvValues = new ReadOnlyObjectWrapper<>();

    // Each time either model changes we re-generate the rows
    firstModelWrapper.addListener((v, o, n) -> generateRows());
    secondModelWrapper.addListener((v, o, n) -> generateRows());

    // Each time either model changes we re-generate the rows
    firstModelWrapper.addListener((v, o, n) -> generateCSV());
    secondModelWrapper.addListener((v, o, n) -> generateCSV());
  }

  /** @return The donor ID for this validation (if available) */
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

  /** @return An image of the validation state */
  public WritableImage getValidationImage() {
    return validationImage;
  }

  /** @param validationImage An image representation of the validation state */
  public void setValidationImage(WritableImage validationImage) {
    this.validationImage = validationImage;
  }

  /** @param model New {@link ValidationModel} for the first column in the table */
  public void setFirstModel(ValidationModel model) {
    firstSourceWrapper.set(model.getSource());
    firstModelWrapper.set(model);
  }

  /** @param model New {@link ValidationModel} for the second column in the table */
  public void setSecondModel(ValidationModel model) {
    secondSourceWrapper.set(model.getSource());
    secondModelWrapper.set(model);
  }

  /** @return A {@link BooleanProperty} tracking the validity of the complete table */
  public ReadOnlyBooleanProperty isValidProperty() {
    return isValidWrapper.getReadOnlyProperty();
  }

  /** @return The {@link ValidationRow}s for this model, e.g. for display */
  public ReadOnlyListProperty<ValidationRow<?>> getValidationRows() {
    return validationRows.getReadOnlyProperty();
  }

  /** @return The B-C Haplotype rows for this model, e.g. for display */
  public ReadOnlyListProperty<BCHaplotypeRow> getBCHaplotypeRows() {
    return bcHaplotypeRows.getReadOnlyProperty();
  }

  /** @return The DRB1-DQB1-DRB345 Haplotype rows for this model, e.g. for display */
  public ReadOnlyListProperty<DRDQHaplotypeRow> getDRDQHaplotypeRows() {
    return drdqHaplotypeRows.getReadOnlyProperty();
  }

  /** Helper method to translate the wrapped {@link ValidationModel}s to rows for display. */
  private void generateRows() {
    // FIXME the row labels and row type should probably be linked in the ValidationModel
    validationRows.clear();
    makeValidationRow(validationRows, "Donor ID", ValidationModel::getDonorId,
                      StringValidationRow::makeRow);
    makeValidationRow(validationRows, "A", ValidationModel::getA1, AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "A", ValidationModel::getA2, AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "B", ValidationModel::getB1, AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "B", ValidationModel::getB2, AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "BW4", ValidationModel::isBw4, StringValidationRow::makeRow);
    makeValidationRow(validationRows, "BW6", ValidationModel::isBw6, StringValidationRow::makeRow);

    makeValidationRow(validationRows, "C", ValidationModel::getC1, AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "C", ValidationModel::getC2, AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "DRB1", ValidationModel::getDRB1,
                      AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "DRB1", ValidationModel::getDRB2,
                      AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "DQB1", ValidationModel::getDQB1,
                      AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "DQB1", ValidationModel::getDQB2,
                      AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "DQA1", ValidationModel::getDQA1,
                      AntigenValidationRow::makeRow);
    makeValidationRow(validationRows, "DQA1", ValidationModel::getDQA2,
                      AntigenValidationRow::makeRow);

    makeValidationRow(validationRows, "DPB1", ValidationModel::getDPB1,
                      AlleleValidationRow::makeRow);
    makeValidationRow(validationRows, "DPB1", ValidationModel::getDPB2,
                      AlleleValidationRow::makeRow);

    makeValidationRow(validationRows, "DR51 1", ValidationModel::getDR51_1,
                      DR345ValidationRow::makeRow);
    makeValidationRow(validationRows, "DR51 2", ValidationModel::getDR51_2,
                      DR345ValidationRow::makeRow);
    makeValidationRow(validationRows, "DR52 1", ValidationModel::getDR52_1,
                      DR345ValidationRow::makeRow);
    makeValidationRow(validationRows, "DR52 2", ValidationModel::getDR52_2,
                      DR345ValidationRow::makeRow);
    makeValidationRow(validationRows, "DR53 1", ValidationModel::getDR53_1,
                      DR345ValidationRow::makeRow);
    makeValidationRow(validationRows, "DR53 2", ValidationModel::getDR53_2,
                      DR345ValidationRow::makeRow);

    // A Table is valid if all its Rows are valid
    ObservableBooleanValue validBinding = null;
    for (ValidationRow<?> row : validationRows.get()) {
      validBinding = JFXPropertyHelper.andHelper(validBinding, row.isValidProperty());
    }
    isValidWrapper.bind(validBinding);

    bcHaplotypeRows.clear();
    drdqHaplotypeRows.clear();

    ValidationModel model = chooseHaplotypeModel(firstModelWrapper.get(), secondModelWrapper.get());
    if (Objects.nonNull(model)) {
      makeBCHaplotypeRows(bcHaplotypeRows, model);
      makeDRDQHaplotypeRows(drdqHaplotypeRows, model);
    }
  }

  private void makeDRDQHaplotypeRows(ReadOnlyListWrapper<DRDQHaplotypeRow> rows,
                                     ValidationModel model) {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      model.getDRDQHaplotypes().get(ethnicity).forEach(haplotype -> {
        rows.add(new DRDQHaplotypeRow(ethnicity, haplotype));
      });
    }
    ;
  }

  /** Use the given model to populate a list of {@link BCHaplotypeRow}s */
  private void makeBCHaplotypeRows(ReadOnlyListWrapper<BCHaplotypeRow> rows,
                                   ValidationModel model) {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      model.getBCHaplotypes().get(ethnicity).forEach(haplotype -> {
        rows.add(new BCHaplotypeRow(ethnicity, haplotype));
      });
    }
    ;
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
                                     Function<ValidationModel, T> getter, RowBuilder<T> builder) {
    rows.add(builder.makeRow(rowLabel, getFirstField(getter), getSecondField(getter)));
  }

  /**
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return The value of that field in the second column's model
   */
  public <T> T getSecondField(Function<ValidationModel, T> getter) {
    return getValueFromModel(getter, secondModelWrapper);
  }

  /**
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return The value of that field in the first column's model
   */
  public <T> T getFirstField(Function<ValidationModel, T> getter) {
    return getValueFromModel(getter, firstModelWrapper);
  }

  /**
   * @return a CSV table representation of the {@link ValidationModel}
   */
  public String generateCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append("Donor ID" + ","
                   + Objects.toString(getFirstField(ValidationModel::getDonorId), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getDonorId), "") + "\n");
    builder.append("A" + "," + Objects.toString(getFirstField(ValidationModel::getA1), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getA1), "") + "\n");
    builder.append("A" + "," + Objects.toString(getFirstField(ValidationModel::getA2), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getA2), "") + "\n");
    builder.append("B" + "," + Objects.toString(getFirstField(ValidationModel::getB1), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getB1), "") + "\n");
    builder.append("B" + "," + Objects.toString(getFirstField(ValidationModel::getB2), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getB2), "") + "\n");
    builder.append("BW4" + "," + Objects.toString(getFirstField(ValidationModel::isBw4), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::isBw4), "") + "\n");
    builder.append("BW6" + "," + Objects.toString(getFirstField(ValidationModel::isBw6), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::isBw6), "") + "\n");
    builder.append("C" + "," + Objects.toString(getFirstField(ValidationModel::getC1), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getC1), "") + "\n");
    builder.append("C" + "," + Objects.toString(getFirstField(ValidationModel::getC2), "") + ","
                   + Objects.toString(getSecondField(ValidationModel::getC2), "") + "\n");
    builder.append("DRB1" + "," + Objects.toString(getFirstField(ValidationModel::getDRB1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDRB1), "") + "\n");
    builder.append("DRB1" + "," + Objects.toString(getFirstField(ValidationModel::getDRB2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDRB2), "") + "\n");
    builder.append("DQB1" + "," + Objects.toString(getFirstField(ValidationModel::getDQB1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDQB1), "") + "\n");
    builder.append("DQB1" + "," + Objects.toString(getFirstField(ValidationModel::getDQB2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDQB2), "") + "\n");
    builder.append("DQA1" + "," + Objects.toString(getFirstField(ValidationModel::getDQA1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDQA1), "") + "\n");
    builder.append("DQA1" + "," + Objects.toString(getFirstField(ValidationModel::getDQA2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDQA2), "") + "\n");
    builder.append("DPB1" + "," + Objects.toString(getFirstField(ValidationModel::getDPB1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDPB1), "") + "\n");
    builder.append("DPB1" + "," + Objects.toString(getFirstField(ValidationModel::getDPB2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDPB2), "") + "\n");
    builder.append("DR51 1" + "," + Objects.toString(getFirstField(ValidationModel::getDR51_1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR51_1), "") + "\n");
    builder.append("DR51 2" + "," + Objects.toString(getFirstField(ValidationModel::getDR51_2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR51_2), "") + "\n");
    builder.append("DR52 1" + "," + Objects.toString(getFirstField(ValidationModel::getDR52_1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR52_1), "") + "\n");
    builder.append("DR52 2" + "," + Objects.toString(getFirstField(ValidationModel::getDR52_2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR52_2), "") + "\n");
    builder.append("DPB3_1" + "," + Objects.toString(getFirstField(ValidationModel::getDR53_1), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR53_1), "") + "\n");
    builder.append("DPB3_2" + "," + Objects.toString(getFirstField(ValidationModel::getDR53_2), "")
                   + "," + Objects.toString(getSecondField(ValidationModel::getDR53_2), "") + "\n");

    return builder.toString();

  }

  public String generateDonorNet() {
    ReadOnlyObjectWrapper<ValidationModel> wrap = null;
    File file = new File(firstSourceWrapper.getValue());
    if (firstModelWrapper.getValue().getSourceType().equals("DonorNet")) {
      wrap = firstModelWrapper;
    } else if (secondModelWrapper.getValue().getSourceType().equals("DonorNet")) {
      wrap = secondModelWrapper;
    }

    if (wrap != null) {
      StringBuilder builder = new StringBuilder();
      getValueFromModel(ValidationModel::getDonorId, wrap);
      builder.append("Donor ID" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDonorId, wrap), "")
                     + "\n");
      builder.append("A" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getA1, wrap), "")
                     + "\n");
      builder.append("A" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getA2, wrap), "")
                     + "\n");
      builder.append("B" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getB1, wrap), "")
                     + "\n");
      builder.append("B" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getB2, wrap), "")
                     + "\n");
      builder.append("BW4" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::isBw4, wrap), "")
                     + "\n");
      builder.append("BW6" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::isBw6, wrap), "")
                     + "\n");
      builder.append("C" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getC1, wrap), "")
                     + "\n");
      builder.append("C" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getC2, wrap), "")
                     + "\n");
      builder.append("DRB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDRB1, wrap), "")
                     + "\n");
      builder.append("DRB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDRB2, wrap), "")
                     + "\n");
      builder.append("DQB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDQB1, wrap), "")
                     + "\n");
      builder.append("DQB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDQB2, wrap), "")
                     + "\n");
      builder.append("DQA1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDQA1, wrap), "")
                     + "\n");
      builder.append("DQA1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDQA2, wrap), "")
                     + "\n");
      builder.append("DPB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDPB1, wrap), "")
                     + "\n");
      builder.append("DPB1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDPB2, wrap), "")
                     + "\n");
      builder.append("DR51 1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR51_1, wrap), "")
                     + "\n");
      builder.append("DR51 2" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR51_2, wrap), "")
                     + "\n");
      builder.append("DR52 1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR52_1, wrap), "")
                     + "\n");
      builder.append("DR52 2" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR52_2, wrap), "")
                     + "\n");
      builder.append("DPB3_1" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR53_1, wrap), "")
                     + "\n");
      builder.append("DPB3_2" + ","
                     + Objects.toString(getValueFromModel(ValidationModel::getDR53_2, wrap), "")
                     + "\n");

      return builder.toString();
    }
    return null;
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
