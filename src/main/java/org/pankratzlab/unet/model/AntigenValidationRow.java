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

import org.pankratzlab.unet.deprecated.hla.SeroType;

/** {@link ValidationRow} for displaying {@link SeroType}s */
public class AntigenValidationRow extends ValidationRow<SeroType> {

  public AntigenValidationRow(String rowLabel, SeroType firstCol, SeroType secondCol,
      boolean wasRemappedFirst, boolean wasRemappedSecond) {
    super(rowLabel, firstCol, secondCol, wasRemappedFirst, wasRemappedSecond);
  }

  @Override
  protected String getDisplayString(@Nullable SeroType toDisplay) throws IllegalStateException {
    if (Objects.isNull(toDisplay)) {
      return "";
    }

    return toDisplay.specString();
  }

  public static AntigenValidationRow makeRow(String rowLabel, SeroType firstCol, SeroType secondCol,
      boolean wasRemapped, boolean wasRemappedSecond) {
    return new AntigenValidationRow(rowLabel, firstCol, secondCol, wasRemapped, wasRemappedSecond);
  }

}
