package org.pankratzlab.unet.validation;

public final class TestExceptions {
  public static final class XMLRemapFileException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public XMLRemapFileException(String string, Exception e) {
      super(string, e);
    }
  }
  public static final class SourceFileParsingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SourceFileParsingException(String string, Exception e) {
      super(string, e);
    }
  }
}
