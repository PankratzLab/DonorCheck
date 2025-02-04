package org.pankratzlab.unet.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.hla.Info;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.ValidationResult;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import org.pankratzlab.unet.validation.TestExceptions.SourceFileParsingException;
import org.pankratzlab.unet.validation.TestExceptions.XMLRemapFileException;
import org.pankratzlab.unet.validation.ValidationTestFileSet.ValidationTestFileSetBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class ValidationTesting {

  private static final String REMAP_XML = "remap.xml";
  private static final String TEST_PROPERTIES = "test.properties";
  private static final String TEST_LOG = "test.log";

  public static final String REL_DIRECTORY = Info.HLA_HOME + "rel_dna_ser_files/";
  public static final String VALIDATION_DIRECTORY =
      new File(Info.HLA_HOME + "validation/").getAbsolutePath() + File.separator;

  private static final String CWD_PROP = "cwd";
  private static final String REL = "rel";
  private static final String COMMENT_PROP = "comment";
  private static final String EXPECTED_PROP = "expected";
  private static final String DC_VER_PROP = "version";
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static List<ValidationTestFileSet> sortTests(List<ValidationTestFileSet> tests) {
    return tests.stream().sorted((t1, t2) -> {
      int c = t1.cwdSource.get().compareTo(t2.cwdSource.get());
      if (c != 0)
        return c;
      return t1.relDnaSerFile.get().compareTo(t2.relDnaSerFile.get());
    }).collect(Collectors.toList());
  }

  public static boolean addToValidationSet(TestInfo file1, TestInfo file2) {
    // Tests can only be added from the validation results screen, which
    // means that we know the test set has exactly two files, and therefore
    // we don't have to check that the ID's match (if they don't match, the result
    // will be a failure)

    File valDir = new File(VALIDATION_DIRECTORY);
    if (!valDir.exists()) {
      valDir.mkdir();
    }

    String reldir = REL_DIRECTORY;
    String subdir = getTestDirectory(file1.label);

    final File testDir = new File(subdir);

    // first check if test already exists
    if (testDir.exists()) {
      AlertHelper.showMessage_TestAlreadyExists(file1, subdir);
      return false;
    }

    // show dialog with warning about avoiding PII
    Optional<ButtonType> selVal = AlertHelper.showMessage_PII();
    if (selVal.isPresent() && selVal.get() == ButtonType.CANCEL) {
      return false;
    }

    testDir.mkdirs();

    try {
      Files.copy(new File(file1.file), new File(subdir + new File(file1.file).getName()));
    } catch (Exception e) {
      AlertHelper.showMessage_ErrorCopyingInputFile(file1, subdir);
      e.printStackTrace(); // TODO log exceptions appropriately;
      // cleanup sub-dir before exiting
      deleteTestDirectory(subdir);
      return false;
    }

    try {
      Files.copy(new File(file2.file), new File(subdir + new File(file2.file).getName()));
    } catch (Exception e) {
      AlertHelper.showMessage_ErrorCopyingInputFile(file2, subdir);
      e.printStackTrace(); // TODO log exceptions appropriately;
      // cleanup sub-dir before exiting
      deleteTestDirectory(subdir);
      return false;
    }

    File relDirFile = new File(reldir);
    if (!relDirFile.exists()) {
      relDirFile.mkdir();
    }

    String relDnaSerFile = HLAProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);
    String newRelFileName;
    String newRelDnaSerFile;
    if (!Strings.isNullOrEmpty(relDnaSerFile) && new File(relDnaSerFile).exists()) {
      File file = new File(relDnaSerFile);
      String fileName = file.getName();
      String version = AntigenDictionary.getVersion(file.getAbsolutePath()).trim();

      newRelFileName = version + "_" + fileName;

      // make sure new filename is filename-safe
      newRelFileName = safeFilename(newRelFileName);

      newRelDnaSerFile = reldir + newRelFileName;

      // copy file to testDir
      File newRelFile = new File(newRelDnaSerFile);
      if (!newRelFile.exists()) {
        try {
          Files.copy(file, newRelFile);
        } catch (Exception e) {
          AlertHelper.showMessage_ErrorCopyingRelFile(newRelFileName);
          e.printStackTrace(); // TODO log exceptions appropriately;
          // cleanup sub-dir before exiting
          deleteTestDirectory(subdir);
          return false;
        }
      }

    } else {
      String fileName = AntigenDictionary.MASTER_MAP_RECORDS;
      String bundledVersion = AntigenDictionary.getBundledVersion();

      newRelFileName = bundledVersion + "_" + fileName;
      // make sure new filename is platform-safe
      newRelFileName = safeFilename(newRelFileName);

      newRelDnaSerFile = reldir + newRelFileName;

      // Copy internal file to testDir
      File newRelFile = new File(reldir + newRelFileName);
      if (!newRelFile.exists()) {
        try (InputStream is = AntigenDictionary.class.getClassLoader()
            .getResourceAsStream(AntigenDictionary.MASTER_MAP_RECORDS)) {
          java.nio.file.Files.copy(is, newRelFile.toPath());
        } catch (Exception e) {
          AlertHelper.showMessage_ErrorCopyingRelFile(newRelFileName);
          e.printStackTrace();
          // cleanup sub-dir before exiting
          deleteTestDirectory(subdir);
          return false;
        }
      }
    }

    if (!file1.remappings.isEmpty() || !file2.remappings.isEmpty()) {

      Document doc = Jsoup.parse("", "", Parser.xmlParser());
      // Set the output settings to XML syntax
      doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

      // Create the root element <remappings>
      Element remappings = doc.appendElement("remappings");

      if (!file1.remappings.isEmpty()) {
        addXMLInfo(remappings, file1);
      }

      if (!file2.remappings.isEmpty()) {
        addXMLInfo(remappings, file2);
      }

      // create remapping file
      try (FileWriter writer = new FileWriter(subdir + REMAP_XML)) {
        writer.write(doc.outerHtml());
      } catch (IOException e) {
        AlertHelper.showMessage_ErrorWritingRemapXMLFile(newRelFileName);
        e.printStackTrace(); // TODO log exceptions appropriately;
        // cleanup sub-dir before exiting
        deleteTestDirectory(subdir);
        return false;
      }
    }

    // write test properties file containing CWD type and rel_dna_ser filename
    Properties props = new Properties();
    props.setProperty(CWD_PROP, CommonWellDocumented.loadPropertyCWDSource().name());
    props.setProperty(REL, newRelFileName);
    props.setProperty(COMMENT_PROP, "");
    props.setProperty(DC_VER_PROP, Info.getVersion());
    try (FileOutputStream fos = new FileOutputStream(new File(subdir + TEST_PROPERTIES))) {
      props.store(fos, null);
    } catch (Exception e) {
      AlertHelper.showMessage_ErrorWritingPropertiesFile(subdir + TEST_PROPERTIES);
      e.printStackTrace(); // TODO log exceptions appropriately;
      // cleanup sub-dir before exiting
      deleteTestDirectory(subdir);
      return false;
    }

    return true;
  }

  public static void runTests(List<ValidationTestFileSet> tests) {
    List<ValidationTestFileSet> sorted = ValidationTesting.sortTests(tests);

    Set<ValidationTestFileSet> successfulTests = Collections.synchronizedSet(new HashSet<>());
    Set<ValidationTestFileSet> failedTests = Collections.synchronizedSet(new HashSet<>());
    Map<ValidationTestFileSet, Exception> exceptionTests = new ConcurrentHashMap<>();
    List<Runnable> tasks = sorted.stream().map(t -> new Runnable() {
      @Override
      public void run() {
        try {
          TestRun result = ValidationTesting.runTest(t);
          if (result.error.isPresent()) {
            exceptionTests.put(t, result.error.get());
          } else {
            if (result.result.isPassing) {
              successfulTests.add(t);
            } else {
              failedTests.add(t);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          exceptionTests.put(t, e);
        }
      }
    }).collect(Collectors.toList());

    Task<Void> runValidationTask = JFXUtilHelper.createProgressTask(tasks);


    EventHandler<WorkerStateEvent> showResults = e -> {
      String contentText = "Test Results:\n\nPassing: " + successfulTests.size() + "\n\nFailing: "
          + failedTests.size() + "\n\nExceptions: " + exceptionTests.size()
          + "\n\n(Please report any exceptions to the developers)";
      if (!exceptionTests.isEmpty()) {
        for (Exception e1 : exceptionTests.values()) {
          contentText += "\n\n" + e1.getClass().getSimpleName() + ": " + e1.getMessage();
        }
      }
      Alert alert1 = new Alert(AlertType.INFORMATION, contentText, ButtonType.CLOSE);
      alert1.setTitle("Test results");
      alert1.setHeaderText("");
      alert1.showAndWait();
    };

    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, showResults);
    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, showResults);

    new Thread(runValidationTask).start();
  }

  public static void updateTestResults(Map<ValidationTestFileSet, TestRun> newResults) {
    for (Entry<ValidationTestFileSet, TestRun> entry : newResults.entrySet()) {
      String subdir = getTestDirectory(entry.getKey());

      entry.getKey().lastRunDate.set(entry.getValue().runTime);
      entry.getKey().lastTestResult.set(entry.getValue().result);
      entry.getKey().donorCheckVersion.set(Info.getVersion());

      StringJoiner sj = new StringJoiner("\t");
      sj.add(DATE_FORMAT.format(entry.getValue().runTime));
      sj.add(Boolean.toString(entry.getValue().result.isPassing));
      sj.add(entry.getValue().result.name());
      sj.add(Info.getVersion());

      String logStr = sj.toString() + System.lineSeparator();

      try {
        java.nio.file.Files.writeString(Path.of(subdir, TEST_LOG), logStr,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
        // TODO handle exceptions appropriately;
        // notify user of inability to write properties file, and cleanup appropriately
        e.printStackTrace();
      }
    }

  }

  /**
   * Will create a test.properties file if it doesn't exist and set the CWD and REL
   * properties.<br />
   * <br />
   * These properties are otherwise unchanged!<br />
   * <br />
   * The *only* property that this method currently modifies is the COMMENT property.
   * 
   * @param test
   */
  public static void updateTestProperties(ValidationTestFileSet test) {
    String subdir = getTestDirectory(test);
    File testPropertiesFile = new File(subdir + TEST_PROPERTIES);
    Properties props = new Properties();

    if (testPropertiesFile.exists()) {
      try (FileInputStream fis = new FileInputStream(testPropertiesFile)) {
        props.load(fis);
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalStateException("Failed to load properties from " + testPropertiesFile);
      }
    } else {
      props.setProperty(CWD_PROP, test.cwdSource.get().name());
      String relName = test.relDnaSerFile.get();
      if (relName.startsWith(REL_DIRECTORY)) {
        relName = relName.substring(REL_DIRECTORY.length());
      }
      props.setProperty(REL, relName);
    }

    String prevComment = Objects.toString(props.setProperty(COMMENT_PROP, test.comment.get()), "");
    String prevVersion =
        Objects.toString(props.setProperty(DC_VER_PROP, test.donorCheckVersion.getValueSafe()), "");

    String prevExpectStr =
        Objects.toString(props.setProperty(EXPECTED_PROP, test.expectedResult.get().name()));

    TEST_EXPECTATION prevExpect = TEST_EXPECTATION.Pass;
    if (prevExpectStr != null && !prevExpectStr.isEmpty()) {
      try {
        prevExpect = TEST_EXPECTATION.valueOf(prevExpectStr);
      } catch (IllegalArgumentException e) {
        // ignore
      }
    }

    try (FileOutputStream fos = new FileOutputStream(new File(subdir + TEST_PROPERTIES))) {
      props.store(fos, null);
    } catch (Exception e) {
      test.comment.set(prevComment);
      test.donorCheckVersion.setValue(prevVersion);
      test.expectedResult.setValue(prevExpect);
      e.printStackTrace();
      throw new IllegalStateException("Failed to save properties from " + testPropertiesFile);
    }
  }

  public static String getTestDirectory(ValidationTestFileSet test) {
    final String testId = test.id.get();
    return getTestDirectory(testId);
  }

  public static String getTestDirectory(final String testId) {
    return VALIDATION_DIRECTORY + safeFilename(testId) + "/";
  }

  public static TestLoadingResults loadValidationDirectory() {
    Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets =
        HashBasedTable.create();

    List<String> invalidTests = new ArrayList<>();
    File dir = new File(VALIDATION_DIRECTORY);
    if (dir.exists() && dir.isDirectory()) {
      // The test directory is a test root, where child directories correspond to individuals.
      // Each individual should be tested individually
      for (File individualFile : dir.listFiles()) {
        if (individualFile.isDirectory()) {

          ValidationTestFileSet testSet;
          try {
            testSet = loadIndividualTest(individualFile);
          } catch (Exception e) {
            // catch exceptions and skip loading test, but will report later
            invalidTests.add(individualFile.getName());
            continue;
          }

          addTestSetToTable(testSets, testSet);
        }
      }
    }

    return new TestLoadingResults(testSets, invalidTests);
  }

  private static ValidationTestFileSet loadIndividualTest(File individualFile) {
    ValidationTestFileSetBuilder builder = ValidationTestFileSet.builder();
    builder.id(individualFile.getName());

    loadTestMetadata(individualFile, builder);
    loadTestLastRunLog(individualFile, builder);

    String remapFile = null;
    Builder<String> inputFilesBuilder = ImmutableList.builder();
    for (File testFile : individualFile.listFiles()) {
      if (testFile.getName().equals(REMAP_XML)) {
        remapFile = testFile.getAbsolutePath();
      } else if (testFile.getName().equals(TEST_PROPERTIES)) {
        // skip
      } else if (testFile.getName().equals(TEST_LOG)) {
        // skip
      } else {
        inputFilesBuilder.add(testFile.getAbsolutePath());
      }
    }
    builder.filePaths(inputFilesBuilder.build());
    builder.remapFile(remapFile);

    // Tests can only be added from the validation results screen, which
    // means that we know the test set has exactly two files, and therefore
    // we don't have to check the number of files present

    return builder.build();
  }

  public static Map<ValidationTestFileSet, Boolean> deleteTests(
      List<ValidationTestFileSet> toRemove) {
    Map<ValidationTestFileSet, Boolean> success = new HashMap<>();
    for (ValidationTestFileSet testSet : toRemove) {
      String dir = getTestDirectory(testSet);
      boolean successDel = deleteTestDirectory(dir);
      success.put(testSet, successDel);
    }
    return success;
  }

  private static TestRun runTest(ValidationTestFileSet test) {
    SOURCE current = CommonWellDocumented.loadPropertyCWDSource();
    String currentRel = HLAProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);

    SOURCE source = test.cwdSource.get();
    String rel = test.relDnaSerFile.get();
    boolean changedCWID = false;
    boolean changedRel = false;

    if (current != source || !CommonWellDocumented.isLoaded()) {
      CommonWellDocumented.loadCIWDVersion(source);
      changedCWID = true;
    }
    if (!new File(currentRel).equals(new File(rel))) {
      HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, rel);
      AntigenDictionary.clearCache();
      changedRel = true;
    }

    TestRun returnVal;
    try {
      TEST_RESULT result = runTestInternal(test);
      returnVal = new TestRun(test, result, new Date(), Optional.empty());
    } catch (XMLRemapFileException e) {
      returnVal = new TestRun(test, TEST_RESULT.INVALID_REMAP_FILE, new Date(), Optional.of(e));
    } catch (SourceFileParsingException e) {
      returnVal = new TestRun(test, TEST_RESULT.INVALID_TEST_FILE, new Date(), Optional.of(e));
    }

    if (changedCWID) {
      CommonWellDocumented.loadCIWDVersion(current);
    }
    if (changedRel) {
      HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, currentRel);
      AntigenDictionary.clearCache();
    }

    updateTestResults(Map.of(test, returnVal));

    return returnVal;
  }

  private static TEST_RESULT runTestInternal(ValidationTestFileSet test) {
    List<ValidationModelBuilder> modelBuilders = new ArrayList<>();
    XMLRemapProcessor remapProcessor = null;

    if (test.filePaths.size() == 1) {
      return TEST_RESULT.NOT_ENOUGH_TEST_FILES;
    }

    for (String filePath : test.filePaths) {
      ValidationModelBuilder builder = new ValidationModelBuilder();
      File file = new File(filePath);

      try {
        SourceType.parseFile(builder, file);
      } catch (Exception e) {
        throw new TestExceptions.SourceFileParsingException(
            "Error parsing file: " + file.getAbsolutePath(), e);
      }

      ValidationResult validationResult = builder.validate(false);
      if (!validationResult.valid) {
        return TEST_RESULT.INVALID_TEST_FILE;
      }

      modelBuilders.add(builder);
    }

    if (test.remapFile != null && test.remapFile.get() != null) {

      // check to make sure remap file exists
      if (!new File(test.remapFile.get()).exists()) {
        return TEST_RESULT.MISSING_REMAP_FILE;
      }

      try {
        remapProcessor = new XMLRemapProcessor(test.remapFile.get());
      } catch (IllegalStateException e) {
        throw new TestExceptions.XMLRemapFileException(
            "Error reading XML remappings file: " + test.remapFile.get(), e);
      }

    }

    List<ValidationModel> models = new ArrayList<>();
    for (ValidationModelBuilder builder : modelBuilders) {

      if (builder.hasCorrections()) {
        // assertion check if builder *should* have corrections
        RemapProcessor remapper;
        if (remapProcessor != null) {

          if (!remapProcessor.hasRemappings(builder.getSourceType())) {
            // builder is expecting remappings for this file, but
            // the remapping file doesn't have the expected remappings
            return TEST_RESULT.INVALID_REMAPPINGS;
          }
          // assertTrue();
          remapper = remapProcessor;
        } else {
          remapper = new XMLRemapProcessor.NoRemapProcessor();
        }

        builder.processCorrections(remapper);
      } else {
        if (remapProcessor != null && remapProcessor.hasRemappings(builder.getSourceType())) {
          // builder says no remappings, but remap file says remappings do exist
          return TEST_RESULT.INVALID_REMAPPINGS;
        }
      }

      models.add(builder.build());
    }

    boolean valid = testIfModelsAgree(models);
    return valid ? TEST_RESULT.TEST_SUCCESS : TEST_RESULT.TEST_FAILURE;
  }

  private static boolean testIfModelsAgree(List<ValidationModel> individualModels) {
    ValidationModel base = individualModels.get(0);
    ValidationTable table = new ValidationTable();
    table.setFirstModel(base);

    // defaulting to true means a test case with only one input file is automatically valid

    // However, tests can only be added from the validation results screen, which
    // means that we know the test set has exactly two files, and therefore
    // we don't have to check the number of files present

    boolean valid = true;
    for (int index = 1; index < individualModels.size(); index++) {
      ValidationModel test = individualModels.get(index);
      table.setSecondModel(test);
      table.getValidationRows();
      valid &= table.isValidProperty().get();
    }
    return valid;
  }

  private static String safeFilename(String newRelFileName) {
    newRelFileName = newRelFileName.replaceAll("[^a-zA-Z0-9-_\\\\.]", "");
    return newRelFileName;
  }

  private static void addXMLInfo(Element remappings, TestInfo file) {
    Element remap = remappings.appendElement("remap");

    // Add <sourceType>Score6</sourceType> under <remap>
    remap.appendElement("sourceType").text(file.sourceType.name());


    file.remappings.entrySet().forEach(e -> {
      // Create <remapLocus> under <remap>
      Element remapLocus = remap.appendElement("remapLocus");
      remapLocus.appendElement("locus").text(e.getKey().name());

      // Add multiple <fromAllele> elements under <remapLocus>
      e.getValue().getLeft().stream().forEach(allele -> {
        remapLocus.appendElement("fromAllele").text(allele.getHlaType().specString());
      });

      // Add multiple <toAllele> elements under <remapLocus>
      e.getValue().getRight().stream().forEach(allele -> {
        remapLocus.appendElement("toAllele").text(allele.getHlaType().specString());
      });
    });

  }

  private static void addTestSetToTable(
      Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets,
      ValidationTestFileSet testSet) {
    if (!testSets.contains(testSet.cwdSource.get(), testSet.relDnaSerFile.get())) {
      testSets.put(testSet.cwdSource.get(), testSet.relDnaSerFile.get(), new ArrayList<>());
    }
    testSets.get(testSet.cwdSource.get(), testSet.relDnaSerFile.get()).add(testSet);
  }

  private static void loadTestMetadata(File individualFile, ValidationTestFileSetBuilder builder) {
    File testPropertiesFile = new File(individualFile, TEST_PROPERTIES);
    Properties props = new Properties();
    if (testPropertiesFile.exists()) {
      try (FileInputStream fis = new FileInputStream(testPropertiesFile)) {
        props.load(fis);
      } catch (Exception e) {
        throw new IllegalStateException(
            "Unable to load test properties file: " + testPropertiesFile.getAbsolutePath(), e);
      }
    }
    String cwdStr = props.getProperty(CWD_PROP);
    String relStr = props.getProperty(REL);
    String commentStr = props.getProperty(COMMENT_PROP, "");
    String expectationStr = props.getProperty(EXPECTED_PROP, "");

    builder.comment(commentStr);

    TEST_EXPECTATION expectation = TEST_EXPECTATION.Pass;
    if (expectationStr != null && !expectationStr.isEmpty()) {
      try {
        expectation = TEST_EXPECTATION.valueOf(expectationStr);
      } catch (Exception e) {
        //
      }
    }
    builder.expectedResult(expectation);

    CommonWellDocumented.SOURCE cwdSource = null;
    try {
      cwdSource = CommonWellDocumented.SOURCE.valueOf(cwdStr);
    } catch (Exception e) {
      // not a valid CWD Source
      throw new IllegalStateException("Invalid CWD Source property value: " + cwdStr, e);
    }
    builder.cwdSource(cwdSource);

    String relPath = REL_DIRECTORY + relStr;
    builder.relDnaSerFile(relPath);
  }

  private static void loadTestLastRunLog(File individualFile,
      ValidationTestFileSetBuilder builder) {
    File file = new File(individualFile, TEST_LOG);
    if (!file.exists())
      return;
    try (ReversedLinesFileReader reader =
        new ReversedLinesFileReader(file, Charset.defaultCharset())) {
      String lastRunLog = reader.readLine();
      String[] parts = lastRunLog.split("\t");

      Date lastRunDate = DATE_FORMAT.parse(parts[0]);
      Boolean lastPassingState = Boolean.parseBoolean(parts[1]);
      String lastPassingResultStr = parts[2];
      String lastRunDonorCheckVersionStr = (parts.length >= 4) ? parts[3] : "<unknown>";
      TEST_RESULT lastPassingResult = null;
      try {
        lastPassingResult = TEST_RESULT.valueOf(lastPassingResultStr);
      } catch (Exception e) {
        // ignore missing test result
      }

      builder.lastRunDate(lastRunDate);

      if (lastPassingResult != null) {
        builder.lastPassingResult(lastPassingResult);
      }

      builder.donorCheckVersion(lastRunDonorCheckVersionStr);

    } catch (Exception e) {
      throw new IllegalStateException("Invalid log: " + individualFile.getAbsolutePath(), e);
    }
  }

  private static boolean deleteTestDirectory(String dir) {
    boolean successDel = true;
    try {
      FileUtils.deleteDirectory(new File(dir));
    } catch (IOException | IllegalArgumentException e) {
      try {
        FileUtils.deleteDirectory(new File(dir));
      } catch (IOException | IllegalArgumentException e1) {
        successDel = false;
      }
    }
    return successDel;
  }

  public static ValidationTestFileSet updateTestID(String oldId, ValidationTestFileSet test) {
    String oldSubdir = getTestDirectory(oldId);
    String newSubdir = getTestDirectory(test);

    try {
      java.nio.file.Files.move(Path.of(oldSubdir), Path.of(newSubdir));
    } catch (IOException e) {
      test.id.set(oldId);
      throw new IllegalStateException(
          "Unable to move test directory: " + oldSubdir + " -> " + newSubdir, e);
    }

    return loadIndividualTest(new File(newSubdir));
  }

  public static class TestLoadingResults {
    public final Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets;
    public final List<String> invalidTests;

    public TestLoadingResults(Table<SOURCE, String, List<ValidationTestFileSet>> testSets,
        List<String> invalidTests) {
      this.testSets = testSets;
      this.invalidTests = invalidTests;
    }

  }

}
