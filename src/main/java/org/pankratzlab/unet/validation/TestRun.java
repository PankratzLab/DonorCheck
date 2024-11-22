package org.pankratzlab.unet.validation;

import java.util.Date;

public class TestRun {
  public final ValidationTestFileSet testFileSet;
  public final TEST_RESULT result;
  public final Date runTime;

  public TestRun(ValidationTestFileSet testFileSet, TEST_RESULT result, Date runTime) {
    this.testFileSet = testFileSet;
    this.result = result;
    this.runTime = runTime;
  }

}