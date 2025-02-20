package org.pankratzlab.unet.validation;

import java.util.Date;
import java.util.Optional;
import org.pankratzlab.unet.model.ValidationTable.TableData;

public class TestRun {
  public final ValidationTestFileSet testFileSet;
  public final TEST_RESULT result;
  public final Date runTime;
  public final Optional<Exception> error;
  public final Optional<TableData> data;

  public TestRun(ValidationTestFileSet testFileSet, TEST_RESULT result, Date runTime, Optional<Exception> error, Optional<TableData> data) {
    this.testFileSet = testFileSet;
    this.result = result;
    this.runTime = runTime;
    this.error = error;
    this.data = data;
  }

}
