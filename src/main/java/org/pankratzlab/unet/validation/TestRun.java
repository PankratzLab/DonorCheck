package org.pankratzlab.unet.validation;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.pankratzlab.unet.model.ModelData;

public class TestRun {

  public static class TestRunMetadata {
    public final TEST_RESULT result;
    public final Date runTime;

    public TestRunMetadata(TEST_RESULT result, Date runTime) {
      this.result = result;
      this.runTime = runTime;
    }
  }

  public final ValidationTestFileSet testFileSet;
  public final TestRunMetadata testRunMetadata;
  public final Optional<Exception> error;

  public final Map<String, ModelData> data;

  public TestRun(ValidationTestFileSet testFileSet, TEST_RESULT result, Date runTime, Optional<Exception> error, Map<String, ModelData> data) {
    this.testFileSet = testFileSet;
    this.testRunMetadata = new TestRunMetadata(result, runTime);
    this.error = error;
    this.data = data;
  }

}
