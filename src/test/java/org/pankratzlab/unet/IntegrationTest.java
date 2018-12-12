package org.pankratzlab.unet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.jorphan.logging.LoggingManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.parser.HtmlDonorParser;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.google.common.collect.ImmutableList;

/**
 * Runs an integration test. To use:
 * <ol>
 * <li>Specify a path with the {@link #TEST_DIR_PROPERTY}</li>
 * <li>In the test directory, add one or more folders - one per patient</li>
 * <li>In each individual patient's directory, add two or more model source files</li>
 * </ol>
 * This test will then ensure all model files pass validation against each other.
 */
@RunWith(Parameterized.class)
public class IntegrationTest {

  private static final String SURETYPER_PDF_FILE = "typer.pdf";
  private static final String SCORE6_XML_FILE = "score6.xml";
  private static final String DONORNET_XML_FILE = "donornet.xml";
  private static final String DONORNET_HTML_FILE = "DonorEdit.html";
  private static final String DONORNET_HTML_DIR_SUFFIX = "_files";
  private static final String TEST_DIR_PROPERTY = "hla.integration.test.dir";
  private final File individualDir;
  private final String individualName;

  public IntegrationTest(File individualDirectory) {
    if (individualDirectory.exists() && individualDirectory.isDirectory()) {
      individualDir = individualDirectory;
      individualName = individualDir.getName();
    } else {
      individualDir = null;
      individualName = "";
      LoggingManager.getLoggerForClass()
          .info("Not a valid individual directory: " + individualDir.getAbsolutePath());
    }
  }

  @Test
  public void integrationTest() {
    if (Objects.nonNull(individualDir)) {
      // An individual's test directory should contain 2 or more model files that we can parse and
      // compare to each other.
      List<ValidationModel> individualModels = parseIndividualFiles(individualDir);
      if (individualModels.isEmpty()) {
        Assert.fail(individualName + ": Can't validate individual. No model files found.");
      }
      if (individualModels.size() == 1) {
        Assert.fail(individualName + ": Can't validate individual - only found one model file.");
      }
      testIfModelsAgree(individualModels);
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
      Assert.assertTrue(
          "Validation failed:\n" + base.toString() + "\n--was not equal to--\n" + test.toString(),
          table.isValidProperty().get());
    }
  }

  /**
   * Scan the given directory for all donor file sources. Parse them to {@link ValidationModel}s and
   * return the results
   */
  private List<ValidationModel> parseIndividualFiles(File individualDir) {
    List<ValidationModel> models = new ArrayList<>();

    for (File individualFile : individualDir.listFiles()) {
      ValidationModelBuilder builder = new ValidationModelBuilder();
      String individualFileName = individualFile.getName().toLowerCase();

      if (individualFile.isDirectory() && individualFileName.endsWith(DONORNET_HTML_DIR_SUFFIX)) {
        File donorNetHtmlFile =
            new File(individualFile.getAbsolutePath() + File.separator + DONORNET_HTML_FILE);
        if (donorNetHtmlFile.exists()) {
          new HtmlDonorParser().parseModel(builder, donorNetHtmlFile);
          models.add(builder.build());
        }
      } else if (individualFileName.endsWith(DONORNET_XML_FILE)
          || individualFileName.endsWith(SCORE6_XML_FILE)) {
        new XmlDonorParser().parseModel(builder, individualFile);
        models.add(builder.build());
      } else if (individualFileName.endsWith(SURETYPER_PDF_FILE)) {
        new PdfDonorParser().parseModel(builder, individualFile);
        models.add(builder.build());
      }
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
      LoggingManager.getLoggerForClass()
          .info("Not a valid test directory: " + dir.getAbsolutePath());
      return ImmutableList.of();
    }
  }
}
