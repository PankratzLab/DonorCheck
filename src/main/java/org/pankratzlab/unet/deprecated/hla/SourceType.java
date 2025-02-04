package org.pankratzlab.unet.deprecated.hla;

import java.io.File;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.HtmlDonorParser;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;

public enum SourceType {
  Score6("SCORE 6"), SureTyper("SureTyper"), DonorNet("DonorNet");

  private final String displayName;

  SourceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static SourceType parseType(File file) {
    if (file.getName().toLowerCase().endsWith(".html")) {
      return SourceType.DonorNet;
    } else if (file.getName().toLowerCase().endsWith(".xml")) {
      return XmlDonorParser.getSourceType(file);
    } else if (file.getName().endsWith(".pdf")) {
      return SourceType.SureTyper;
    }
    throw new IllegalArgumentException("Unknown File Type: " + file.getName());
  }

  public static void parseFile(ValidationModelBuilder builder, File file) {
    if (file.getName().toLowerCase().endsWith(".html")) {
      new HtmlDonorParser().parseModel(builder, file);
    } else if (file.getName().toLowerCase().endsWith(".xml")) {
      new XmlDonorParser().parseModel(builder, file);
    } else if (file.getName().endsWith(".pdf")) {
      new PdfDonorParser().parseModel(builder, file);
    }
  }


}
