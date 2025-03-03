package org.pankratzlab.unet.validation;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class ValidationTestFileSet {

  public final StringProperty id;

  public final StringProperty comment;

  public final ObjectProperty<TEST_EXPECTATION> expectedResult;

  public final ReadOnlyListProperty<String> filePaths;

  public final ReadOnlySetProperty<SourceType> sourceTypes;

  public final ReadOnlyStringProperty remapFile;

  public final ReadOnlySetProperty<HLALocus> remappedLoci;

  public final ReadOnlyObjectProperty<CommonWellDocumented.SOURCE> cwdSource;

  public final ReadOnlyStringProperty relDnaSerFile;

  // Last Run Date and Passing State are not read-only as they may change
  public final StringProperty donorCheckVersion;

  public final ObjectProperty<Date> lastRunDate;

  public final ObjectProperty<TEST_RESULT> lastTestResult;

  public static class ValidationTestFileSetBuilder {
    private String id;
    private String comment;
    private TEST_EXPECTATION expectedResult;
    private List<String> filePaths;
    private String remapFile;
    private Set<HLALocus> remappedLoci;
    private CommonWellDocumented.SOURCE cwdSource;
    private String relDnaSerFile;
    private String donorCheckVersion;
    private Date lastRunDate;
    private TEST_RESULT lastPassingResult;

    public ValidationTestFileSetBuilder id(String id) {
      this.id = id;
      return this;
    }

    public ValidationTestFileSetBuilder comment(String comment) {
      this.comment = comment;
      return this;
    }

    public ValidationTestFileSetBuilder expectedResult(TEST_EXPECTATION expectedResult) {
      this.expectedResult = expectedResult;
      return this;
    }

    public ValidationTestFileSetBuilder filePaths(List<String> filePaths) {
      this.filePaths = filePaths;
      return this;
    }

    public ValidationTestFileSetBuilder remapFile(String remapFile) {
      this.remapFile = remapFile;

      if (remapFile != null && !remapFile.isBlank() && new File(remapFile).exists()) {
        XMLRemapProcessor processor = new XMLRemapProcessor(remapFile);
        this.remappedLoci = processor.getAllRemappedLoci();
      } else {
        this.remappedLoci = new HashSet<>();
      }

      return this;
    }

    public ValidationTestFileSetBuilder cwdSource(CommonWellDocumented.SOURCE cwdSource) {
      this.cwdSource = cwdSource;
      return this;
    }

    public ValidationTestFileSetBuilder relDnaSerFile(String relDnaSerFile) {
      this.relDnaSerFile = relDnaSerFile;
      return this;
    }

    public ValidationTestFileSetBuilder donorCheckVersion(String donorCheckVersion) {
      this.donorCheckVersion = donorCheckVersion;
      return this;
    }

    public ValidationTestFileSetBuilder lastRunDate(Date lastRunDate) {
      this.lastRunDate = lastRunDate;
      return this;
    }

    public ValidationTestFileSetBuilder lastPassingResult(TEST_RESULT lastPassingResult) {
      this.lastPassingResult = lastPassingResult;
      return this;
    }

    public ValidationTestFileSet build() {
      return new ValidationTestFileSet(id, comment, expectedResult, filePaths, remapFile, remappedLoci, cwdSource, relDnaSerFile, donorCheckVersion,
          lastRunDate, lastPassingResult);
    }

  }

  public static ValidationTestFileSetBuilder builder() {
    return new ValidationTestFileSetBuilder();
  }

  private ValidationTestFileSet(String id, String comment, TEST_EXPECTATION expectedResult, List<String> filePaths, String remapFile,
      Set<HLALocus> remappedLoci, CommonWellDocumented.SOURCE cwdSource, String relDnaSerFile, String donorCheckVersion, Date lastRunDate,
      TEST_RESULT lastTestResult) {
    this.id = new SimpleStringProperty(id);
    this.comment = new SimpleStringProperty(comment);
    this.expectedResult = new SimpleObjectProperty<>(expectedResult);
    this.filePaths = new ReadOnlyListWrapper<>(FXCollections.observableList(filePaths));
    final ObservableSet<SourceType> observableSet = FXCollections.observableSet(filePaths.stream().map(s -> {
      try {
        return SourceType.parseType(new File(s));
      } catch (IllegalArgumentException e) {
        // TODO should deal with nulls somehow?
        return null;
      }
    }).collect(Collectors.toSet()));
    this.sourceTypes = new ReadOnlySetWrapper<>(observableSet);
    this.remapFile = new ReadOnlyStringWrapper(remapFile);
    this.remappedLoci = new ReadOnlySetWrapper<>(FXCollections.observableSet(remappedLoci));
    this.cwdSource = new ReadOnlyObjectWrapper<>(cwdSource);
    this.relDnaSerFile = new ReadOnlyStringWrapper(relDnaSerFile);
    this.donorCheckVersion = new SimpleStringProperty(donorCheckVersion);
    this.lastRunDate = new SimpleObjectProperty<>(lastRunDate);
    this.lastTestResult = new SimpleObjectProperty<>(lastTestResult);
  }

}
