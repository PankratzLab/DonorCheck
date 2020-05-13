package org.pankratzlab.unet.jfx.macui;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.pankratzlab.unet.deprecated.hla.CurrentDirectoryProvider;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.NullType;
import org.pankratzlab.unet.deprecated.jfx.JFXUtilHelper;
import org.pankratzlab.unet.jfx.DonorNetUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.parser.DonorFileParser;
import org.pankratzlab.unet.parser.XmlDonorParser;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 * Controller for panel to select files to produce input formatted for MAC UI:
 * https://hml.nmdp.org/MacUI/
 */
public class MACUIController {

  @FXML private ResourceBundle resources;

  @FXML private URL location;

  @FXML private Text selectedFileName;

  @FXML private TextField strandOneTextField;
  @FXML private TextField strandTwoTextField;

  @FXML
  void selectScore6File(ActionEvent event) {
    XmlDonorParser parser = new XmlDonorParser();
    selectDonorFile(event, parser);
  }

  private void selectDonorFile(ActionEvent event, DonorFileParser donorParser) {
    Optional<File> optionalFile =
        DonorNetUtils.getFile(
            ((Node) event.getSource()),
            donorParser.fileChooserHeader(),
            donorParser.initialName(),
            donorParser.extensionDescription(),
            donorParser.extensionFilter(),
            true);

    if (optionalFile.isPresent()) {
      Task<Void> loadFileTask =
          JFXUtilHelper.createProgressTask(
              () -> {
                File selectedFile = optionalFile.get();

                ValidationModelBuilder builder = new ValidationModelBuilder();

                try {
                  donorParser.parseModel(builder, selectedFile);
                  selectedFileName.setText(selectedFile.getName());
                  ImmutableMultimap<Strand, HLAType> dpbHaplotypes = builder.getDpbHaplotypes();
                  strandOneTextField.setText(
                      makeCALString(HLALocus.DPB1, dpbHaplotypes.get(Strand.FIRST)));
                  strandTwoTextField.setText(
                      makeCALString(HLALocus.DPB1, dpbHaplotypes.get(Strand.SECOND)));
                } catch (Exception e) {
                  Platform.runLater(
                      () -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setHeaderText(
                            donorParser.getErrorText()
                                + "\nPlease notify the developers as this may indicate the data has changed."
                                + "\nOffending file: "
                                + selectedFile.getName());
                        alert.showAndWait();
                        e.printStackTrace();
                      });
                }

                CurrentDirectoryProvider.setBaseDir(selectedFile.getParentFile());
              });

      new Thread(loadFileTask).start();
    }
  }

  private String makeCALString(HLALocus locus, ImmutableCollection<HLAType> values) {
    if (values.isEmpty()) {
      return "N/A";
    }
    String result =
        locus.toString()
            + "*"
            + values
                .stream()
                // Ignore null types with > 2 fields. Because we're truncating to 2 fields,
                // converting something like 04:01:01:24N to 04:01N is incorrect - the N designation
                // only applied to that specific 4-field allele.
                .filter(a -> !(a instanceof NullType) || a.spec().size() < 3)
                .sorted()
                .map(
                    a ->
                        format(a.spec().get(0))
                            + ":"
                            + format(a.spec().get(1))
                            + (a instanceof NullType ? "N" : ""))
                .distinct()
                .collect(Collectors.joining("/"));
    return result;
  }

  private String format(Integer i) {
    return String.format("%02d", i);
  }

  @FXML
  void initialize() {
    assert selectedFileName != null
        : "fx:id=\"selectedFileName\" was not injected: check your FXML file 'MACUIConversionPanel.fxml'.";
    assert strandOneTextField != null
        : "fx:id=\"strandOneTextField\" was not injected: check your FXML file 'MACUIConversionPanel.fxml'.";
    assert strandTwoTextField != null
        : "fx:id=\"strandTwoTextField\" was not injected: check your FXML file 'MACUIConversionPanel.fxml'.";
  }
}
