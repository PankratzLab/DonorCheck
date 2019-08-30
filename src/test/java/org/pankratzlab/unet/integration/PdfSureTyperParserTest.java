package org.pankratzlab.unet.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.parser.util.PdfSureTyperParser;

public class PdfSureTyperParserTest {
  // test files located in test/resources
  private static final String Test_File1 = "UnitTestPDFTyper.pdf";
  private static final String Test_File2 = "UnitTestPDFTyper_2.pdf";
  private static final String Test_File3 = "UnitTestPDFTyper_3.pdf";
  private static final String Test_File4 = "UnitTestPDFTyper_4.pdf";
  private static final String Test_File5 = "UnitTestPDFTyper_5.pdf";
  private static final String Test_File6 = "UnitTestPDFTyper_6.pdf";
  private static final String Test_File7 = "UnitTestPDFTyper_7.pdf";
  private static final String Test_File8 = "UnitTestPDFTyper_8.pdf";
  private static final String Test_File9 = "UnitTestPDFTyper_9.pdf";
  private static final String Test_File10 = "UnitTestPDFTyper_X.pdf";

  // create parameters to be used as CSV source for checking Donor ID parsing
  @DisplayName("Donor ID parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    Test_File1 + ", AFJQ146",
    Test_File2 + ", AFLE337",
    Test_File3 + ", AFK3449",
    Test_File4 + ", AFK3166",
    Test_File5 + ", AGEZ412",
    Test_File6 + ", AGD4110",
    Test_File7 + ", AGDX065",
    Test_File8 + ", AGEA128",
    Test_File9 + ", SABR",
    Test_File10 + ", AGID359"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param donorId expected Donor ID for each respective donor file
   */
  public void twoDRB3_PdfSureTyperTest_getDonorId(String fileName, String donorId) {
    assertEquals(donorId, createModel(fileName).getDonorId());
  }

  // create the parameters to be used as a method source for the allele parser to run through
  private static Stream<Arguments> testGetABC() {
    return Stream.of(
        Arguments.of(
            Test_File1,
            new SeroType("A", 30),
            new SeroType("A", 36),
            new SeroType("B", 07),
            new SeroType("B", 49),
            new SeroType("C", 07),
            null),
        Arguments.of(
            Test_File2,
            new SeroType("A", 2),
            new SeroType("A", 33),
            new SeroType("B", 45),
            new SeroType("B", 53),
            new SeroType("C", 04),
            new SeroType("C", 16)),
        Arguments.of(
            Test_File3,
            new SeroType("A", 2),
            new SeroType("A", 3),
            new SeroType("B", 57),
            new SeroType("B", 65),
            new SeroType("C", 06),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File4,
            new SeroType("A", 3),
            new SeroType("A", 24),
            new SeroType("B", 27),
            new SeroType("B", 35),
            new SeroType("C", 02),
            new SeroType("C", 04)),
        Arguments.of(
            Test_File5,
            new SeroType("A", 2),
            new SeroType("A", 24),
            new SeroType("B", 18),
            new SeroType("B", 35),
            new SeroType("C", 04),
            new SeroType("C", 07)),
        Arguments.of(
            Test_File6,
            new SeroType("A", 02),
            new SeroType("A", 29),
            new SeroType("B", 57),
            new SeroType("B", 58),
            new SeroType("C", 06),
            new SeroType("C", 07)),
        Arguments.of(
            Test_File7,
            new SeroType("A", 02),
            new SeroType("A", 24),
            new SeroType("B", 62),
            new SeroType("B", 72),
            new SeroType("C", 02),
            new SeroType("C", 9)),
        Arguments.of(
            Test_File8,
            new SeroType("A", 02),
            new SeroType("A", 68),
            new SeroType("B", 44),
            new SeroType("B", 65),
            new SeroType("C", 5),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File9,
            new SeroType("A", 23),
            new SeroType("A", 33),
            new SeroType("B", 45),
            new SeroType("B", 65),
            new SeroType("C", 6),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File10,
            new SeroType("A", 1),
            new SeroType("A", 31),
            new SeroType("B", 8),
            new SeroType("B", 60),
            new SeroType("C", 7),
            new SeroType("C", 10)));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param A1 first expected laboratory assigned serotype for HLA-A
   * @param A2 second expected laboratory assigned serotype for HLA-A
   * @param B1 first expected laboratory assigned serotype for HLA-B
   * @param B2 second expected laboratory assigned serotype for HLA-B
   * @param C1 first expected laboratory assigned serotype for HLA-C
   * @param C2 second expected laboratory assigned serotype for HLA-C
   */
  @DisplayName("Allele A, B and C parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testGetABC")
  public void PdfSureTyperTest_getABC(
      String fileName,
      SeroType A1,
      SeroType A2,
      SeroType B1,
      SeroType B2,
      SeroType C1,
      SeroType C2) {
    ValidationModel model = createModel(fileName);
    assertEquals(A1, model.getA1());
    assertEquals(A2, model.getA2());
    assertEquals(B1, model.getB1());
    assertEquals(B2, model.getB2());
    assertEquals(C1, model.getC1());
    assertEquals(C2, model.getC2());
  }

  // create parameters to be used as CSV source for checking Bw4 and Bw6 parsing
  @DisplayName("Bw4 and Bw6 parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    Test_File1 + ", Positive, Positive",
    Test_File2 + ", Positive, Positive",
    Test_File3 + ", Positive, Positive",
    Test_File4 + ", Negative, Positive",
    Test_File5 + ", Negative, Positive",
    Test_File6 + ", Positive, Negative",
    Test_File7 + ", Negative, Positive",
    Test_File8 + ", Positive, Positive",
    Test_File9 + ", Negative, Positive",
    Test_File10 + ", Negative, Positive"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param Bw4Result expected Bw4 result(Positive or Negative)
   * @param Bw6Result expected Bw6 result(Positive or Negative)
   */
  public void PdfSureTyperTest_isBw(String fileName, String Bw4Result, String Bw6Result) {
    ValidationModel model = createModel(fileName);
    assertEquals(Bw4Result, model.isBw4());
    assertEquals(Bw6Result, model.isBw6());
  }

  // create parameters to be used as a method source for checking the DRB345 parser
  private static Stream<Arguments> testGetDRB() {
    return Stream.of(
        Arguments.of(Test_File1, null, null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(
            Test_File2, new HLAType("DRB5", 1), null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(Test_File3, null, null, new HLAType("DRB3", 3), null, null, null),
        Arguments.of(
            Test_File4, null, null, null, null, new HLAType("DRB4", 1), new HLAType("DRB4", 1)),
        Arguments.of(Test_File5, null, null, null, null, new HLAType("DRB4", 1), null),
        Arguments.of(Test_File6, null, null, null, null, null, null),
        Arguments.of(
            Test_File7, null, null, new HLAType("DRB3", 1), new HLAType("DRB3", 3), null, null),
        Arguments.of(
            Test_File8, null, null, new HLAType("DRB3", 2), null, new HLAType("DRB4", 1), null),
        Arguments.of(
            Test_File9, null, null, new HLAType("DRB3", 2), null, new HLAType("DRB4", 1), null),
        Arguments.of(
            Test_File10, null, null, new HLAType("DRB3", 2), null, new HLAType("DRB4", 1), null));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param DR51_1 first expected DRB5 results
   * @param DR51_2 second expected DRB5 results
   * @param DR52_1 first expected DRB3 results
   * @param DR52_2 second expected DRB3 results
   * @param DR53_1 first expected DRB4 results
   * @param DR53_2 second expected DRB4 results
   */
  @DisplayName("DRB345 parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testGetDRB")
  public void PdfSureTyperTest_getDRB(
      String fileName,
      HLAType DR51_1,
      HLAType DR51_2,
      HLAType DR52_1,
      HLAType DR52_2,
      HLAType DR53_1,
      HLAType DR53_2) {
    ValidationModel model = createModel(fileName);
    assertEquals(DR51_1, model.getDR51_1());
    assertEquals(DR51_2, model.getDR51_2());
    assertEquals(DR52_1, model.getDR52_1());
    assertEquals(DR52_2, model.getDR52_2());
    assertEquals(DR53_1, model.getDR53_1());
    assertEquals(DR53_2, model.getDR53_2());
  }
  // create the parameters to be used as a method source for the haplotype allele parser to run
  // through
  private static Stream<Arguments> testHaplotype() {
    return Stream.of(
        Arguments.of(
            Test_File1,
            "\n\tCAU\n\tHaplotype [types=[B*07:02:01, C*07:02:01]]\n\tHaplotype [types=[B*49:01:01, C*07:01:01]]\n\tAFA\n\tHaplotype [types=[B*07:02:01, C*07:02:01]]\n\tHaplotype [types=[B*49:01:01, C*07:01:01]]\n\tAPI\n\tHaplotype [types=[B*07:02:01, C*07:02:01]]\n\tHaplotype [types=[B*49:01:01, C*07:01:01]]\n\tHIS\n\tHaplotype [types=[B*07:02:01, C*07:02:01]]\n\tHaplotype [types=[B*49:01:01, C*07:01:01]]\n\tNAM\n\tHaplotype [types=[B*07:02:01, C*07:02:01]]\n\tHaplotype [types=[B*49:01:01, C*07:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:02:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:02:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:02:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:02:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:02:01, DRB3*02:02:01, DQB1*03:01:01]]"),
        Arguments.of(
            Test_File2,
            "\n\tCAU\n\tHaplotype [types=[B*45:01:01, C*16:01:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tAFA\n\tHaplotype [types=[B*45:01:01, C*16:01:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tAPI\n\tHaplotype [types=[B*45:01:01, C*16:01:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tHIS\n\tHaplotype [types=[B*45:01:01, C*16:01:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tNAM\n\tHaplotype [types=[B*45:01:01, C*16:01:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*03:01:01, DRB3*01:01:02, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*15:01:01, DRB5*01:01:01, DQB1*06:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*15:03:01, DRB5*01:01:01, DQB1*06:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*15:01:01, DRB5*01:01:01, DQB1*06:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*15:01:01, DRB5*01:01:01, DQB1*06:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*03:01:01, DRB3*01:01:02, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*15:01:01, DRB5*01:01:01, DQB1*06:02:01]]"),
        Arguments.of(
            Test_File3,
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tAFA\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tAPI\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tHIS\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tNAM\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]"),
        Arguments.of(
            Test_File4,
            "\n\tCAU\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAFA\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAPI\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHIS\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tNAM\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]"),
        Arguments.of(
            Test_File5,
            "\n\tCAU\n\tHaplotype [types=[B*18:01:01, C*07:01:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAFA\n\tHaplotype [types=[B*18:01:01, C*07:04:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAPI\n\tHaplotype [types=[B*18:01:01, C*07:01:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHIS\n\tHaplotype [types=[B*18:01:01, C*07:01:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tNAM\n\tHaplotype [types=[B*18:01:01, C*07:01:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*01:02:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]"),
        Arguments.of(
            Test_File6,
            "\n\tCAU\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHaplotype [types=[B*58:01:01, C*07:01:01]]\n\tAFA\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHaplotype [types=[B*58:01:01, C*07:01:01]]\n\tAPI\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHaplotype [types=[B*58:01:01, C*07:01:01]]\n\tHIS\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHaplotype [types=[B*58:01:01, C*07:01:01]]\n\tNAM\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHaplotype [types=[B*58:01:01, C*07:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*04:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*04:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:02, DRB3*00:00N, DQB1*04:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:02, DRB3*00:00N, DQB1*04:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:04:04, DRB3*00:00N, DQB1*04:02:01]]"),
        Arguments.of(
            Test_File7,
            "\n\tCAU\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*15:03:01, C*02:02:02]]\n\tAFA\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*15:03:01, C*02:02:02]]\n\tAPI\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*15:03:01, C*02:02:02]]\n\tHIS\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*15:03:01, C*02:02:02]]\n\tNAM\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*15:03:01, C*02:02:02]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHaplotype [types=[DRB1*13:03:01, DRB3*01:01:02, DQB1*03:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHaplotype [types=[DRB1*13:03:01, DRB3*01:01:02, DQB1*03:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHaplotype [types=[DRB1*13:03:01, DRB3*01:01:02, DQB1*03:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHaplotype [types=[DRB1*13:03:01, DRB3*01:01:02, DQB1*03:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHaplotype [types=[DRB1*13:03:01, DRB3*01:01:02, DQB1*03:01:01]]"),
        Arguments.of(
            Test_File8,
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*44:02:01, C*05:01:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*44:02:01, C*05:01:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*44:02:01, C*05:01:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*44:02:01, C*05:01:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*44:02:01, C*05:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*03:01:01, DRB3*01:01:02, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*03:01:01, DRB3*01:01:02, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]"),
        Arguments.of(
            Test_File9,
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*45:01:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*45:01:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*45:01:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*45:01:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*45:01:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*09:01:02, DRB4*01:01:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*06:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*09:01:02, DRB4*01:01:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*06:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*09:01:02, DRB4*01:01:01, DQB1*06:02:01]]\n\tHaplotype [types=[DRB1*11:19:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*09:01:02, DRB4*01:01:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*06:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*09:01:02, DRB4*01:01:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*06:02:01]]"),
        Arguments.of(
            Test_File10,
            "\n\tCAU\n\tHaplotype [types=[B*08:01:01, C*07:01:01]]\n\tHaplotype [types=[B*40:01:01, C*03:04:02]]\n\tAFA\n\tHaplotype [types=[B*08:01:01, C*07:01:01]]\n\tHaplotype [types=[B*40:01:01, C*03:04:02]]\n\tAPI\n\tHaplotype [types=[B*08:01:01, C*07:02:01]]\n\tHaplotype [types=[B*40:01:01, C*03:04:02]]\n\tHIS\n\tHaplotype [types=[B*08:01:01, C*07:01:01]]\n\tHaplotype [types=[B*40:01:01, C*03:04:02]]\n\tNAM\n\tHaplotype [types=[B*08:01:01, C*07:01:01]]\n\tHaplotype [types=[B*40:01:01, C*03:04:02]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tAFA\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tAPI\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tHIS\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]\n\tNAM\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*13:01:01, DRB3*01:01:02, DQB1*06:03:01]]"));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedHaplotype string expected as the output of the string version of {@link
   *     ValidationModel} when split to just be haplotype section
   */
  @DisplayName("Haplotype Alleles parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testHaplotype")
  public void PDFSureTyperParserTest_haplotype(String fileName, String expectedHaplotype) {
    // get test/resource file path for frequency tables
    String CBFilePath = getClass().getClassLoader().getResource("C_B.xls").getFile();
    String DRDQFilePath =
        getClass().getClassLoader().getResource("DRB3-4-5_DRB1_DQB1.xls").getFile();
    // set {@link HaplotypeFrequencies} to frequency table path
    HLAProperties.get().setProperty(HaplotypeFrequencies.NMDP_CB_PROP, CBFilePath);
    HLAProperties.get().setProperty(HaplotypeFrequencies.NMDP_DRDQ_PROP, DRDQFilePath);
    HaplotypeFrequencies.doInitialization();
    ValidationModel model = createModel(fileName);
    if (HaplotypeFrequencies.successfullyInitialized()) {
      assertEquals(expectedHaplotype, model.toString().split("B-C Haplotype")[1]);
    } else {
      System.err.println("Error: Frequency files not loaded in");
    }
  }

  // create the parameters to be used as a method source for the haplotype BC frequency test to run
  // through
  // five races, two strands per race and for each race the lesser value must go first in the array
  private static Stream<Arguments> testBCHaplotypeFrequency() {
    double[] freq1 = {
      0.01611, 0.123, 0.02804, 0.05984, 0.00287, 0.02923, 0.02362, 0.05652, 0.01377, 0.09628
    };
    double[] freq2 = {
      0.00072, 0.00363, 0.03735, 0.1079, 0.00041, 0.00079, 0.0092, 0.0153, 0.00285, 0.00804
    };
    double[] freq3 = {
      0.02719, 0.03371, 0.00664, 0.02134, 0.00155, 0.03078, 0.01043, 0.04161, 0.0207, 0.02662
    };
    double[] freq4 = {0, 0.05588, 0, 0.05482, 0, 0.03018, 0, 0.06563, 0, 0.08644};
    double[] freq5 = {
      0.01992, 0.05588, 0.00405, 0.05482, 0.00553, 0.03018, 0.00972, 0.06563, 0.01258, 0.08644
    };
    double[] freq6 = {
      0.00522, 0.03371, 0.00664, 0.0234, 0.00009, 0.03078, 0.00906, 0.01043, 0.00429, 0.02662
    };
    double[] freq7 = {
      0.00076, 0.03177, 0.00579, 0.06076, 0.00012, 0.01548, 0.01129, 0.01458, 0.00329, 0.02668
    };
    double[] freq8 = {
      0.02719, 0.07064, 0.0176, 0.02134, 0.00155, 0.00729, 0.0381, 0.04161, 0.0207, 0.06176
    };
    double[] freq9 = {
      0.00459, 0.02719, 0.01064, 0.02134, 0.00064, 0.00155, 0.00796, 0.04161, 0.00413, 0.0207
    };
    double[] freq10 = {
      0.0492, 0.10394, 0.01131, 0.02734, 0.01588, 0.03027, 0.01481, 0.03867, 0.04881, 0.07723
    };

    return Stream.of(
        Arguments.of(Test_File1, createBigDecimalRoundedFrequencyArray(freq1)),
        Arguments.of(Test_File2, createBigDecimalRoundedFrequencyArray(freq2)),
        Arguments.of(Test_File3, createBigDecimalRoundedFrequencyArray(freq3)),
        Arguments.of(Test_File4, createBigDecimalRoundedFrequencyArray(freq4)),
        Arguments.of(Test_File5, createBigDecimalRoundedFrequencyArray(freq5)),
        Arguments.of(Test_File6, createBigDecimalRoundedFrequencyArray(freq6)),
        Arguments.of(Test_File7, createBigDecimalRoundedFrequencyArray(freq7)),
        Arguments.of(Test_File8, createBigDecimalRoundedFrequencyArray(freq8)),
        Arguments.of(Test_File9, createBigDecimalRoundedFrequencyArray(freq9)),
        Arguments.of(Test_File10, createBigDecimalRoundedFrequencyArray(freq10)));
  }

  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedBCFrequencyArray an array of BigDecimals for expected frequencies in CAU, AFA,
   *     API, HIS, NAM order
   */
  @SuppressWarnings("restriction")
  @DisplayName("Haplotype BC Frequency parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testBCHaplotypeFrequency")
  public void PDFSureTyperParserTest_BCHaplotypeFrequency(
      String fileName, BigDecimal[] expectedBCFrequencyArray) {
    String CBFilePath = getClass().getClassLoader().getResource("C_B.xls").getFile();
    HLAProperties.get().setProperty(HaplotypeFrequencies.NMDP_CB_PROP, CBFilePath);
    HaplotypeFrequencies.doInitialization();
    if (HaplotypeFrequencies.successfullyInitialized()) {
      ValidationModel model = createModel(fileName);
      ValidationTable table = new ValidationTable();
      table.setFirstModel(model);
      table.setSecondModel(model);
      // iterate through haplotype frequency data
      for (int i = 0; i < 10; i = i + 2) {
        BigDecimal haplotypeFrequencyStrandOne =
            table.getBCHaplotypeRows().get(i).frequencyProperty().getValue().stripTrailingZeros();
        BigDecimal haplotypeFrequencyStrandTwo =
            table
                .getBCHaplotypeRows()
                .get(i + 1)
                .frequencyProperty()
                .getValue()
                .stripTrailingZeros();
        // BCHaplotypeRows are sorted by race, but individual haplotypes may be in either order
        if (haplotypeFrequencyStrandOne.compareTo(haplotypeFrequencyStrandTwo) < 0) {
          assertEquals(expectedBCFrequencyArray[i], haplotypeFrequencyStrandOne);
          assertEquals(expectedBCFrequencyArray[i + 1], haplotypeFrequencyStrandTwo);
        } else {
          assertEquals(expectedBCFrequencyArray[i], haplotypeFrequencyStrandTwo);
          assertEquals(expectedBCFrequencyArray[i + 1], haplotypeFrequencyStrandOne);
        }
      }
    } else {
      System.err.println("Error: Frequency files not loaded in.");
    }
  }

  // create the parameters to be used as a method source for the haplotype DRDQ frequency test to
  // run through
  // five races, two strands per race and for each race the lesser value must go first in the array
  private static Stream<Arguments> testDRDQHaplotypeFrequency() {
    double[] freq1 = {
      0.00078, 0.00282, 0.03548, 0.05223, 0.00012, 0.00016, 0.00497, 0.01253, 0.00372, 0.00474
    };
    double[] freq2 = {
      0.09431, 0.12689, 0.05172, 0.11672, 0.03514, 0.05492, 0.03637, 0.06023, 0.0707, 0.10037
    };
    double[] freq3 = {
      0.03364, 0.03741, 0.00295, 0.03757, 0.01847, 0.02507, 0.01206, 0.02784, 0.02339, 0.02771
    };
    double[] freq4 = {
      0.04557, 0.09595, 0.01512, 0.09746, 0.03671, 0.07397, 0.06337, 0.09536, 0.04735, 0.07928
    };
    double[] freq5 = {
      0.08444, 0.09595, 0.04014, 0.09746, 0.02642, 0.07397, 0.04482, 0.09536, 0.06747, 0.07928
    };
    double[] freq6 = {
      0.00177, 0.03364, 0.00226, 0.00295, 0.00583, 0.02507, 0.01206, 0.06882, 0.02302, 0.02339
    };
    double[] freq7 = {
      0.01192, 0.03588, 0.01657, 0.0194, 0.00072, 0.02211, 0.0121, 0.0276, 0.00867, 0.024
    };
    double[] freq8 = {
      0.04557, 0.09431, 0.01512, 0.05172, 0.03671, 0.05492, 0.03637, 0.06337, 0.04735, 0.0707
    };
    double[] freq9 = {0.00001, 0.00039, 0.02864, 0.03893, 0, 0, 0.00259, 0.00534, 0.00112, 0.00125};
    double[] freq10 = {
      0.03588, 0.04557, 0.01512, 0.0194, 0.02211, 0.03671, 0.0276, 0.06337, 0.024, 0.04735
    };

    return Stream.of(
        Arguments.of(Test_File1, createBigDecimalRoundedFrequencyArray(freq1)),
        Arguments.of(Test_File2, createBigDecimalRoundedFrequencyArray(freq2)),
        Arguments.of(Test_File3, createBigDecimalRoundedFrequencyArray(freq3)),
        Arguments.of(Test_File4, createBigDecimalRoundedFrequencyArray(freq4)),
        Arguments.of(Test_File5, createBigDecimalRoundedFrequencyArray(freq5)),
        Arguments.of(Test_File6, createBigDecimalRoundedFrequencyArray(freq6)),
        Arguments.of(Test_File7, createBigDecimalRoundedFrequencyArray(freq7)),
        Arguments.of(Test_File8, createBigDecimalRoundedFrequencyArray(freq8)),
        Arguments.of(Test_File9, createBigDecimalRoundedFrequencyArray(freq9)),
        Arguments.of(Test_File10, createBigDecimalRoundedFrequencyArray(freq10)));
  }

  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedDRDQFrequencyArray an array of BigDecimals for expected frequencies in CAU, AFA,
   *     API, HIS, NAM order
   */
  @SuppressWarnings("restriction")
  @DisplayName("Haplotype DRDQ Frequency parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testDRDQHaplotypeFrequency")
  public void PDFSureTyperParserTest_DRDQHaplotypeFrequency(
      String fileName, BigDecimal[] expectedDRDQFrequencyArray) {
    String DRDQFilePath =
        getClass().getClassLoader().getResource("DRB3-4-5_DRB1_DQB1.xls").getFile();
    HLAProperties.get().setProperty(HaplotypeFrequencies.NMDP_DRDQ_PROP, DRDQFilePath);
    HaplotypeFrequencies.doInitialization();
    if (HaplotypeFrequencies.successfullyInitialized()) {
      ValidationModel model = createModel(fileName);
      ValidationTable table = new ValidationTable();
      table.setFirstModel(model);
      table.setSecondModel(model);
      // iterate through haplotype frequency data
      for (int i = 0; i < 10; i = i + 2) {
        BigDecimal haplotypeFrequencyStrandOne =
            table.getDRDQHaplotypeRows().get(i).frequencyProperty().getValue().stripTrailingZeros();
        BigDecimal haplotypeFrequencyStrandTwo =
            table
                .getDRDQHaplotypeRows()
                .get(i + 1)
                .frequencyProperty()
                .getValue()
                .stripTrailingZeros();
        // DRDQHaplotypeRows are sorted by race, but individual haplotypes may be in either order
        if (haplotypeFrequencyStrandOne.compareTo(haplotypeFrequencyStrandTwo) < 0) {
          assertEquals(expectedDRDQFrequencyArray[i], haplotypeFrequencyStrandOne);
          assertEquals(expectedDRDQFrequencyArray[i + 1], haplotypeFrequencyStrandTwo);
        } else {
          assertEquals(expectedDRDQFrequencyArray[i], haplotypeFrequencyStrandTwo);
          assertEquals(expectedDRDQFrequencyArray[i + 1], haplotypeFrequencyStrandOne);
        }
      }
    } else {
      System.err.println("Error: Frequency files not loaded in.");
    }
  }

  private ValidationModel createModel(String input) {
    ValidationModelBuilder builder = new ValidationModelBuilder();
    builder.source(input);
    try (PDDocument pdf = PDDocument.load(getClass().getClassLoader().getResourceAsStream(input))) {
      PDFTextStripper tStripper = new PDFTextStripper();
      tStripper.setSortByPosition(true);
      // Extract all text from the PDF and split it into lines
      String pdfText = tStripper.getText(pdf);
      String[] pdfLines = pdfText.split(System.getProperty("line.separator"));
      PdfSureTyperParser.parseTypes(builder, pdfLines);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    ValidationModel model = null;
    model = builder.build();
    return model;
  }
  /**
   * @param frequencies an array of doubles for expected frequency results
   * @return an array of BigDecimals rounded in the same fashion as the frequency table output
   */
  private static BigDecimal[] createBigDecimalRoundedFrequencyArray(double[] frequencies) {
    BigDecimal[] returnArray = new BigDecimal[10];
    for (int i = 0; i < 10; i++) {
      returnArray[i] =
          new BigDecimal(frequencies[i])
              .setScale(
                  HaplotypeFrequencies.UNKNOWN_HAP_SIG_FIGS,
                  HaplotypeFrequencies.UNKNOWN_HAP_ROUNDING_MODE)
              .stripTrailingZeros();
    }
    return returnArray;
  }
}
