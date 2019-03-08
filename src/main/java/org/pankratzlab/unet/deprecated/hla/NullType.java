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
public class NullType extends HLAType {

  // Corresponding DRB345 allele to match DRB1's with no reported type
  // NB: MUST match the "null" DRB345 type in any haplotype file (2013_DRB3-4-5_DRB1_DQB1.csv)
  public static final NullType UNREPORTED_DRB345 = new NullType(HLALocus.DRB3, "00", "00");

  private static final long serialVersionUID = -171980058023333597L;
  public static final Pattern TYPE_PATTERN;

  public NullType(String locus, String... fields) {
    super(locus, fields);
  }

  public NullType(String locus, int... fields) {
    super(locus, fields);
  }

  public NullType(HLALocus locus, String... fields) {
    super(locus, fields);
  }

  public NullType(HLALocus locus, int... fields) {
    super(locus, fields);
  }

  public NullType(HLALocus locus, List<Integer> fields) {
    super(locus, fields);
  }

  public NullType(HLAType root) {
    super(root.locus(), root.spec());
  }

  /**
   * @return A {@link HLAType} representation of the given string
   */
  public static NullType valueOf(String typeString) {
    RawType rt = new RawType(typeString, TYPE_PATTERN);
    return new NullType(rt.locus(), rt.spec());
  }

  @Override
  public String toString() {
    return super.toString() + "N";
  }

  static {
    TYPE_PATTERN = Pattern.compile(HLAType.TYPE_PATTERN.pattern() + "N");
  }
}
