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
package org.pankratzlab.unet.deprecated.jfx;

import javax.annotation.Nullable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableBooleanValue;

/**
 * Static utility class for working with JavaFX {@link Property}
 */
public final class JFXPropertyHelper {

  private JFXPropertyHelper() {
    // Prevent instantiation of utility class
  }

  /**
   * Helper method to handle null checking when ORing two {@link ObservableBooleanValue}s.
   *
   * @return If {@code optional} is null, {@code reference} is returned. Otherwise a boolean OR
   *         binding combining the two individual booleans is created and returned.
   */
  public static ObservableBooleanValue orHelper(@Nullable ObservableBooleanValue optional,
      ObservableBooleanValue reference) {
    if (optional == null) {
      return reference;
    }
    return Bindings.or(optional, reference);
  }

  public static ObservableBooleanValue andHelper(@Nullable ObservableBooleanValue optional,
      ObservableBooleanValue reference) {
    if (optional == null) {
      return reference;
    }
    return Bindings.and(optional, reference);
  }
}
