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

import javax.annotation.Nullable;

import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;

import com.google.common.collect.ImmutableSet;

/**
 * {@link AlleleValidationRow} for alleles of the DRB345 loci. Unlike other alleles, instead of
 * displaying blank when absent (null), these display a {@link #NULL_STRING}
 */
public class DR345ValidationRow extends AlleleValidationRow {
  private static final String NULL_STRING = "Negative";
  private static final ImmutableSet<HLALocus> VALID_LOCI = ImmutableSet.of(HLALocus.DRB3,
                                                                           HLALocus.DRB4,
                                                                           HLALocus.DRB5);

  public DR345ValidationRow(String rowLabel, HLAType firstCol, HLAType secondCol,
                            boolean wasRemapped) {
    super(rowLabel, firstCol, secondCol, wasRemapped);

    if (isInvalidCol(firstCol) || isInvalidCol(secondCol)) {
      throw new IllegalArgumentException("Invalid DRB345 types: " + firstCol + ", " + secondCol);
    }
  }

  /** @return true if the given type is an HLA345 type. */
  private boolean isInvalidCol(HLAType colValue) {
    // An invalid value is a non-null HLAType with a locus outside the valid set
    return !(Objects.isNull(colValue) || VALID_LOCI.contains(colValue.locus()));
  }

  @Override
  protected String getDisplayString(@Nullable HLAType toDisplay) throws IllegalStateException {
    if (Objects.isNull(toDisplay)) {
      return NULL_STRING;
    }

    return super.getDisplayString(toDisplay);
  }

  public static DR345ValidationRow makeRow(String rowLabel, HLAType firstCol, HLAType secondCol,
                                           boolean wasRemapped) {
    return new DR345ValidationRow(rowLabel, firstCol, secondCol, wasRemapped);
  }
}
