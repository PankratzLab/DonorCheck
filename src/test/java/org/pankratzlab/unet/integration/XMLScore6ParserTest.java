package org.pankratzlab.unet.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.HaplotypeFrequencies;
import org.pankratzlab.unet.hapstats.RaceGroup;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.XmlScore6Parser;
import com.google.common.collect.SetMultimap;

public class XMLScore6ParserTest {
  // test files located in test/resources
  private static final String Test_File1 = "UnitTestXMLScore6.xml";
  private static final String Test_File2 = "UnitTestXMLScore6_2.xml";
  private static final String Test_File3 = "UnitTestXMLScore6_3.xml";
  private static final String Test_File4 = "UnitTestXMLScore6_4.xml";
  private static final String Test_File5 = "UnitTestXMLScore6_5.xml";
  private static final String Test_File6 = "UnitTestXMLScore6_6.xml";
  private static final String Test_File7 = "UnitTestXMLScore6_7.xml";

  // populate the haplotype frequency tables for testing
  @BeforeAll
  private static void init() {
    HaplotypeTestingUtils.initiateFreqTablesTesting();
  }

  // create parameters to be used as CSV source for checking Donor ID parsing
  @DisplayName("Donor ID parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    Test_File1 + ", AFJQ146",
    Test_File2 + ", AFLG047",
    Test_File3 + ", AFK3387",
    Test_File4 + ", AFLK097",
    Test_File5 + ", AGFJ449",
    Test_File6 + ", AFK3449",
    Test_File7 + ", AGJS214"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param donorId expected Donor ID for each respective donor file
   */
  public void twoDRB3_XMLScore6ParserTest_getDonorId(String fileName, String donorId) {
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
            new SeroType("A", 3),
            new SeroType("A", 32),
            new SeroType("B", 35),
            new SeroType("B", 61),
            new SeroType("C", 2),
            new SeroType("C", 4)),
        Arguments.of(
            Test_File3,
            new SeroType("A", 1),
            new SeroType("A", 2),
            new SeroType("B", 27),
            new SeroType("B", 44),
            new SeroType("C", 2),
            null),
        Arguments.of(
            Test_File4,
            new SeroType("A", 2),
            new SeroType("A", 29),
            new SeroType("B", 44),
            new SeroType("B", 51),
            new SeroType("C", 15),
            new SeroType("C", 16)),
        Arguments.of(
            Test_File5,
            new SeroType("A", 2),
            new SeroType("A", 3),
            new SeroType("B", 44),
            new SeroType("B", 62),
            new SeroType("C", 7),
            new SeroType("C", 9)),
        Arguments.of(
            Test_File6,
            new SeroType("A", 2),
            new SeroType("A", 3),
            new SeroType("B", 57),
            new SeroType("B", 65),
            new SeroType("C", 6),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File7,
            new SeroType("A", 1),
            new SeroType("A", 24),
            new SeroType("B", 35),
            new SeroType("B", 57),
            new SeroType("C", 6),
            new SeroType("C", 9)));
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
  public void XMLScore6ParserTest_getABC(
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
    Test_File2 + ", Negative, Positive",
    Test_File3 + ", Positive, Negative",
    Test_File4 + ", Positive, Negative",
    Test_File5 + ", Positive, Positive",
    Test_File6 + ", Positive, Positive",
    Test_File7 + ", Positive, Positive"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param Bw4Result expected Bw4 result(Positive or Negative)
   * @param Bw6Result expected Bw6 result(Positive or Negative)
   */
  public void XMLScore6ParserTest_isBw(String fileName, String Bw4Result, String Bw6Result) {
    ValidationModel model = createModel(fileName);
    assertEquals(Bw4Result, model.isBw4());
    assertEquals(Bw6Result, model.isBw6());
  }

  // create parameters to be used as a method source for checking the DRB345 parser
  private static Stream<Arguments> testGetDRB() {
    return Stream.of(
        Arguments.of(Test_File1, null, null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(Test_File2, null, null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(Test_File3, null, null, null, null, null, null),
        Arguments.of(
            Test_File4, null, null, null, null, new HLAType("DRB4", 1), new HLAType("DRB4", 1)),
        Arguments.of(
            Test_File5, null, null, new HLAType("DRB3", 2), new HLAType("DRB3", 2), null, null),
        Arguments.of(Test_File6, null, null, new HLAType("DRB3", 3), null, null, null),
        Arguments.of(Test_File7, null, null, null, null, null, null));
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
  public void XMLScore6ParserTest_getDRB(
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
            "\n\tCAU\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHaplotype [types=[B*40:02:01, C*02:02:02]]\n\tAFA\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHaplotype [types=[B*40:02:01, C*02:02:02]]\n\tAPI\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHaplotype [types=[B*40:02:01, C*02:02:02]]\n\tHIS\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHaplotype [types=[B*40:02:01, C*02:02:02]]\n\tNAM\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHaplotype [types=[B*40:02:01, C*02:02:02]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*01:01:02, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*11:01:02, DRB3*02:02:01, DQB1*03:01:01]]"),
        Arguments.of(
            Test_File3,
            "\n\tCAU\n\tHaplotype [types=[B*27:05:02, C*02:02:02]]\n\tHaplotype [types=[B*44:05, C*02:02:02]]\n\tAFA\n\tHaplotype [types=[B*27:05:02, C*02:02:02]]\n\tHaplotype [types=[B*44:05, C*02:02:02]]\n\tAPI\n\tHaplotype [types=[B*27:05:02, C*02:02:02]]\n\tHaplotype [types=[B*44:05, C*02:02:02]]\n\tHIS\n\tHaplotype [types=[B*27:05:02, C*02:02:02]]\n\tHaplotype [types=[B*44:05, C*02:02:02]]\n\tNAM\n\tHaplotype [types=[B*27:05:02, C*02:02:02]]\n\tHaplotype [types=[B*44:05, C*02:02:02]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]"),
        Arguments.of(
            Test_File4,
            "\n\tCAU\n\tHaplotype [types=[B*44:03:02, C*16:01:01]]\n\tHaplotype [types=[B*51:01:01, C*15:02:01]]\n\tAFA\n\tHaplotype [types=[B*44:03:02, C*16:01:01]]\n\tHaplotype [types=[B*51:01:01, C*15:02:01]]\n\tAPI\n\tHaplotype [types=[B*44:03:02, C*16:01:01]]\n\tHaplotype [types=[B*51:01:01, C*15:02:01]]\n\tHIS\n\tHaplotype [types=[B*44:03:02, C*16:01:01]]\n\tHaplotype [types=[B*51:01:01, C*15:02:01]]\n\tNAM\n\tHaplotype [types=[B*44:03:02, C*16:01:01]]\n\tHaplotype [types=[B*51:01:01, C*15:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]"),
        Arguments.of(
            Test_File5,
            "\n\tCAU\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*44:03:02, C*07:01:01]]\n\tAFA\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*44:03:02, C*07:01:01]]\n\tAPI\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*44:03:02, C*07:01:01]]\n\tHIS\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*44:03:02, C*07:01:01]]\n\tNAM\n\tHaplotype [types=[B*15:01:01, C*03:03:04]]\n\tHaplotype [types=[B*44:03:02, C*07:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*11:03:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:04:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*11:03:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:04:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*11:03:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:04:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*11:03:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:04:02, DRB3*02:02:01, DQB1*03:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*11:03:01, DRB3*02:02:01, DQB1*03:01:01]]\n\tHaplotype [types=[DRB1*11:04:02, DRB3*02:02:01, DQB1*03:01:01]]"),
        Arguments.of(
            Test_File6,
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tAFA\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tAPI\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tHIS\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tNAM\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]"),
        Arguments.of(
            Test_File7,
            "\n\tCAU\n\tHaplotype [types=[B*35:01:01, C*03:03:04]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*35:01:01, C*03:03:04]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*35:01:01, C*03:03:04]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*35:01:01, C*03:03:04]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*35:01:01, C*03:03:04]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*08:01:01, DRB3*00:00N, DQB1*04:02:01]]"));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedHaplotype string expected as the output of the string version of {@link
   *     ValidationModel} when split to just be haplotype section
   */
  @DisplayName("Haplotype Alleles parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testHaplotype")
  public void XMLScore6ParserTest_haplotype(String fileName, String expectedHaplotype) {
    ValidationModel model = createModel(fileName);
    if (HaplotypeFrequencies.successfullyInitialized()) {
      assertEquals(expectedHaplotype, model.toString().split("B-C Haplotype")[1]);
    } else {
      System.err.println("Error: Frequency files not loaded in");
    }
  }

  // create the parameters to be used as a method source for the haplotype BC frequency test to run
  // through
  // Expected results frequencies in CAU, AFA, API, HIS, NAM order
  private static Stream<Arguments> testBCHaplotypeFrequency() {
    double[] freq1 = {
      0.01611, 0.123, 0.02804, 0.05984, 0.00287, 0.02923, 0.02362, 0.05652, 0.01377, 0.09628
    };
    double[] freq2 = {
      0.01149, 0.05588, 0.00211, 0.05482, 0.00012, 0.03018, 0.00613, 0.06563, 0.00602, 0.08644
    };
    double[] freq3 = {
      0.00337, 0.02006, 0.00029, 0.00444, 0.00006, 0.00512, 0.00112, 0.01064, 0.00135, 0.03779
    };
    double[] freq4 = {
      0.02042, 0.02676, 0.00413, 0.00635, 0.00039, 0.00977, 0.03005, 0.03805, 0.02386, 0.03576
    };
    double[] freq5 = {
      0.00051, 0.03177, 0.00398, 0.00579, 0.01548, 0.03859, 0.00146, 0.01458, 0.00309, 0.02668
    };
    double[] freq6 = {
      0.02719, 0.03371, 0.00664, 0.02134, 0.00155, 0.03078, 0.01043, 0.04161, 0.0207, 0.02662
    };
    double[] freq7 = {
      0.00074, 0.03371, 0.00664, 0.00004, 0.01373, 0.03078, 0.01043, 0.00008, 0.0001, 0.02662
    };
    return Stream.of(
        Arguments.of(Test_File1, HaplotypeTestingUtils.createTestMultimap(freq1)),
        Arguments.of(Test_File2, HaplotypeTestingUtils.createTestMultimap(freq2)),
        Arguments.of(Test_File3, HaplotypeTestingUtils.createTestMultimap(freq3)),
        Arguments.of(Test_File4, HaplotypeTestingUtils.createTestMultimap(freq4)),
        Arguments.of(Test_File5, HaplotypeTestingUtils.createTestMultimap(freq5)),
        Arguments.of(Test_File6, HaplotypeTestingUtils.createTestMultimap(freq6)),
        Arguments.of(Test_File7, HaplotypeTestingUtils.createTestMultimap(freq7)));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedBCMultimap a multimap of RaceGroup ethnicities with BigDecimal expected
   *     frequencies
   */
  @DisplayName("Haplotype BC Frequency parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testBCHaplotypeFrequency")
  public void XMLScore6ParserTest_BCHaplotypeFrequency(
      String fileName, SetMultimap<RaceGroup, BigDecimal> expectedBCMultimap) {
    if (HaplotypeFrequencies.successfullyInitialized()) {
      ValidationModel model = createModel(fileName);
      assertEquals(
          "Difference between generated and expected: {}Difference between expected and generated: {}",
          HaplotypeTestingUtils.testBCHaplotypes(model, expectedBCMultimap));
    } else {
      System.err.println("Error: Frequency files not loaded in.");
    }
  }

  // create the parameters to be used as a method source for the haplotype DRDQ frequency test to
  // run through
  //
  // Expected results frequencies in CAU, AFA, API, HIS, NAM order
  private static Stream<Arguments> testDRDQHaplotypeFrequency() {
    double[] freq1 = {
      0.00078, 0.00282, 0.03548, 0.05223, 0.00012, 0.00016, 0.00497, 0.01253, 0.00372, 0.00474
    };
    double[] freq2 = {
      0.06304, 0.08444, 0.02587, 0.03555, 0.02642, 0.05341, 0.03747, 0.04482, 0.04266, 0.06747
    };
    double[] freq3 = {
      0.02349, 0.08444, 0.00362, 0.02587, 0.00169, 0.02642, 0.01091, 0.04482, 0.01519, 0.06747
    };
    double[] freq4 = {
      0.0327, 0.09595, 0.0081, 0.09746, 0.01148, 0.07397, 0.0464, 0.09536, 0.04735, 0.07928
    };
    double[] freq5 = {
      0.00686, 0.03528, 0.00069, 0.00395, 0.00024, 0.00962, 0.00376, 0.02922, 0.00322, 0.01672
    };
    double[] freq6 = {
      0.0087, 0.03364, 0.00295, 0.03757, 0.01746, 0.02507, 0.01004, 0.01206, 0.00998, 0.02339
    };
    double[] freq7 = {
      0.02349, 0.03364, 0.00295, 0.00362, 0.00169, 0.02507, 0.01091, 0.01206, 0.01519, 0.02339
    };
    return Stream.of(
        Arguments.of(Test_File1, HaplotypeTestingUtils.createTestMultimap(freq1)),
        Arguments.of(Test_File2, HaplotypeTestingUtils.createTestMultimap(freq2)),
        Arguments.of(Test_File3, HaplotypeTestingUtils.createTestMultimap(freq3)),
        Arguments.of(Test_File4, HaplotypeTestingUtils.createTestMultimap(freq4)),
        Arguments.of(Test_File5, HaplotypeTestingUtils.createTestMultimap(freq5)),
        Arguments.of(Test_File6, HaplotypeTestingUtils.createTestMultimap(freq6)),
        Arguments.of(Test_File7, HaplotypeTestingUtils.createTestMultimap(freq7)));
  }
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedDRDQMultimap a multimap of RaceGroup ethnicities with BigDecimal expected
   *     frequencies
   */
  @DisplayName("Haplotype DRDQ Frequency parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testDRDQHaplotypeFrequency")
  public void XMLScore6ParserTest_DRDQHaplotypeFrequency(
      String fileName, SetMultimap<RaceGroup, BigDecimal> expectedDRDQMultimap) {
    if (HaplotypeFrequencies.successfullyInitialized()) {
      ValidationModel model = createModel(fileName);
      assertEquals(
          "Difference between generated and expected: {}Difference between expected and generated: {}",
          HaplotypeTestingUtils.testDRDQHaplotypes(model, expectedDRDQMultimap));
    } else {
      System.err.println("Error: Frequency files not loaded in.");
    }
  }

  private ValidationModel createModel(String input) {
    ValidationModelBuilder builder = new ValidationModelBuilder();
    builder.source(input);
    try (InputStream xmlStream = getClass().getClassLoader().getResourceAsStream(input)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      XmlScore6Parser.buildModelFromXML(builder, parsed);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Invalid XML file: " + input);
    }
    ValidationModel model = null;
    model = builder.build();
    return model;
  }
}
