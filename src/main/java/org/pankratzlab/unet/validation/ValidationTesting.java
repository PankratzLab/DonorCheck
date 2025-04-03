package org.pankratzlab.unet.validation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.pankratzlab.unet.deprecated.hla.AntigenDictionary;
import org.pankratzlab.unet.deprecated.hla.DonorCheckProperties;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.Info;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.SOURCE;
import org.pankratzlab.unet.jfx.LandingController;
import org.pankratzlab.unet.jfx.prop.DCProperty;
import org.pankratzlab.unet.jfx.wizard.OutputCSVConfig;
import org.pankratzlab.unet.model.ModelData;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import org.pankratzlab.unet.model.ValidationModelBuilder.ValidationResult;
import org.pankratzlab.unet.model.ValidationTable;
import org.pankratzlab.unet.model.ValidationTable.ValidationKey;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import org.pankratzlab.unet.validation.TestRun.TestRunMetadata;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;

public class ValidationTesting {

  private static final String CSV_CONFIG_FXML = "/csvConfig.fxml";
  private static final String REMAP_XML = "remap.xml";
  private static final String TEST_PROPERTIES = "test.properties";
  private static final String TEST_LOG = "test.log";

  public static final String REL_DIRECTORY = Info.DONOR_CHECK_HOME + "rel_dna_ser_files/";
  public static final String VALIDATION_DIRECTORY = new File(Info.DONOR_CHECK_HOME + "validation/").getAbsolutePath() + File.separator;

  private static final String CWD_PROP = "cwd";
  private static final String REL = "rel";
  private static final String COMMENT_PROP = "comment";
  private static final String EXPECTED_PROP = "expected";
  private static final String DC_VER_PROP = "version";
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String EXPECTED_PASS = "Expected pass";
  private static final String EXPECTED_FAILURE = "Expected failure";
  private static final String UNEXPECTED_PASS = "Unexpected pass";
  private static final String UNEXPECTED_FAILURE = "Unexpected failure";
  private static final String UNEXPECTED_ERROR = "Unexpected error";
  private static final String EXPECTED_ERROR = "Expected error";
  private static final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy' at 'HH:mm:ss");

  public static List<ValidationTestFileSet> sortTests(List<ValidationTestFileSet> tests) {
    return tests.stream().sorted((t1, t2) -> {
      int c = t1.cwdSource.get().compareTo(t2.cwdSource.get());
      if (c != 0)
        return c;
      return t1.relDnaSerFile.get().compareTo(t2.relDnaSerFile.get());
    }).collect(Collectors.toList());
  }

  public static boolean addToValidationSet(TestInfo file11, TestInfo file21) {
    TestInfo file1 = file11;
    TestInfo file2 = file21;
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

    File testDir = new File(subdir);

    // first check if test already exists
    if (testDir.exists()) {
      Optional<String> newLbl = AlertHelper.showMessage_TestAlreadyExists(file1, subdir);
      if (newLbl.isEmpty()) {
        return false;
      } else {
        file1 = new TestInfo(newLbl.get(), file1.file, file1.remappings, file1.sourceType);
        file2 = new TestInfo(newLbl.get(), file2.file, file2.remappings, file2.sourceType);
        subdir = getTestDirectory(file1.label);
        testDir = new File(subdir);
      }
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
      String fName = new File(file2.file).getName();
      if (file1.file.equals(file2.file)) {
        String ext = FilenameUtils.getExtension(file2.file);
        fName = FilenameUtils.getBaseName(file2.file);
        fName = fName + "_duplicate." + ext;
      }
      Files.copy(new File(file2.file), new File(subdir + fName));
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

    String relDnaSerFile = DonorCheckProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);
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
        try (InputStream is = AntigenDictionary.class.getClassLoader().getResourceAsStream(AntigenDictionary.MASTER_MAP_RECORDS)) {
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
    for (String p : DC_PERSISTED_PROPS) {
      props.setProperty(p, DonorCheckProperties.getOrDefault(p));
    }
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

  public static void runTests(Pane root, List<ValidationTestFileSet> tests) {
    List<ValidationTestFileSet> sorted = ValidationTesting.sortTests(tests);

    Map<ValidationTestFileSet, Map<String, ModelData>> dataResults = new HashMap<>();
    Set<ValidationTestFileSet> successfulTests = Collections.synchronizedSet(new HashSet<>());
    Set<ValidationTestFileSet> failedTests = Collections.synchronizedSet(new HashSet<>());
    Map<ValidationTestFileSet, Exception> exceptionTests = new ConcurrentHashMap<>();
    Map<ValidationTestFileSet, TEST_RESULT> testResults = new ConcurrentHashMap<>();
    List<Runnable> tasks = sorted.stream().map(t -> new Runnable() {
      @Override
      public void run() {
        try {
          TestRun result = ValidationTesting.runTest(t);
          testResults.put(t, result.testRunMetadata.result);
          dataResults.put(t, result.data);
          if (result.error.isPresent()) {
            exceptionTests.put(t, result.error.get());
          } else {
            if (result.testRunMetadata.result.isPassing) {
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
      String contentText = "Test results:\n\nPassing: " + successfulTests.size() + "\n\nFailing: " + failedTests.size() + "\n\nExceptions: "
          + exceptionTests.size() + "\n\n(Please report any exceptions to the developers)";
      if (!exceptionTests.isEmpty()) {
        for (Exception e1 : exceptionTests.values()) {
          contentText += "\n\n" + e1.getClass().getSimpleName() + ": " + e1.getMessage();
        }
      }
      ButtonType writeCSVType = new ButtonType("Write results to file", ButtonData.APPLY);
      Alert alert1 = new Alert(AlertType.INFORMATION, contentText, ButtonType.CLOSE, writeCSVType);
      alert1.setTitle("Test results");
      alert1.setHeaderText("");
      Optional<ButtonType> sel = alert1.showAndWait();
      if (sel.isPresent() && sel.get().equals(writeCSVType)) {
        OutputConfig oc = promptConfig(root);
        if (oc != null) {
          writeCSV(oc, tests, dataResults, exceptionTests, testResults);
        }
      }
    };

    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, showResults);
    runValidationTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, showResults);

    new Thread(runValidationTask).start();
  }

  private static OutputConfig promptConfig(Pane root) {
    OutputCSVConfig controller = new OutputCSVConfig();

    try {
      FXMLLoader loader = new FXMLLoader(LandingController.class.getResource(CSV_CONFIG_FXML));
      loader.setController(controller);
      GridPane gridPane = loader.load();
      Dialog<ButtonType> inputDialog = new Dialog<>();
      inputDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
      inputDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
      Button okBtn = (Button) inputDialog.getDialogPane().lookupButton(ButtonType.OK);
      controller.setOKButton(okBtn);
      inputDialog.initOwner(root.getScene().getWindow());
      inputDialog.initModality(Modality.APPLICATION_MODAL);
      inputDialog.setTitle("DonorCheck " + Info.getVersion());
      inputDialog.setResizable(true);
      inputDialog.getDialogPane().setContent(gridPane);
      Optional<ButtonType> btn = inputDialog.showAndWait();
      if (btn.isPresent() && btn.get() == ButtonType.OK) {
        return controller.getConfig();
      }
    } catch (IOException e1) {
      e1.printStackTrace();
      Alert alert1 = new Alert(AlertType.ERROR, "Error loading test data: " + e1.getMessage(), ButtonType.CLOSE);
      alert1.setTitle("Error");
      alert1.setHeaderText("");
      alert1.showAndWait();
    }
    return null;
  }

  public static record OutputConfig(String filePath, boolean includeID, boolean maskID) {
  }

  private static void writeCSV(OutputConfig config, List<ValidationTestFileSet> tests, Map<ValidationTestFileSet, Map<String, ModelData>> dataResults,
      Map<ValidationTestFileSet, Exception> exceptionTests, Map<ValidationTestFileSet, TEST_RESULT> testResults) {

    File fileOut = new File(config.filePath);

    // Write the output
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(fileOut)); XSSFWorkbook wb = new XSSFWorkbook();) {

      int index = 0;
      for (ValidationTestFileSet t : tests) {
        index++;
        Map<String, ModelData> data = dataResults.get(t);
        Exception e = exceptionTests.get(t);
        TEST_RESULT result = testResults.get(t);
        writeSheet(index, config, wb, t, data, e, result);
      }

      wb.write(output);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static String getData(String filePath, ValidationKey key, Map<String, ModelData> data) {
    if (!data.containsKey(filePath)) {
      return "failed to parse";
    }

    return data.get(filePath).get(key);
  }

  private static boolean check(String filePath1, String filePath2, Map<String, ModelData> data) {
    return data.containsKey(filePath1) && data.containsKey(filePath2);
  }

  private static void writeSheet(int donorIndex, OutputConfig config, XSSFWorkbook wb, ValidationTestFileSet t, Map<String, ModelData> data,
      Exception e, TEST_RESULT result) {
    XSSFSheet sheet = wb.createSheet(!config.includeID() || config.maskID() ? "" + donorIndex : t.id.get());
    int rowNum = 0;

    if (data != null && !data.isEmpty()) {
      String first = t.filePaths.get(0);
      String second = t.filePaths.get(1);

      for (ValidationKey key : ValidationKey.values()) {
        if (key == ValidationKey.DONORID && !config.includeID) {
          continue;
        }
        XSSFRow row = sheet.createRow(rowNum++);

        XSSFCell cell0 = row.createCell(0);
        cell0.setCellValue(key.fieldName);

        XSSFCell cell1 = row.createCell(1);
        String v;
        if (key == ValidationKey.DONORID && config.maskID) {
          v = "*****";
        } else if (key == ValidationKey.SOURCE && !data.containsKey(first)) {
          v = findSourceType(first);
        } else {
          v = getData(first, key, data);
        }
        cell1.setCellValue(v);

        XSSFCell cell2 = row.createCell(2);
        if (key == ValidationKey.DONORID && config.maskID) {
          v = "*****";
        } else if (key == ValidationKey.SOURCE && !data.containsKey(second)) {
          v = findSourceType(second);
        } else {
          v = getData(second, key, data);
        }
        cell2.setCellValue(v);

        if (key == ValidationKey.SOURCE)
          continue;
        XSSFCell cell3 = row.createCell(3);
        String m = "";
        if (check(first, second, data)) {
          m = getData(first, key, data).equals(getData(second, key, data)) ? "" : "mismatched";
        }
        cell3.setCellValue(m);
      }

      sheet.createRow(rowNum++);
    }

    sheet.createRow(rowNum++);
    XSSFRow row = sheet.createRow(rowNum++);
    XSSFCell cell0 = row.createCell(0);
    if (e != null) {
      cell0.setCellValue("Validation could not be attempted due to the following error: " + DCProperty.convert(result.name()));
    } else {
      cell0.setCellValue(result.isPassing ? "Validation Succeeded" : "Validation Failed");
    }

    if (data != null && !data.isEmpty()) {
      sheet.createRow(rowNum++);

      for (Entry<String, ModelData> mdEntry : data.entrySet()) {
        ModelData md = mdEntry.getValue();
        String[] remap1 = getRemapLog(md);
        for (String r1 : remap1) {
          row = sheet.createRow(rowNum++);
          cell0 = row.createCell(0);
          cell0.setCellValue(r1);
        }
        for (String auditLine : md.getAudit()) {
          row = sheet.createRow(rowNum++);
          cell0 = row.createCell(0);
          cell0.setCellValue(auditLine);
        }
        String[] manAssign = generateManualAssignments(md);
        for (String r1 : manAssign) {
          row = sheet.createRow(rowNum++);
          cell0 = row.createCell(0);
          cell0.setCellValue(r1);
        }
      }
    }

    sheet.createRow(rowNum++);
    sheet.createRow(rowNum++);

    XSSFCell cell1;

    // DonorCheck version
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("DonorCheck version:");
    cell1.setCellValue(t.donorCheckVersion.getValue());

    // rel_dna_ser file version
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Serotype lookup version:");
    String v = "Unknown";
    v = AntigenDictionary.getVersion(t.relDnaSerFile.get());
    if (v == null) {
      v = "File missing (" + t.relDnaSerFile.get() + ")";
    }
    cell1.setCellValue(v);

    // CIWD version
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Frequency lookup version:");
    cell1.setCellValue(t.cwdSource.get().toString());

    // expected result
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Expected result:");
    cell1.setCellValue(t.expectedResult.get().name());

    // interpretation
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Interpretation:");
    cell1.setCellValue(computeInterpretation(t));

    // comment
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Comment from user:");
    cell1.setCellValue(t.comment.get());

    // date of last run
    row = sheet.createRow(rowNum++);
    cell0 = row.createCell(0);
    cell1 = row.createCell(1);
    cell0.setCellValue("Last validated on:");
    cell1.setCellValue(format.format(t.lastRunDate.getValue()));

    sheet.createRow(rowNum++);
    sheet.createRow(rowNum++);

    if (e != null) {
      row = sheet.createRow(rowNum++);
      cell0 = row.createCell(0);
      cell0.setCellValue("Validation Exception: " + e.getClass().getSimpleName());

      String usrNm = System.getProperty("user.name");
      String donorId = null;

      if (data != null && !data.isEmpty()) {
        for (Entry<String, ModelData> mdEntry : data.entrySet()) {
          ModelData md = mdEntry.getValue();
          donorId = md.get(ValidationKey.DONORID);
          if (donorId != null && !donorId.isEmpty())
            break;
        }
      }

      String[] f = ExceptionUtils.getStackFrames(e);
      for (String frame : f) {
        row = sheet.createRow(rowNum++);
        cell0 = row.createCell(0);
        String frm = frame;
        if (usrNm != null && !usrNm.isEmpty()) {
          frm = frm.replaceAll(Pattern.quote(usrNm), "****");
        }
        if (donorId != null && !donorId.isEmpty()) {
          frm = frm.replaceAll(Pattern.quote(donorId), "****");
        }
        cell0.setCellValue(frm);
      }
    }

    sheet.setColumnWidth(0, 24 * 256);
    sheet.setColumnWidth(1, 21 * 256);
    sheet.setColumnWidth(2, 21 * 256);
    sheet.setColumnWidth(3, 12 * 256);
  }

  private static String findSourceType(String first) {
    try {
      return SourceType.parseType(new File(first)).getDisplayName();
    } catch (IllegalArgumentException e) {
      return "Unknown";
    }
  }

  private static String[] generateManualAssignments(ModelData md) {
    return md.getManuallyAssignedLoci().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
        .map(e -> "HLA-" + e.getKey().name() + " was manually assigned to ["
            + e.getValue().stream().sorted().map(HLAType::toString).collect(Collectors.joining(" / ")) + "] in " + md.get(ValidationKey.SOURCE))
        .toArray(String[]::new);
  }

  private static String[] getRemapLog(ModelData md) {
    return md.getRemaps().entrySet().stream().filter(ent -> {
      final String collectFrom =
          ent.getValue().getLeft().stream().sorted().map(TypePair::getHlaType).map((h) -> h.specString()).collect(Collectors.joining(" / "));
      final String collectTo =
          ent.getValue().getRight().stream().sorted().map(TypePair::getHlaType).map((h) -> h.specString()).collect(Collectors.joining(" / "));
      return !collectFrom.equals(collectTo);
    }).map(ent -> {
      final String collectFrom =
          ent.getValue().getLeft().stream().sorted().map(TypePair::getHlaType).map((h) -> h.specString()).collect(Collectors.joining(" / "));
      final String collectTo =
          ent.getValue().getRight().stream().sorted().map(TypePair::getHlaType).map((h) -> h.specString()).collect(Collectors.joining(" / "));
      return "HLA-" + ent.getKey().name() + " was remapped from { " + collectFrom + " } to { " + collectTo + " } in " + md.get(ValidationKey.SOURCE);
    }).sorted().distinct().toArray(String[]::new);
  }

  public static void updateTestResults(Map<ValidationTestFileSet, TestRunMetadata> newResults) {
    for (Entry<ValidationTestFileSet, TestRunMetadata> entry : newResults.entrySet()) {
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
        java.nio.file.Files.writeString(Path.of(subdir, TEST_LOG), logStr, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException e) {
        // TODO handle exceptions appropriately;
        // notify user of inability to write properties file, and cleanup appropriately
        e.printStackTrace();
      }
    }
  }

  /**
   * Will create a test.properties file if it doesn't exist and set the CWD and REL properties.<br>
   * <br>
   * These properties are otherwise unchanged!<br>
   * <br>
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
      for (String p : DC_PERSISTED_PROPS) {
        props.setProperty(p, DonorCheckProperties.getOrDefault(p));
      }
    }

    String prevComment = Objects.toString(props.setProperty(COMMENT_PROP, test.comment.get()), "");
    String prevVersion = Objects.toString(props.setProperty(DC_VER_PROP, test.donorCheckVersion.getValueSafe()), "");

    String prevExpectStr = Objects.toString(props.setProperty(EXPECTED_PROP, test.expectedResult.get().name()));
    TEST_EXPECTATION prevExpect = TEST_EXPECTATION.Pass;
    if (prevExpectStr != null && !prevExpectStr.isEmpty()) {
      try {
        prevExpect = TEST_EXPECTATION.valueOf(prevExpectStr);
      } catch (IllegalArgumentException e) {
        // ignore
      }
    }

    Map<String, String> prevProps = new HashMap<>();
    for (String p : DC_PERSISTED_PROPS) {
      if (!test.dcProperties.containsKey(p)) {
        test.dcProperties.put(p, DonorCheckProperties.getOrDefault(p));
      }
      prevProps.put(p, Objects.toString(props.setProperty(p, test.dcProperties.get(p))));
    }

    try (FileOutputStream fos = new FileOutputStream(new File(subdir + TEST_PROPERTIES))) {
      props.store(fos, null);
    } catch (Exception e) {
      test.comment.set(prevComment);
      test.donorCheckVersion.setValue(prevVersion);
      test.expectedResult.setValue(prevExpect);
      test.dcProperties.clear();
      test.dcProperties.putAll(prevProps);
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
    Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets = HashBasedTable.create();

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
        try {
          SourceType.parseType(testFile);
          inputFilesBuilder.add(testFile.getAbsolutePath());
        } catch (IllegalArgumentException e) {
          // not a valid source file
        }
      }
    }
    builder.filePaths(inputFilesBuilder.build());
    builder.remapFile(remapFile);

    // Tests can only be added from the validation results screen, which
    // means that we know the test set has exactly two files, and therefore
    // we don't have to check the number of files present

    return builder.build();
  }

  public static Map<ValidationTestFileSet, Boolean> deleteTests(List<ValidationTestFileSet> toRemove) {
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
    String currentRel = DonorCheckProperties.get().getProperty(AntigenDictionary.REL_DNA_SER_PROP);
    Map<String, String> currentProps = DC_PERSISTED_PROPS.stream().collect(Collectors.toMap(k -> k, v -> DonorCheckProperties.getOrDefault(v)));

    SOURCE source = test.cwdSource.get();
    String rel = test.relDnaSerFile.get();
    boolean changedCWID = false;
    boolean changedRel = false;
    boolean changedProps = false;

    if (current != source || !CommonWellDocumented.isLoaded()) {
      CommonWellDocumented.loadCIWDVersion(source);
      changedCWID = true;
    }
    if ((currentRel == null && rel != null) || !new File(currentRel).equals(new File(rel))) {
      DonorCheckProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, rel);
      AntigenDictionary.clearCache();
      changedRel = true;
    }

    for (String p : DC_PERSISTED_PROPS) {
      if (test.dcProperties.containsKey(p) && !currentProps.get(p).equals(test.dcProperties.get(p))) {
        changedProps = true;
        DonorCheckProperties.get().put(p, test.dcProperties.get(p));
      }
    }

    TestResultWithMultiData resultM = runTestInternal(test);
    TestRun returnVal = new TestRun(test, resultM.testResult(), new Date(), resultM.getException(), resultM.getValidModels());

    if (changedCWID) {
      CommonWellDocumented.loadCIWDVersion(current);
    }
    if (changedRel) {
      DonorCheckProperties.get().setProperty(AntigenDictionary.REL_DNA_SER_PROP, Optional.ofNullable(currentRel).orElse(""));
      AntigenDictionary.clearCache();
    }

    if (changedProps) {
      for (String p : currentProps.keySet()) {
        DonorCheckProperties.get().put(p, currentProps.get(p));
      }
    }

    updateTestResults(Map.of(test, returnVal.testRunMetadata));

    return returnVal;
  }

  private static TestResultWithMultiData runTestInternal(ValidationTestFileSet test) {
    Map<String, ValidationModelBuilder> modelBuilders = new HashMap<>();

    if (test.filePaths.size() == 1) {
      // TODO parse into model anyway
      return new TestResultWithMultiData(TEST_RESULT.NOT_ENOUGH_TEST_FILES);
    }

    Map<String, Exception> fileExceptions = new HashMap<>();
    Set<String> invalidFile = new HashSet<>();

    for (String filePath : test.filePaths) {
      ValidationModelBuilder builder = new ValidationModelBuilder();
      File file = new File(filePath);

      try {
        SourceType.parseFile(builder, file);

        ValidationResult validationResult = builder.validate(false);
        modelBuilders.put(filePath, builder);
        if (!validationResult.valid) {
          invalidFile.add(filePath);
        }
      } catch (Exception e) {
        fileExceptions.put(filePath, e);
      }
    }

    XMLRemapProcessor remapProcessor = null;
    boolean missingRemap = false;
    Exception remapReadException = null;
    if (test.remapFile != null && test.remapFile.get() != null) {

      // check to make sure remap file exists
      if (!new File(test.remapFile.get()).exists()) {
        missingRemap = true;
      } else {
        try {
          remapProcessor = new XMLRemapProcessor(test.remapFile.get());
        } catch (IllegalStateException e) {
          remapReadException = e;
        }
      }
    }

    List<String> invalidRemappings = new ArrayList<>();
    Map<String, ValidationModel> models = new HashMap<>();

    for (Entry<String, ValidationModelBuilder> builderEntry : modelBuilders.entrySet()) {

      if (builderEntry.getValue().hasCorrections()) {
        // assertion check if builder *should* have corrections
        RemapProcessor remapper;
        if (remapProcessor != null) {

          if (!remapProcessor.hasRemappings(builderEntry.getValue().getSourceType())) {
            // builder is expecting remappings for this file, but
            // the remapping file doesn't have the expected remappings
            invalidRemappings.add(builderEntry.getKey());
            remapper = new XMLRemapProcessor.NoRemapProcessor();
          } else {
            remapper = remapProcessor;
          }
        } else {
          remapper = new XMLRemapProcessor.NoRemapProcessor();
        }

        builderEntry.getValue().processCorrections(remapper);
      } else {
        if (remapProcessor != null && remapProcessor.hasRemappings(builderEntry.getValue().getSourceType())) {
          // builder says no remappings, but remap file says remappings do exist
          invalidRemappings.add(builderEntry.getKey());
        }
      }

      models.put(builderEntry.getKey(), builderEntry.getValue().build());
    }

    if (models.size() == test.filePaths.size()) {
      TestResultWithMultiData valid = testIfModelsAgree2(models);
      return valid;
    } else {

      Map<String, Exception> errorFiles = new HashMap<>();
      Map<String, ModelData> invalidRemapFiles = new HashMap<>();
      Map<String, ModelData> invalidModels = new HashMap<>();
      Map<String, ModelData> validModels = new HashMap<>();

      boolean missingRemapFile = missingRemap;
      Optional<Exception> remapException = Optional.ofNullable(remapReadException);

      for (String file : test.filePaths) {
        if (fileExceptions.containsKey(file)) {
          errorFiles.put(file, fileExceptions.get(file));
        } else if (invalidFile.contains(file)) {
          invalidModels.put(file, new ModelData(models.get(file)));
        } else if (invalidRemappings.contains(file)) {
          invalidRemapFiles.put(file, new ModelData(models.get(file)));
        } else if (models.containsKey(file)) {
          validModels.put(file, new ModelData(models.get(file)));
        }
      }

      TEST_RESULT result;
      if (!fileExceptions.isEmpty()) {
        result = TEST_RESULT.ERROR_LOADING_TEST_FILE;
      } else if (!invalidFile.isEmpty()) {
        result = TEST_RESULT.INVALID_TEST_FILE;
      } else if (missingRemap) {
        result = TEST_RESULT.MISSING_REMAP_FILE;
      } else if (remapException.isPresent()) {
        result = TEST_RESULT.INVALID_REMAP_FILE;
      } else if (invalidRemappings.isEmpty()) {
        result = TEST_RESULT.INVALID_REMAPPINGS;
      } else {
        // TODO
        result = TEST_RESULT.TEST_FAILURE;
      }

      return new TestResultWithMultiData(result, errorFiles, invalidRemapFiles, invalidModels, validModels, missingRemapFile, remapException);
    }
  }

  private static TestResultWithMultiData testIfModelsAgree2(Map<String, ValidationModel> individualModels) {
    Map<String, ModelData> modelData = new HashMap<>();

    List<String> keys = new ArrayList<>(individualModels.keySet());

    ValidationTable table = new ValidationTable();
    ValidationModel base = individualModels.get(keys.get(0));
    modelData.put(keys.get(0), new ModelData(individualModels.get(keys.get(0))));
    table.setFirstModel(base);

    // defaulting to true means a test case with only one input file is automatically valid

    // However, tests can only be added from the validation results screen, which
    // means that we know the test set has exactly two files, and therefore
    // we don't have to check the number of files present

    boolean valid = true;
    for (int index = 1; index < keys.size(); index++) {
      ValidationModel test = individualModels.get(keys.get(index));
      modelData.put(keys.get(index), new ModelData(individualModels.get(keys.get(index))));
      table.setSecondModel(test);
      table.getValidationRows();
      valid &= table.isValidProperty().get();
    }

    return new TestResultWithMultiData(valid ? TEST_RESULT.TEST_SUCCESS : TEST_RESULT.TEST_FAILURE, modelData);
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

  public static String computeInterpretation(ValidationTestFileSet value) {
    TEST_RESULT testResult = value.lastTestResult.get();
    if (testResult == null) {
      return "---";
    }

    String v = "";
    switch (value.expectedResult.get()) {
      case Error:
        // expecting an error
        if (testResult != TEST_RESULT.TEST_SUCCESS && testResult != TEST_RESULT.TEST_FAILURE) {
          // got an error
          v = EXPECTED_ERROR;
        } else {
          if (testResult == TEST_RESULT.TEST_SUCCESS) {
            // didn't get an error
            v = UNEXPECTED_PASS;
          } else if (testResult == TEST_RESULT.TEST_FAILURE) {
            // didn't get an error
            v = UNEXPECTED_FAILURE;
          }
        }
        break;
      case Pass:
        if (testResult == TEST_RESULT.TEST_SUCCESS) {
          v = EXPECTED_PASS;
        } else if (testResult == TEST_RESULT.TEST_FAILURE) {
          v = UNEXPECTED_FAILURE;
        } else {
          v = UNEXPECTED_ERROR + " (" + DCProperty.convert(testResult.name()) + ")";
        }
        break;
      case Fail:
        if (testResult == TEST_RESULT.TEST_SUCCESS) {
          v = UNEXPECTED_PASS;
        } else if (testResult == TEST_RESULT.TEST_FAILURE) {
          v = EXPECTED_FAILURE;
        } else {
          v = UNEXPECTED_ERROR + " (" + DCProperty.convert(testResult.name()) + ")";
        }
        break;
    };
    return v;
  }

  public static String computeColor(String value) {
    String color = null;
    if (value.startsWith(EXPECTED_PASS) || value.startsWith(EXPECTED_FAILURE) || value.startsWith(EXPECTED_ERROR)) {
      color = "LimeGreen";
    } else if (value.startsWith(UNEXPECTED_FAILURE)) {
      color = "Orange";
    } else if (value.startsWith(UNEXPECTED_PASS)) {
      color = "Lime";
    } else if (value.startsWith(UNEXPECTED_ERROR)) {
      color = "OrangeRed";
    }
    return color;
  }

  private static void addTestSetToTable(Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets,
      ValidationTestFileSet testSet) {
    if (!testSets.contains(testSet.cwdSource.get(), testSet.relDnaSerFile.get())) {
      testSets.put(testSet.cwdSource.get(), testSet.relDnaSerFile.get(), new ArrayList<>());
    }
    testSets.get(testSet.cwdSource.get(), testSet.relDnaSerFile.get()).add(testSet);
  }

  public static final List<String> DC_PERSISTED_PROPS =
      List.of(DonorCheckProperties.USE_SCORE_6_ALLELE_CALL, DonorCheckProperties.SURETYPER_ALLOW_INVALID_DQA_ALLELES);

  private static void loadTestMetadata(File individualFile, ValidationTestFileSetBuilder builder) {
    File testPropertiesFile = new File(individualFile, TEST_PROPERTIES);
    Properties props = new Properties();
    if (testPropertiesFile.exists()) {
      try (FileInputStream fis = new FileInputStream(testPropertiesFile)) {
        props.load(fis);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to load test properties file: " + testPropertiesFile.getAbsolutePath(), e);
      }
    }
    String cwdStr = props.getProperty(CWD_PROP);
    String relStr = props.getProperty(REL);
    String commentStr = props.getProperty(COMMENT_PROP, "");
    String expectationStr = props.getProperty(EXPECTED_PROP, "");
    String donorCheckVersionStr = props.getProperty(DC_VER_PROP, "");

    for (String p : DC_PERSISTED_PROPS) {
      if (props.getProperty(p) != null) {
        builder.setDonorCheckProperty(p, props.getProperty(p));
      }
    }

    builder.comment(commentStr);
    builder.donorCheckVersion(donorCheckVersionStr);

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

  private static void loadTestLastRunLog(File individualFile, ValidationTestFileSetBuilder builder) {
    File file = new File(individualFile, TEST_LOG);
    if (!file.exists())
      return;
    try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, Charset.defaultCharset())) {
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
      throw new IllegalStateException("Unable to move test directory: " + oldSubdir + " -> " + newSubdir, e);
    }

    return loadIndividualTest(new File(newSubdir));
  }

  public static class TestLoadingResults {
    public final Table<CommonWellDocumented.SOURCE, String, List<ValidationTestFileSet>> testSets;
    public final List<String> invalidTests;

    public TestLoadingResults(Table<SOURCE, String, List<ValidationTestFileSet>> testSets, List<String> invalidTests) {
      this.testSets = testSets;
      this.invalidTests = invalidTests;
    }
  }
}
