package org.pankratzlab.unet.hapstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pankratzlab.unet.deprecated.hla.HLAType;

public class HaplotypeTest {
  private static final List<HLAType> listOfAlleles =
      new ArrayList<>(Arrays.asList(HLAType.valueOf("A*24:03:03"), HLAType.valueOf("A*01:02"),
          HLAType.valueOf("B*15:63")));
  private static final List<HLAType> listOfAlleles_2 =
      new ArrayList<>(Arrays.asList(HLAType.valueOf("A*24:03:01"), HLAType.valueOf("A*01:02:01"),
          HLAType.valueOf("B*15:62")));
  private static final Haplotype haplotypeFull = new Haplotype(listOfAlleles);
  private static final Haplotype haplotypeFull_2 = new Haplotype(listOfAlleles_2);

  @Test
  public void Haplotype_toShortString() {
    // toShortString: converts haplotype of alleles into a string with +'s to separate alleles
    assertEquals("A*01:02 + A*24:03:03 + B*15:63", haplotypeFull.toShortString());
    assertEquals("A*01:02:01 + A*24:03:01 + B*15:62", haplotypeFull_2.toShortString());
  }

  @Test
  public void Haplotype_hashCode() {
    // hashCode: Converts haplotype of alleles to hashCode number
    // Should repeatedly give the same integer for the same haplotype.
    int tmp = haplotypeFull.hashCode();
    assertEquals(tmp, haplotypeFull.hashCode());
    tmp = haplotypeFull_2.hashCode();
    assertEquals(tmp, haplotypeFull_2.hashCode());
  }

  @Test
  public void Haplotype_equals() {
    // equals
    assertEquals(true, haplotypeFull.equals(haplotypeFull));
    assertEquals(false, haplotypeFull.equals(haplotypeFull_2));
  }

  @Test
  public void Haplotype_toString() {
    // toString
    assertEquals("Haplotype [types=[A*01:02, A*24:03:03, B*15:63]]", haplotypeFull.toString());
    assertEquals("Haplotype [types=[A*01:02:01, A*24:03:01, B*15:62]]", haplotypeFull_2.toString());
  }

  @Test
  public void Haplotype_compareTo() {
    // compareTo
    assertEquals(-1, haplotypeFull.compareTo(haplotypeFull_2));
  }
}
