package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.Test;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.parser.util.DRAssociations;

public class DRAssociationsTest {

  @Test
  public void DRAssociations_getDRBLocus() {
    // value in the table and DRB3
    assertEquals(HLALocus.valueOf("DRB3"),
        DRAssociations.getDRBLocus(new SeroType("DRB3", 3)).get());
    // value in the table and DRB4
    assertEquals(HLALocus.valueOf("DRB4"),
        DRAssociations.getDRBLocus(new SeroType("DRB4", 07)).get());
    // value in the table and DRB5
    assertEquals(HLALocus.valueOf("DRB5"),
        DRAssociations.getDRBLocus(new SeroType("DRB5", 16)).get());
    // value not in the table
    assertEquals(false, DRAssociations.getDRBLocus(new SeroType("DRB3", 1337)).isPresent());
    // value not even DRB
    assertEquals(false, DRAssociations.getDRBLocus(new SeroType("B", 15)).isPresent());
  }

}
