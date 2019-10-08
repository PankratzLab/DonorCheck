package org.pankratzlab.unet.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.pankratzlab.unet.parser.util.XmlSureTyperParser;
import com.google.common.collect.SetMultimap;

public class XMLSureTyperParserTest {
  // test files located in test/resources
  private static final String Test_File1 = "UnitTestXMLSureTyper.xml";
  private static final String Test_File2 = "UnitTestXMLSureTyper_2.xml";
  private static final String Test_File3 = "UnitTestXMLSureTyper_3.xml";
  private static final String Test_File4 = "UnitTestXMLSureTyper_4.xml";

  // populate the haplotype frequency tables for testing
  @BeforeAll
  private static void init() {
    HaplotypeTestingUtils.initiateFreqTablesTesting();
  }

  // create parameters to be used as CSV source for checking Donor ID parsing
  @DisplayName("Donor ID parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({
    Test_File1 + ", AFK3449",
    Test_File2 + ", AFLG241",
    Test_File3 + ", AFK3166",
    Test_File4 + ", AFLG065"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param donorId expected Donor ID for each respective donor file
   */
  public void twoDRB3_XMLSureTyperParserTest_getDonorId(String fileName, String donorId) {
    assertEquals(donorId, createModel(fileName).getDonorId());
  }

  // create the parameters to be used as a method source for the allele parser to run through
  private static Stream<Arguments> testGetABC() {
    return Stream.of(
        Arguments.of(
            Test_File1,
            new SeroType("A", 2),
            new SeroType("A", 3),
            new SeroType("B", 57),
            new SeroType("B", 65),
            new SeroType("C", 06),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File2,
            new SeroType("A", 68),
            null,
            new SeroType("B", 48),
            new SeroType("B", 58),
            new SeroType("C", 6),
            new SeroType("C", 8)),
        Arguments.of(
            Test_File3,
            new SeroType("A", 3),
            new SeroType("A", 24),
            new SeroType("B", 27),
            new SeroType("B", 35),
            new SeroType("C", 2),
            new SeroType("C", 4)),
        Arguments.of(
            Test_File4,
            new SeroType("A", 68),
            null,
            new SeroType("B", 53),
            new SeroType("B", 65),
            new SeroType("C", 4),
            new SeroType("C", 8)));
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
  public void XMLSureTyperParserTest_getABC(
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
    Test_File3 + ", Negative, Positive",
    Test_File4 + ", Positive, Positive"
  })
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param Bw4Result expected Bw4 result(Positive or Negative)
   * @param Bw6Result expected Bw6 result(Positive or Negative)
   */
  public void XMLSureTyperParserTest_isBw(String fileName, String Bw4Result, String Bw6Result) {
    ValidationModel model = createModel(fileName);
    assertEquals(Bw4Result, model.isBw4());
    assertEquals(Bw6Result, model.isBw6());
  }

  // create parameters to be used as a method source for checking the DRB345 parser
  private static Stream<Arguments> testGetDRB() {
    return Stream.of(
        Arguments.of(Test_File1, null, null, new HLAType("DRB3", 3), null, null, null),
        Arguments.of(
            Test_File2, null, null, new HLAType("DRB3", 2), null, new HLAType("DRB4", 1), null),
        Arguments.of(Test_File3, null, null, null, null, new HLAType("DRB4", 1), null),
        Arguments.of(Test_File4, null, null, null, null, new HLAType("DRB4", 1), null));
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
  public void XMLSureTyperParserTest_getDRB(
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
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*57:01:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tAFA\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:09:01]]\n\tAPI\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tHIS\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]\n\tNAM\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*03:03:02]]\n\tHaplotype [types=[DRB1*13:02:01, DRB3*03:01:01, DQB1*06:04:01]]"),
        Arguments.of(
            Test_File2,
            "\n\tCAU\n\tHaplotype [types=[B*48:01:01, C*08:03:01]]\n\tHaplotype [types=[B*58:02:01, C*06:02:01]]\n\tAFA\n\tHaplotype [types=[B*48:01:01, C*08:01:01]]\n\tHaplotype [types=[B*58:02:01, C*06:02:01]]\n\tAPI\n\tHaplotype [types=[B*48:01:01, C*08:01:01]]\n\tHaplotype [types=[B*58:02:01, C*06:02:01]]\n\tHIS\n\tHaplotype [types=[B*48:01:01, C*08:01:01]]\n\tHaplotype [types=[B*58:02:01, C*06:02:01]]\n\tNAM\n\tHaplotype [types=[B*48:01:01, C*08:01:01]]\n\tHaplotype [types=[B*58:02:01, C*06:02:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*03:01:01, DRB3*02:02:01, DQB1*02:01:01]]\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]"),
        Arguments.of(
            Test_File3,
            "\n\tCAU\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAFA\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tAPI\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tHIS\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\n\tNAM\n\tHaplotype [types=[B*27:08, C*02:10:01]]\n\tHaplotype [types=[B*35:01:01, C*04:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAFA\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tAPI\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tHIS\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]\n\tNAM\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHaplotype [types=[DRB1*07:01:01, DRB4*01:01:01, DQB1*02:01:01]]"),
        Arguments.of(
            Test_File4,
            "\n\tCAU\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tAFA\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tAPI\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tHIS\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\n\tNAM\n\tHaplotype [types=[B*14:02:01, C*08:02:01]]\n\tHaplotype [types=[B*53:01:01, C*04:01:01]]\nDR-DQ Haplotype\n\tCAU\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*04:01:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAFA\n\tHaplotype [types=[DRB1*01:02:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*04:05:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tAPI\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*04:03:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tHIS\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*04:07:01, DRB4*01:01:01, DQB1*03:02:01]]\n\tNAM\n\tHaplotype [types=[DRB1*01:01:01, DRB3*00:00N, DQB1*05:01:01]]\n\tHaplotype [types=[DRB1*04:04:01, DRB4*01:01:01, DQB1*03:02:01]]"));
  }

  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedHaplotype string expected as the output of the string version of {@link
   *     ValidationModel} when split to just be haplotype section
   */
  @DisplayName("Haplotype Alleles parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testHaplotype")
  public void XMLSureTyperParserTest_haplotype(String fileName, String expectedHaplotype) {
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
      .02719, .03371, .00664, 0.02134, 0.00155, 0.03078, .01043, 0.04161, 0.0207, 0.02662
    };
    double[] freq2 = {
      0.00013, .00042, 0.00034, 0.04008, 0.00007, 0.0123, 0.00365, 0.01583, 0.00217, 0.01307
    };
    double[] freq3 = {0, .05588, 0, .05482, 0, .03018, 0, .06563, 0, .08644};
    double[] freq4 = {.00363, .02719, .02134, .1079, .00079, .00155, .0153, .04161, .00804, .0207};

    return Stream.of(
        Arguments.of(Test_File1, HaplotypeTestingUtils.createTestMultimap(freq1)),
        Arguments.of(Test_File2, HaplotypeTestingUtils.createTestMultimap(freq2)),
        Arguments.of(Test_File3, HaplotypeTestingUtils.createTestMultimap(freq3)),
        Arguments.of(Test_File4, HaplotypeTestingUtils.createTestMultimap(freq4)));
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
  // Expected results frequencies in CAU, AFA, API, HIS, NAM order
  private static Stream<Arguments> testDRDQHaplotypeFrequency() {
    double[] freq1 = {
      0.03364, 0.03741, 0.00295, 0.03757, 0.01847, 0.02507, 0.01206, 0.02784, 0.02339, 0.02771
    };
    double[] freq2 = {
      0.01961, 0.04557, 0.01512, 0.05172, 0.03671, 0.05492, 0.03637, 0.06337, 0.01776, 0.04735
    };
    double[] freq3 = {
      0.04557, 0.09595, 0.01512, 0.09746, 0.03671, 0.07397, 0.06337, 0.09536, 0.04735, 0.07928
    };
    double[] freq4 = {
      0.04557, 0.08444, 0.01512, 0.04014, 0.02642, 0.03671, 0.04482, 0.06337, 0.04735, 0.06747
    };

    return Stream.of(
        Arguments.of(Test_File1, HaplotypeTestingUtils.createTestMultimap(freq1)),
        Arguments.of(Test_File2, HaplotypeTestingUtils.createTestMultimap(freq2)),
        Arguments.of(Test_File3, HaplotypeTestingUtils.createTestMultimap(freq3)),
        Arguments.of(Test_File4, HaplotypeTestingUtils.createTestMultimap(freq4)));
  }

  /**
   * @param fileName String file name for test file being passed to createModel
   * @param expectedDRDQMultimap a multimap of RaceGroup ethnicities with BigDecimal expected
   *     frequencies
   */
  @DisplayName("Haplotype DRDQ Frequency parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testDRDQHaplotypeFrequency")
  public void XMLSureTyperParserTest_DRDQHaplotypeFrequency(
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

  /**
   * @param input String of the file name in the resources directory being used to create the model
   * @return {@link ValidationModel}
   */
  private ValidationModel createModel(String input) {
    ValidationModelBuilder builder = new ValidationModelBuilder();
    builder.source(input);
    try {
      File file = new File(getClass().getClassLoader().getResource(input).getFile());
      try (FileInputStream xmlStream = new FileInputStream(file)) {
        Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
        XmlSureTyperParser.buildModelFromXML(builder, parsed);
      } catch (IOException e) {
        throw new IllegalStateException("Invalid XML file: " + file);
      }
    } catch (Exception e) {
      System.err.println("Missing resource file:  " + input);
      throw new RuntimeException(e);
    }
    ValidationModel model = builder.build();
    return model;
  }
}
