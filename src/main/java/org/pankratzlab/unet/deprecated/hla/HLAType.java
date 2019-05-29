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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.primitives.Ints;

/** {@link Antigen} implementation for HLA antigens */
public class HLAType extends Antigen<HLALocus, HLAType> {
  private static final long serialVersionUID = 6L;

  /** As {@link #TYPE_PATTERN} but will match locus-only strings */
  public static final Pattern PARTIAL_PATTERN;

  /** Match {@link HLALocus} strings */
  public static final Pattern LOCI_PATTERN;

  /**
   * Matches string representations of {@link HLAType}s
   *
   * <p>Group 1 is the {@link HLALocus}, group 2 is the {@link #SPEC_DELIM}ited specification, and
   * group 3 is the parent specification e.g. 2(5) (NB: group 0 is the complete match in the {@link
   * Matcher#group(int)} api)
   */
  public static final Pattern TYPE_PATTERN;

  public static final int LATEST_REVISION = 1 + Antigen.LATEST_REVISION;

  private int revision = LATEST_REVISION;

  /** @see HLAType#HLAType(String, int...) */
  public HLAType(String l, String... p) {
    this(HLALocus.valueOf(sanitize(l)), p);
  }

  /** Auto-parses {@link HLALocus} */
  public HLAType(String l, int... p) {
    this(HLALocus.valueOf(sanitize(l)), p);
  }

  /** @see Antigen#Antigen(Locus, String...) */
  public HLAType(HLALocus l, String... p) {
    super(l, p);
  }

  /** @see Antigen#Antigen(Locus, int...) */
  public HLAType(HLALocus l, int... p) {
    super(l, p);
  }

  /** @see Antigen#Antigen(Locus, List) */
  public HLAType(HLALocus l, List<Integer> p) {
    super(l, p);
  }

  /**
   * Note: this method is similar to {@link AntigenDictionary#lookup(HLAType)}, with two exceptions:
   *
   * <ul>
   *   <li>In the case of multiple {@link SeroType} mappings, only the first will be returned
   *   <li>If there is no explicit mapping for this type, a {@code SeroType} will be created using
   *       the equivalent {@link SeroLocus} and the first value in this type's {@link #spec()}
   * </ul>
   *
   * @return {@link SeroType} equivalent of this antigen
   * @throws IllegalStateException If this type has ambiguous serotype equivalencies
   */
  public SeroType equiv() {
    try {
      Set<SeroType> lookup = AntigenDictionary.lookup(this);
      if (lookup.size() == 1) {
        return lookup.iterator().next();
      } else if (lookup.size() > 1) {
        throw new IllegalStateException(
            "HLA type: " + this + " has multiple serotype equivalencies");
      }
    } catch (IllegalArgumentException e) {
      // No-op
    }

    // If we don't have an explicit mapping of this HLAType, just use the first spec
    return lowResEquiv();
  }

  /**
   * As {@link #equiv()} but will not throw an {@link IllegalStateException}, instead always
   * constructing a naive equivalent from the first spec
   */
  public SeroType equivSafe() {
    try {
      return equiv();
    } catch (IllegalStateException e) {
      // If we don't have an explicit mapping of this HLAType, just use the first spec
      return lowResEquiv();
    }
  }

  /**
   * @return The {@link SeroType} equivalent of this allele without a lookup in antigen equivalences
   *     table.
   */
  public SeroType lowResEquiv() {
    return new SeroType(locus().sero(), spec().get(0));
  }

  @Override
  protected String getLocusDelim() {
    return LOCUS_DELIM;
  }

  @Override
  protected String getSpecDelim() {
    return Antigen.SPEC_DELIM;
  }

  @Override
  protected List<Integer> parse(int[] p) {
    List<Integer> values = Ints.asList(p);

    return values;
  }

  /** @see Antigen#is(String, Pattern) */
  public static boolean is(String text) {
    return Antigen.is(text, TYPE_PATTERN);
  }

  /** @return Set of {@link HLAType}s parsed from the input strings, per {@link #valueOf(String)} */
  public static Set<HLAType> valueOf(String... alleles) {
    Set<HLAType> alleleSet = new LinkedHashSet<>();
    for (String allele : alleles) {
      alleleSet.add(valueOf(allele));
    }
    return alleleSet;
  }

  /** @return A {@link HLAType} representation of the given string */
  public static HLAType valueOf(String typeString) {
    RawType rt = new RawType(typeString, TYPE_PATTERN);
    return new HLAType(rt.locus(), rt.spec());
  }

  /**
   * Will return {@code null} for types not present in the {@link AntigenDictionary}
   *
   * @see #valueOf(String)
   */
  public static HLAType valueOfStrict(String a) {
    HLAType type = valueOf(a);
    try {
      AntigenDictionary.lookup(type);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return type;
  }

  /** @see Antigen#parseTypes(String, java.util.regex.Pattern, Function) */
  public static List<HLAType> parseTypes(String text) {
    return parseTypes(text, HLAType::valueOf);
  }

  /** @see Antigen#parseTypes(String, java.util.regex.Pattern, Function) */
  public static <T extends Antigen<?, T>> List<T> parseTypes(
      String text, Function<String, T> typeFunction) {
    return Antigen.parseTypes(text, LOCI_PATTERN, typeFunction);
  }

  static {
    // Static initializer to create patterns
    // See also SeroType

    LOCI_PATTERN =
        makePattern(
            Arrays.stream(HLALocus.values()).map(HLALocus::name).collect(Collectors.toList()));

    TYPE_PATTERN = Pattern.compile(LOCI_PATTERN.pattern() + SPEC_PATTERN.pattern());
    PARTIAL_PATTERN =
        Pattern.compile(LOCI_PATTERN.pattern() + "(?:" + SPEC_PATTERN.pattern() + ")?");

    /*
     * --- WARNING --- Changing the patterns in a way that affects the number of groups can have
     * terrible/unexpected reprecussions. Currently we do not have a way to guarantee compliance
     * with a particular group count, etc. It would be safer to refactor these patterns into an
     * Object with proper accessors that can be updated to expose the underlying groups.
     */
  }
}
