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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

/** Abstract superclass for general antigen information. Allows comparison and sorting. */
public abstract class Antigen<L extends Locus<L>, A extends Antigen<L, A>>
    implements Serializable, Comparable<A> {

  private static final long serialVersionUID = 5L;

  /** Specificity delimiter */
  public static final String SPEC_DELIM = ":";

  /** Delimiter between locus and specificity */
  public static final String LOCUS_DELIM = "*";

  /** {@link Pattern} for matching antigen specificities. */
  public static final Pattern SPEC_PATTERN =
      Pattern.compile("\\" + LOCUS_DELIM + "?([0-9]+[:[0-9]+]*)(?:\\(([0-9]+[:[0-9]+]*)\\))*");

  public static final int LATEST_REVISION = 1;

  private int revision = LATEST_REVISION;

  private final L locus;
  private final int[] specificity;
  private transient String stringVal;
  private transient int hash;
  private transient List<Integer> specList = null;

  /**
   * Construct an {@link Antigen} with the given {@link Locus} and specificities(s)
   *
   * @param l {@link Locus} for this {@link Antigen}
   * @param p Array of integer specificities
   */
  public Antigen(L l, String... p) {
    Objects.requireNonNull(l);
    locus = l;
    specificity = parse(p).stream().mapToInt(i -> i).toArray();
  }

  /**
   * Construct an {@link Antigen} with the given {@link Locus} and specificities(s)
   *
   * @param l {@link Locus} for this {@link Antigen}
   * @param p Array of integer specificities
   */
  public Antigen(L l, int... p) {
    Objects.requireNonNull(l);
    locus = l;
    specificity = parse(p).stream().mapToInt(i -> i).toArray();
  }

  /**
   * Construct an {@link Antigen} with the given {@link Locus} and specificities(s)
   *
   * @param l {@link Locus} for this {@link Antigen}
   * @param p Specificities
   */
  public Antigen(L l, List<Integer> p) {
    Objects.requireNonNull(l);
    locus = l;

    specificity = p.stream().mapToInt(i -> i).toArray();
  }

  /** @return The {@link Locus} for this {@link Antigen} */
  public L locus() {
    return locus;
  }

  /** @return The number of molecular positions known for this {@link Antigen}'s specificity */
  public int resolution() {
    return specificity.length;
  }

  /** @return Specificity of this {@link Antigen} */
  public List<Integer> spec() {
    if (specList == null) {
      specList = ImmutableList.copyOf(Ints.asList(specificity));
    }
    return specList;
  }

  /** @return The string representation of this specification */
  public String specString() {
    if (spec().size() == 1) {
      return String.format("%d", spec().get(0));
    }
    return spec()
        .stream()
        .map(i -> String.format("%02d", i))
        .collect(Collectors.joining(getSpecDelim()));
  }

  /**
   * @return Distance between two {@link Antigen}s. Similar alleles will have smaller values.. e.g.
   *     A*21:03 is closer to A*21:04 than to A*20:03
   */
  public int distance(A a) {
    // Heavily penalize alleles on other loci
    if (!locus.equals(a.locus())) {
      return Integer.MAX_VALUE;
    }

    int d = 0;

    final int commonRes = Math.min(resolution(), a.resolution());
    for (int i = 0; i < commonRes; i++) {
      // Weight the earlier positions
      final int weight = (int) Math.pow(10, commonRes - i);
      d += weight * Math.abs(Integer.compare(specificity[i], a.spec().get(i)));
    }

    return d;
  }

  /**
   * @param other Another antigen to compare loci
   * @return true if this antigen's locus is the same as the other's
   */
  public boolean onLocus(Antigen<L, A> other) {
    return locus.equals(other.locus());
  }

  @Override
  public int compareTo(A a) {
    // Check loci first
    int c = locus.compareTo(a.locus());

    // Iterate over each common resolution item
    for (int i = 0; c == 0 && i < Math.min(resolution(), a.resolution()); i++) {
      c = Integer.compare(specificity[i], a.spec().get(i));
    }

    // if those are still the same, put the lower resolution item first
    if (c == 0) {
      c = Integer.compare(resolution(), a.resolution());
    }

    return c;
  }

  @Override
  public int hashCode() {
    int code = hash;
    if (code == 0) {
      final int prime = 31;
      code = 1;
      code = prime * code + ((locus == null) ? 0 : locus.hashCode());
      code = prime * code + Arrays.hashCode(specificity);
      hash = code;
    }
    return code;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Antigen<?, ?> other = (Antigen<?, ?>) obj;
    if (locus != other.locus()) return false;
    if (!Arrays.equals(specificity, other.specificity)) return false;
    return true;
  }

  @Override
  public String toString() {
    if (stringVal == null) {
      stringVal = toString(locus.name());
    }
    return stringVal;
  }

  /** Helper method for building the molecular position string */
  private String toString(String locusString) {

    return locusString + getLocusDelim() + specString();
  }

  /** @see #parse(int[]) */
  private List<Integer> parse(String[] p) {
    if (p.length == 1) {
      String spec = p[0];

      int[] iVal = new int[1];
      Matcher matcher = SPEC_PATTERN.matcher(spec);
      matcher.find();
      // Extracts value from parents if they were present
      String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(1);

      if (val.contains(SPEC_DELIM)) {
        // Was passed XX:YY
        return parse(val.split(SPEC_DELIM));
      }
      // "0105" should be "01:05"
      if (val.length() > 2 && val.charAt(0) == '0') {
        return parse(new String[] {spec.substring(0, 2), spec.substring(2)});
      }
      iVal[0] = Integer.parseInt(val);
      return parse(iVal);
    }
    // Arrays of length > 1 are parsed "as-is"
    return parse(Arrays.stream(p).mapToInt(s -> Integer.parseInt(s.trim())).toArray());
  }

  /**
   * Convert the given list of integers to positions. For example, given the position 4013, the
   * actual positions are [40, 13].
   *
   * @param pos Array of positions, some which may be conflating two+ positions
   * @return List of molecular positions
   */
  protected abstract List<Integer> parse(int[] pos);

  /** @return The (optional) delimiter between locus and spec */
  protected String getLocusDelim() {
    return "";
  }

  /** @return The (optional) delimiter between positions in the spec */
  protected String getSpecDelim() {
    return "";
  }

  /** @return true iff this text can be parsed according to the given type pattern */
  public static boolean is(String text, Pattern typePattern) {
    if (text == null || text.isEmpty()) {
      return false;
    }

    return typePattern.matcher(sanitize(text)).find();
  }

  /**
   * @param antigens Collection of antigens
   * @return Sorted string of antigen strings
   */
  public static <L extends Locus<L>, T extends Antigen<L, T>> String toString(
      Collection<T> antigens) {
    return antigens
        .stream()
        .sorted()
        .map(Antigen::toString)
        .collect(Collectors.joining(", "))
        .toString();
  }

  /**
   * @param antigenString Input which may or may not complying to expected pattern formats.
   * @return A well-behaved, upcased,standardized representation of the input string, appropriate
   *     for locus or antigen parsing.
   */
  public static String sanitize(String antigenString) {
    antigenString = antigenString.trim();
    // Some Loci have a "w" after the locus name. But Bw5 is still a B locus allele, and there isn't
    // just a "B*5". So we'll handle this by removing the "w" and replacing it with the standard "*"
    // (if it doesn't already have one)
    if (antigenString.contains("w")) {
      antigenString = antigenString.replaceAll("w", "*");
      // if we created a **, condense!
      antigenString.replace("\\*\\*", "*");
    }

    // Upcase for enum matching
    return antigenString.toUpperCase();
  }

  /**
   * Helper method to parse a sequence of {@link Antigen} types from a shorthand notation string
   *
   * @param text String representing zero or more antigen types according to the given {@code
   *     typeParser}. Shorthand notation omitting redundant loci is accepted, e.g. {@code A21:03,15
   *     B*2} would translate to {@code A*21:03, A*15, B*02}
   * @param lociPattern A {@link Pattern} for parsing out the {@link Locus}
   * @param typeParser Method for converting individual locus + spec string combinations to a
   *     particular type
   * @return A list of the <b>unique</b> and <b>non-null</b> antigens parsed from the text, in the
   *     order of their first appearance
   */
  public static <T extends Antigen<?, T>> List<T> parseTypes(
      String text, Pattern lociPattern, Function<String, T> typeParser) {
    if (text == null || text.isEmpty()) {
      return Collections.emptyList();
    }
    // LinkedHashSet will get us unique values in order of discovery
    Set<T> found = new LinkedHashSet<>();

    // List of the start positions for each locus, in order within the string
    List<Integer> locusStarts = new ArrayList<>();
    // Map of start positions to loci strings
    Map<Integer, String> locusMap = new HashMap<>();

    // First we parse the boundaries of each Locus in the string
    Matcher lociMatcher = lociPattern.matcher(text);
    while (lociMatcher.find()) {
      // If we find a locus, it will be used as the "context" from this location to
      // either the end
      // of the string, or the next locus
      String locus = lociMatcher.group();
      int breakpoint = lociMatcher.start();
      locusMap.put(breakpoint, locus);
      locusStarts.add(breakpoint);
    }

    // Now we loop through the loci we identified, identifying the substring ranges
    // between loci and parsing those ranges for specs.
    for (int locusIndex = 0; locusIndex < locusStarts.size(); locusIndex++) {
      int locusStartPos = locusStarts.get(locusIndex);
      String locus = locusMap.get(locusStartPos);
      // The current locus has a range to the start pos of the next locus, or the end of the string
      // if this is the last locus.
      int rangeEnd =
          locusIndex + 1 == locusStarts.size() ? text.length() : locusStarts.get(locusIndex + 1);
      // The search space substring is where we look for specs
      String searchSpace = text.substring(locusStartPos + locus.length(), rangeEnd);
      Matcher specMatcher = SPEC_PATTERN.matcher(searchSpace);
      // Now we find all our specs in the search space for the current locus
      while (specMatcher.find()) {
        String spec = specMatcher.group();
        // Combine the spec with the current locus and parse a type
        T parsed = typeParser.apply(locus + spec);
        if (parsed != null) {
          found.add(parsed);
        }
      }
    }

    return new ArrayList<>(found);
  }

  /**
   * Helper method to turn a list of strings into an OR'd {@link Pattern}, with longer strings
   * taking priority in the match.
   */
  public static Pattern makePattern(List<String> strings) {
    // Remove redundant entries and then sort by length first and then normal string ordering,
    // with the intention that longer strings will take precedence.
    TreeSet<String> sortedEntries =
        new TreeSet<>(
            new Comparator<String>() {
              @Override
              public int compare(String o1, String o2) {
                int c = Integer.compare(o2.length(), o1.length());
                if (c == 0) {
                  c = o1.compareTo(o2);
                }
                return c;
              }
            });

    StringJoiner patternJoiner = new StringJoiner("|", "(?i)(", ")");

    // Sort the strings
    for (String value : strings) {
      sortedEntries.add(value);
    }

    // Add the strings to the joiner
    for (String entry : sortedEntries) {
      patternJoiner.add(entry);
    }

    return Pattern.compile(patternJoiner.toString());
  }

  /** Abstract representation of an {@link Antigen} (unknown type) */
  public static class RawType {
    private final String locus;
    private final String specificity;

    /**
     * @param antigen String to parse to a type
     * @param antigenPattern {@link Pattern} to use to verify the input text is valid
     */
    public RawType(String antigen, Pattern antigenPattern) {
      antigen = sanitize(antigen);
      Matcher matcher = antigenPattern.matcher(antigen);
      if (!matcher.find()) {
        throw new IllegalArgumentException("Not a supported type string: " + antigen);
      }

      locus = matcher.group(1);

      // Use the more specific spec if available
      String spec = matcher.group(3);
      if (spec == null) {
        spec = matcher.group(2);
      }
      specificity = spec;
    }

    /** @return String representation of this {@link Antigen}'s {@link Locus} */
    public String locus() {
      return locus;
    }

    /** @return String representation for this {@link Antigen}'s specificity */
    public String spec() {
      return specificity;
    }
  }
}
