package org.pankratzlab.unet.jfx;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.common.collect.Multimap;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
public class StyleableContingentChoiceDialog<T> extends Dialog<T> {

  /**************************************************************************
   *
   * Fields
   *
   **************************************************************************/

  private final GridPane grid;
  private final Label label;
  private final RadioButton opt1Choice;
  private final RadioButton opt2Choice;
  private final RadioButton manualChoice;
  private final ComboBox<T> comboBox1;
  private final ComboBox<T> comboBox2;
  private final CheckBox checkboxFilter1;
  private final CheckBox checkboxFilter2;
  private final Predicate<T> filterPredicate;
  private final Option<T> opt1;
  private final Option<T> opt2;


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
   * @param InvalidCoices Choices which, if selected, will disable the "Ok" button
   * 
   *        TODO Encapsulate filterName / filterPredicate so they are optional
   */
  public StyleableContingentChoiceDialog(T defaultChoice, Collection<T> choices,
      Collection<T> invalidChoices, Multimap<T, T> secondChoiceMap,
      Map<T, ? extends Object> valueMap, String filterName,
      Predicate<T> filter/*
                          * , BiPredicate<String, T> textEntryMatcher
                          */, Option<T> opt1, Option<T> opt2, Node manualChoiceGraphic) {

    // this.textEntryPredicate = textEntryMatcher;
    this.opt1 = opt1;
    this.opt2 = opt2;

    final DialogPane dialogPane = getDialogPane();

    // -- grid
    this.grid = new GridPane();
    this.grid.setHgap(10);
    this.grid.setVgap(10);
    this.grid.setMaxWidth(Double.MAX_VALUE);
    this.grid.setAlignment(Pos.CENTER_LEFT);

    // -- label
    label = createContentLabel(dialogPane.getContentText());
    label.textProperty().bind(dialogPane.contentTextProperty());

    setTitle(ControlResources.getString("Dialog.confirm.title"));
    dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
    dialogPane.getStyleClass().add("choice-dialog");
    dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    final double MIN_WIDTH = 50;

    comboBox2 = new ComboBox<T>();
    comboBox2.setMinWidth(MIN_WIDTH);
    comboBox2.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(comboBox2, Priority.SOMETIMES);
    GridPane.setFillWidth(comboBox2, true);
    comboBox2.setMaxHeight(10);
    comboBox2.setDisable(true);

    comboBox1 = new ComboBox<T>();
    // comboBox1.setEditable(true);
    comboBox1.setMinWidth(MIN_WIDTH);
    if (choices != null) {
      comboBox1.getItems().addAll(choices);
    }
    comboBox1.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(comboBox1, Priority.SOMETIMES);
    GridPane.setFillWidth(comboBox1, true);
    comboBox1.setMaxHeight(10);

    this.filterPredicate = filter;

    checkboxFilter1 = new CheckBox(filterName);
    checkboxFilter2 = new CheckBox(filterName);

    checkboxFilter1.selectedProperty()
        .addListener((obs, oldV, newV) -> updateFirstComboChoices(newV, choices));
    checkboxFilter2.selectedProperty()
        .addListener((obs, oldV, newV) -> updateSecondComboState(secondChoiceMap,
            comboBox1.getSelectionModel().getSelectedItem(), newV));

    opt1Choice = new RadioButton();
    opt2Choice = new RadioButton();

    opt1.graphic.setOnMouseClicked(opt1Choice.getOnMouseClicked());
    opt1.graphic.setOnMouseEntered(opt1Choice.getOnMouseEntered());
    opt1.graphic.setOnMouseExited(opt1Choice.getOnMouseExited());
    opt1.graphic.setOnMousePressed(opt1Choice.getOnMousePressed());
    opt1.graphic.setOnMouseReleased(opt1Choice.getOnMouseReleased());

    opt2.graphic.setOnMouseClicked(opt2Choice.getOnMouseClicked());
    opt2.graphic.setOnMouseEntered(opt2Choice.getOnMouseEntered());
    opt2.graphic.setOnMouseExited(opt2Choice.getOnMouseExited());
    opt2.graphic.setOnMousePressed(opt2Choice.getOnMousePressed());
    opt2.graphic.setOnMouseReleased(opt2Choice.getOnMouseReleased());

    manualChoice = new RadioButton();
    manualChoice.setGraphic(manualChoiceGraphic);

    ToggleGroup group = new ToggleGroup();
    opt1Choice.setToggleGroup(group);
    opt2Choice.setToggleGroup(group);
    manualChoice.setToggleGroup(group);

    if (defaultChoice != null) {
      comboBox1.getSelectionModel().select(defaultChoice);
    }

    // these are basic bindings
    checkboxFilter1.disableProperty().bind(manualChoice.selectedProperty().not());
    checkboxFilter2.disableProperty().bind(manualChoice.selectedProperty().not());
    comboBox1.disableProperty().bind(manualChoice.selectedProperty().not());

    // this is a more complex binding:
    // we want to disable the combobox if no options are available
    comboBox2.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      boolean enable = manualChoice.selectedProperty().get()
          && !comboBox1.getSelectionModel().isEmpty() && !comboBox2.getItems().isEmpty();
      return !enable;
    }, manualChoice.selectedProperty(), comboBox1.selectionModelProperty(),
        comboBox1.getSelectionModel().selectedItemProperty(), comboBox2.itemsProperty()));

    // update the second combobox items when the first combobox selection changes
    comboBox1.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
      // comboBox1.valueProperty().addListener((obs, oldV, newV) -> {
      try {
        updateSecondComboState(secondChoiceMap, newV, checkboxFilter2.isSelected());
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    });

    Node okButton = dialogPane.lookupButton(ButtonType.OK);

    okButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return (comboBox2.disableProperty().get()
          || invalidChoices.contains(comboBox1.getSelectionModel().getSelectedItem())
          || invalidChoices.contains(comboBox2.getSelectionModel().getSelectedItem())
          || comboBox2.getSelectionModel().isEmpty())
          && !(opt1Choice.selectedProperty().get() || opt2Choice.selectedProperty().get());
      // for this .isEmpty() call to work,
      // the binding must include the
      // selectedItemProperty as a dependency
    }, comboBox2.disableProperty(), comboBox2.getSelectionModel().selectedItemProperty(),
        opt1Choice.selectedProperty(), opt2Choice.selectedProperty()));

    dialogPane.contentTextProperty()
        .addListener(o -> updateGrid(this.getDialogPane().lookupButton(ButtonType.CANCEL)));

    updateGrid(this.getDialogPane().lookupButton(ButtonType.CANCEL));

    setResultConverter((dialogButton) -> {
      ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
      return data == ButtonData.OK_DONE ? getSelectedItem() : null;
    });


  }

  private ObservableList<T> getFirstComboChoices(boolean filterActive, Collection<T> choices) {
    ObservableList<T> filteredChoices =
        choices.stream().filter(item -> !filterActive || filterPredicate.test(item))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
    return filteredChoices;
  }

  private ObservableList<T> getSecondComboChoices(Multimap<T, T> secondChoiceMap,
      T selectedFirstItem, boolean filterActive) {
    ObservableList<T> secondChoices;
    secondChoices = FXCollections.observableArrayList(secondChoiceMap.get(selectedFirstItem));
    if (filterActive) {
      secondChoices = secondChoices.stream().filter(filterPredicate)
          .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    return secondChoices;
  }

  private void updateFirstComboChoices(boolean filterActive, Collection<T> choices) {
    ObservableList<T> filteredChoices = getFirstComboChoices(filterActive, choices);
    comboBox1.setItems(filteredChoices);
    if (!filteredChoices.contains(comboBox1.getSelectionModel().getSelectedItem())) {
      comboBox1.getSelectionModel().clearSelection();
    }
  }

  private void updateSecondComboState(Multimap<T, T> secondChoiceMap, T selectedFirstItem,
      boolean filterActive) {
    ObservableList<T> secondChoices;
    secondChoices = getSecondComboChoices(secondChoiceMap, selectedFirstItem, filterActive);
    comboBox2.setItems(secondChoices);
    if (!secondChoices.contains(comboBox2.getSelectionModel().getSelectedItem())) {
      comboBox2.getSelectionModel().clearSelection();
    }
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
    if (manualChoice.isSelected()) {
      return comboBox1.getSelectionModel().getSelectedItem();
    } else if (opt1Choice.isSelected()) {
      return opt1.opt1;
    } else if (opt2Choice.isSelected()) {
      return opt2.opt1;
    }
    return null;
  }

  public final T getSelectedSecondItem() {
    if (manualChoice.isSelected()) {
      return comboBox2.getSelectionModel().getSelectedItem();
    } else if (opt1Choice.isSelected()) {
      return opt1.opt2;
    } else if (opt2Choice.isSelected()) {
      return opt2.opt2;
    }
    return null;
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
    HBox hBox1 = new HBox();
    hBox1.getChildren().add(opt1Choice);
    hBox1.getChildren().add(opt1.graphic);
    HBox.setHgrow(opt1.graphic, Priority.ALWAYS);


    grid.add(hBox1, 0, 1);
    HBox hBox2 = new HBox();
    hBox2.getChildren().add(opt2Choice);
    hBox2.getChildren().add(opt2.graphic);
    HBox.setHgrow(opt2.graphic, Priority.ALWAYS);
    grid.add(hBox2, 0, 2);
    grid.add(manualChoice, 0, 3);
    VBox subGrid = new VBox();
    subGrid.setSpacing(10);
    subGrid.setPadding(new Insets(0, 0, 0, 20));
    subGrid.getChildren().add(checkboxFilter1);
    subGrid.getChildren().add(comboBox1);
    subGrid.getChildren().add(checkboxFilter2);
    subGrid.getChildren().add(comboBox2);
    grid.add(subGrid, 0, 4);

    grid.setPadding(new Insets(10, 0, 10, 25));

    getDialogPane().setContent(grid);

    Platform.runLater(() -> focusReq.requestFocus());
  }

  public static class Option<T> {
    final Node graphic;
    final T opt1;
    final T opt2;

    public Option(Node graphic, T opt1, T opt2) {
      this.graphic = graphic;
      this.opt1 = opt1;
      this.opt2 = opt2;
    }

  }

}
