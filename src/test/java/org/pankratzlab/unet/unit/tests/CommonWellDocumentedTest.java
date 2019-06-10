package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;

public class CommonWellDocumentedTest {
  @Test
  public void CommonWellDocumented_getStatus() {
    // getStatus: an unknown allele
    assertEquals(
        CommonWellDocumented.Status.UNKNOWN,
        CommonWellDocumented.getStatus(HLAType.valueOf("A*24:03:03")));
    // getStatus: a common allele
    assertEquals(
        CommonWellDocumented.Status.COMMON,
        CommonWellDocumented.getStatus(HLAType.valueOf("A*01:02")));
    // getStatus: a well documented allele
    assertEquals(
        CommonWellDocumented.Status.WELL_DOCUMENTED,
        CommonWellDocumented.getStatus(HLAType.valueOf("B*15:63")));
  }

  @Test
  public void CommonWellDocumented_valueOf() {
    // cwdScore: one of each type
    List<HLAType> x =
        new ArrayList<>(
            Arrays.asList(
                HLAType.valueOf("A*24:03:03"),
                HLAType.valueOf("A*01:02"),
                HLAType.valueOf("B*15:63")));
    assertEquals(1.5, CommonWellDocumented.cwdScore(x));
    // cwdScore: two commons and a well documented
    List<HLAType> y =
        new ArrayList<>(
            Arrays.asList(
                HLAType.valueOf("DRB4*03:01N"),
                HLAType.valueOf("B*46:01:01"),
                HLAType.valueOf("B*15:71")));
    assertEquals(2.0, CommonWellDocumented.cwdScore(y));
    // cwdScore: three unknowns
    List<HLAType> z =
        new ArrayList<>(
            Arrays.asList(
                HLAType.valueOf("A*29:13"),
                HLAType.valueOf("B*18:27"),
                HLAType.valueOf("DRB1*16:11")));
    assertEquals(0.0, CommonWellDocumented.cwdScore(z));
  }
}
