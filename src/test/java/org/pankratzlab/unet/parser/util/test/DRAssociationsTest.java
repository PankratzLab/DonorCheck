package org.pankratzlab.unet.parser.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.Test;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.parser.util.DRAssociations;

public class DRAssociationsTest {

  @Test
  public void DRAssociations_getDRBLocus() {
    // value in the table and DRB3
    assertEquals(HLALocus.valueOf("DRB3"), DRAssociations.getDRBLocus(new SeroType("DRB3", 3)));
    // value in the table and DRB4
    assertEquals(HLALocus.valueOf("DRB4"), DRAssociations.getDRBLocus(new SeroType("DRB4", 07)));
    // value in the table and DRB5
    assertEquals(HLALocus.valueOf("DRB5"), DRAssociations.getDRBLocus(new SeroType("DRB5", 16)));
    // value not in the table
    assertEquals(null, DRAssociations.getDRBLocus(new SeroType("DRB3", 1337)));
    // value not even DRB
    assertEquals(null, DRAssociations.getDRBLocus(new SeroType("B", 15)));
  }

}
