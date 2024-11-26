package org.pankratzlab.unet.validation;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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

  public final ReadOnlyListProperty<String> filePaths;

  public final ReadOnlySetProperty<SourceType> sourceTypes;

  public final ReadOnlyStringProperty remapFile;

  public final ReadOnlyObjectProperty<CommonWellDocumented.SOURCE> cwdSource;

  public final ReadOnlyStringProperty relDnaSerFile;

  // Last Run Date and Passing State are not read-only as they may change
  public final ObjectProperty<Date> lastRunDate;

  public final ObjectProperty<Boolean> lastPassingState;

  public final ObjectProperty<TEST_RESULT> lastTestResult;

  public static class ValidationTestFileSetBuilder {
    private String id;
    private String comment;
    private List<String> filePaths;
    private String remapFile;
    private CommonWellDocumented.SOURCE cwdSource;
    private String relDnaSerFile;
    private Date lastRunDate;
    private Boolean lastPassingState;
    private TEST_RESULT lastPassingResult;

    public ValidationTestFileSetBuilder id(String id) {
      this.id = id;
      return this;
    }

    public ValidationTestFileSetBuilder comment(String comment) {
      this.comment = comment;
      return this;
    }

    public ValidationTestFileSetBuilder filePaths(List<String> filePaths) {
      this.filePaths = filePaths;
      return this;
    }

    public ValidationTestFileSetBuilder remapFile(String remapFile) {
      this.remapFile = remapFile;
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

    public ValidationTestFileSetBuilder lastRunDate(Date lastRunDate) {
      this.lastRunDate = lastRunDate;
      return this;
    }

    public ValidationTestFileSetBuilder lastPassingState(Boolean lastPassingState) {
      this.lastPassingState = lastPassingState;
      return this;
    }

    public ValidationTestFileSetBuilder lastPassingResult(TEST_RESULT lastPassingResult) {
      this.lastPassingResult = lastPassingResult;
      return this;
    }

    public ValidationTestFileSet build() {
      return new ValidationTestFileSet(id, comment, filePaths, remapFile, cwdSource, relDnaSerFile,
          lastRunDate, lastPassingState, lastPassingResult);
    }

  }

  public static ValidationTestFileSetBuilder builder() {
    return new ValidationTestFileSetBuilder();
  }

  private ValidationTestFileSet(String id, String comment, List<String> filePaths, String remapFile,
      CommonWellDocumented.SOURCE cwdSource, String relDnaSerFile, Date lastRunDate,
      Boolean lastPassingState, TEST_RESULT lastTestResult) {
    this.id = new SimpleStringProperty(id);
    this.comment = new SimpleStringProperty(comment);
    this.filePaths = new ReadOnlyListWrapper<>(FXCollections.observableList(filePaths));
    final ObservableSet<SourceType> observableSet =
        FXCollections.observableSet(filePaths.stream().map(s -> {
          try {
            return SourceType.parseType(new File(s));
          } catch (IllegalArgumentException e) {
            // TODO should deal with nulls somehow?
            return null;
          }
        }).collect(Collectors.toSet()));
    this.sourceTypes = new ReadOnlySetWrapper<>(observableSet);
    this.remapFile = new ReadOnlyStringWrapper(remapFile);
    this.cwdSource = new ReadOnlyObjectWrapper<>(cwdSource);
    this.relDnaSerFile = new ReadOnlyStringWrapper(relDnaSerFile);
    this.lastRunDate = new SimpleObjectProperty<>(lastRunDate);
    this.lastPassingState = new SimpleObjectProperty<>(lastPassingState);
    this.lastTestResult = new SimpleObjectProperty<>(lastTestResult);
  }

}
