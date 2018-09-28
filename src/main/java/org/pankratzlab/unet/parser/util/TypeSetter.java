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
package org.pankratzlab.unet.parser.util;

import java.util.function.BiConsumer;
import org.pankratzlab.unet.model.ValidationModelBuilder;

public class TypeSetter {
  private String tokenPrefix;
  private BiConsumer<ValidationModelBuilder, String> setter;

  public TypeSetter() {}

  public TypeSetter(String prefix, BiConsumer<ValidationModelBuilder, String> setter) {
    tokenPrefix = prefix;
    this.setter = setter;
  }

  public String getTokenPrefix() {
    return tokenPrefix;
  }

  public BiConsumer<ValidationModelBuilder, String> getSetter() {
    return setter;
  }
}
