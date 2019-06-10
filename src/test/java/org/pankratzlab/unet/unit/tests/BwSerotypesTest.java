package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.Test;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.parser.util.BwSerotypes;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;


public class BwSerotypesTest {

  @Test
  public void BwSerotypes_toString() {
    // test each B antigen group from BwGroup
    assertEquals("Bw4", BwGroup.Bw4.toString());
    assertEquals("Bw6", BwGroup.Bw6.toString());
    assertEquals("No entry", BwGroup.Unknown.toString());
  }

  @Test
  public void BwSerotypes_getBwGroupAntigen() {
    // test each B antigen group from BwGroup
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup("B47"));
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup("B4005"));
    assertEquals(BwGroup.Unknown, BwSerotypes.getBwGroup("Random String"));
  }

  @Test
  public void BwSerotypes_getBwGroupSerotypeAntigen() {
    // test each B antigen group from BwGroup
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup(new SeroType("B", 47)));
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup(new SeroType("B", 4005)));
    assertEquals(BwGroup.Unknown, BwSerotypes.getBwGroup(new SeroType("B", 849)));
  }

  @Test
  public void BwSerotypes_getBwGroupAllele() {
    // test each B antigen group from BwGroup
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup(new HLAType(HLALocus.valueOf("B"), "15:16")));
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup(new HLAType(HLALocus.valueOf("B"), "15:15")));
    assertEquals(BwGroup.Unknown,
        BwSerotypes.getBwGroup(new HLAType(HLALocus.valueOf("B"), "15:849")));

  }
}
