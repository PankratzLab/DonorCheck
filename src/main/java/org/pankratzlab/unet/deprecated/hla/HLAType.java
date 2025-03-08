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
import java.util.Arrays;
import java.util.HashSet;
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
   * <p>
   * Group 1 is the {@link HLALocus}, group 2 is the {@link #SPEC_DELIM}ited specification, and group
   * 3 is the parent specification e.g. 2(5) (NB: group 0 is the complete match in the
   * {@link Matcher#group(int)} api)
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
   * <li><b>In the case of multiple {@link SeroType} mappings, only the first will be returned</b>
   * <li>If there is no explicit mapping for this type, a {@code SeroType} will be created using the
   * equivalent {@link SeroLocus} and the first value in this type's {@link #spec()}
   * </ul>
   *
   * @return {@link SeroType} equivalent of this antigen <br />
   *         <br />
   * 
   */
  public SeroType equiv() {
    Set<SeroType> lookup = new HashSet<>();
    try {
      lookup = AntigenDictionary.lookup(this);
    } catch (IllegalArgumentException e) {
      if (this.resolution() < 3) {
        // grow spec with 01s until a mapping (if any) is found)
        HLAType t = this;
        while (t != null && lookup.isEmpty()) {
          t = growSpec(t);
          try {
            lookup = AntigenDictionary.lookup(t);
          } catch (IllegalArgumentException e2) {
            // might not need this
            lookup = new HashSet<>();
          }
        }
      } else if (this.resolution() > 2) {
        // reduce spec by removing any 01s
        HLAType t = this;
        while (t != null && lookup.isEmpty()) {
          t = reduceSpec(t);
          try {
            lookup = AntigenDictionary.lookup(t);
          } catch (IllegalArgumentException e2) {
            // might not need this
            lookup = new HashSet<>();
          }
        }
      }
      // No-op
    }

    if (lookup.size() == 1) {
      return lookup.iterator().next();
    } else if (lookup.size() > 1) {
      return lookup.iterator().next();
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
   *         table.
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

  /**
   * @param equivType Input type to reduce
   * @return The input {@link HLAType} with its tailing "01" field removed, or null if the allele can
   *         not be reduced
   */
  public static HLAType reduceSpec(HLAType equivType) {
    List<Integer> spec = equivType.spec();

    if (spec.size() < 2 || (spec.size() < 4 && spec.get(spec.size() - 1) != 1)) {
      // We can only remove a trailing "01 "specificity, and only if we have 3- or more fields
      return null;
    }

    spec = spec.subList(0, spec.size() - 1);
    return HLAType.modifiedSpec(equivType, spec);
  }

  /**
   * @param equivType Input type to expand
   * @return The input {@link HLAType} with an additional "01" field, or null if the allele can not be
   *         further expanded
   */
  public static HLAType growSpec(HLAType equivType) {
    List<Integer> spec = new ArrayList<>(equivType.spec());

    if (spec.size() >= 4) {
      // We can only expand 2- and 3-field specificities
      return null;
    }

    spec.add(1);

    return HLAType.modifiedSpec(equivType, spec);
  }

  /** Helper method to create an updated HLAType */
  private static HLAType modifiedSpec(HLAType equivType, List<Integer> spec) {
    if (equivType instanceof NullType) {
      return new NullType(equivType.locus(), spec);
    }
    return new HLAType(equivType.locus(), spec);
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
  public static <T extends Antigen<?, T>> List<T> parseTypes(String text, Function<String, T> typeFunction) {
    return Antigen.parseTypes(text, LOCI_PATTERN, typeFunction);
  }

  /**
   * Compares two HLAType objects in a permissive manner, allowing for different resolution levels.
   * Two HLA types are considered permissively equal if they have the same locus and either: 1. They
   * match exactly up to the resolution of the shorter one, or 2. They differ only by trailing "01"
   * fields (value 1)
   *
   * Special case: Single-field alleles (e.g., A*01) are treated as allele groups and will only match
   * other identical single-field alleles.
   *
   * @param allele1 First HLAType to compare
   * @param allele2 Second HLAType to compare
   * @return true if the alleles match under permissive comparison rules, false otherwise
   */
  public static boolean permissiveEquals(HLAType allele1, HLAType allele2) {
    // Null checks
    if (allele1 == null || allele2 == null) {
      return allele1 == allele2;
    }

    // Check for exact equality first (optimization)
    if (allele1.equals(allele2)) {
      return true;
    }

    // Loci must match
    if (!allele1.locus().equals(allele2.locus())) {
      return false;
    }

    List<Integer> spec1 = allele1.spec();
    List<Integer> spec2 = allele2.spec();

    // Special case: Single-field alleles (allele groups) only match exact single-field alleles
    if (spec1.size() == 1 || spec2.size() == 1) {
      return spec1.size() == 1 && spec2.size() == 1 && spec1.get(0).equals(spec2.get(0));
    }

    int minLength = Math.min(spec1.size(), spec2.size());

    // Compare up to the shortest length
    for (int i = 0; i < minLength; i++) {
      if (!spec1.get(i).equals(spec2.get(i))) {
        return false; // Early mismatch
      }
    }

    // Check if remaining fields are only "01", allowing permissiveness
    if (spec1.size() > spec2.size()) {
      for (int i = minLength; i < spec1.size(); i++) {
        if (spec1.get(i) != 1)
          return false;
      }
    } else if (spec2.size() > spec1.size()) {
      for (int i = minLength; i < spec2.size(); i++) {
        if (spec2.get(i) != 1)
          return false;
      }
    }

    return true;
  }

  static {
    // Static initializer to create patterns
    // See also SeroType

    LOCI_PATTERN = makePattern(Arrays.stream(HLALocus.values()).map(HLALocus::name).collect(Collectors.toList()));

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
