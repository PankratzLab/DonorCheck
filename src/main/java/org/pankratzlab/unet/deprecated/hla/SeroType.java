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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@link Antigen} implementation for serological antigens */
public class SeroType extends Antigen<SeroLocus, SeroType> {
  private static final long serialVersionUID = 6L;

  /** Match {@link SeroLocus} strings */
  public static final Pattern LOCI_PATTERN;

  /** As {@link #TYPE_PATTERN} but will match locus-only strings */
  public static final Pattern PARTIAL_PATTERN;

  /**
   * Matches string representations of {@link SeroType}s
   *
   * <p>
   * Group 1 is the {@link SeroLocus}, group 2 is the {@link #SPEC_DELIM}ited specification, and group
   * 3 is the parent specification e.g. 2(5) (NB: group 0 is the complete match in the
   * {@link Matcher#group(int)} api)
   */
  public static final Pattern TYPE_PATTERN;

  public static final int LATEST_REVISION = 1 + Antigen.LATEST_REVISION;

  private int revision = LATEST_REVISION;

  /** @see SeroType#SeroType(String, int...) */
  public SeroType(String l, String... p) {
    this(SeroLocus.safeValueOf(l), p);
  }

  /** Auto-parses {@link SeroLocus} value */
  public SeroType(String l, int... p) {
    this(SeroLocus.safeValueOf(l), p);
  }

  /** @see Antigen#Antigen(Locus, String...) */
  public SeroType(SeroLocus l, String... p) {
    super(l, p);
  }

  /** @see Antigen#Antigen(Locus, int...) */
  public SeroType(SeroLocus l, int... p) {
    super(l, p);
  }

  /** @see Antigen#Antigen(Locus, List) */
  public SeroType(SeroLocus l, List<Integer> p) {
    super(l, p);
  }

  @Override
  protected List<Integer> parse(int[] p) {
    List<Integer> values = new ArrayList<>();

    int ind = 0;
    for (int i = 0; i < p.length; i++) {
      int v = p[i];
      int iter = 0;
      while (v > 200) {
        // Divide by 100, extracting the rightmost two digits
        // ones column 4013 > 401, 3
        int split = v % 10;
        v /= 10;
        // 10s column 401, 3 > 40, 13
        split += (10 * (v % 10));
        v /= 10;
        values.add(ind, split);
        iter++;
      }
      values.add(ind, v);
      ind += iter + 1;
    }

    return values;
  }

  /** @see Antigen#is(String, Pattern) */
  public static boolean is(String text) {
    return Antigen.is(text, TYPE_PATTERN);
  }

  /**
   * @return Set of {@link SeroType}s parsed from the input strings, per {@link #valueOf(String)}
   */
  public static Set<SeroType> valueOf(String... antigens) {
    Set<SeroType> antigenSet = new LinkedHashSet<>();
    for (String antigen : antigens) {
      antigenSet.add(SeroType.valueOf(antigen));
    }
    return antigenSet;
  }

  /** A {@link SeroType} representation of the given string */
  public static SeroType valueOf(String typeString) {
    RawType rt = new RawType(typeString, TYPE_PATTERN);
    return new SeroType(rt.locus(), rt.spec());
  }

  /** @see Antigen#parseTypes(String, java.util.regex.Pattern, java.util.function.Function) */
  public static List<SeroType> parseTypes(String text) {
    return Antigen.parseTypes(text, LOCI_PATTERN, SeroType::valueOf);
  }

  static {
    // Static initializer to create patterns
    // See also SeroType
    LOCI_PATTERN = makePattern(SeroLocus.valuesWithAliases());

    TYPE_PATTERN = Pattern.compile(LOCI_PATTERN.pattern() + SPEC_PATTERN.pattern());
    PARTIAL_PATTERN = Pattern.compile(LOCI_PATTERN.pattern() + "(?:" + SPEC_PATTERN.pattern() + ")?");

    /*
     * --- WARNING --- Changing the patterns in a way that affects the number of groups can have
     * terrible/unexpected reprecussions. Currently we do not have a way to guarantee compliance with a
     * particular group count, etc. It would be safer to refactor these patterns into an Object with
     * proper accessors that can be updated to expose the underlying groups.
     */
  }
}
