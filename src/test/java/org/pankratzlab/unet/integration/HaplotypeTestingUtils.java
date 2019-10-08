package org.pankratzlab.unet.integration;

import java.math.BigDecimal;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.BCHaplotypeRow;
import org.pankratzlab.unet.model.DRDQHaplotypeRow;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public class HaplotypeTestingUtils {
  public static void initiateFreqTablesTesting() {
    // get test/resource file path for frequency files
    String bcFilePath =
        XMLSureTyperParserTest.class.getClassLoader().getResource("C_B.xls").getFile();
    String drdqFilePath =
        XMLSureTyperParserTest.class
            .getClassLoader()
            .getResource("DRB3-4-5_DRB1_DQB1.xls")
            .getFile();
    // initialize frequency tables
    HaplotypeFrequencies.doInitialization(bcFilePath, drdqFilePath);
  }

  public static SetMultimap<RaceGroup, BigDecimal> createTestMultimap(double[] freqArray) {
    SetMultimap<RaceGroup, BigDecimal> outputMultimap = HashMultimap.create();
    RaceGroup ethnicitiesArr[] = {
      RaceGroup.CAU,
      RaceGroup.CAU,
      RaceGroup.AFA,
      RaceGroup.AFA,
      RaceGroup.API,
      RaceGroup.API,
      RaceGroup.HIS,
      RaceGroup.HIS,
      RaceGroup.NAM,
      RaceGroup.NAM
    };
    for (int i = 0; i < 10; i++) {
      outputMultimap.put(ethnicitiesArr[i], roundBigDecimal(freqArray[i]));
    }
    return outputMultimap;
  }

  /**
   * @param freq double which needs to be rounded in the same fashion as HaplotypeFrequencies
   * @return BigDecimal rounded in the same fashion as the frequency table output
   */
  public static BigDecimal roundBigDecimal(double freq) {
    return new BigDecimal(freq)
        .setScale(
            HaplotypeFrequencies.UNKNOWN_HAP_SIG_FIGS,
            HaplotypeFrequencies.UNKNOWN_HAP_ROUNDING_MODE)
        .stripTrailingZeros();
  }

  @SuppressWarnings("restriction")
  public static String testBCHaplotypes(
      ValidationModel model, SetMultimap<RaceGroup, BigDecimal> expectedBCMultimap) {
    ValidationTable table = new ValidationTable();
    table.setFirstModel(model);
    SetMultimap<RaceGroup, BigDecimal> receivedBCMultimap = HashMultimap.create();
    // iterate through haplotype frequency data
    for (BCHaplotypeRow b : table.getBCHaplotypeRows()) {
      BigDecimal freq = b.frequencyProperty().getValue().stripTrailingZeros();
      RaceGroup ethnicity = b.ethnicityProperty().getValue();
      receivedBCMultimap.put(ethnicity, freq);
    }
    SetMultimap<RaceGroup, BigDecimal> generatedExpectedDifference =
        Multimaps.filterEntries(
            receivedBCMultimap, e -> !expectedBCMultimap.containsEntry(e.getKey(), e.getValue()));

    SetMultimap<RaceGroup, BigDecimal> expectedGeneratedDifference =
        Multimaps.filterEntries(
            expectedBCMultimap, e -> !receivedBCMultimap.containsEntry(e.getKey(), e.getValue()));
    return "Difference between generated and expected: "
        + generatedExpectedDifference.toString()
        + "Difference between expected and generated: "
        + expectedGeneratedDifference.toString();
  }

  @SuppressWarnings("restriction")
  public static String testDRDQHaplotypes(
      ValidationModel model, SetMultimap<RaceGroup, BigDecimal> expectedDRDQMultimap) {
    ValidationTable table = new ValidationTable();
    table.setFirstModel(model);
    SetMultimap<RaceGroup, BigDecimal> receivedDRDQMultimap = HashMultimap.create();
    // iterate through haplotype frequency data
    for (DRDQHaplotypeRow d : table.getDRDQHaplotypeRows()) {
      BigDecimal freq = d.frequencyProperty().getValue().stripTrailingZeros();
      RaceGroup ethnicity = d.ethnicityProperty().getValue();
      receivedDRDQMultimap.put(ethnicity, freq);
    }
    SetMultimap<RaceGroup, BigDecimal> generatedExpectedDifference =
        Multimaps.filterEntries(
            receivedDRDQMultimap,
            e -> !expectedDRDQMultimap.containsEntry(e.getKey(), e.getValue()));

    SetMultimap<RaceGroup, BigDecimal> expectedGeneratedDifference =
        Multimaps.filterEntries(
            expectedDRDQMultimap,
            e -> !receivedDRDQMultimap.containsEntry(e.getKey(), e.getValue()));
    return "Difference between generated and expected: "
        + generatedExpectedDifference.toString()
        + "Difference between expected and generated: "
        + expectedGeneratedDifference.toString();
  }
}
