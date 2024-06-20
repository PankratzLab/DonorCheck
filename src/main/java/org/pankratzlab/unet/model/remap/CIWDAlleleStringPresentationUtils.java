package org.pankratzlab.unet.model.remap;

import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class CIWDAlleleStringPresentationUtils {


  public static TextFlow getText(String allele) {
    TextFlow flow = new TextFlow();
    if (allele.contains("-")) {
      String[] a = allele.split("-");
      addTextNodes(flow, a[0], false);
      flow.getChildren().add(new Text(" - "));
      addTextNodes(flow, a[1], true);
    } else {
      addTextNodes(flow, allele, true);
    }

    return flow;
  }

  static void addTextNodes(TextFlow flow, final String allele, boolean addSero) {
    HLAType alleleType = HLAType.valueOf(allele);
    HLAType cwdType1 = CommonWellDocumented.getCWDType(alleleType);
    Status status1 = CommonWellDocumented.getStatus(alleleType);

    flow.getChildren().add(new Text(alleleType.locus().name() + "*"));

    String specString = alleleType.specString();
    boolean match = allele.matches(ValidationModelBuilder.NOT_EXPRESSED)
        || allele.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE);

    if (status1 != Status.UNKNOWN) {

      if (cwdType1.specString().length() < specString.length()) {
        Text t1 = new Text(cwdType1.specString());
        t1.setStyle("-fx-font-weight:bold;");
        flow.getChildren().add(t1);
        flow.getChildren().add(new Text(specString.substring(cwdType1.specString().length())));
      } else {
        Text t1 = new Text(specString);
        t1.setStyle("-fx-font-weight:bold;");
        flow.getChildren().add(t1);
      }

      if (match) {
        flow.getChildren().add(new Text("" + allele.charAt(allele.length() - 1)));
      }

    } else {
      flow.getChildren()
          .add(new Text(specString + (match ? ("" + allele.charAt(allele.length() - 1)) : "")));
    }

    if (addSero) {
      flow.getChildren().add(
          new Text(" (" + alleleType.locus().name() + alleleType.equivSafe().specString() + ")"));
    }
  }

}
