package org.pankratzlab.unet.model.remap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.BackgroundDataProcessor;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SeroType;
import org.pankratzlab.unet.hapstats.CommonWellDocumented;
import org.pankratzlab.unet.hapstats.CommonWellDocumented.Status;
import org.pankratzlab.unet.jfx.StyleableContingentChoiceDialog;
import org.pankratzlab.unet.jfx.StyleableContingentChoiceDialog.Option;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.AllelePairings;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.SortedSetMultimapBuilder;
import com.google.common.collect.Sets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class GUIRemapProcessor implements RemapProcessor {

  private final BackgroundDataProcessor<HLALocus, GUIRemapProcessor.PresentableAlleleChoices> choiceSupplier;

  public GUIRemapProcessor(
      BackgroundDataProcessor<HLALocus, GUIRemapProcessor.PresentableAlleleChoices> choiceSupplier) {
    this.choiceSupplier = choiceSupplier;
  }

  @Override
  public Pair<Set<TypePair>, Set<TypePair>> processRemapping(HLALocus locus,
      ValidationModelBuilder builder) throws CancellationException {
    AllelePairings allelePairs = builder.getPossibleAllelePairsForLocus(locus);
    AllelePairings donorPairs = builder.getDonorAllelePairsForLocus(locus);

    // Set<SeroType> locusSet = builder.getCWDSeroTypesForLocus(locus);
    Set<SeroType> locusSetNonCWD = builder.getAllSeroTypesForLocus(locus);
    Set<HLAType> typesSet = builder.getCWDTypesForLocus(locus);
    Set<HLAType> typesSetNonCWD = builder.getAllTypesForLocus(locus);

    if (allelePairs == null || donorPairs == null) {
      // TODO
      System.out.println("No allele pairs for locus " + locus);
      return null;
    }
    if (typesSetNonCWD == null || typesSetNonCWD.isEmpty()) {
      // TODO
      System.out.println("No types for locus " + locus);
      return null;
    }

    // FIXME TODO this needs to be done on a Task thread to show a progress spinner if it blocks
    // while loading. However, the choice dialogs need to be created on the JavaFX thread.
    // But also, this method overall needs to return a single validation result object ...
    GUIRemapProcessor.PresentableAlleleChoices choices = choiceSupplier.get(locus);

    Iterator<HLAType> typeIter;
    Iterator<SeroType> seroIter;

    typeIter = typesSet.iterator();
    HLAType hlaType1_CIWD = typeIter.next();
    HLAType hlaType2_CIWD = typeIter.hasNext() ? typeIter.next() : hlaType1_CIWD;

    // seroIter = locusSet.iterator();
    // SeroType seroType1_CIWD = seroIter.next();
    // SeroType seroType2_CIWD = seroIter.hasNext() ? seroIter.next() : seroType1_CIWD;

    typeIter = typesSetNonCWD.iterator();
    HLAType hlaType1_First = typeIter.next();
    HLAType hlaType2_First = typeIter.hasNext() ? typeIter.next() : hlaType1_CIWD;

    seroIter = locusSetNonCWD.iterator();
    SeroType seroType1_First = seroIter.next();
    SeroType seroType2_First = seroIter.hasNext() ? seroIter.next() : seroType1_First;

    HBox assignedPane = new HBox(10);
    assignedPane.setMaxWidth(Double.MAX_VALUE);
    assignedPane.setAlignment(Pos.CENTER_LEFT);
    Label assignedHeader = new Label("Assigned allele pair: ");
    TextFlow assignedTextFlow = new TextFlow();
    CIWDAlleleStringPresentationUtils.addTextNodes(assignedTextFlow, hlaType1_First.toString(),
        true);
    assignedTextFlow.getChildren().add(new Text(" | "));
    CIWDAlleleStringPresentationUtils.addTextNodes(assignedTextFlow, hlaType2_First.toString(),
        true);
    Label assignedHeaderLbl = new Label("", assignedTextFlow);
    assignedHeaderLbl.setMaxWidth(Double.MAX_VALUE);
    assignedHeaderLbl.setMaxHeight(Double.MAX_VALUE);
    assignedPane.getChildren().add(assignedHeader);
    assignedPane.getChildren().add(getSpacer());
    assignedPane.getChildren().add(assignedHeaderLbl);

    HBox ciwdPane = new HBox(10);
    ciwdPane.setMaxWidth(Double.MAX_VALUE);
    ciwdPane.setAlignment(Pos.CENTER_LEFT);
    Label ciwdHeader = new Label("Common / Well-Documented allele pair: ");
    TextFlow ciwdTextFlow = new TextFlow();
    CIWDAlleleStringPresentationUtils.addTextNodes(ciwdTextFlow, hlaType1_CIWD.toString(), true);
    ciwdTextFlow.getChildren().add(new Text(" | "));
    CIWDAlleleStringPresentationUtils.addTextNodes(ciwdTextFlow, hlaType2_CIWD.toString(), true);
    Label ciwdHeaderLbl = new Label("", ciwdTextFlow);
    ciwdHeaderLbl.setMaxWidth(Double.MAX_VALUE);
    ciwdHeaderLbl.setMaxHeight(Double.MAX_VALUE);
    ciwdPane.getChildren().add(ciwdHeader);
    ciwdPane.getChildren().add(getSpacer());
    ciwdPane.getChildren().add(ciwdHeaderLbl);

    String text = "Or manually select allele pair for this locus";
    String text1 =
        "(selecting the first allele will populate valid pairings for the second allele)";

    final List<Supplier<TextFlow>> allChoices = choices.getAllChoices();

    Function<Supplier<TextFlow>, String> tooltipProvider = (tf) -> {
      return Objects.toString(choices.dataMap.get(tf), null);
    };

    String filterName = "Show CIWD Alleles Only";
    Predicate<Supplier<TextFlow>> filter = (tf) -> {
      return isAnyPartCWD(choices.getData(tf));
    };

    StyleableContingentChoiceDialog.Option<Supplier<TextFlow>> opt1 =
        new Option<>(assignedPane, choices.getChoiceForAllele(hlaType1_First.toString()),
            choices.getChoiceForAllele(hlaType2_First.toString()));

    final Supplier<TextFlow> choiceForAllele1 =
        choices.getChoiceForAllele(hlaType1_CIWD.toString());
    final Supplier<TextFlow> choiceForAllele2 =
        choices.getChoiceForAllele(hlaType2_CIWD.toString());
    StyleableContingentChoiceDialog.Option<Supplier<TextFlow>> opt2 =
        new Option<>(ciwdPane, choiceForAllele1, choiceForAllele2);

    Label manualChoice = new Label(text + System.lineSeparator() + text1);

    StyleableContingentChoiceDialog<Supplier<TextFlow>> cd = new StyleableContingentChoiceDialog<>(
        null, allChoices, Lists.newArrayList(), choices.getAllSecondChoices(), choices.dataMap,
        filterName, filter/* , textEntryMatcher */, opt1, opt2, manualChoice);
    cd.setTitle("Select HLA-" + locus.name() + " Alleles");

    cd.setHeaderText(
        "Assigned allele pair for HLA-" + locus.name() + " locus is not Common / Well-Documented.");
    cd.setContentText("Please select desired allele pair for this locus:");

    cd.setCombo1CellFactory(listView -> new SimpleTableObjectListCell(tooltipProvider));
    cd.setCombo1ButtonCell(new SimpleTableObjectListCell(tooltipProvider));
    cd.setCombo2CellFactory(listView -> new SimpleTableObjectListCell(tooltipProvider));
    cd.setCombo2ButtonCell(new SimpleTableObjectListCell(tooltipProvider));
    cd.setConverter1(new AlleleStringConverter(choices));
    cd.setConverter2(new AlleleStringConverter(choices));

    Optional<Supplier<TextFlow>> result = cd.showAndWait();

    if (!result.isPresent()) {
      throw new CancellationException();
    }

    Supplier<TextFlow> selAllele1 = result.get();
    Supplier<TextFlow> selAllele2 = cd.getSelectedSecondItem();

    String allele1 = choices.getData(selAllele1);
    String allele2 = choices.getData(selAllele2);

    HLAType hlaType1 = HLAType.valueOf(allele1);
    HLAType hlaType2 = HLAType.valueOf(allele2);
    SeroType seroType1 = hlaType1.equivSafe();
    SeroType seroType2 = hlaType2.equivSafe();

    boolean a1Match =
        hlaType1.compareTo(hlaType1_First) == 0 || hlaType1.compareTo(hlaType2_First) == 0;
    boolean a2Match =
        hlaType2.compareTo(hlaType1_First) == 0 || hlaType2.compareTo(hlaType2_First) == 0;

    if (!a1Match || !a2Match) {
      // locusSet.clear();
      // locusSet.add(seroType1);
      // locusSet.add(seroType2);
      // typesSet.clear();
      // typesSet.add(hlaType1);
      // typesSet.add(hlaType2);

      ImmutableSortedSet<TypePair> prevSet =
          ImmutableSortedSet.of(new TypePair(hlaType1_First, seroType1_First),
              new TypePair(hlaType2_First, seroType2_First));

      ImmutableSortedSet<TypePair> newSet = ImmutableSortedSet.of(new TypePair(hlaType1, seroType1),
          new TypePair(hlaType2, seroType2));


      return Pair.of(prevSet, newSet);
    }

    return null;
  }

  private Region getSpacer() {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    return spacer;
  }

  private boolean isAnyPartCWD(String allele) {
    HLAType alleleType = HLAType.valueOf(allele);
    Status status1 = CommonWellDocumented.getStatus(alleleType);

    return status1 != Status.UNKNOWN;
  }

  public final static class AlleleStringConverter extends StringConverter<Supplier<TextFlow>> {
    private final GUIRemapProcessor.PresentableAlleleChoices choices;

    private AlleleStringConverter(GUIRemapProcessor.PresentableAlleleChoices choices) {
      this.choices = choices;
    }

    @Override
    public String toString(Supplier<TextFlow> object) {
      final String selectedData = choices.getData(object);
      return selectedData == null ? "" : selectedData;
    }

    @Override
    public Supplier<TextFlow> fromString(String string) {
      return choices.getPresentation(string);
    }
  }

  public abstract static class PresentableDataChoices<T, R> {
    List<T> choices;
    Multimap<T, T> secondChoices;
    public BiMap<T, R> dataMap;

    public PresentableDataChoices(List<T> choices, Multimap<T, T> secondChoices,
        BiMap<T, R> presentationToDataMap) {
      this.choices = choices;
      this.secondChoices = secondChoices;
      this.dataMap = presentationToDataMap;
    }

    public List<T> getAllChoices() {
      return choices;
    }

    public Multimap<T, T> getAllSecondChoices() {
      return secondChoices;
    }

    public Collection<T> getSecondChoices(T firstChoice) {
      return secondChoices.get(firstChoice);
    }

    public abstract List<T> getMatchingChoices(String userInput);

    public R getData(T presentable) {
      return dataMap.get(presentable);
    }

    public T getPresentation(R data) {
      return dataMap.inverse().get(data);
    }

  }

  public static class PresentableAlleleChoices
      extends PresentableDataChoices<Supplier<TextFlow>, String> {

    public static PresentableAlleleChoices create(HLALocus locus,
        ValidationModelBuilder.AllelePairings allelePairings) {

      List<Supplier<TextFlow>> userChoices = new ArrayList<>();
      ListMultimap<Supplier<TextFlow>, Supplier<TextFlow>> secondChoices =
          SortedSetMultimapBuilder.hashKeys().arrayListValues().build();
      BiMap<Supplier<TextFlow>, String> presentationToDataMap = HashBiMap.create();

      List<String> alleleKeys = new ArrayList<>(allelePairings.getAlleleKeys());
      sortAlleleStrings(alleleKeys);

      LinkedListMultimap<String, String> choiceList =
          condenseIntoGroups(allelePairings, alleleKeys, true);

      for (String allele : choiceList.keySet()) {
        Supplier<TextFlow> supp = () -> CIWDAlleleStringPresentationUtils.getText(allele);

        userChoices.add(supp);
        presentationToDataMap.put(supp, allele);
      }


      for (Supplier<TextFlow> choice : userChoices) {
        String data = presentationToDataMap.get(choice);
        if (data == null) {
          continue;
        }
        String pairingAllele = data.contains("-") ? data.split("-")[0] : data;

        List<String> pairings = new ArrayList<>(allelePairings.getValidPairings(pairingAllele));
        sortAlleleStrings(pairings);

        LinkedListMultimap<String, String> secondChoicePairings =
            condenseIntoGroups(allelePairings, pairings, false);

        for (String pairing : secondChoicePairings.keySet()) {
          Supplier<TextFlow> presentationView = presentationToDataMap.inverse().get(pairing);
          if (presentationView == null) {
            presentationView = () -> CIWDAlleleStringPresentationUtils.getText(pairing);
            presentationToDataMap.put(presentationView, pairing);
          }
          secondChoices.put(choice, presentationView);
        }
      }

      Map<String, String> alleleToGroupMap = new HashMap<>();
      choiceList.entries().stream().forEach(e -> {
        alleleToGroupMap.put(e.getValue(), e.getKey());
      });

      return new PresentableAlleleChoices(userChoices, secondChoices, presentationToDataMap,
          allelePairings, alleleToGroupMap);
    }

    private static LinkedListMultimap<String, String> condenseIntoGroups(
        ValidationModelBuilder.AllelePairings allelePairings, List<String> alleleKeys,
        boolean checkPairings) {

      List<List<String>> subsets = new ArrayList<>();
      List<String> subset = new ArrayList<>();

      HLAType prev3FieldType = null;

      // all allele strings should be in the correct order now
      // now let's condense into subsets of the same serotypes, with
      // separate subsets for N/n and LSCAQ/lscaq alleles
      for (String allele : alleleKeys) {
        if (allele.matches(ValidationModelBuilder.NOT_EXPRESSED)
            || allele.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE)) {
          // make sure to add the currently-being-built subset first!
          if (subset.size() > 0) {
            subsets.add(subset);
            subset = new ArrayList<>();
          }
          // add null or lscaq alleles as single entries
          subsets.add(Lists.newArrayList(allele));
          // clear out known three-field type
          prev3FieldType = null;
          continue;
        }

        HLAType hType = HLAType.valueOf(allele);
        if (hType.resolution() <= 2) {
          // make sure to add the currently-being-built subset first!
          if (subset.size() > 0) {
            subsets.add(subset);
            subset = new ArrayList<>();
          }
          // add two-field alleles as single entries
          subsets.add(Lists.newArrayList(allele));
          // clear out known three-field type
          prev3FieldType = null;
          continue;
        }

        // we know resolution is >= 3

        HLAType curr3FieldType = (hType.resolution() == 3) ? hType
            : new HLAType(hType.locus(), hType.spec().get(0), hType.spec().get(1),
                hType.spec().get(2));
        if (prev3FieldType != null && prev3FieldType.compareTo(curr3FieldType) != 0) {
          // new three field type
          subsets.add(subset);
          subset = new ArrayList<>();
        }
        // same or new three-field type, either way:
        // update known three-field type, and
        // add four-field type to subset
        prev3FieldType = curr3FieldType;
        subset.add(allele);

      }
      if (subset.size() > 0) {
        subsets.add(subset);
      }

      LinkedListMultimap<String, String> groupToAllelesMap = LinkedListMultimap.create();

      // now we need to process the subset lists
      // single element subsets can be added directly
      // -- this preserves null/not-expressed subsets
      // multiple element subsets need to be further
      // separated into subsets: all alleles in a subset must
      // map to the same serotype and the same second choice alleles

      for (List<String> sub : subsets) {
        if (sub.size() == 1) {
          groupToAllelesMap.put(sub.get(0), sub.get(0));
          continue;
        }

        List<List<String>> subSubsets = new ArrayList<>();
        List<String> subSubset = new ArrayList<>();

        SeroType prevSero = null;
        Set<String> prevPairings = null;

        for (String subAllele : sub) {
          HLAType type = HLAType.valueOf(subAllele);
          SeroType newSero = type.equivSafe();
          HashSet<String> newPairings =
              checkPairings ? Sets.newHashSet(allelePairings.getValidPairings(subAllele))
                  : new HashSet<>();
          if (prevSero == null) {
            prevSero = newSero;
            prevPairings = newPairings;
          } else if (prevSero.compareTo(newSero) != 0) {
            if (subSubset.size() > 0) {
              subSubsets.add(subSubset);
              subSubset = new ArrayList<>();
            }
            prevSero = newSero;
            prevPairings = newPairings;
          } else if (!prevPairings.containsAll(newPairings)
              || !newPairings.containsAll(prevPairings)) {
            if (subSubset.size() > 0) {
              subSubsets.add(subSubset);
              subSubset = new ArrayList<>();
            }
            prevSero = newSero;
            prevPairings = newPairings;
          }
          subSubset.add(subAllele);
        }
        if (subSubset.size() > 0) {
          subSubsets.add(subSubset);
          subSubset = new ArrayList<>();
        }

        for (List<String> subS : subSubsets) {
          if (subS.size() == 1) {
            groupToAllelesMap.put(subS.get(0), subS.get(0));
          } else {
            String groupName = subS.get(0) + "-" + subS.get(subS.size() - 1);
            for (String s : subS) {
              groupToAllelesMap.put(groupName, s);
            }
          }
        }
      }

      return groupToAllelesMap;
    }

    private static void sortAlleleStrings(List<String> alleleKeys) {
      alleleKeys.sort((s1, s2) -> {
        boolean check1N = s1.matches(ValidationModelBuilder.NOT_EXPRESSED);
        boolean check1C = s1.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE);
        boolean check2N = s2.matches(ValidationModelBuilder.NOT_EXPRESSED);
        boolean check2C = s2.matches(ValidationModelBuilder.NOT_ON_CELL_SURFACE);
        boolean check1 = check1N || check1C;
        boolean check2 = check2N || check2C;
        // char s1C = s1.charAt(s1.length() - 1);
        // char s2C = s2.charAt(s2.length() - 1);
        String s1Hs = check1 ? s1.substring(0, s1.length() - 1) : s1;
        String s2Hs = check2 ? s2.substring(0, s2.length() - 1) : s2;

        HLAType h1 = HLAType.valueOf(s1Hs);
        HLAType h2 = HLAType.valueOf(s2Hs);

        int comp;
        if ((comp = h1.compareTo(h2)) != 0)
          return comp;

        if (check1 && !check2) {
          // first element ends with special character, meaning second element should come first
          return 1;
        } else if (check2 && !check1) {
          // second element ends with special character, meaning first element should come first
          return -1;
        }

        // both end with a special character
        if (check1N && check2N) {
          // both null and same HLAType - these are the same allele
          return 0;
        } else if (check1N && !check2N) {
          // first element is null, second is lscaq, second comes first
          return 1;
        } else if (!check1N && check2N) {
          // first element is lscaq, second is null, first comes first
          return -1;
        }


        // this block could probably be condensed based on knowing null status from previous checks,
        // but it's easier to just duplicate the logic for now...

        // both end with a special character
        if (check1C && check2C) {
          // both null and same HLAType - these are the same allele
          return 0;
        } else if (check1C && !check2C) {
          // first element is null, second is lscaq, second comes first
          return 1;
        } else if (!check1C && check2C) {
          // first element is lscaq, second is null, first comes first
          return -1;
        }

        // TODO dunno what's going on here... probably should do something special?
        return 0;
      });
    }

    private final ValidationModelBuilder.AllelePairings allelePairs;

    Map<String, String> alleleToGroupKeyMap;

    public Supplier<TextFlow> getChoiceForAllele(String allele) {
      return getPresentation(alleleToGroupKeyMap.get(allele));
    }

    private PresentableAlleleChoices(List<Supplier<TextFlow>> choices,
        Multimap<Supplier<TextFlow>, Supplier<TextFlow>> secondChoices,
        BiMap<Supplier<TextFlow>, String> presentationToDataMap,
        ValidationModelBuilder.AllelePairings allelePairs, Map<String, String> alleleToGroupMap) {
      super(choices, secondChoices, presentationToDataMap);
      this.allelePairs = allelePairs;
      this.alleleToGroupKeyMap = alleleToGroupMap;
    }

    @Override
    public List<Supplier<TextFlow>> getMatchingChoices(String userInput) {
      return allelePairs.getMatchingAlleles(userInput).stream().map(s -> dataMap.inverse().get(s))
          .filter(Predicates.notNull()).collect(Collectors.toList());
    }

  }

  static class SimpleTableObjectListCell extends ListCell<Supplier<TextFlow>> {

    // private Function<Supplier<TextFlow>, Supplier<TextFlow>> tooltipProvider;
    private Function<Supplier<TextFlow>, String> tooltipProvider;
    private Tooltip tooltip = new Tooltip();

    public SimpleTableObjectListCell(Function<Supplier<TextFlow>, String> tooltipProvider) {
      // Function<Supplier<TextFlow>, Supplier<TextFlow>> tooltipProvider) {
      this.tooltipProvider = tooltipProvider;

      // hack for adjusting tooltip delay / etc
      // from https://stackoverflow.com/a/43291239/875496
      // TODO FIXME change when JavaFX9+ is available
      try {
        Class<?> clazz = tooltip.getClass().getDeclaredClasses()[0];
        Constructor<?> constructor = clazz.getDeclaredConstructor(Duration.class, Duration.class,
            Duration.class, boolean.class);
        constructor.setAccessible(true);
        Object tooltipBehavior = constructor.newInstance(new Duration(50), // open
            new Duration(500000), // visible
            new Duration(100), // close
            false);
        Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
        fieldBehavior.setAccessible(true);
        fieldBehavior.set(tooltip, tooltipBehavior);
      } catch (Throwable t) {
        t.printStackTrace();
      }

      tooltip.setWrapText(true);
      tooltip.setMaxWidth(600);

      /*
       * NB: TODO FIXME setting style here because I couldn't figure out how to locate it in an
       * actual CSS style sheet -- 04-18-2024, b.cole
       */
      tooltip.setStyle(
          "-fx-background: rgba(230,230,230); -fx-text-fill: black; -fx-background-color: rgba(230,230,230,0.95); -fx-background-radius: 5px; -fx-background-insets: 0; -fx-padding: 0.667em 0.75em 0.667em 0.75em; -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.5) , 10, 0.0 , 0 , 3 ); -fx-font-size: 0.95em;");
    }

    @Override
    public void updateItem(Supplier<TextFlow> item, boolean empty) {
      try {
        super.updateItem(item, empty);
        if (item != null && !empty) {
          setGraphic(item.get());

          String tf = tooltipProvider.apply(item);
          // Supplier<TextFlow> tf = tooltipProvider.apply(item);
          if (tf == null || tf.length() < 150) {
            // TODO don't show tooltips if item isn't too long!
            setTooltip(null);
          } else {
            // System.out.println("tool:: " + tf);
            tooltip.setText(tf);
            // final TextFlow value = tf.get();
            // value.setMaxWidth(tooltip.getMaxWidth());
            // tooltip.setGraphic(value);
            setTooltip(tooltip);
          }
        } else {
          setGraphic(null);
          setTooltip(null);
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

  }

}
