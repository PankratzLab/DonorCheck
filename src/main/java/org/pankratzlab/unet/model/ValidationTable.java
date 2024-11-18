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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.deprecated.jfx.JFXPropertyHelper;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import org.pankratzlab.unet.model.ValidationRow.RowBuilder;
import com.google.common.collect.ImmutableMap;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.image.WritableImage;

/**
 * Backing model for a validation, wrapping multiple {@link ValidationModel}s being compared. Use
 * {@link #getValidationRows()} for accessing a row-dominant view of these models.
 */
public class ValidationTable {

  private final ReadOnlyBooleanWrapper isValidWrapper;
  private final ReadOnlyObjectWrapper<String> firstFileWrapper;
  private final ReadOnlyObjectWrapper<String> firstSourceWrapper;
  private final ReadOnlyObjectWrapper<SourceType> firstSourceTypeWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> firstModelWrapper;
  private final ReadOnlyObjectWrapper<String> secondFileWrapper;
  private final ReadOnlyObjectWrapper<String> secondSourceWrapper;
  private final ReadOnlyObjectWrapper<SourceType> secondSourceTypeWrapper;
  private final ReadOnlyObjectWrapper<ValidationModel> secondModelWrapper;
  private final ReadOnlyListWrapper<ValidationRow<?>> validationRows;
  private final ReadOnlyListWrapper<String> auditLogLines;
  private final ReadOnlyListWrapper<BCHaplotypeRow> bcHaplotypeRows;
  private final ReadOnlyListWrapper<DRDQHaplotypeRow> drdqHaplotypeRows;
  private WritableImage validationImage = null;

  public final static String REMAP_SYMBOL = "⦿";

  public ValidationTable() {
    isValidWrapper = new ReadOnlyBooleanWrapper();
    firstFileWrapper = new ReadOnlyObjectWrapper<>();
    firstSourceWrapper = new ReadOnlyObjectWrapper<>();
    firstModelWrapper = new ReadOnlyObjectWrapper<>();
    firstSourceTypeWrapper = new ReadOnlyObjectWrapper<>();
    secondFileWrapper = new ReadOnlyObjectWrapper<>();
    secondSourceWrapper = new ReadOnlyObjectWrapper<>();
    secondModelWrapper = new ReadOnlyObjectWrapper<>();
    secondSourceTypeWrapper = new ReadOnlyObjectWrapper<>();
    validationRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    auditLogLines = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    bcHaplotypeRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    drdqHaplotypeRows = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

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

  public ReadOnlyObjectProperty<String> firstColFile() {
    return firstFileWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<String> secondColFile() {
    return secondFileWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<String> firstColSource() {
    return firstSourceWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<String> secondColSource() {
    return secondSourceWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<SourceType> firstColSourceType() {
    return firstSourceTypeWrapper.getReadOnlyProperty();
  }

  public ReadOnlyObjectProperty<SourceType> secondColSourceType() {
    return secondSourceTypeWrapper.getReadOnlyProperty();
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
    firstFileWrapper.set(model.getFile());
    firstSourceWrapper.set(model.getSource());
    firstSourceTypeWrapper.set(model.getSourceType());
    firstModelWrapper.set(model);
  }

  /** @param model New {@link ValidationModel} for the second column in the table */
  public void setSecondModel(ValidationModel model) {
    secondFileWrapper.set(model.getFile());
    secondSourceWrapper.set(model.getSource());
    secondSourceTypeWrapper.set(model.getSourceType());
    secondModelWrapper.set(model);
  }

  /** @return A {@link BooleanProperty} tracking the validity of the complete table */
  public ReadOnlyBooleanProperty isValidProperty() {
    return isValidWrapper.getReadOnlyProperty();
  }

  /** @return A {@link BooleanProperty} tracking the validity of the complete table */
  public BooleanBinding hasAuditLines() {
    return auditLogLines.emptyProperty().not();
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
        StringValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, generateRowLabel(HLALocus.A), ValidationModel::getA1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.A, firstModelWrapper),
        wasRemapped(HLALocus.A, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.A), ValidationModel::getA2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.A, firstModelWrapper),
        wasRemapped(HLALocus.A, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.B), ValidationModel::getB1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.B, firstModelWrapper),
        wasRemapped(HLALocus.B, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.B), ValidationModel::getB2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.B, firstModelWrapper),
        wasRemapped(HLALocus.B, secondModelWrapper));

    makeValidationRow(validationRows, "BW4", ValidationModel::isBw4, StringValidationRow::makeRow,
        false, false);
    makeValidationRow(validationRows, "BW6", ValidationModel::isBw6, StringValidationRow::makeRow,
        false, false);

    makeValidationRow(validationRows, generateRowLabel(HLALocus.C), ValidationModel::getC1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.C, firstModelWrapper),
        wasRemapped(HLALocus.C, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.C), ValidationModel::getC2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.C, firstModelWrapper),
        wasRemapped(HLALocus.C, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.DRB1), ValidationModel::getDRB1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DRB1, firstModelWrapper),
        wasRemapped(HLALocus.DRB1, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.DRB1), ValidationModel::getDRB2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DRB1, firstModelWrapper),
        wasRemapped(HLALocus.DRB1, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.DQB1), ValidationModel::getDQB1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DQB1, firstModelWrapper),
        wasRemapped(HLALocus.DQB1, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.DQB1), ValidationModel::getDQB2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DQB1, firstModelWrapper),
        wasRemapped(HLALocus.DQB1, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.DQA1), ValidationModel::getDQA1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DQA1, firstModelWrapper),
        wasRemapped(HLALocus.DQA1, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.DQA1), ValidationModel::getDQA2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DQA1, firstModelWrapper),
        wasRemapped(HLALocus.DQA1, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.DPA1), ValidationModel::getDPA1,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DPA1, firstModelWrapper),
        wasRemapped(HLALocus.DPA1, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.DPA1), ValidationModel::getDPA2,
        AntigenValidationRow::makeRow, wasRemapped(HLALocus.DPA1, firstModelWrapper),
        wasRemapped(HLALocus.DPA1, secondModelWrapper));

    makeValidationRow(validationRows, generateRowLabel(HLALocus.DPB1), ValidationModel::getDPB1,
        AlleleValidationRow::makeRow, wasRemapped(HLALocus.DPB1, firstModelWrapper),
        wasRemapped(HLALocus.DPB1, secondModelWrapper));
    makeValidationRow(validationRows, generateRowLabel(HLALocus.DPB1), ValidationModel::getDPB2,
        AlleleValidationRow::makeRow, wasRemapped(HLALocus.DPB1, firstModelWrapper),
        wasRemapped(HLALocus.DPB1, secondModelWrapper));

    makeValidationRow(validationRows, "DR51 1", ValidationModel::getDR51_1,
        DR345ValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, "DR51 2", ValidationModel::getDR51_2,
        DR345ValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, "DR52 1", ValidationModel::getDR52_1,
        DR345ValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, "DR52 2", ValidationModel::getDR52_2,
        DR345ValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, "DR53 1", ValidationModel::getDR53_1,
        DR345ValidationRow::makeRow, false, false);
    makeValidationRow(validationRows, "DR53 2", ValidationModel::getDR53_2,
        DR345ValidationRow::makeRow, false, false);

    // A Table is valid if all its Rows are valid
    ObservableBooleanValue validBinding = null;
    for (ValidationRow<?> row : validationRows.get()) {
      validBinding = JFXPropertyHelper.andHelper(validBinding, row.isValidProperty());
    }
    isValidWrapper.bind(validBinding);

    bcHaplotypeRows.clear();
    drdqHaplotypeRows.clear();

    ValidationModel model = chooseHaplotypeModel(firstModelWrapper.get(), secondModelWrapper.get());
    if (Objects.nonNull(model) && HaplotypeFrequencies.successfullyInitialized()) {
      makeBCHaplotypeRows(bcHaplotypeRows, model);
      makeDRDQHaplotypeRows(drdqHaplotypeRows, model);
    }

    generateAuditLogLines();
  }

  private String generateRowLabel(HLALocus locus) {
    return locus.name()
        + (wasRemapped(locus, firstModelWrapper) || wasRemapped(locus, secondModelWrapper)
            ? (" " + REMAP_SYMBOL)
            : "");
  }

  private boolean wasRemapped(HLALocus locus, ReadOnlyObjectWrapper<ValidationModel> modelWrapper) {
    return (modelWrapper.isNotNull().get() && modelWrapper.get().wasRemapped(locus));
  }

  private void makeDRDQHaplotypeRows(ReadOnlyListWrapper<DRDQHaplotypeRow> rows,
      ValidationModel model) {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      model.getDRDQHaplotypes().get(ethnicity).forEach(haplotype -> {
        rows.add(new DRDQHaplotypeRow(ethnicity, haplotype));
      });
    } ;
  }

  /** Use the given model to populate a list of {@link BCHaplotypeRow}s */
  private void makeBCHaplotypeRows(ReadOnlyListWrapper<BCHaplotypeRow> rows,
      ValidationModel model) {
    for (RaceGroup ethnicity : RaceGroup.values()) {
      model.getBCHaplotypes().get(ethnicity).forEach(haplotype -> {
        rows.add(new BCHaplotypeRow(ethnicity, haplotype));
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
      Function<ValidationModel, T> getter, RowBuilder<T> builder, boolean wasRemappedFirst,
      boolean wasRemappedSecond) {
    rows.add(builder.makeRow(rowLabel, getFirstField(getter), getSecondField(getter),
        wasRemappedFirst, wasRemappedSecond));
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
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return comma seperated first and second field for the gene given
   */
  private String getComaSeperatedFieldSpecStrings(Function<ValidationModel, SeroType> getter) {
    String s1 = "";
    String s2 = "";
    if (getFirstField(getter) != null) {
      s1 = getFirstField(getter).specString();
    }
    if (getSecondField(getter) != null) {
      s2 = getSecondField(getter).specString();
    }
    return s1 + "," + s2;
  }

  /**
   * @param getter Accessor for {@link ValidationModel} field of interest
   * @return comma seperated first and second field for the gene given
   */
  private String getComaSeperatedFieldSpecStringsHLA(Function<ValidationModel, HLAType> getter) {
    String s1 = "";
    String s2 = "";
    if (getFirstField(getter) != null) {
      s1 = getFirstField(getter).specString();
    }
    if (getSecondField(getter) != null) {
      s2 = getSecondField(getter).specString();
    }
    return s1 + "," + s2;
  }

  /**
   * @return a CSV table representation of the {@link ValidationModel}
   */
  public String generateCSV() {
    StringBuilder builder = new StringBuilder();

    builder
        .append("Donor ID" + "," + Objects.toString(getFirstField(ValidationModel::getDonorId), "")
            + "," + Objects.toString(getSecondField(ValidationModel::getDonorId), "") + "\n");
    builder
        .append("Source" + "," + Objects.toString(getFirstField(ValidationModel::getSourceType), "")
            + "," + Objects.toString(getSecondField(ValidationModel::getSourceType), "") + "\n");
    builder.append("A" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getA1) + "\n");
    builder.append("A" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getA2) + "\n");
    builder.append("B" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getB1) + "\n");
    builder.append("B" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getB2) + "\n");
    builder.append("BW4" + "," + Objects.toString(getFirstField(ValidationModel::isBw4), "") + ","
        + Objects.toString(getSecondField(ValidationModel::isBw4), "") + "\n");
    builder.append("BW6" + "," + Objects.toString(getFirstField(ValidationModel::isBw6), "") + ","
        + Objects.toString(getSecondField(ValidationModel::isBw6), "") + "\n");
    builder.append("C" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getC1) + "\n");
    builder.append("C" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getC2) + "\n");
    builder
        .append("DRB1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDRB1) + "\n");
    builder
        .append("DRB1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDRB2) + "\n");
    builder
        .append("DQB1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDQB1) + "\n");
    builder
        .append("DQB1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDQB2) + "\n");
    builder
        .append("DQA1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDQA1) + "\n");
    builder
        .append("DQA1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDQA2) + "\n");
    builder.append(
        "DPB1" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDPB1) + "\n");
    builder.append(
        "DPB1" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDPB2) + "\n");
    builder
        .append("DPA1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDPA1) + "\n");
    builder
        .append("DPA1" + "," + getComaSeperatedFieldSpecStrings(ValidationModel::getDPA2) + "\n");
    builder.append(
        "DR51 1" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR51_1) + "\n");
    builder.append(
        "DR51 2" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR51_2) + "\n");
    builder.append(
        "DR52 1" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR52_1) + "\n");
    builder.append(
        "DR52 2" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR52_2) + "\n");
    builder.append(
        "DPB3 1" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR53_1) + "\n");
    builder.append(
        "DPB3 2" + "," + getComaSeperatedFieldSpecStringsHLA(ValidationModel::getDR53_2) + "\n");

    return builder.toString();

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

  private void generateAuditLogLines() {
    auditLogLines.clear();
    if (firstModelWrapper.isNotNull().get())
      auditLogLines.addAll(generateRemappings(firstModelWrapper.get()));
    if (secondModelWrapper.isNotNull().get())
      auditLogLines.addAll(generateRemappings(secondModelWrapper.get()));
  }

  public ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> getFirstRemappings() {
    if (firstModelWrapper.isNotNull().get())
      return firstModelWrapper.get().getRemappings();
    return ImmutableMap.of();
  }

  public ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> getSecondRemappings() {
    if (secondModelWrapper.isNotNull().get())
      return secondModelWrapper.get().getRemappings();
    return ImmutableMap.of();
  }

  private String[] generateRemappings(ValidationModel model) {
    return model.getRemappings().entrySet().stream().map(e -> {
      final String collectFrom = e.getValue().getLeft().stream().sorted().map(TypePair::getHlaType)
          .map((h) -> h.specString()).collect(Collectors.joining(" / "));
      final String collectTo = e.getValue().getRight().stream().sorted().map(TypePair::getHlaType)
          .map((h) -> h.specString()).collect(Collectors.joining(" / "));
      return "HLA-" + e.getKey().name() + " was remapped from { " + collectFrom + " } to { "
          + collectTo + " } in " + model.getSourceType();
    }).sorted().distinct().toArray(String[]::new);
  }

  public ObservableValue<String> getAuditLogLines() {
    return Bindings.createStringBinding(() -> auditLogLines.stream()
        .map(s -> REMAP_SYMBOL + " " + s).collect(Collectors.joining("\n")), auditLogLines);
  }

  public ObservableValue<Number> getAuditLogLineCount() {
    return Bindings.createIntegerBinding(() -> auditLogLines.size(), auditLogLines);
  }
}
