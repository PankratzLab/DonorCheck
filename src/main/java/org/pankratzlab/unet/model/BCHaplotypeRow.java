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

import java.util.Objects;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.hapstats.Haplotype;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.parser.util.BwSerotypes;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/** One row of a Haplotype table (showing all haplotypes for an individual) */
public class BCHaplotypeRow extends AbstractHaplotypeRow {
  public static final String C_ALLELE_PROP = "alleleC";
  public static final String B_ALLELE_PROP = "alleleB";
  public static final String BW_GROUP_PROP = "bwGroup";

  private final ReadOnlyObjectWrapper<HLAType> alleleC;
  private final ReadOnlyObjectWrapper<HLAType> alleleB;
  private final ReadOnlyStringWrapper bwGroup;

  public BCHaplotypeRow(RaceGroup ethnicity, Haplotype haplotype) {
    super(ethnicity, haplotype);
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

    alleleC = getAlleleWrapper(c);
    alleleB = getAlleleWrapper(b);
    BwGroup group = BwSerotypes.getBwGroup(b);
    bwGroup = new ReadOnlyStringWrapper(group.toString());
  }

  /** @return Property for the C allele of this row's {@link Haplotype} */
  public ReadOnlyObjectProperty<HLAType> alleleCProperty() {
    return alleleC.getReadOnlyProperty();
  }

  /** @return Property for the B allele of this row's {@link Haplotype} */
  public ReadOnlyObjectProperty<HLAType> alleleBProperty() {
    return alleleB.getReadOnlyProperty();
  }

  /** @return Property for the Bw group status of this row's B allele */
  public ReadOnlyStringProperty bwGroupProperty() {
    return bwGroup.getReadOnlyProperty();
  }
}
