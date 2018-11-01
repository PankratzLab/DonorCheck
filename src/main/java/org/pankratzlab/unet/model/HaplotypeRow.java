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

import java.util.ArrayList;
import java.util.List;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies.Ethnicity;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * One row of a Haplotype table (showing all haplotypes for an individual)
 */
public class HaplotypeRow {
  public static final String ETHNICITY_PROP = "ethnicityDisplay";
  public static final String ALLELE_1_PROP = "alleleOne";
  public static final String ALLELE_2_PROP = "alleleTwo";

  private final ReadOnlyStringWrapper ethnicityDisplay;
  private final ReadOnlyObjectWrapper<Haplotype> haplotype;
  private final ReadOnlyObjectWrapper<Ethnicity> ethnicity;
  private final ReadOnlyStringWrapper alleleOne;
  private final ReadOnlyStringWrapper alleleTwo;

  public HaplotypeRow(Ethnicity ethnicity, Haplotype haplotype) {
    super();
    this.ethnicity = new ReadOnlyObjectWrapper<>(ethnicity);
    ethnicityDisplay = new ReadOnlyStringWrapper(ethnicity.displayString());
    this.haplotype = new ReadOnlyObjectWrapper<>(haplotype);
    List<HLAType> typeList = new ArrayList<>(haplotype.getTypes());
    alleleOne = new ReadOnlyStringWrapper(typeList.get(0).toString());
    alleleTwo = new ReadOnlyStringWrapper(typeList.get(1).toString());
  }

  /**
   * @return Property for this row's {@link Ethnicity}
   */
  public ReadOnlyObjectProperty<Ethnicity> ethnicityProperty() {
    return ethnicity.getReadOnlyProperty();
  }

  /**
   * @return Property for a display string for this row's {@link Ethnicity}
   */
  public ReadOnlyStringProperty ethnicityDisplayProperty() {
    return ethnicityDisplay.getReadOnlyProperty();
  }

  /**
   * @return Property for the first allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleOneProperty() {
    return alleleOne.getReadOnlyProperty();
  }

  /**
   * @return Property for the second allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleTwoProperty() {
    return alleleTwo.getReadOnlyProperty();
  }

  /**
   * @return Property for this row's {@link Haplotype}
   */
  public ReadOnlyObjectProperty<Haplotype> haplotypeProperty() {
    return haplotype.getReadOnlyProperty();
  }
}
