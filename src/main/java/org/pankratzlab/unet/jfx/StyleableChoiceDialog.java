package org.pankratzlab.unet.jfx;

import java.util.Collection;
import java.util.Map;
import com.google.common.collect.Multimap;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A dialog that shows a list of choices to the user, from which they can pick one item at most.
 *
 * @see Dialog
 * @param <T> The type of the items to show to the user, and the type that is returned via
 *        {@link #getResult()} when the dialog is dismissed.
 * @since JavaFX 8u40
 */
@SuppressWarnings("restriction")
public class StyleableChoiceDialog<T> extends Dialog<T> {

  /**************************************************************************
   *
   * Fields
   *
   **************************************************************************/

  private final GridPane grid;
  private final Label label;
  private final ComboBox<T> comboBox1;
  private final ComboBox<T> comboBox2;
  private final T defaultChoice;

  /**************************************************************************
   *
   * Constructor
   *
   **************************************************************************/

  /**
   * Creates a new ChoiceDialog instance with the first argument specifying the default choice that
   * should be shown to the user, and the second argument specifying a collection of all available
   * choices for the user. It is expected that the defaultChoice be one of the elements in the
   * choices collection. If this is not true, then defaultChoice will be set to null and the dialog
   * will show with the initial choice set to the first item in the list of choices.
   *
   * @param defaultChoice The item to display as the pre-selected choice in the dialog. This item
   *        must be contained within the choices varargs array.
   * @param choices All possible choices to present to the user.
   */
  public StyleableChoiceDialog(T defaultChoice, Collection<T> choices,
      Multimap<T, T> secondChoiceMap, Map<T, ? extends Object> valueMap) {
    final DialogPane dialogPane = getDialogPane();

    // -- grid
    this.grid = new GridPane();
    this.grid.setHgap(10);
    this.grid.setVgap(10);
    this.grid.setMaxWidth(Double.MAX_VALUE);
    this.grid.setAlignment(Pos.CENTER_LEFT);

    // -- label
    label = createContentLabel(dialogPane.getContentText());
    label.setPrefWidth(Region.USE_COMPUTED_SIZE);
    label.textProperty().bind(dialogPane.contentTextProperty());

    setTitle(ControlResources.getString("Dialog.confirm.title"));
    dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
    dialogPane.getStyleClass().add("choice-dialog");
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    final double MIN_WIDTH = 150;

    comboBox2 = new ComboBox<T>();
    comboBox2.setMinWidth(MIN_WIDTH);
    if (choices != null) {
      comboBox2.getItems().addAll(choices);
    }
    comboBox2.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(comboBox2, Priority.ALWAYS);
    GridPane.setFillWidth(comboBox2, true);
    comboBox2.setMaxHeight(10);
    comboBox2.getSelectionModel().selectFirst();

    comboBox1 = new ComboBox<T>();
    // comboBox1.setEditable(true);
    comboBox1.setMinWidth(MIN_WIDTH);
    if (choices != null) {
      comboBox1.getItems().addAll(choices);
    }
    comboBox1.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(comboBox1, Priority.ALWAYS);
    GridPane.setFillWidth(comboBox1, true);
    comboBox1.setMaxHeight(10);

    // comboBox1.setOnKeyTyped((kv) -> {
    // try {
    // comboBox1.getEditor().getText();
    // comboBox1.getItems().clear();
    //
    // comboBox1.getItems().addAll(choices);
    // comboBox1.show();
    // } catch (Throwable t) {
    // t.printStackTrace();
    // }
    // // TODO
    // });

    comboBox1.valueProperty().addListener((obs, oldV, newV) -> {
      try {
        final Collection<T> c = secondChoiceMap.get(newV);

        comboBox2.getSelectionModel().clearSelection();
        comboBox2.getItems().setAll(c);
        comboBox2.setDisable(c.isEmpty());
        comboBox2.setPromptText(c.isEmpty() ? "No valid pairings found." : null);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    });

    this.defaultChoice = comboBox1.getItems().contains(defaultChoice) ? defaultChoice : null;

    if (defaultChoice == null) {
      comboBox1.getSelectionModel().selectFirst();
    } else {
      comboBox1.getSelectionModel().select(defaultChoice);
    }

    dialogPane.contentTextProperty().addListener(o -> updateGrid(comboBox1));

    updateGrid(comboBox1);

    setResultConverter((dialogButton) -> {
      ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
      return data == ButtonData.OK_DONE ? getSelectedItem() : null;
    });
  }

  /**************************************************************************
   *
   * Public API
   *
   **************************************************************************/

  /**
   * Returns the currently selected item in the dialog.
   */
  public final T getSelectedItem() {
    return comboBox1.getSelectionModel().getSelectedItem();
  }

  public final T getSelectedSecondItem() {
    return comboBox2.getSelectionModel().getSelectedItem();
  }

  public void setCombo1CellFactory(Callback<ListView<T>, ListCell<T>> value) {
    comboBox1.setCellFactory(value);
  }

  public void setCombo2CellFactory(Callback<ListView<T>, ListCell<T>> value) {
    comboBox2.setCellFactory(value);
  }

  public void setCombo1ButtonCell(ListCell<T> value) {
    comboBox1.setButtonCell(value);
  }

  public void setCombo2ButtonCell(ListCell<T> value) {
    comboBox2.setButtonCell(value);
  }

  public void setConverter1(StringConverter<T> c) {
    comboBox1.setConverter(c);
  }

  public void setConverter2(StringConverter<T> c) {
    comboBox2.setConverter(c);
  }

  /**
   * Returns the property representing the currently selected item in the dialog.
   */
  public final ReadOnlyObjectProperty<T> selectedItemProperty() {
    return comboBox1.getSelectionModel().selectedItemProperty();
  }

  /**
   * Sets the currently selected item in the dialog.
   * 
   * @param item The item to select in the dialog.
   */
  public final void setSelectedItem(T item) {
    comboBox1.getSelectionModel().select(item);
  }

  /**
   * Returns the list of all items that will be displayed to users. This list can be modified by the
   * developer to add, remove, or reorder the items to present to the user.
   */
  public final ObservableList<T> getItems() {
    return comboBox1.getItems();
  }

  /**
   * Returns the default choice that was specified in the constructor.
   */
  public final T getDefaultChoice() {
    return defaultChoice;
  }

  /**************************************************************************
   *
   * Private Implementation
   *
   **************************************************************************/

  private static Label createContentLabel(String text) {
    Label label = new Label(text);
    label.setMaxWidth(Double.MAX_VALUE);
    label.setMaxHeight(Double.MAX_VALUE);
    label.getStyleClass().add("content");
    label.setWrapText(true);
    label.setPrefWidth(360);
    return label;
  }

  private void updateGrid(Node focusReq) {
    grid.getChildren().clear();

    grid.add(label, 0, 0);
    grid.add(comboBox1, 0, 1);
    grid.add(comboBox2, 0, 2);
    getDialogPane().setContent(grid);

    Platform.runLater(() -> focusReq.requestFocus());
  }
}
