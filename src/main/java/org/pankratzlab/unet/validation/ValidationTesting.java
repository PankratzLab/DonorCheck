package org.pankratzlab.unet.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAProperties;
import org.pankratzlab.unet.deprecated.hla.Info;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import org.pankratzlab.unet.model.ValidationModelBuilder.ValidationResult;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import org.pankratzlab.unet.validation.ValidationTestFileSet.ValidationTestFileSetBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.common.io.Files;

public class ValidationTesting {

  private static final String SURETYPER_PDF_FILE = "typer.pdf";
  private static final String SCORE6_XML_FILE = "score6.xml";
  private static final String DONORNET_XML_FILE = "donornet.xml";
  private static final String DONORNET_HTML_FILE = "DonorEdit.html";
  private static final String REMAP_XML = "remap.xml";
  private static final String TEST_PROPERTIES = "test.properties";

  public static final String VALIDATION_DIRECTORY = Info.HLA_HOME + "validation/";

  private static final String CWD_PROP = "cwd";
  private static final String REL = "rel";
  private static final String LAST_RUN_PROP = "last_run";
  private static final String LAST_RESULT_PROP = "last_result";
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static class TestInfo {
    public final String label;
    public final String file;
    public final ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remappings;
    public final SourceType sourceType;

    public TestInfo(String label, String file,
        ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remappings,
        SourceType sourceType) {
      this.label = label;
      this.file = file;
      this.remappings = remappings;
      this.sourceType = sourceType;
    }

  }

  public static TestRun runTest(ValidationTestFileSet test) {
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

    TEST_RESULT result = runTestInternal(test);
    TestRun returnVal = new TestRun(test, result, new Date());

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

  public static List<ValidationTestFileSet> sortTests(List<ValidationTestFileSet> tests) {
    return tests.stream().sorted((t1, t2) -> {
      int c = t1.cwdSource.get().compareTo(t2.cwdSource.get());
      if (c != 0)
        return c;
      return t1.relDnaSerFile.get().compareTo(t2.relDnaSerFile.get());
    }).collect(Collectors.toList());
  }

  public static ImmutableMap<ValidationTestFileSet, TestRun> runTests(
      List<ValidationTestFileSet> tests) {

    Map<ValidationTestFileSet, TestRun> results = new HashMap<>();

    Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> table =
        HashBasedTable.create();

    for (ValidationTestFileSet test : tests) {
      addTestSetToTable(table, test);
    }

    SOURCE current = CommonWellDocumented.loadPropertyCWDSource();
    String currentRel = HLAProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);
    boolean changedCWID = false;
    boolean changedRel = false;

    for (SOURCE source : SOURCE.values()) {
      Map<String, List<ValidationTestFileSet>> relMap = table.row(source);
      if (relMap.isEmpty()) {
        continue;
      }

      if (current != source || !CommonWellDocumented.isLoaded()) {
        CommonWellDocumented.loadCIWDVersion(source);
        changedCWID = true;
      }

      for (String rel : relMap.keySet()) {
        List<ValidationTestFileSet> testFiles = relMap.get(rel);

        if (!new File(currentRel).equals(new File(rel))) {
          HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, rel);
          AntigenDictionary.clearCache();
          changedRel = true;
        }

        for (ValidationTestFileSet test : testFiles) {
          // TODO log test run
          System.out.println("Running test: " + test.filePaths.toString());

          TEST_RESULT result = runTestInternal(test);

          // TODO log test result
          System.out.println("Result: " + result);

          results.put(test, new TestRun(test, result, new Date()));
        }
      }
    }

    if (changedCWID) {
      CommonWellDocumented.loadCIWDVersion(current);
    }
    if (changedRel) {
      HLAProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, currentRel);
      AntigenDictionary.clearCache();
    }

    updateTestResults(results);

    return ImmutableMap.copyOf(results);
  }

  public enum TEST_RESULT {
    NO_TEST_FILES(false), // test directory doesn't contain any test files'
    NOT_ENOUGH_TEST_FILES(false), // test directory only have one test file
    INVALID_TEST_FILE(false), // input file is invalid
    ERROR_LOADING_TEST_FILE(false), // error when loading file
    MISSING_REMAP_FILE(false), // file requires remappings but remap file doesn't exist
    INVALID_REMAP_FILE(false), // error when loading/parsing remap file
    INVALID_REMAPPINGS(false), // remappings in file don't match required remappings
    TEST_FAILURE(false), // validation failed
    TEST_SUCCESS(true); // validation passed

    public final boolean isPassing;

    private TEST_RESULT(boolean passes) {
      this.isPassing = passes;
    }

  }

  private static TEST_RESULT runTestInternal(ValidationTestFileSet test) {
    List<ValidationModelBuilder> modelBuilders = new ArrayList<>();
    XMLRemapProcessor remapProcessor = null;

    for (String filePath : test.filePaths) {
      ValidationModelBuilder builder = new ValidationModelBuilder();
      File file = new File(filePath);

      try {
        SourceType.parseFile(builder, file);
      } catch (Exception e) {
        // TODO include which file is invalid (will need to test both if first is invalid)
        e.printStackTrace(); // TODO log exception somehow
        return TEST_RESULT.ERROR_LOADING_TEST_FILE;
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
        // TODO respond appropriately to invalid remap file
        e.printStackTrace();
        return TEST_RESULT.INVALID_REMAP_FILE;
      }

    }

    List<ValidationModel> models = new ArrayList<>();
    for (ValidationModelBuilder builder : modelBuilders) {

      if (builder.hasCorrections()) {
        // assertion check if builder *should* have corrections
        RemapProcessor remapper;
        if (remapProcessor != null) {

          if (!remapProcessor.hasRemappings(builder.getSourceType())) {
            // TODO test to ensure this is correct behavior
            // if the remap file doesn't have the correct remappings
            return TEST_RESULT.INVALID_REMAPPINGS;
          }
          // assertTrue();
          remapper = remapProcessor;
        } else {
          remapper = new XMLRemapProcessor.NoRemapProcessor();
        }

        // TODO determine if result is necessary or not
        /* ValidationResult result = */builder.processCorrections(remapper);

        // TODO replace asserts with actual behavior
        // assertTrue(result.valid);
        // assertFalse(result.validationMessage.isPresent());
      } else {
        if (remapProcessor != null && remapProcessor.hasRemappings(builder.getSourceType())) {
          // builder says no remappings, but remap file says remappings do exist
          return TEST_RESULT.INVALID_REMAPPINGS;
        }
      }

      models.add(builder.build());
    }

    // TODO do something with eventual return value
    boolean valid = testIfModelsAgree(models);
    return valid ? TEST_RESULT.TEST_SUCCESS : TEST_RESULT.TEST_FAILURE;
  }

  private static boolean testIfModelsAgree(List<ValidationModel> individualModels) {
    ValidationModel base = individualModels.get(0);
    ValidationTable table = new ValidationTable();
    table.setFirstModel(base);

    // TODO defaulting to true means a test case with only one input file is automatically valid
    boolean valid = true;
    for (int index = 1; index < individualModels.size(); index++) {
      ValidationModel test = individualModels.get(index);
      table.setSecondModel(test);
      table.getValidationRows();
      valid &= table.isValidProperty().get();
    }
    return valid;
  }

  public static void addToValidationSet(TestInfo file1, TestInfo file2) {
    // TODO ensure labels match between file1 and file2

    File valDir = new File(VALIDATION_DIRECTORY);
    if (!valDir.exists()) {
      valDir.mkdir();
    }

    // TODO invalid characters from label
    String reldir = VALIDATION_DIRECTORY + REL + "/";
    String subdir = VALIDATION_DIRECTORY + safeFilename(file1.label) + "/";

    final File testDir = new File(subdir);

    if (testDir.exists()) {
      // TODO notify user, show directory path and cancel operation
    }

    testDir.mkdirs();

    try {
      Files.copy(new File(file1.file), new File(subdir + new File(file1.file).getName()));
      Files.copy(new File(file2.file), new File(subdir + new File(file2.file).getName()));
    } catch (Exception e) {
      // TODO handle exceptions appropriately;
      // notify user of inability to add files and cleanup appropriately
      e.printStackTrace();
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
          // TODO handle exceptions appropriately;
          // notify user of inability to copy rel_dna_ser file, and cleanup appropriately
          e.printStackTrace();
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
          java.nio.file.Files.copy(is, newRelFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
          // TODO handle exceptions appropriately;
          // notify user of inability to copy bundled rel_dna_ser file, and cleanup appropriately
          e.printStackTrace();
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
        // TODO handle exceptions appropriately;
        // notify user of inability to copy bundled rel_dna_ser file, and cleanup appropriately
        e.printStackTrace();
      }
    }

    // write test properties file containing CWD type and rel_dna_ser filename
    Properties props = new Properties();
    props.setProperty(CWD_PROP, CommonWellDocumented.loadPropertyCWDSource().name());
    props.setProperty(REL, newRelFileName);
    try (FileOutputStream fos = new FileOutputStream(new File(subdir + TEST_PROPERTIES))) {
      props.store(fos, null);
    } catch (Exception e) {
      // TODO handle exceptions appropriately;
      // notify user of inability to copy bundled rel_dna_ser file, and cleanup appropriately
      e.printStackTrace();
    }

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
      e.getValue().getLeft().stream().forEach(allele -> {
        remapLocus.appendElement("toAllele").text(allele.getHlaType().specString());
      });
    });

  }

  public static class TestRun {
    public final ValidationTestFileSet testFileSet;
    public final TEST_RESULT result;
    public final Date runTime;

    public TestRun(ValidationTestFileSet testFileSet, TEST_RESULT result, Date runTime) {
      this.testFileSet = testFileSet;
      this.result = result;
      this.runTime = runTime;
    }

  }

  public static void updateTestResults(Map<ValidationTestFileSet, TestRun> newResults) {
    for (Entry<ValidationTestFileSet, TestRun> entry : newResults.entrySet()) {
      String subdir = VALIDATION_DIRECTORY + safeFilename(entry.getKey().id.get()) + "/";
      File testPropertiesFile = new File(subdir + TEST_PROPERTIES);
      Properties props = new Properties();
      if (testPropertiesFile.exists()) {
        try (FileInputStream fis = new FileInputStream(testPropertiesFile)) {
          props.load(fis);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        props.setProperty(CWD_PROP, entry.getKey().cwdSource.get().name());
        String relName = entry.getKey().relDnaSerFile.get();
        if (relName.startsWith(VALIDATION_DIRECTORY + REL + "/")) {
          relName = relName.substring((VALIDATION_DIRECTORY + REL + "/").length());
        }
        props.getProperty(REL, relName);
      }

      entry.getKey().lastRunDate.set(entry.getValue().runTime);
      props.setProperty(LAST_RUN_PROP, DATE_FORMAT.format(entry.getValue().runTime));

      entry.getKey().lastPassingState.set(entry.getValue().result.isPassing);
      props.setProperty(LAST_RESULT_PROP, Boolean.toString(entry.getValue().result.isPassing));

      try (FileOutputStream fos = new FileOutputStream(new File(subdir + TEST_PROPERTIES))) {
        props.store(fos, null);
      } catch (Exception e) {
        // TODO handle exceptions appropriately;
        // notify user of inability to copy bundled rel_dna_ser file, and cleanup appropriately
        e.printStackTrace();
      }
    }

  }

  public static Table<SOURCE, String, List<ValidationTestFileSet>> loadValidationDirectory() {
    Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets =
        HashBasedTable.create();

    File dir = new File(VALIDATION_DIRECTORY);
    if (dir.exists() && dir.isDirectory()) {
      // The test directory is a test root, where child directories correspond to individuals.
      // Each individual should be tested individually
      for (File individualFile : dir.listFiles()) {
        if (individualFile.isDirectory() && !individualFile.getName().equals(REL)) {

          ValidationTestFileSetBuilder builder = ValidationTestFileSet.builder();
          builder.id(individualFile.getName());

          loadTestMetadata(individualFile, builder);

          String remapFile = null;
          Builder<String> inputFilesBuilder = ImmutableList.builder();
          for (File testFile : individualFile.listFiles()) {
            if (testFile.getName().equals(REMAP_XML)) {
              remapFile = testFile.getAbsolutePath();
            } else if (testFile.getName().equals(TEST_PROPERTIES)) {
              // skip
            } else {
              inputFilesBuilder.add(testFile.getAbsolutePath());
            }
          }
          builder.filePaths(inputFilesBuilder.build());
          builder.remapFile(remapFile);

          // TODO ensure testSet has at least two input files

          ValidationTestFileSet testSet = builder.build();

          addTestSetToTable(testSets, testSet);
        }
      }
    }

    return testSets;
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
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    String cwdStr = props.getProperty(CWD_PROP);
    String relStr = props.getProperty(REL);

    // TODO check that properties are valid

    CommonWellDocumented.SOURCE cwdSource = null;
    try {
      cwdSource = CommonWellDocumented.SOURCE.valueOf(cwdStr);
    } catch (Exception e) {
      // TODO not a valid CWD Source, respond appropriately
      e.printStackTrace();
    }
    builder.cwdSource(cwdSource);

    String relPath = VALIDATION_DIRECTORY + REL + "/" + relStr;
    builder.relDnaSerFile(relPath);

    // TODO currently saving only one run date & result
    // TODO store results for multiple runs in a new file
    String lastRunStr = props.getProperty(LAST_RUN_PROP);
    String lastPassingStr = props.getProperty(LAST_RESULT_PROP);

    // load last run date
    Date lastRunDate = null;
    if (lastRunStr != null) {
      try {
        lastRunDate = DATE_FORMAT.parse(lastRunStr);
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    builder.lastRunDate(lastRunDate);

    // load last passing state
    Boolean lastPassingState = lastPassingStr == null ? null : Boolean.parseBoolean(lastPassingStr);
    builder.lastPassingState(lastPassingState);

  }

  public static Map<ValidationTestFileSet, Boolean> deleteTests(
      List<ValidationTestFileSet> toRemove) {
    Map<ValidationTestFileSet, Boolean> success = new HashMap<>();
    for (ValidationTestFileSet testSet : toRemove) {
      boolean successDel = true;
      String dir = VALIDATION_DIRECTORY + safeFilename(testSet.id.get()) + "/";
      try {
        FileUtils.deleteDirectory(new File(dir));
      } catch (IOException | IllegalArgumentException e) {
        try {
          FileUtils.deleteDirectory(new File(dir));
        } catch (IOException | IllegalArgumentException e1) {
          successDel = false;
        }
      }
      success.put(testSet, successDel);
    }
    return success;
  }

}
