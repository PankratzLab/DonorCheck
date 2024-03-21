package org.pankratzlab.unet.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.XmlDonorNetParser;

public class XMLDonorParserTest {
  // test files located in test/resources
  private static final String Test_File1 = "UnitTestXMLDonorNet.xml";
  private static final String Test_File2 = "UnitTestXMLDonorNet_2.xml";
  private static final String Test_File3 = "UnitTestXMLDonorNet_3.xml";
  private static final String Test_File4 = "UnitTestXMLDonorNet_4.xml";

  // create parameters to be used as CSV source for checking Donor ID parsing
  @DisplayName("Donor ID parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({Test_File1 + ", AFK1128", Test_File2 + ", AFK3166", Test_File3 + ", AFK3387",
      Test_File4 + ", AFLK097"})
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param donorId expected Donor ID for each respective donor file
   */
  public void twoDRB3_XMLDonorParserTest_getDonorId(String fileName, String donorId) {
    assertEquals(donorId, createModel(fileName).getDonorId());
  }

  // create the parameters to be used as a method source for the allele parser to run through
  private static Stream<Arguments> testGetABC() {
    return Stream.of(
        Arguments.of(Test_File1, new SeroType("A", 11), new SeroType("A", 23),
            new SeroType("B", 35), new SeroType("B", 44), new SeroType("C", 04), null),
        Arguments.of(Test_File2, new SeroType("A", 3), new SeroType("A", 24), new SeroType("B", 27),
            new SeroType("B", 35), new SeroType("C", 2), new SeroType("C", 4)),
        Arguments.of(Test_File3, new SeroType("A", 1), new SeroType("A", 2), new SeroType("B", 27),
            new SeroType("B", 44), new SeroType("C", 2), null),
        Arguments.of(Test_File4, new SeroType("A", 2), new SeroType("A", 29), new SeroType("B", 44),
            new SeroType("B", 51), new SeroType("C", 15), new SeroType("C", 16)));
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
  public void XMLDonorParserTest_getABC(String fileName, SeroType A1, SeroType A2, SeroType B1,
      SeroType B2, SeroType C1, SeroType C2) {
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
  @CsvSource({Test_File1 + ", Positive, Positive", Test_File2 + ", Negative, Positive",
      Test_File3 + ", Positive, Negative", Test_File4 + ", Positive, Negative"})
  /**
   * @param fileName String file name for test file being passed to createModel
   * @param Bw4Result expected Bw4 result(Positive or Negative)
   * @param Bw6Result expected Bw6 result(Positive or Negative)
   */
  public void XMLDonorParserTest_isBw(String fileName, String Bw4Result, String Bw6Result) {
    ValidationModel model = createModel(fileName);
    assertEquals(Bw4Result, model.isBw4());
    assertEquals(Bw6Result, model.isBw6());
  }

  // create parameters to be used as a method source for checking the DRB345 parser
  private static Stream<Arguments> testGetDRB() {
    return Stream.of(Arguments.of(Test_File1, null, null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(Test_File2, null, null, null, null, new HLAType("DRB4", 1),
            new HLAType("DRB4", 1)),
        Arguments.of(Test_File3, null, null, null, null, null, null), Arguments.of(Test_File4, null,
            null, null, null, new HLAType("DRB4", 1), new HLAType("DRB4", 1)));
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
  public void XMLDonorParserTest_getDRB(String fileName, HLAType DR51_1, HLAType DR51_2,
      HLAType DR52_1, HLAType DR52_2, HLAType DR53_1, HLAType DR53_2) {
    ValidationModel model = createModel(fileName);
    assertEquals(DR51_1, model.getDR51_1());
    assertEquals(DR51_2, model.getDR51_2());
    assertEquals(DR52_1, model.getDR52_1());
    assertEquals(DR52_2, model.getDR52_2());
    assertEquals(DR53_1, model.getDR53_1());
    assertEquals(DR53_2, model.getDR53_2());
  }

  /**
   * @param input String of the file name in the resources directory being used to create the model
   * @return {@link ValidationModel}
   */
  private ValidationModel createModel(String input) {
    ValidationModelBuilder builder = new ValidationModelBuilder();
    builder.source(input);
    try (InputStream xmlStream = getClass().getClassLoader().getResourceAsStream(input)) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      XmlDonorNetParser.buildModelFromXML(builder, parsed);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("Invalid XML file: " + input);
    }
    ValidationModel model = null;
    model = builder.build();
    return model;
  }
}
