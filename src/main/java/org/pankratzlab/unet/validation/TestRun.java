package org.pankratzlab.unet.validation;

import java.util.Date;
import java.util.Optional;

public class TestRun {
  public final ValidationTestFileSet testFileSet;
  public final TEST_RESULT result;
  public final Date runTime;
  public final Optional<Exception> error;

  public TestRun(ValidationTestFileSet testFileSet, TEST_RESULT result, Date runTime,
      Optional<Exception> error) {
    this.testFileSet = testFileSet;
    this.result = result;
    this.runTime = runTime;
    this.error = error;
  }

}
