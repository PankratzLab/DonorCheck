package org.pankratzlab.unet.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.ValidationResult;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import org.pankratzlab.unet.parser.HtmlDonorParser;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.collect.ImmutableList;

/**
 * Runs an integration test. To use:
 *
 * <ol>
 * <li>Specify a path with the {@link #TEST_DIR_PROPERTY}
 * <li>In the test directory, add one or more folders - one per patient
 * <li>In each individual patient's directory, add two or more model source files
 * </ol>
 *
 * This test will then ensure all model files pass validation against each other.
 */
@RunWith(Parameterized.class)
public class IntegrationTest {

  private static final String REMAP_FILE = "remap.xml";
  private static final String SURETYPER_PDF_FILE = "typer.pdf";
  private static final String SCORE6_XML_FILE = "score6.xml";
  private static final String DONORNET_XML_FILE = "donornet.xml";
  private static final String DONORNET_HTML_FILE = "DonorEdit.html";
  private static final String DONORNET_HTML_DIR_SUFFIX = "_files";
  private static final String TEST_DIR_PROPERTY = "hla.integration.test.dir";
  private File individualDir;

  public IntegrationTest(File individualDirectory) {
    individualDir = individualDirectory;
  }

  @Before
  public void setup() {
    assumeTrue(Objects.nonNull(System.getProperty(TEST_DIR_PROPERTY)));
    assumeTrue(Objects.nonNull(individualDir));

    CommonWellDocumented.loadCIWD300();
  }

  @Test
  public void validateInput() {
    assertTrue("Validation failed - no directory provided.", Objects.nonNull(individualDir));
    String individualName = individualDir.getName();

    assertTrue(
        individualName + ": Can't validate individual. Directory doesn't exist:" + individualDir,
        individualDir.exists());

    assertTrue(
        individualName + ": Can't validate individual. Input not a directory: " + individualDir,
        individualDir.isDirectory());
  }

  @Test
  public void integrationTest() {
    assumeNotNull(individualDir);
    assumeTrue(individualDir.exists());
    assumeTrue(individualDir.isDirectory());

    String individualName = individualDir.getName();

    try {
      List<ValidationModel> individualModels = parseIndividualFiles(individualDir);

      assertFalse(individualName + ": Can't validate individual. No model files found.",
          individualModels.isEmpty());

      assertFalse(individualName + ": Can't validate individual - only found one model file.",
          individualModels.size() == 1);

      testIfModelsAgree(individualModels);
    } catch (Throwable e) {
      e.printStackTrace(System.err);
      fail(individualName + ": failed with exception " + e.toString(), e);
    }
  }

  /**
   * Compare all models in the given list. If a {@link VlaidationTable} would fail with a given
   * pairing, the test for this individual fails.
   */
  private void testIfModelsAgree(List<ValidationModel> individualModels) {
    ValidationModel base = individualModels.get(0);
    ValidationTable table = new ValidationTable();
    table.setFirstModel(base);
    for (int index = 1; index < individualModels.size(); index++) {
      ValidationModel test = individualModels.get(index);
      table.setSecondModel(test);
      table.getValidationRows();
      assertTrue(
          "Validation failed:\n" + base.toString() + "\n--was not equal to--\n" + test.toString(),
          table.isValidProperty().get());
    }
  }

  /**
   * Scan the given directory for all donor file sources. Parse them to {@link ValidationModel}s and
   * return the results
   */
  private List<ValidationModel> parseIndividualFiles(File individualDir) {
    List<ValidationModelBuilder> modelBuilders = new ArrayList<>();
    XMLRemapProcessor remapProcessor = null;

    for (File individualFile : individualDir.listFiles()) {
      ValidationModelBuilder builder = new ValidationModelBuilder();
      String individualFileName = individualFile.getName().toLowerCase();

      if (individualFile.isDirectory() && individualFileName.endsWith(DONORNET_HTML_DIR_SUFFIX)) {
        File donorNetHtmlFile =
            new File(individualFile.getAbsolutePath() + File.separator + DONORNET_HTML_FILE);
        if (donorNetHtmlFile.exists()) {
          new HtmlDonorParser().parseModel(builder, donorNetHtmlFile);
        }
      } else if (individualFileName.endsWith(DONORNET_HTML_FILE.toLowerCase())) {
        new HtmlDonorParser().parseModel(builder, individualFile);
      } else if (individualFileName.endsWith(DONORNET_XML_FILE)
          || individualFileName.endsWith(SCORE6_XML_FILE)) {
        new XmlDonorParser().parseModel(builder, individualFile);
      } else if (individualFileName.endsWith(SURETYPER_PDF_FILE)) {
        new PdfDonorParser().parseModel(builder, individualFile);
      } else if (individualFileName.endsWith(REMAP_FILE)) {
        // construct RemapProcessor from XML remap file
        remapProcessor = new XMLRemapProcessor(individualFile.getAbsolutePath());
        // no model to be built from this file, so continue;
        continue;
      } else {
        // skip any other files
        continue;
      }
      modelBuilders.add(builder);
    }

    List<ValidationModel> models = new ArrayList<>();
    for (ValidationModelBuilder builder : modelBuilders) {

      if (builder.hasCorrections()) {
        // assertion check if builder *should* have corrections
        RemapProcessor remapper;
        if (remapProcessor != null) {
          assertTrue(remapProcessor.hasRemappings(builder.getSourceType()));
          remapper = remapProcessor;
        } else {
          remapper = new XMLRemapProcessor.NoRemapProcessor();
        }

        ValidationResult result = builder.processCorrections(remapper);
        assertTrue(result.valid);
        assertFalse(result.validationMessage.isPresent());
      } else {
        assertTrue(
            remapProcessor == null || !remapProcessor.hasRemappings(builder.getSourceType()));
      }

      models.add(builder.build());
    }

    return models;
  }

  @Parameterized.Parameters
  public static Collection<File[]> getDirs() {
    File dir = new File(System.getProperty(TEST_DIR_PROPERTY, ""));
    if (dir.exists() && dir.isDirectory()) {
      // The test directory is a test root, where child directories correspond to individuals.
      // Each individual should be tested individually
      List<File[]> individuals = new ArrayList<>();
      for (File individualFile : dir.listFiles()) {
        individuals.add(new File[] {individualFile});
      }
      return ImmutableList.copyOf(individuals);
    } else {
      System.err.println("Not a valid test directory: " + dir.getAbsolutePath());
      return ImmutableList.of();
    }
  }
}
