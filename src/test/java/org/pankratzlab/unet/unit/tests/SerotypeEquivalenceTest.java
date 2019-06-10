package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.parser.util.SerotypeEquivalence;

public class SerotypeEquivalenceTest {
  // public static SeroType get(HLAType allele) {
  @Test
  public void getTestTwoFields() {
    // Serotype equivalence is based on two field alleles
    assertEquals(new SeroType("B", 65), SerotypeEquivalence.get(HLAType.valueOf("B*14:02")));
  }

  @Test
  public void getTestOneField() {
    // If only one field it should return null
    assertEquals(null, SerotypeEquivalence.get(HLAType.valueOf("B*14")));
  }

  @Test
  public void getTestMoreThanTwoFields() {
    // If longer it is expected to return the value based on the first two fields.
    assertEquals(new SeroType("B", 65), SerotypeEquivalence.get(HLAType.valueOf("B*14:02:05:01")));
  }
}
