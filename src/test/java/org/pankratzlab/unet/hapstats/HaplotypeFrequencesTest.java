package org.pankratzlab.unet.hapstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pankratzlab.unet.deprecated.hla.HLAType;

public class HaplotypeFrequencesTest {

  private static final List<HLAType> listOfAlleles =
      new ArrayList<>(Arrays.asList(HLAType.valueOf("A*24:03:01"), HLAType.valueOf("A*01:02:01"),
          HLAType.valueOf("B*15:62")));
  private static final Haplotype haplotypeFull = new Haplotype(listOfAlleles);

  @Test
  public void HaplotypeFrequences_successfullyInitialized() {
    // successfullyInitialized should return false as it has not been initialized
    assertEquals(false, HaplotypeFrequencies.successfullyInitialized());
  }

  @Test
  public void HaplotypeFrequences_doInitialization() {
    // doInitialization should also return false as it as insufficient data to initiate
    assertEquals(false, HaplotypeFrequencies.doInitialization());
  }

  @Test
  public void HaplotypeFrequences_getMissingTableMessage() {
    // should return null as no tables have been searched for since the initialization has not
    // happened
    assertEquals(null, HaplotypeFrequencies.getMissingTableMessage());
    HaplotypeFrequencies.doInitialization();
    // getMissingableMessage should return all tables
    assertEquals(
        "The following frequency table(s) are missing. Corresponding haplotype frequencies will not be used.\nCB\nDRB345-DRB1-DQB1\n\nYou can edit the table paths via the 'Haplotypes' menu.",
        HaplotypeFrequencies.getMissingTableMessage());
  }

  @Test
  public void HaplotypeFrequences_getFrequency() {
    assertEquals(0.0, HaplotypeFrequencies.getFrequency(RaceGroup.CAU,
        HLAType.valueOf("C*15:02:02:02G"), HLAType.valueOf("C*15:01G")));
    assertEquals(0.0, HaplotypeFrequencies.getFrequency(RaceGroup.AFA, haplotypeFull));
  }
}
