package org.pankratzlab.unet.unit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.hapstats.AlleleGroups;


@RunWith(JUnitPlatform.class)
public class AlleleGroupsTest {
  @Test
  public void AlleleGroups_getGroupAllele() {
    // Mock object e.g.: MyClass mockedClass = Mockito.mock( MyClass.class );
    // getGroupAllele: Located in P group with multiple alleles
    assertEquals(HLAType.valueOf("B*15:02"), AlleleGroups.getGroupAllele("B*15:02:02P"));
    // getGroupAllele: Located in P group alone
    assertEquals(HLAType.valueOf("B*07:259"), AlleleGroups.getGroupAllele("B*07:259P"));
    // getGroupAllele: Located in G group with multiple alleles
    assertEquals(HLAType.valueOf("DQB1*05:02:01"),
        AlleleGroups.getGroupAllele("DQB1*05:02:01:04G"));
    // getGroupAllele: Located in G group alone
    assertEquals(HLAType.valueOf("DPB1*94:01"), AlleleGroups.getGroupAllele("DPB1*94:01G"));
    // getGroupAllele: Located in G group alone suffixed with N
    assertEquals(NullType.valueOf("DPB1*94:01N"), AlleleGroups.getGroupAllele("DPB1*94:01N"));
    // getGroupAllele: Located in P group with multiple alleles suffixed with N
    assertEquals(NullType.valueOf("B*15:02:02N"), AlleleGroups.getGroupAllele("B*15:02:02N"));
    // getGroupAllele: Located in neither group and suffixed with N
    assertEquals(NullType.valueOf("B*1321:02:02N"), AlleleGroups.getGroupAllele("B*1321:02:02N"));
  }

  @Test
  public void AlleleGroups_getGGroup() {
    // getGGroup: Located in G group with multiple alleles
    assertEquals(HLAType.valueOf("C*15:02:02"),
        AlleleGroups.getGGroup(HLAType.valueOf("C*15:02:02:02G")));
    // getGGroup: Located in G group alone
    assertEquals(HLAType.valueOf("C*15:02:05"),
        AlleleGroups.getGGroup(HLAType.valueOf("C*15:02:05G")));
  }

  @Test
  public void AlleleGroups_getPGroup() {
    // getPGroup: Located in P Group alone
    assertEquals(HLAType.valueOf("A*24:24"), AlleleGroups.getPGroup(HLAType.valueOf("A*24:24P")));
    // getPGroup: Located in P Group with multiple alleles
    assertEquals(HLAType.valueOf("A*24:03"),
        AlleleGroups.getPGroup(HLAType.valueOf("A*24:03:03P")));
  }


}
