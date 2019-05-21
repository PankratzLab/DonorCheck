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

import java.math.BigDecimal;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public abstract class AbstractHaplotypeRow implements HaplotypeRow {

  private final ReadOnlyStringWrapper ethnicityDisplay;
  private final ReadOnlyObjectWrapper<RaceGroup> ethnicity;
  private final ReadOnlyObjectWrapper<Haplotype> haplotype;
  private final ReadOnlyObjectWrapper<BigDecimal> frequencyProperty;

  public AbstractHaplotypeRow(RaceGroup ethnicity, Haplotype haplotype) {
    super();
    this.ethnicity = new ReadOnlyObjectWrapper<>(ethnicity);
    ethnicityDisplay = new ReadOnlyStringWrapper(ethnicity.toString());
    this.haplotype = new ReadOnlyObjectWrapper<>(haplotype);
    BigDecimal frequency = HaplotypeFrequencies.getFrequency(ethnicity, haplotype);
    frequencyProperty =
        new ReadOnlyObjectWrapper<>(frequency.setScale(HaplotypeFrequencies.UNKNOWN_HAP_SIG_FIGS,
            HaplotypeFrequencies.UNKNOWN_HAP_ROUNDING_MODE));
  }

  /**
   * @return Property for this row's {@link RaceGroup}
   */
  public ReadOnlyObjectProperty<RaceGroup> ethnicityProperty() {
    return ethnicity.getReadOnlyProperty();
  }

  /**
   * @return Property for a display string for this row's {@link RaceGroup}
   */
  public ReadOnlyStringProperty ethnicityDisplayProperty() {
    return ethnicityDisplay.getReadOnlyProperty();
  }

  /**
   * @return Property for this row's {@link Haplotype}
   */
  public ReadOnlyObjectProperty<Haplotype> haplotypeProperty() {
    return haplotype.getReadOnlyProperty();
  }

  @Override
  public ReadOnlyObjectProperty<BigDecimal> frequencyProperty() {
    return frequencyProperty.getReadOnlyProperty();
  }

  protected ReadOnlyObjectWrapper<HLAType> getAlleleWrapper(HLAType allele) {
    return new ReadOnlyObjectWrapper<>(allele);
  }
}
