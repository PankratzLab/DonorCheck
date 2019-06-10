package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.model.Strand;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class HaplotypeUtilsTest {
  @Test
  public void HaplotypeUtils_parseAllelesToStrandMap() {
    // parseAllelesToStrandMap(String specString, String locus, int strandIndex,
    // Multimap<Strand, HLAType> haplotypeMap)
    Multimap<Strand, HLAType> haplotypeMap = HashMultimap.create();
    Multimap<Strand, HLAType> expectedResults = HashMultimap.create();
    haplotypeMap.put(Strand.SECOND, HLAType.valueOf("C*15:02:05G"));
    haplotypeMap.put(Strand.SECOND, HLAType.valueOf("C*15:03:05G"));
    haplotypeMap.put(Strand.SECOND, HLAType.valueOf("C*15:04:05G"));
    expectedResults.put(Strand.SECOND, HLAType.valueOf("C*15:02:05G"));
    expectedResults.put(Strand.SECOND, HLAType.valueOf("C*15:03:05G"));
    expectedResults.put(Strand.SECOND, HLAType.valueOf("C*15:04:05G"));
    // Single value, not a range and does not end with "N"
    HLAType tmp = new HLAType(HLALocus.valueOf("C"), "15:15");
    expectedResults.put(Strand.values()[0], tmp);
    HaplotypeUtils.parseAllelesToStrandMap("15:15", "C", 0, haplotypeMap);
    assertEquals(haplotypeMap, expectedResults);
    // Single value, ends with "N"
    tmp = new NullType(HLALocus.valueOf("C"), "15:15");
    expectedResults.put(Strand.values()[0], tmp);
    HaplotypeUtils.parseAllelesToStrandMap("15:15N", "C", 0, haplotypeMap);
    assertEquals(haplotypeMap, expectedResults);
    // Range of values
    tmp = new HLAType(HLALocus.valueOf("C"), "15:14");
    expectedResults.put(Strand.values()[0], tmp);
    tmp = new HLAType(HLALocus.valueOf("C"), "15:15");
    expectedResults.put(Strand.values()[0], tmp);
    HaplotypeUtils.parseAllelesToStrandMap("15:14-15:15", "C", 0, haplotypeMap);
    assertEquals(haplotypeMap, expectedResults);
  }

}
