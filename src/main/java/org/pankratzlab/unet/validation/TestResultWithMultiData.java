package org.pankratzlab.unet.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.pankratzlab.unet.model.ModelData;

class TestResultWithMultiData {

  TEST_RESULT result;

  Map<String, Exception> errorFiles = new HashMap<>();
  Map<String, ModelData> invalidRemapFiles = new HashMap<>();
  Map<String, ModelData> invalidModels = new HashMap<>();
  Map<String, ModelData> validModels = new HashMap<>();

  boolean missingRemapFile = false;
  Optional<Exception> remapException = Optional.empty();

  public TestResultWithMultiData(TEST_RESULT result) {
    this.result = result;
  }

  public TestResultWithMultiData(TEST_RESULT result, Map<String, ModelData> validModels) {
    this.result = result;
    this.validModels = validModels;
  }

  public TestResultWithMultiData(TEST_RESULT result, Map<String, Exception> errorFiles, Map<String, ModelData> invalidRemapFiles,
      Map<String, ModelData> invalidModels, Map<String, ModelData> validModels, boolean missingRemapFile, Optional<Exception> remapException) {
    this.result = result;
    this.errorFiles = errorFiles;
    this.invalidRemapFiles = invalidRemapFiles;
    this.invalidModels = invalidModels;
    this.validModels = validModels;
    this.missingRemapFile = missingRemapFile;
    this.remapException = remapException;
  }

  public TEST_RESULT testResult() {
    return result;
  }

  public Optional<Exception> getException() {
    if (remapException.isPresent())
      return remapException;
    if (!errorFiles.isEmpty()) {
      return Optional.of(errorFiles.values().iterator().next());
    }
    return Optional.empty();
  }

  public Map<String, ModelData> getValidModels() {
    return validModels;
  }

}
