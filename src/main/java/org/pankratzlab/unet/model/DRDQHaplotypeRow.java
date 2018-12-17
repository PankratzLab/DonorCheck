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
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.RaceGroup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * One row of a Haplotype table (showing all haplotypes for an individual)
 */
public class DRDQHaplotypeRow implements HaplotypeRow {
  public static final String ETHNICITY_PROP = "ethnicityDisplay";
  public static final String DRB1_ALLELE_PROP = "alleleDRB1";
  public static final String DQB1_ALLELE_PROP = "alleleDQB1";
  public static final String DRB345_PROP = "alleleDRB345";

  private final ReadOnlyStringWrapper ethnicityDisplay;
  private final ReadOnlyObjectWrapper<Haplotype> haplotype;
  private final ReadOnlyObjectWrapper<RaceGroup> ethnicity;
  private final ReadOnlyStringWrapper alleleDRB1;
  private final ReadOnlyStringWrapper alleleDQB1;
  private final ReadOnlyStringWrapper alleleDRB345;

  public DRDQHaplotypeRow(RaceGroup ethnicity, Haplotype haplotype) {
    super();
    this.ethnicity = new ReadOnlyObjectWrapper<>(ethnicity);
    ethnicityDisplay = new ReadOnlyStringWrapper(ethnicity.toString());
    this.haplotype = new ReadOnlyObjectWrapper<>(haplotype);
    HLAType drb1 = null;
    HLAType dqb1 = null;
    HLAType drb345 = null;

    for (HLAType hlaType : haplotype.getTypes()) {
      switch (hlaType.locus()) {
        case DQB1:
          dqb1 = hlaType;
          break;
        case DRB1:
          drb1 = hlaType;
          break;
        case DRB3:
        case DRB4:
        case DRB5:
          drb345 = hlaType;
          break;
        default:
          break;

      }
    }
    if (Objects.isNull(dqb1) || Objects.isNull(drb1) || Objects.isNull(drb345)) {
      throw new IllegalArgumentException(
          "Invalid DRB345-DRB1-DQB1 haplotype: " + haplotype.toShortString());
    }

    alleleDRB1 = new ReadOnlyStringWrapper(drb1.toString());
    alleleDQB1 = new ReadOnlyStringWrapper(dqb1.toString());
    alleleDRB345 = new ReadOnlyStringWrapper(drb345.toString());
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
   * @return Property for the C allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleDRB1Property() {
    return alleleDRB1.getReadOnlyProperty();
  }

  /**
   * @return Property for the B allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleDQB1Property() {
    return alleleDQB1.getReadOnlyProperty();
  }

  /**
   * @return Property for the Bw group status of this row's B allele
   */
  public ReadOnlyStringProperty alleleDRB345Property() {
    return alleleDRB345.getReadOnlyProperty();
  }

  /**
   * @return Property for this row's {@link Haplotype}
   */
  public ReadOnlyObjectProperty<Haplotype> haplotypeProperty() {
    return haplotype.getReadOnlyProperty();
  }
}
