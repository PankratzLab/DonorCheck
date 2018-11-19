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
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies.Ethnicity;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import com.google.common.collect.ImmutableMap;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * One row of a Haplotype table (showing all haplotypes for an individual)
 */
public class BCHaplotypeRow {
  public static final String ETHNICITY_PROP = "ethnicityDisplay";
  public static final String C_ALLELE_PROP = "alleleC";
  public static final String B_ALLELE_PROP = "alleleB";
  public static final String BW_GROUP_PROP = "bwGroup";

  private final ReadOnlyStringWrapper ethnicityDisplay;
  private final ReadOnlyObjectWrapper<Haplotype> haplotype;
  private final ReadOnlyObjectWrapper<Ethnicity> ethnicity;
  private final ReadOnlyStringWrapper alleleC;
  private final ReadOnlyStringWrapper alleleB;
  private final ReadOnlyStringWrapper bwGroup;

  public BCHaplotypeRow(Ethnicity ethnicity, Haplotype haplotype,
      ImmutableMap<HLAType, BwGroup> bwMap) {
    super();
    this.ethnicity = new ReadOnlyObjectWrapper<>(ethnicity);
    ethnicityDisplay = new ReadOnlyStringWrapper(ethnicity.displayString());
    this.haplotype = new ReadOnlyObjectWrapper<>(haplotype);
    HLAType c = null;
    HLAType b = null;
    for (HLAType hlaType : haplotype.getTypes()) {
      switch (hlaType.locus()) {
        case B:
          b = hlaType;
          break;
        case C:
          c = hlaType;
          break;
        default:
          break;

      }
    }
    if (Objects.isNull(b) || Objects.isNull(c)) {
      throw new IllegalArgumentException("Invalid B-C haplotype: " + haplotype.toShortString());
    }

    alleleC = new ReadOnlyStringWrapper(c.toString());
    alleleB = new ReadOnlyStringWrapper(b.toString());
    BwGroup group = BwGroup.Unknown;
    if (Objects.nonNull(bwMap) && bwMap.containsKey(b)) {
      group = bwMap.get(b);
    }
    bwGroup = new ReadOnlyStringWrapper(group.toString());
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
   * @return Property for the C allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleCProperty() {
    return alleleC.getReadOnlyProperty();
  }

  /**
   * @return Property for the B allele of this row's {@link Haplotype}
   */
  public ReadOnlyStringProperty alleleBProperty() {
    return alleleB.getReadOnlyProperty();
  }

  /**
   * @return Property for the Bw group status of this row's B allele
   */
  public ReadOnlyStringProperty bwGroupProperty() {
    return bwGroup.getReadOnlyProperty();
  }

  /**
   * @return Property for this row's {@link Haplotype}
   */
  public ReadOnlyObjectProperty<Haplotype> haplotypeProperty() {
    return haplotype.getReadOnlyProperty();
  }
}
