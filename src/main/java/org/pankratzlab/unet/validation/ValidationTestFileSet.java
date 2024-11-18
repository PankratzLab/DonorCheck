package org.pankratzlab.unet.validation;

import java.util.Date;
import java.util.List;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public class ValidationTestFileSet {

  public final ReadOnlyStringProperty id;

  public final ReadOnlyListProperty<String> filePaths;

  public final ReadOnlyStringProperty remapFile;

  public final ReadOnlyObjectProperty<CommonWellDocumented.SOURCE> cwdSource;

  public final ReadOnlyStringProperty relDnaSerFile;

  // Last Run Date and Passing State are not read-only as they may change
  public final ObjectProperty<Date> lastRunDate;

  public final ObjectProperty<Boolean> lastPassingState;

  public static class ValidationTestFileSetBuilder {
    private String id;
    private List<String> filePaths;
    private String remapFile;
    private CommonWellDocumented.SOURCE cwdSource;
    private String relDnaSerFile;
    private Date lastRunDate;
    private Boolean lastPassingState;

    public ValidationTestFileSetBuilder id(String id) {
      this.id = id;
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

    public ValidationTestFileSet build() {
      return new ValidationTestFileSet(id, filePaths, remapFile, cwdSource, relDnaSerFile,
          lastRunDate, lastPassingState);
    }

  }

  public static ValidationTestFileSetBuilder builder() {
    return new ValidationTestFileSetBuilder();
  }

  private ValidationTestFileSet(String id, List<String> filePaths, String remapFile,
      CommonWellDocumented.SOURCE cwdSource, String relDnaSerFile, Date lastRunDate,
      Boolean lastPassingState) {
    this.id = new ReadOnlyStringWrapper(id);
    this.filePaths = new ReadOnlyListWrapper<>(FXCollections.observableList(filePaths));
    this.remapFile = new ReadOnlyStringWrapper(remapFile);
    this.cwdSource = new ReadOnlyObjectWrapper<>(cwdSource);
    this.relDnaSerFile = new ReadOnlyStringWrapper(relDnaSerFile);
    this.lastRunDate = new SimpleObjectProperty<>(lastRunDate);
    this.lastPassingState = new SimpleObjectProperty<>(lastPassingState);
  }


}
