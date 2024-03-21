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
package org.pankratzlab.unet.deprecated.hla;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A null type is a normal allele ({@link HLAType}) that is not expressed. They are indicated with a
 * "N" after the allele's field values.
 */
public class LabelledType extends HLAType {

  /** */
  private static final long serialVersionUID = 3014888603304504292L;

  public static final Pattern TYPE_PATTERN;

  private final char label;

  public LabelledType(HLAType root, char label) {
    super(root.locus(), root.spec());
    if (!matchesLabel(label)) {
      throw new IllegalArgumentException("Invalid label: " + label);
    }
    this.label = label;
  }

  public LabelledType(HLALocus parsedLocus, List<Integer> spec, char charAt) {
    this(new HLAType(parsedLocus, spec), charAt);
  }

  /** @return A {@link HLAType} representation of the given string */
  public static LabelledType valueOf(String typeString) {
    RawType rt = new RawType(typeString, TYPE_PATTERN);
    return new LabelledType(new HLAType(rt.locus(), rt.spec()),
        typeString.charAt(typeString.length() - 1));
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public char getLabel() {
    return label;
  }

  static {
    TYPE_PATTERN = Pattern.compile(HLAType.TYPE_PATTERN.pattern() + "([LSCAQ])");
  }

  public static boolean matches(String specString) {
    return matchesLabel(specString.charAt(specString.length() - 1));
  }

  public static boolean matchesLabel(char label) {
    return "LSCAQ".contains(label + "");
  }
}
