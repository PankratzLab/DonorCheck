package org.pankratzlab.unet.parser.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.util.PdfSureTyperParser;

public class PdfSureTyperParserTest {
  private static final String Test_File1 = "UnitTestPDFTyper.pdf";
  private static final String Test_File2 = "UnitTestPDFTyper_2.pdf";
  private static final String Test_File3 = "UnitTestPDFTyper_3.pdf";
  private static final String Test_File4 = "UnitTestPDFTyper_4.pdf";
  private static final String Test_File5 = "UnitTestPDFTyper_5.pdf";

  @DisplayName("Donor ID parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({Test_File1 + ", AFJQ146", Test_File2 + ", AFLE337", Test_File3 + ", AFK3449",
      Test_File4 + ", AFK3166", Test_File5 + ", AGEZ412"})
  public void twoDRB3_PdfSureTyperTest_getDonorId(String fileName, String donorId) {
    assertEquals(donorId, createModel(fileName).getDonorId());
  }

  // create the parameters to be used as a method source for the allele parser to run through
  private static Stream<Arguments> testGetABC() {
    return Stream.of(
        Arguments.of(Test_File1, new SeroType("A", 30), new SeroType("A", 36),
            new SeroType("B", 07), new SeroType("B", 49), new SeroType("C", 07), null),
        Arguments.of(Test_File2, new SeroType("A", 2), new SeroType("A", 33), new SeroType("B", 45),
            new SeroType("B", 53), new SeroType("C", 04), new SeroType("C", 16)),
        Arguments.of(Test_File3, new SeroType("A", 2), new SeroType("A", 3), new SeroType("B", 57),
            new SeroType("B", 65), new SeroType("C", 06), new SeroType("C", 8)),
        Arguments.of(Test_File4, new SeroType("A", 3), new SeroType("A", 24), new SeroType("B", 27),
            new SeroType("B", 35), new SeroType("C", 02), new SeroType("C", 04)),
    Arguments.of(Test_File5, new SeroType("A", 2), new SeroType("A", 24), new SeroType("B", 18),
            new SeroType("B", 35), new SeroType("C", 04), new SeroType("C", 07)));
  }

  @DisplayName("Allele A, B and C parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testGetABC")
  public void PdfSureTyperTest_getABC(String fileName, SeroType A1, SeroType A2, SeroType B1,
      SeroType B2, SeroType C1, SeroType C2) {
    ValidationModel model = createModel(fileName);
    assertEquals(A1, model.getA1());
    assertEquals(A2, model.getA2());
    assertEquals(B1, model.getB1());
    assertEquals(B2, model.getB2());
    assertEquals(C1, model.getC1());
    assertEquals(C2, model.getC2());
  }


  @DisplayName("Bw4 and Bw6 parsing")
  @ParameterizedTest(name = "{0}")
  @CsvSource({Test_File1 + ", Positive, Positive", Test_File2 + ", Positive, Positive",
      Test_File3 + ", Positive, Positive", Test_File4 + ", Negative, Positive", Test_File5 + ", Negative, Positive"})
  public void PdfSureTyperTest_isBw(String fileName, String Bw4Result, String Bw6Result) {
    ValidationModel model = createModel(fileName);
    assertEquals(Bw4Result, model.isBw4());
    assertEquals(Bw6Result, model.isBw6());
  }

  // create parameters to be used as a method source for checking the DRB345 parser
  private static Stream<Arguments> testGetDRB() {
    return Stream.of(Arguments.of(Test_File1, null, null, new HLAType("DRB3", 2), null, null, null),
        Arguments.of(Test_File2, new HLAType("DRB5", 1), null, new HLAType("DRB3", 2), null, null,
            null),
        Arguments.of(Test_File3, null, null, new HLAType("DRB3", 3), null, null, null),
        Arguments.of(Test_File4, null, null, null, null, new HLAType("DRB4", 1),
            new HLAType("DRB4", 1)),
    Arguments.of(Test_File5, null, null, null, null, new HLAType("DRB4", 1),
            new HLAType("DRB4", 1)));
  }

  @DisplayName("DRB345 parsing")
  @ParameterizedTest(name = "{0}")
  @MethodSource("testGetDRB")
  public void PdfSureTyperTest_getDRB(String fileName, HLAType DR51_1, HLAType DR51_2,
      HLAType DR52_1, HLAType DR52_2, HLAType DR53_1, HLAType DR53_2) {
    ValidationModel model = createModel(fileName);
    assertEquals(DR51_1, model.getDR51_1());
    assertEquals(DR51_2, model.getDR51_2());
    assertEquals(DR52_1, model.getDR52_1());
    assertEquals(DR52_2, model.getDR52_2());
    assertEquals(DR53_1, model.getDR53_1());
    assertEquals(DR53_2, model.getDR53_2());
  }

  private ValidationModel createModel(String input) {
    ValidationModelBuilder builder = new ValidationModelBuilder();
    builder.source(input);
    File file = new File(getClass().getClassLoader().getResource(input).getFile());
    try (PDDocument pdf = PDDocument.load(file)) {
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
}
