package org.pankratzlab.unet;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroLocus;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.parser.util.BwSerotypes;
import org.pankratzlab.unet.parser.util.BwSerotypes.BwGroup;

/** Unit tests for {@link BwSerotypes} */
public class BwSerotypeTest {

  @Test
  public void testValidBw() {
    // Test valid Bw4's
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup("B47"));
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup(new SeroType(SeroLocus.B, "B37")));
    // This allele is in the table
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup(new HLAType(HLALocus.B, "15:17")));
    // This allele is not but should fall through to serotype
    assertEquals(BwGroup.Bw4, BwSerotypes.getBwGroup(new HLAType(HLALocus.B, "15:17")));

    // Test valid Bw6's
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup("B46"));
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup(new SeroType(SeroLocus.B, "B54")));
    // This allele is in the table
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup(new HLAType(HLALocus.B, "27:08")));
    // This allele is not but should fall through to serotype
    assertEquals(BwGroup.Bw6, BwSerotypes.getBwGroup(new HLAType(HLALocus.B, "40:99")));
  }

  @Test
  public void testMissingBw() {
    assertEquals(BwGroup.Unknown, BwSerotypes.getBwGroup("B90"));
    assertEquals(BwGroup.Unknown, BwSerotypes.getBwGroup(new SeroType(SeroLocus.B, "B66")));
    // This allele is in the table
    assertEquals(BwGroup.Unknown, BwSerotypes.getBwGroup(new HLAType(HLALocus.B, "12:01")));
  }
}
