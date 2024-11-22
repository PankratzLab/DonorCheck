package org.pankratzlab.unet.validation;

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