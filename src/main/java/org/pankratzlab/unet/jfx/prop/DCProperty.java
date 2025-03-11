package org.pankratzlab.unet.jfx.prop;

import static com.dlsc.preferencesfx.util.Constants.DEFAULT_DIVIDER_POSITION;
import static com.dlsc.preferencesfx.util.Constants.DEFAULT_PREFERENCES_HEIGHT;
import static com.dlsc.preferencesfx.util.Constants.DEFAULT_PREFERENCES_POS_X;
import static com.dlsc.preferencesfx.util.Constants.DEFAULT_PREFERENCES_POS_Y;
import static com.dlsc.preferencesfx.util.Constants.DEFAULT_PREFERENCES_WIDTH;
import static com.dlsc.preferencesfx.util.Constants.DIVIDER_POSITION;
import static com.dlsc.preferencesfx.util.Constants.SELECTED_CATEGORY;
import static com.dlsc.preferencesfx.util.Constants.WINDOW_HEIGHT;
import static com.dlsc.preferencesfx.util.Constants.WINDOW_POS_X;
import static com.dlsc.preferencesfx.util.Constants.WINDOW_POS_Y;
import static com.dlsc.preferencesfx.util.Constants.WINDOW_WIDTH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pankratzlab.unet.deprecated.hla.DonorCheckProperties;
import org.pankratzlab.unet.parser.HtmlDonorParser;
import org.pankratzlab.unet.parser.PdfDonorParser;
import org.pankratzlab.unet.parser.XmlDonorParser;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.util.StorageHandler;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class DCProperty<T> {

  public enum DCCATEGORY {
    INPUT_OPTIONS("Input Options"), DATA_SOURCE_OPTIONS("Data Sources");

    private final String categoryName;

    private DCCATEGORY(String categoryName) {
      this.categoryName = categoryName;
    }

    public String getCategoryName() {
      return categoryName;
    }

  }

  public enum DCSUBCATEGORY {

    SCORE6("SCORE 6"), SURETYPER("SureTyper");

    private final String groupName;

    private DCSUBCATEGORY(String groupName) {
      this.groupName = groupName;
    }

    public String getGroupName() {
      return groupName;
    }

  }

  private final String key;
  private final String name;
  private final DCCATEGORY category;
  private final DCSUBCATEGORY group;
  private final Function<String, Object> fromConverter;
  private final Function<Object, String> toConverter;

  DCProperty(String key, String name, DCCATEGORY category, DCSUBCATEGORY group, Function<String, Object> fromConverter,
      Function<Object, String> toConverter) {
    this.key = key;
    this.name = name;
    this.category = category;
    this.group = group;
    this.fromConverter = fromConverter;
    this.toConverter = toConverter;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public DCCATEGORY getCategory() {
    return category;
  }

  public DCSUBCATEGORY getGroup() {
    return group;
  }

  public Setting getSetting() {
    Setting s = buildSetting();
    register(s, this);
    return s;
  }

  // abstract methods

  @SuppressWarnings("rawtypes")
  protected abstract Setting buildSetting();

  public void setStringValue(String value) {
    setValue((T) fromConverter.apply(value));
  }

  public abstract void setValue(T value);


  public static class DCListProperty<T> extends DCProperty<T> {

    T defaultSelection;
    T[] possibleValues;

    @SafeVarargs
    public DCListProperty(String key, String name, DCCATEGORY category, DCSUBCATEGORY group, Function<String, Object> fromConverter,
        Function<Object, String> toConverter, T value, T... values) {
      super(key, name, category, group, fromConverter, toConverter);
      this.defaultSelection = value;
      this.possibleValues = values;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Setting buildSetting() {
      ObservableList<T> list = FXCollections.observableArrayList(possibleValues);
      SimpleObjectProperty<T> prop = new SimpleObjectProperty<>(defaultSelection);
      return Setting.of(this.getName(), list, prop).customKey(this.getKey());
    }

    @Override
    public void setValue(T value) {
      this.defaultSelection = value;
    }

  }

  public final static class DCStringListProperty extends DCListProperty<String> {

    public DCStringListProperty(String key, String name, DCCATEGORY category, DCSUBCATEGORY group, String value, String... values) {
      super(key, name, category, group, s -> s, s -> s.toString(), value, values);
    }

  }

  public final static class DCBooleanProperty extends DCProperty<Boolean> {

    boolean defaultValue;

    public DCBooleanProperty(String key, String name, DCCATEGORY category, DCSUBCATEGORY group, boolean value) {
      super(key, name, category, group, Boolean::parseBoolean, b -> b.toString());
      this.defaultValue = value;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Setting buildSetting() {
      BooleanProperty p = new SimpleBooleanProperty(defaultValue);
      Setting s = Setting.of(this.getName(), p);
      s = s.customKey(this.getKey());
      return s;
    }

    @Override
    public void setValue(Boolean value) {
      this.defaultValue = value;
    }

  }

  public static final DCStringListProperty FIRST_TYPE =
      new DCStringListProperty(DonorCheckProperties.FIRST_TYPE, "First source type dropdown default", DCCATEGORY.INPUT_OPTIONS, null,
          DonorCheckProperties.FIRST_TYPE_DEFAULT, PdfDonorParser.getTypeString(), XmlDonorParser.getTypeString(), HtmlDonorParser.getTypeString());

  public static final DCStringListProperty SECOND_TYPE =
      new DCStringListProperty(DonorCheckProperties.SECOND_TYPE, "Second source type dropdown default", DCCATEGORY.INPUT_OPTIONS, null,
          DonorCheckProperties.SECOND_TYPE_DEFAULT, PdfDonorParser.getTypeString(), XmlDonorParser.getTypeString(), HtmlDonorParser.getTypeString());

  public static final DCBooleanProperty USE_ALLELE_CALL =
      new DCBooleanProperty(DonorCheckProperties.USE_ALLELE_CALL, "Use manual allele assignments (<alleleCall>) value if present",
          DCCATEGORY.DATA_SOURCE_OPTIONS, DCSUBCATEGORY.SCORE6, Boolean.valueOf(DonorCheckProperties.USE_ALLELE_CALL_DEFAULT));

  public static final DCStringListProperty FAIL_OR_DISCARD_IF_AC_INVALID =
      new DCStringListProperty(DonorCheckProperties.FAIL_OR_DISCARD_IF_AC_INVALID,
          "How to handle invalid alleles in SCORE 6 manual allele assignments (<alleleCall>)", DCCATEGORY.DATA_SOURCE_OPTIONS, DCSUBCATEGORY.SCORE6,
          DonorCheckProperties.AC_INVALID_DISCARD, DonorCheckProperties.AC_INVALID_FAIL, DonorCheckProperties.AC_INVALID_DISCARD);

  public static final DCBooleanProperty SURETYPER_ALLOW_INVALID_DQA_ALLELES =
      new DCBooleanProperty(DonorCheckProperties.SURETYPER_ALLOW_INVALID_DQA_ALLELES,
          "XML Files: Allow non-official (invalid) allele group names in DQA locus (e.g. DQA06 instead of DQA1*06)", DCCATEGORY.DATA_SOURCE_OPTIONS,
          DCSUBCATEGORY.SURETYPER, Boolean.valueOf(DonorCheckProperties.SURETYPER_ALLOW_INVALID_DQA_ALLELES_DEFAULT));

  private static final List<DCProperty<?>> ALL_PROPS = new ArrayList<>() {
    {
      add(FIRST_TYPE);
      add(SECOND_TYPE);
      add(USE_ALLELE_CALL);
      // add(FAIL_OR_DISCARD_IF_AC_INVALID); // not currently allowing as an option
      add(SURETYPER_ALLOW_INVALID_DQA_ALLELES);
    }
  };

  private static final Map<String, DCProperty<?>> KEY_MAP = new HashMap<>();

  private static Setting register(Setting s, DCProperty p) {
    s.breadcrumbProperty().addListener((observable, oldValue, newValue) -> {
      if (oldValue != null && !oldValue.isBlank()) {
        KEY_MAP.remove(oldValue);
      }
      KEY_MAP.put(newValue, p);
    });
    KEY_MAP.put(p.getKey(), p);
    return s;
  }

  @SuppressWarnings("rawtypes")
  public static Category[] getTopLevelCategories() {
    List<Category> all = new ArrayList<>();

    for (DCCATEGORY c : DCCATEGORY.values()) {
      // find all properties with this category
      List<DCProperty<?>> catProps = ALL_PROPS.stream().filter(dcp -> dcp.getCategory() == c).collect(Collectors.toList());

      // find all "top-level" properties for this category
      List<DCProperty<?>> noSubProps = catProps.stream().filter(dcp -> dcp.getGroup() == null).collect(Collectors.toList());

      // final all sub-category properties and group together in-order
      Multimap<DCSUBCATEGORY, DCProperty<?>> subMap = ArrayListMultimap.create();
      catProps.stream().filter(dcp -> dcp.getGroup() != null).forEach(dcp -> subMap.put(dcp.getGroup(), dcp));

      Setting[] topLevel = noSubProps.stream().map(DCProperty::getSetting).toArray(Setting[]::new);

      Category[] subCats = subMap.keySet().stream().sorted().map(subCat -> {
        Setting[] catLevel = subMap.get(subCat).stream().map(DCProperty::getSetting).toArray(Setting[]::new);
        return Category.of(subCat.getGroupName(), catLevel);
      }).toArray(Category[]::new);

      all.add(Category.of(c.getCategoryName(), topLevel).subCategories(subCats));
    }

    return all.toArray(Category[]::new);
  }

  public static final class PropertiesStorageHandler implements StorageHandler {

    private static final String DCP = "DCP_";

    Properties preferences;

    public PropertiesStorageHandler() {
      preferences = DonorCheckProperties.get();

      for (DCProperty<?> dcp : ALL_PROPS) {
        if (preferences.containsKey(dcp.getKey())) {
          dcp.setStringValue(preferences.getProperty(dcp.getKey()));
        }
      }
    }

    public void saveSelectedCategory(String breadcrumb) {
      preferences.setProperty(DCP + SELECTED_CATEGORY, breadcrumb);
    }

    /**
     * Gets the last selected category in TreeSearchView.
     *
     * @return the breadcrumb string of the selected category. null if none is found
     */
    public String loadSelectedCategory() {
      return preferences.getProperty(DCP + SELECTED_CATEGORY, null);
    }

    /**
     * Stores the given divider position of the MasterDetailPane.
     *
     * @param dividerPosition the divider position to be stored
     */
    public void saveDividerPosition(double dividerPosition) {
      putDouble(DCP + DIVIDER_POSITION, dividerPosition);
    }

    /**
     * Gets the stored divider position of the MasterDetailPane.
     *
     * @return the double value of the divider position. 0.2 if none is found
     */
    public double loadDividerPosition() {
      return getDouble(DCP + DIVIDER_POSITION, DEFAULT_DIVIDER_POSITION);
    }

    /**
     * Stores the window width of the PreferencesFxDialog.
     *
     * @param windowWidth the width of the window to be stored
     */
    public void saveWindowWidth(double windowWidth) {
      putDouble(DCP + WINDOW_WIDTH, windowWidth);
    }

    /**
     * Searches for the window width of the PreferencesFxDialog.
     *
     * @return the double value of the window width. 1000 if none is found
     */
    public double loadWindowWidth() {
      return getDouble(DCP + WINDOW_WIDTH, DEFAULT_PREFERENCES_WIDTH);
    }

    /**
     * Stores the window height of the PreferencesFxDialog.
     *
     * @param windowHeight the height of the window to be stored
     */
    public void saveWindowHeight(double windowHeight) {
      putDouble(DCP + WINDOW_HEIGHT, windowHeight);
    }

    /**
     * Searches for the window height of the PreferencesFxDialog.
     *
     * @return the double value of the window height. 700 if none is found
     */
    public double loadWindowHeight() {
      return getDouble(DCP + WINDOW_HEIGHT, DEFAULT_PREFERENCES_HEIGHT);
    }

    /**
     * Stores the position of the PreferencesFxDialog in horizontal orientation.
     *
     * @param windowPosX the double value of the window position in horizontal orientation
     */
    public void saveWindowPosX(double windowPosX) {
      putDouble(DCP + WINDOW_POS_X, windowPosX);
    }

    /**
     * Searches for the horizontal window position.
     *
     * @return the double value of the horizontal window position
     */
    public double loadWindowPosX() {
      return getDouble(DCP + WINDOW_POS_X, DEFAULT_PREFERENCES_POS_X);
    }

    /**
     * Stores the position of the PreferencesFxDialog in vertical orientation.
     *
     * @param windowPosY the double value of the window position in vertical orientation
     */
    public void saveWindowPosY(double windowPosY) {
      putDouble(DCP + WINDOW_POS_Y, windowPosY);
    }

    /**
     * Searches for the vertical window position.
     *
     * @return the double value of the vertical window position
     */
    public double loadWindowPosY() {
      return getDouble(DCP + WINDOW_POS_Y, DEFAULT_PREFERENCES_POS_Y);
    }

    private void putDouble(String key, double value) {
      preferences.setProperty(key, Double.toString(value));
    }

    private double getDouble(String key, double defaultValue) {
      return Double.parseDouble(preferences.getProperty(key, Double.toString(defaultValue)));
    }

    @Override
    public void saveObject(String breadcrumb, Object object) {
      DCProperty dcp = KEY_MAP.get(breadcrumb);
      Function<Object, String> toF = dcp.toConverter;
      preferences.setProperty(dcp.getKey(), toF.apply(object));
    }

    @Override
    public Object loadObject(String breadcrumb, Object defaultObject) {
      DCProperty dcp = KEY_MAP.get(breadcrumb);
      Function<String, Object> fromF = dcp.fromConverter;
      String p = preferences.getProperty(dcp.getKey());
      if (p == null)
        return defaultObject;
      return fromF.apply(p);
    }

    @Override
    public <T> T loadObject(String breadcrumb, Class<T> type, T defaultObject) {
      DCProperty dcp = KEY_MAP.get(breadcrumb);
      Function<String, Object> fromF = dcp.fromConverter;
      String p = preferences.getProperty(dcp.getKey());
      if (p == null)
        return defaultObject;
      return (T) fromF.apply(p);
    }

    private static final String listSeparator = ";";

    @Override
    public ObservableList loadObservableList(String breadcrumb, ObservableList defaultObservableList) {
      DCProperty dcp = KEY_MAP.get(breadcrumb);
      String p = preferences.getProperty(dcp.getKey());
      String[] pts = p.split(listSeparator);
      return FXCollections.observableArrayList(Arrays.stream(pts).map(dcp.fromConverter).collect(Collectors.toList()));
    }

    @Override
    public <T> ObservableList<T> loadObservableList(String breadcrumb, Class<T> type, ObservableList<T> defaultObservableList) {
      DCProperty dcp = KEY_MAP.get(breadcrumb);
      String p = preferences.getProperty(dcp.getKey());
      String[] pts = p.split(listSeparator);
      List<T> cL = Arrays.stream(pts).map(pt -> dcp.fromConverter.apply(pt)).map(o -> type.cast(o)).collect(Collectors.toList());
      return FXCollections.observableArrayList(cL);
    }

    @Override
    public boolean clearPreferences() {
      preferences.clear();
      return true;
    }

  }


}
