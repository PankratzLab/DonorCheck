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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.pankratzlab.unet.deprecated.util.SerializeUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

/** Persistent map of {@link HLAType}s to equivalent {@link SeroType}s. Use when converting. */
public final class AntigenDictionary implements Serializable {
  private static final long serialVersionUID = 12L;

  public static final String REL_DNA_SER_PROP = "rel.dna.ser.file";

  public static final String SERIALIZED_MAP = Info.DONOR_CHECK_HOME + ".hla/map.ser";
  public static final String MASTER_MAP_RECORDS = "rel_dna_ser.txt";
  private static final String COMMENT = "#";
  private static final String COL_DELIM = ";";
  private static final String SPEC_DELIM = ":";
  private static final String TYPE_DELIM = "/";
  private static final String UNKNOWN_TYPE = "?";
  private static final String NULL_TYPE = "0";

  /**
   * This is a paper-thin wrapper around the HLA and SeroType dictionaries, to unify them in a
   * single serializable object.
   */
  private static AntigenDictionary map;

  // Dictionary instances
  private final SetMultimap<HLAType, SeroType> hlaDict;
  private final SetMultimap<SeroType, HLAType> seroDict;
  private final Set<HLAType> validTypes;

  private AntigenDictionary(SetMultimap<HLAType, SeroType> hla, SetMultimap<SeroType, HLAType> sero,
      Set<HLAType> valid) {
    hlaDict = hla;
    seroDict = sero;
    validTypes = valid;
  }

  /**
   * @param hla Type to query
   * @return The set of {@link SeroType}s that are implied by the query type
   * @throws IllegalArgumentException If the type is not known to this dictionary
   */
  public static Set<SeroType> lookup(HLAType hla) {
    init();
    return get(map.hlaDict, hla);
  }

  /**
   * @param sero Type to query
   * @return All known {@link HLAType}s that map to the query type
   * @throws IllegalArgumentException If the type is not known to this dictionary
   */
  public static Set<HLAType> lookup(SeroType sero) {
    init();
    return get(map.seroDict, sero);
  }

  /** @return All {@link HLAType}s known to this map */
  public static Set<HLAType> validHLA() {
    init();
    return map.validTypes;
  }

  /** @return All {@link SeroType}s known to this map */
  public static Set<SeroType> validSero() {
    init();
    return map.seroDict.keySet();
  }

  /** @return true if the type is in the {@link #validHLA()} set */
  public static boolean isValid(HLAType type) {
    return validHLA().contains(type);
  }

  /** @return true if the type is in the {@link #validSero()} set */
  public static boolean isValid(SeroType type) {
    return validSero().contains(type);
  }

  /**
   * @param unknownType Generic {@link Antigen} to look up
   * @return true if the type meets {@link #isValid} criteria.
   */
  public static <L extends Locus<L>, T extends Antigen<L, T>> boolean isValid(T unknownType) {
    if (unknownType instanceof HLAType) {
      return isValid((HLAType) unknownType);
    }
    if (unknownType instanceof SeroType) {
      return isValid((SeroType) unknownType);
    }
    return false;
  }

  /**
   * Helper method to do a dictionary lookup
   *
   * @throws IllegalArgumentException If the key is not present in the given map
   */
  private static <K, V> Set<V> get(SetMultimap<K, V> dictionary, K key) {
    if (!dictionary.containsKey(key)) {
      throw new IllegalArgumentException("Unrecognized type: " + key);
    }
    return dictionary.get(key);
  }

  /** Helper method to double lock the {@link #loadDictionaries()} method */
  private static void init() {
    if (map == null) {
      loadDictionaries();
    }
  }

  /** Build the dictionaries. Synchronized to ensured they are initialized only once */
  private static synchronized void loadDictionaries() {
    String filePath = DonorCheckProperties.get().getProperty(REL_DNA_SER_PROP);
    if (map == null) {
      if (!Strings.isNullOrEmpty(filePath) && (new File(filePath)).exists()) {
        parseDictionaries(() -> new FileReader(filePath));
      } else {
        parseDictionaries(() -> new InputStreamReader(
            AntigenDictionary.class.getClassLoader().getResourceAsStream(MASTER_MAP_RECORDS)));
      }
    }
  }

  /**
   * If a cached map can be loaded, do so. If not, we parse the source file
   * 
   */
  private static void parseDictionaries(Callable<Reader> readerSupplier) {
    if (readCachedMap()) {
      return;
    }
    ImmutableSetMultimap.Builder<HLAType, SeroType> hlaBuilder = ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<SeroType, HLAType> seroBuilder = ImmutableSetMultimap.builder();

    // NB: what's considered a valid HLA type diverges from the HLA map keyset and thus must be
    // tracked separately
    Builder<HLAType> validHLATypes = ImmutableSet.builder();
    try (BufferedReader reader = new BufferedReader(readerSupplier.call())) {
      while (reader.ready()) {
        // Read one mapping at a time
        String line = reader.readLine();
        // Skip lines without mappings
        if (line.isEmpty() || line.startsWith(COMMENT)) {
          continue;
        }

        // Each row is a set of delimited columns representing an allele mapping
        // Described in:
        // https://github.com/ANHIG/IMGTHLA/blob/8f540a9fb67f53c1d6f43f7e9250b10c9da4e8f7/wmda/README.md
        // C0 - Locus
        // C1 - Specificities (may have trailing letter)
        // C2 - Unambiguous serotype
        // C3 - Possible serotype
        // C4 - Assumed serotype
        // C5 - Expert-assigned serotype
        // 0 - null
        // ? - unknown
        // / - divides multiple values in a single column

        // Split the input line to columns
        String[] columns = line.split(COL_DELIM);

        // Parse out the serological specificities for this mapping
        // Since the columns are ordered by specificity, we use the first column with valid entries
        // Set<String> seroSpecs = new LinkedHashSet<>();
        List<String> seroSpecs = new ArrayList<>();
        if (!columns[2].isEmpty()) {
          seroSpecs.add(columns[2]);
        }
        // for (int i = 2; i <= 5 && i < columns.length; i++) {
        // String types = columns[i];
        // // Each HLA type may map to multiple serotypes
        // for (String t : types.split(TYPE_DELIM)) {
        // if (!t.isEmpty() && !seroSpecs.contains(t)) {
        // seroSpecs.add(t);
        // }
        // }
        // }

        // Skip null types
        if (seroSpecs.stream().anyMatch(NULL_TYPE::equals)) {
          continue;
        }

        // Get the HLA locus
        HLALocus l = null;
        try {
          l = HLALocus.valueOf(columns[0].substring(0, columns[0].length() - 1));
        } catch (IllegalArgumentException e) {
          // Unsupported locus. Skip for now
          continue;
        }

        // Parse the HLA specificity
        List<Integer> spec = new ArrayList<>();
        String[] specValues = columns[1].split(SPEC_DELIM);
        for (int i = 0; i < specValues.length; i++) {
          specValues[i] = specValues[i].trim().replaceAll("[^0-9]", "");
          spec.add(Integer.parseInt(specValues[i]));
        }

        HLAType hlaType = new HLAType(l, spec);

        SeroLocus sl = l.sero();
        for (String t : seroSpecs) {
          // Convert unknown types to the first spec value
          if (UNKNOWN_TYPE.equals(t)) {
            t = specValues[0];
          }
          SeroType seroType = new SeroType(sl, t);
          hlaBuilder.put(hlaType, seroType);

          // Only map from sero > hla if we have 2 or more specificities
          if (spec.size() > 1) {
            validHLATypes.add(hlaType);
            seroBuilder.put(seroType, hlaType);
          }
        }
        // }
      }

      // Build the singleton map and write it to disk
      AntigenDictionary typeMap =
          new AntigenDictionary(hlaBuilder.build(), seroBuilder.build(), validHLATypes.build());
      SerializeUtils.write(typeMap, SERIALIZED_MAP);
      map = typeMap;

      // List<HLAType> multi = map.hlaDict.keySet().stream()
      // .filter(ht -> map.hlaDict.get(ht).size() > 1).collect(Collectors.toList());
      // System.out.println("Found " + multi.size() + " HLAType(s) with multiple SeroType
      // mappings:");
      // for (HLAType t : multi) {
      // System.out.println("\t" + t.toString() + " --> "
      // + map.hlaDict.get(t).stream().map(s -> s.toString()).collect(Collectors.joining(", ")));
      // }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Helper method to load a cached {@link AntigenDictionary}
   *
   * @return true iff a cached map is found and read successfully.
   */
  private static boolean readCachedMap() {
    map = SerializeUtils.read(SERIALIZED_MAP, AntigenDictionary.class);
    return map != null;
  }

  public static String getVersion() {
    String filePath = DonorCheckProperties.get().getProperty(REL_DNA_SER_PROP);
    if (filePath == null || Strings.isNullOrEmpty(filePath) || !(new File(filePath)).exists()) {
      return getBundledVersion() + " (bundled)";
    }
    return getVersion(filePath);
  }

  public static String getVersion(String file) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return findVersion(reader);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getBundledVersion() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        AntigenDictionary.class.getClassLoader().getResourceAsStream(MASTER_MAP_RECORDS)))) {
      return findVersion(reader);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  private static String findVersion(BufferedReader reader) throws IOException {
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("# date: ")) {
        return line.split(": ")[1];
      }
    }
    return null;
  }

  public static void clearCache() {
    try {
      if (new File(SERIALIZED_MAP).exists()) {
        Files.delete(Paths.get(SERIALIZED_MAP));
      }
      map = null;
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to delete serotype lookup cache file (" + SERIALIZED_MAP + ")", e);
    }
  }

}
