/*-
 * #%L
 * DonorCheck
 * %%
 * Copyright (C) 2018 - 2019 Computational Pathology - University of Minnesota
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.pankratzlab.unet.model;

/**
 * Mutable builder class for creating a {@link ValidationModel}.
 *
 * <p>
 * Use the locus-specific setters to build up the donor typing. Each must be called at least once
 * (for homozygous) and at most twice (for heterozygous). Each requires on the specificty string for
 * the given locus (e.g. "24" for A24).
 */

public class ValidationModelBuilderNew {
  // private static final Map<RaceGroup, EthnicityHaplotypeComp> comparators =
  // new EnumMap<>(RaceGroup.class);
  // private static final Table<Haplotype, RaceGroup, BigDecimal> frequencyTable =
  // HashBasedTable.create();
  // private static final String NEGATIVE_ALLELE = "N-Negative";
  // public static final String NOT_ON_CELL_SURFACE = ".+[0-9]+[LSCAQlscaq]$";
  // public static final String NOT_EXPRESSED = ".+[0-9]+[Nn]$";
  //
  // static {
  // for (RaceGroup ethnicity : RaceGroup.values()) {
  // comparators.put(ethnicity, new EthnicityHaplotypeComp(ethnicity));
  // }
  // }
  //
  // private String donorId;
  // private String source;
  // private String sourceType;
  //
  // // Encapsulated locus data structures
  // private final LocusData aLocusData = new LocusData(SeroLocus.A, HLALocus.A);
  // private final LocusData bLocusData = new LocusData(SeroLocus.B, HLALocus.B);
  // private final LocusData cLocusData = new LocusData(SeroLocus.C, HLALocus.C);
  //
  // private final DRDQData drdqData = new DRDQData();
  //
  // private Map<HLALocus, AllelePairings> possibleAllelePairings = new HashMap<>();
  // private Map<HLALocus, AllelePairings> donorAllelePairings = new HashMap<>();
  //
  // private Set<HLALocus> nonCWDLoci = new TreeSet<>();
  // private Map<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remapping = new HashMap<>();
  //
  // private Boolean bw4;
  // private Boolean bw6;
  // private Multimap<Strand, HLAType> bHaplotypes = HashMultimap.create();
  // private Multimap<Strand, HLAType> cHaplotypes = HashMultimap.create();
  // private Multimap<Strand, HLAType> drb1Haplotypes = HashMultimap.create();
  // private Multimap<Strand, HLAType> dqb1Haplotypes = HashMultimap.create();
  // private Multimap<Strand, HLAType> dpb1Haplotypes = HashMultimap.create();
  // private Multimap<Strand, HLAType> dr345Haplotypes = HashMultimap.create();
  //
  // public ValidationModelBuilder donorId(String donorId) {
  // this.donorId = donorId;
  // return this;
  // }
  //
  // public ValidationModelBuilder source(String source) {
  // this.source = source;
  // return this;
  // }
  //
  // public ValidationModelBuilder sourceType(String sourceType) {
  // this.sourceType = sourceType;
  // return this;
  // }
  //
  // public ValidationModelBuilder a(String aSeroType) {
  // return aLocusData.addSeroType(aSeroType) ? this : null;
  // }
  //
  // public ValidationModelBuilder aType(HLAType aType) {
  // aLocusData.addType(aType);
  // return this;
  // }
  //
  // public ValidationModelBuilder aTypeNonCWD(HLAType aType) {
  // aLocusData.addTypeNonCWD(aType);
  // return this;
  // }
  //
  // public ValidationModelBuilder aNonCWD(String aType) {
  // return aLocusData.addNonCWD(aType) ? this : null;
  // }
  //
  // public ValidationModelBuilder b(String bType) {
  // return bLocusData.addSeroType(bType) ? this : null;
  // }
  //
  // public ValidationModelBuilder bType(HLAType bType) {
  // bLocusData.addType(bType);
  // return this;
  // }
  //
  // public ValidationModelBuilder bTypeNonCWD(HLAType bType) {
  // bLocusData.addTypeNonCWD(bType);
  // return this;
  // }
  //
  // public ValidationModelBuilder bNonCWD(String bType) {
  // return bLocusData.addNonCWD(bType) ? this : null;
  // }
  //
  // public ValidationModelBuilder c(String cType) {
  // return cLocusData.addSeroType(cType) ? this : null;
  // }
  //
  // public ValidationModelBuilder cType(HLAType cType) {
  // cLocusData.addType(cType);
  // return this;
  // }
  //
  // public ValidationModelBuilder cTypeNonCWD(HLAType cType) {
  // cLocusData.addTypeNonCWD(cType);
  // return this;
  // }
  //
  // public ValidationModelBuilder cNonCWD(String cType) {
  // return cLocusData.addNonCWD(cType) ? this : null;
  // }
  //
  // public ValidationModelBuilder possibleAllelePairings(HLALocus locus, AllelePairings alleles) {
  // possibleAllelePairings.put(locus, alleles);
  // return this;
  // }
  //
  // public ValidationModelBuilder donorAllelePairings(HLALocus locus, AllelePairings alleles) {
  // donorAllelePairings.put(locus, alleles);
  // return this;
  // }
  //
  // public ValidationModelBuilder drb(String drbType) {
  // drdqData.addDrbSeroType(drbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder drbNonCWD(String drbType) {
  // drdqData.addDrbNonCWD(drbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dr51(String dr51) {
  // drdqData.addDr51(dr51);
  // return this;
  // }
  //
  // public ValidationModelBuilder dr52(String dr52) {
  // drdqData.addDr52(dr52);
  // return this;
  // }
  //
  // public ValidationModelBuilder dr53(String dr53) {
  // drdqData.addDr53(dr53);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqbNonCWD(String dqbType) {
  // return drdqData.addDqbNonCWD(dqbType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dqaNonCWD(String dqaType) {
  // return drdqData.addDqaNonCWD(dqaType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dpaNonCWD(String dpaType) {
  // return drdqData.addDpaNonCWD(dpaType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dpbNonCWD(String dpbType) {
  // drdqData.addDpbNonCWD(dpbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqb(String dqbType) {
  // return drdqData.addDqbSeroType(dqbType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dqa(String dqaType) {
  // return drdqData.addDqaSeroType(dqaType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dpa(String dpaType) {
  // return drdqData.addDpaSeroType(dpaType) ? this : null;
  // }
  //
  // public ValidationModelBuilder dpb(HLAType dpbType) {
  // drdqData.addDpbType(dpbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dpb(String dpbType) {
  // drdqData.addDpbType(dpbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dpbNonCWD(HLAType dpbType) {
  // drdqData.addDpbTypeNonCWD(dpbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqbType(HLAType dqbType) {
  // drdqData.addDqbType(dqbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqbTypeNonCWD(HLAType dqbType) {
  // drdqData.addDqbTypeNonCWD(dqbType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqaType(HLAType dqaType) {
  // drdqData.addDqaType(dqaType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dqaTypeNonCWD(HLAType dqaType) {
  // drdqData.addDqaTypeNonCWD(dqaType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dpaType(HLAType dpaType) {
  // drdqData.addDpaType(dpaType);
  // return this;
  // }
  //
  // public ValidationModelBuilder dpaTypeNonCWD(HLAType dpaType) {
  // drdqData.addDpaTypeNonCWD(dpaType);
  // return this;
  // }
  //
  // public Set<SeroType> getDrbLocus() {
  // return drdqData.getDrbLocus();
  // }
  //
  // public Set<SeroType> getDqaLocus() {
  // return drdqData.getDqaLocus();
  // }
  //
  // public Set<SeroType> getDqbLocus() {
  // return drdqData.getDqbLocus();
  // }
  //
  // public Set<SeroType> getDpaLocus() {
  // return drdqData.getDpaLocus();
  // }
  //
  // public Set<HLAType> getDpbLocusTypes() {
  // return drdqData.getDpbLocusTypes();
  // }
  //
  // public List<HLAType> getDr51Locus() {
  // return drdqData.getDr51Locus();
  // }
  //
  // public List<HLAType> getDr52Locus() {
  // return drdqData.getDr52Locus();
  // }
  //
  // public List<HLAType> getDr53Locus() {
  // return drdqData.getDr53Locus();
  // }
  //
  // public Set<HLAType> getFinalDPBTypes() {
  // return drdqData.getFinalDPBTypes();
  // }
  //
  // public LocusData getLocusData(HLALocus locus) {
  // switch (locus) {
  // case A:
  // return aLocusData;
  // case B:
  // return bLocusData;
  // case C:
  // return cLocusData;
  // default:
  // return drdqData.getLocusData(locus);
  // }
  // }
  //
  // public ValidationModelBuilder bw4(boolean bw4) {
  // this.bw4 = bw4;
  // return this;
  // }
  //
  // public ValidationModelBuilder bw6(boolean bw6) {
  // this.bw6 = bw6;
  // return this;
  // }
  //
  // public ValidationModelBuilder bHaplotype(Multimap<Strand, HLAType> types) {
  // bHaplotypes.putAll(types);
  // return this;
  // }
  //
  // public ValidationModelBuilder cHaplotype(Multimap<Strand, HLAType> types) {
  // cHaplotypes.putAll(types);
  // return this;
  // }
  //
  // public ValidationModelBuilder drHaplotype(Multimap<Strand, HLAType> types) {
  // drb1Haplotypes.putAll(types);
  //
  // for (Strand strand : types.keySet()) {
  // SeroType drbType = types.get(strand).iterator().next().lowResEquiv();
  // if (!DRAssociations.getDRBLocus(drbType).isPresent()) {
  // dr345Haplotypes.put(strand, NullType.UNREPORTED_DRB345);
  // }
  // }
  // return this;
  // }
  //
  // public ValidationModelBuilder dqHaplotype(Multimap<Strand, HLAType> types) {
  // dqb1Haplotypes.putAll(types);
  // return this;
  // }
  //
  // public ValidationModelBuilder dpHaplotype(Multimap<Strand, HLAType> types) {
  // dpb1Haplotypes.putAll(types);
  // return this;
  // }
  //
  // public ValidationModelBuilder dr345Haplotype(Multimap<Strand, HLAType> types) {
  // for (Strand originalKey : types.keySet()) {
  // Strand newKey = originalKey;
  // if (dr345Haplotypes.containsKey(newKey)) {
  // newKey = newKey.flip();
  // }
  // dr345Haplotypes.putAll(newKey, types.get(originalKey));
  // }
  // return this;
  // }
  //
  // public ImmutableMultimap<Strand, HLAType> getDpbHaplotypes() {
  // return ImmutableMultimap.copyOf(dpb1Haplotypes);
  // }
  //
  // public void locusIsNonCIWD(HLAType locus) {
  // nonCWDLoci.add(locus.locus());
  // }
  //
  // public class ValidationResult {
  //
  // public final boolean valid;
  // public final Optional<String> validationMessage;
  //
  // public ValidationResult(boolean valid, Optional<String> validationMessage) {
  // this.valid = valid;
  // this.validationMessage = validationMessage;
  // }
  // }
  //
  // public ValidationResult validate() {
  // return ensureValidity();
  // }
  //
  // public ValidationModel build() {
  // Multimap<RaceGroup, Haplotype> bcCwdHaplotypes = buildBCHaplotypes(bHaplotypes, cHaplotypes);
  // Multimap<RaceGroup, Haplotype> drDqDR345Haplotypes =
  // buildHaplotypes(ImmutableList.of(drb1Haplotypes, dqb1Haplotypes, dr345Haplotypes));
  //
  // frequencyTable.clear();
  //
  // ValidationModel validationModel = new ValidationModel(donorId, source, sourceType,
  // aLocusData.getCWD(), bLocusData.getCWD(), cLocusData.getCWD(), drdqData.getDrbLocus(),
  // drdqData.getDqbLocus(), drdqData.getDqaLocus(), drdqData.getDpaLocus(), getFinalDPBTypes(),
  // bw4, bw6, drdqData.getDr51Locus(), drdqData.getDr52Locus(), drdqData.getDr53Locus(),
  // bcCwdHaplotypes, drDqDR345Haplotypes, remapping);
  // return validationModel;
  // }
  //
  // private Multimap<RaceGroup, Haplotype> buildBCHaplotypes(Multimap<Strand, HLAType> bHaps,
  // Multimap<Strand, HLAType> cHaps) {
  // if (bw4 && bw6) {
  // Multimap<Strand, HLAType> s4s6 =
  // enforceBws(BwSerotypes.BwGroup.Bw4, BwSerotypes.BwGroup.Bw6, bHaps);
  // Multimap<Strand, HLAType> s6s4 =
  // enforceBws(BwSerotypes.BwGroup.Bw6, BwSerotypes.BwGroup.Bw4, bHaps);
  // Multimap<RaceGroup, Haplotype> s4s6Haplotypes =
  // s4s6.isEmpty() ? ImmutableMultimap.of() : buildHaplotypes(ImmutableList.of(s4s6, cHaps));
  // Multimap<RaceGroup, Haplotype> s6s4Haplotypes =
  // s6s4.isEmpty() ? ImmutableMultimap.of() : buildHaplotypes(ImmutableList.of(s6s4, cHaps));
  //
  // List<ScoredHaplotypes> scoredHaplotypePairs = new ArrayList<>();
  // for (RaceGroup raceGroup : RaceGroup.values()) {
  // if (s6s4Haplotypes.containsKey(raceGroup)) {
  // scoredHaplotypePairs.add(new ScoredHaplotypes(s6s4Haplotypes.get(raceGroup)));
  // }
  //
  // if (s4s6Haplotypes.containsKey(raceGroup)) {
  // scoredHaplotypePairs.add(new ScoredHaplotypes(s4s6Haplotypes.get(raceGroup)));
  // }
  // }
  //
  // Multimap<RaceGroup, Haplotype> haplotypesByEthnicity = HashMultimap.create();
  //
  // if (!scoredHaplotypePairs.isEmpty()) {
  // for (RaceGroup ethnicity : RaceGroup.values()) {
  // ScoredHaplotypes max =
  // Collections.max(scoredHaplotypePairs, new EthnicityHaplotypeComp(ethnicity));
  //
  // for (Haplotype t : max) {
  // haplotypesByEthnicity.put(ethnicity, t);
  // }
  // }
  // }
  // return haplotypesByEthnicity;
  // } else if (bw4) {
  // Multimap<Strand, HLAType> s4s4 =
  // enforceBws(BwSerotypes.BwGroup.Bw4, BwSerotypes.BwGroup.Bw4, bHaps);
  // return buildHaplotypes(ImmutableList.of(s4s4, cHaps));
  // } else if (bw6) {
  // Multimap<Strand, HLAType> s6s6 =
  // enforceBws(BwSerotypes.BwGroup.Bw6, BwSerotypes.BwGroup.Bw6, bHaps);
  // return buildHaplotypes(ImmutableList.of(s6s6, cHaps));
  // }
  //
  // return ArrayListMultimap.create();
  // }
  //
  // private Multimap<Strand, HLAType> enforceBws(BwSerotypes.BwGroup strandOneGroup,
  // BwSerotypes.BwGroup strandTwoGroup, Multimap<Strand, HLAType> haplotypes) {
  // ListMultimap<Strand, HLAType> enforced = ArrayListMultimap.create();
  // for (HLAType t : haplotypes.get(Strand.FIRST)) {
  // if (!HLALocus.B.equals(t.locus()) || strandOneGroup.equals(BwSerotypes.getBwGroup(t))
  // || BwSerotypes.BwGroup.Unknown.equals(BwSerotypes.getBwGroup(t))) {
  // enforced.put(Strand.FIRST, t);
  // }
  // }
  // for (HLAType t : haplotypes.get(Strand.SECOND)) {
  // if (!HLALocus.B.equals(t.locus()) || strandTwoGroup.equals(BwSerotypes.getBwGroup(t))
  // || BwSerotypes.BwGroup.Unknown.equals(BwSerotypes.getBwGroup(t))) {
  // enforced.put(Strand.SECOND, t);
  // }
  // }
  // return enforced;
  // }
  //
  // private ValidationResult ensureValidity() {
  // for (Object o : Lists.newArrayList(donorId, source, aLocusData.getCWD(), bLocusData.getCWD(),
  // cLocusData.getCWD(), drdqData.getDrbLocus(), drdqData.getDqbLocus(), drdqData.getDqaLocus(),
  // drdqData.getDpbLocusTypes(), bw4, bw6)) {
  // if (Objects.isNull(o)) {
  // return new ValidationResult(false, Optional.of("ValidationModel incomplete"));
  // }
  // }
  // for (Set<?> set : ImmutableList.of(aLocusData.getCWD(), bLocusData.getCWD(),
  // cLocusData.getCWD(), drdqData.getDrbLocus(), drdqData.getDqbLocus(), drdqData.getDqaLocus(),
  // drdqData.getDpbLocusTypes())) {
  // if (set.isEmpty() || set.size() > 2) {
  // return new ValidationResult(false,
  // Optional.of("ValidationModel contains invalid allele count: " + set));
  // }
  // }
  //
  // if (Objects.isNull(drdqData.getDpaLocus())) {
  // JOptionPane.showMessageDialog(new JFrame(), "DPA is missing from this file", "Dialog",
  // JOptionPane.WARNING_MESSAGE);
  // }
  //
  // Collections.sort(drdqData.getDr51Locus());
  // Collections.sort(drdqData.getDr52Locus());
  // Collections.sort(drdqData.getDr53Locus());
  //
  // return new ValidationResult(true, Optional.empty());
  // }
  //
  // public boolean hasCorrections() {
  // return !nonCWDLoci.isEmpty();
  // }
  //
  // public ValidationResult processCorrections(RemapProcessor remapProcessor) {
  // boolean cancelled = false;
  //
  // for (HLALocus locus : nonCWDLoci) {
  // Pair<Set<TypePair>, Set<TypePair>> remapPair = null;
  //
  // try {
  // remapPair = remapProcessor.processRemapping(locus, this);
  // } catch (CancellationException e) {
  // cancelled = true;
  // } catch (Throwable t) {
  // t.printStackTrace();
  // }
  //
  // if (remapPair != null) {
  // remapping.put(locus, remapPair);
  // }
  // }
  //
  // if (cancelled) {
  // return new ValidationResult(false, Optional.empty());
  // }
  //
  // return new ValidationResult(true, Optional.empty());
  // }
  //
  // public Set<SeroType> getCWDSeroTypesForLocus(HLALocus locus) {
  // return getLocusData(locus).getCWD();
  // }
  //
  // public Set<SeroType> getAllSeroTypesForLocus(HLALocus locus) {
  // return getLocusData(locus).getAll();
  // }
  //
  // public Set<HLAType> getCWDTypesForLocus(HLALocus locus) {
  // return getLocusData(locus).getCWDTypes();
  // }
  //
  // public Set<HLAType> getAllTypesForLocus(HLALocus locus) {
  // return getLocusData(locus).getAllTypes();
  // }
  //
  // public AllelePairings getPossibleAllelePairsForLocus(HLALocus locus) {
  // return possibleAllelePairings.get(locus);
  // }
  //
  // public AllelePairings getDonorAllelePairsForLocus(HLALocus locus) {
  // return donorAllelePairings.get(locus);
  // }
  //
  // public AllelePairings getAllelePairs(HLALocus locus) {
  // return possibleAllelePairings.get(locus);
  // }
  //
  // private void addToLocus(Set<HLAType> locusSet, HLALocus locus, HLAType typeString) {
  // locusSet.add(typeString);
  // }
  //
  // private void addToLocus(Set<SeroType> locusSet, SeroLocus locus, String typeString) {
  // if (typeString.length() > 2) {
  // typeString = typeString.substring(0, typeString.length() - 2);
  // }
  // locusSet.add(new SeroType(locus, typeString));
  // }
  //
  // private static <T> void setOrAdd(List<T> list, T toAdd, int index) {
  // if (index == list.size()) {
  // list.add(toAdd);
  // } else {
  // list.set(index, toAdd);
  // }
  // }
  //
  // private class LocusData {
  // private final SeroLocus seroLocus;
  // private final HLALocus hlaLocus;
  // private Set<SeroType> cwdSeroTypes = new LinkedHashSet<>();
  // private Set<SeroType> nonCwdSeroTypes = new LinkedHashSet<>();
  // private Set<HLAType> cwdTypes = new LinkedHashSet<>();
  // private Set<HLAType> nonCwdTypes = new LinkedHashSet<>();
  //
  // public LocusData(SeroLocus seroLocus, HLALocus hlaLocus) {
  // this.seroLocus = seroLocus;
  // this.hlaLocus = hlaLocus;
  // }
  //
  // public boolean addSeroType(String seroType) {
  // if (seroType == null || seroType.isEmpty())
  // return false;
  // if (!seroType.matches(".*\\d.*") || seroType.equals("98")) {
  // return false;
  // }
  // addToLocus(cwdSeroTypes, seroLocus, seroType);
  // return true;
  // }
  //
  // public void addType(HLAType type) {
  // cwdTypes.add(type);
  // }
  //
  // public void addTypeNonCWD(HLAType type) {
  // nonCwdTypes.add(type);
  // }
  //
  // public boolean addNonCWD(String seroType) {
  // if (seroType == null || seroType.isEmpty())
  // return false;
  // if (!seroType.matches(".*\\d.*") || seroType.equals("98")) {
  // return false;
  // }
  // addToLocus(nonCwdSeroTypes, seroLocus, seroType);
  // return true;
  // }
  //
  // public Set<SeroType> getCWD() {
  // return cwdSeroTypes;
  // }
  //
  // public Set<SeroType> getAll() {
  // return nonCwdSeroTypes;
  // }
  //
  // public Set<HLAType> getCWDTypes() {
  // return cwdTypes;
  // }
  //
  // public Set<HLAType> getAllTypes() {
  // return nonCwdTypes;
  // }
  // }
  //
  // class DRDQData {
  // private final Set<SeroType> drbLocus = new HashSet<>();
  // private final Set<SeroType> dqaLocus = new HashSet<>();
  // private final Set<SeroType> dqbLocus = new HashSet<>();
  // private final Set<SeroType> dpaLocus = new HashSet<>();
  // private final Set<HLAType> dpbLocusTypes = new HashSet<>();
  // private final List<HLAType> dr51Locus = new ArrayList<>();
  // private final List<HLAType> dr52Locus = new ArrayList<>();
  // private final List<HLAType> dr53Locus = new ArrayList<>();
  // private final Set<HLAType> dpbLocusTypesNonCWD = new HashSet<>();
  // private final Set<HLAType> dqbLocusTypes = new HashSet<>();
  // private final Set<HLAType> dqbLocusTypesNonCWD = new HashSet<>();
  // private final Set<HLAType> dqaLocusTypes = new HashSet<>();
  // private final Set<HLAType> dqaLocusTypesNonCWD = new HashSet<>();
  // private final Set<HLAType> dpaLocusTypes = new HashSet<>();
  // private final Set<HLAType> dpaLocusTypesNonCWD = new HashSet<>();
  //
  // public void addDrbSeroType(String drbType) {
  // if (drbType == null || drbType.isEmpty())
  // return;
  // drbLocus.add(new SeroType(SeroLocus.DRB, drbType));
  // }
  //
  // public void addDrbNonCWD(String drbType) {
  // if (drbType == null || drbType.isEmpty() || drbType.equals("98"))
  // return;
  // drbLocus.add(new SeroType(SeroLocus.DRB, drbType));
  // }
  //
  // public void addDr51(String dr51) {
  // if (dr51 == null || dr51.isEmpty())
  // return;
  // dr51Locus.add(new HLAType(HLALocus.DRB1, dr51));
  // }
  //
  // public void addDr52(String dr52) {
  // if (dr52 == null || dr52.isEmpty())
  // return;
  // dr52Locus.add(new HLAType(HLALocus.DRB1, dr52));
  // }
  //
  // public void addDr53(String dr53) {
  // if (dr53 == null || dr53.isEmpty())
  // return;
  // dr53Locus.add(new HLAType(HLALocus.DRB1, dr53));
  // }
  //
  // public boolean addDqbNonCWD(String dqbType) {
  // if (dqbType == null || dqbType.isEmpty() || dqbType.equals("98"))
  // return false;
  // dqbLocusTypesNonCWD.add(new HLAType(HLALocus.DQB1, dqbType));
  // return true;
  // }
  //
  // public boolean addDqaNonCWD(String dqaType) {
  // if (dqaType == null || dqaType.isEmpty() || dqaType.equals("98"))
  // return false;
  // dqaLocusTypesNonCWD.add(new HLAType(HLALocus.DQA1, dqaType));
  // return true;
  // }
  //
  // public boolean addDpaNonCWD(String dpaType) {
  // if (dpaType == null || dpaType.isEmpty() || dpaType.equals("98"))
  // return false;
  // dpaLocusTypesNonCWD.add(new HLAType(HLALocus.DPA1, dpaType));
  // return true;
  // }
  //
  // public void addDpbNonCWD(String dpbType) {
  // if (dpbType == null || dpbType.isEmpty() || dpbType.equals("98"))
  // return;
  // dpbLocusTypesNonCWD.add(new HLAType(HLALocus.DPB1, dpbType));
  // }
  //
  // public boolean addDqbSeroType(String dqbType) {
  // if (dqbType == null || dqbType.isEmpty())
  // return false;
  // dqbLocus.add(new SeroType(SeroLocus.DQB, dqbType));
  // return true;
  // }
  //
  // public boolean addDqaSeroType(String dqaType) {
  // if (dqaType == null || dqaType.isEmpty())
  // return false;
  // dqaLocus.add(new SeroType(SeroLocus.DQA, dqaType));
  // return true;
  // }
  //
  // public boolean addDpaSeroType(String dpaType) {
  // if (dpaType == null || dpaType.isEmpty())
  // return false;
  // dpaLocus.add(new SeroType(SeroLocus.DPA, dpaType));
  // return true;
  // }
  //
  // public void addDpbType(HLAType dpbType) {
  // if (dpbType == null)
  // return;
  // dpbLocusTypes.add(dpbType);
  // }
  //
  // public void addDpbType(String dpbType) {
  // if (dpbType == null || dpbType.isEmpty())
  // return;
  // dpbLocusTypes.add(new HLAType(HLALocus.DPB1, dpbType));
  // }
  //
  // public void addDpbTypeNonCWD(HLAType dpbType) {
  // if (dpbType == null)
  // return;
  // dpbLocusTypesNonCWD.add(dpbType);
  // }
  //
  // public void addDqbType(HLAType dqbType) {
  // if (dqbType == null)
  // return;
  // dqbLocusTypes.add(dqbType);
  // }
  //
  // public void addDqbTypeNonCWD(HLAType dqbType) {
  // if (dqbType == null)
  // return;
  // dqbLocusTypesNonCWD.add(dqbType);
  // }
  //
  // public void addDqaType(HLAType dqaType) {
  // if (dqaType == null)
  // return;
  // dqaLocusTypes.add(dqaType);
  // }
  //
  // public void addDqaTypeNonCWD(HLAType dqaType) {
  // if (dqaType == null)
  // return;
  // dqaLocusTypesNonCWD.add(dqaType);
  // }
  //
  // public void addDpaType(HLAType dpaType) {
  // if (dpaType == null)
  // return;
  // dpaLocusTypes.add(dpaType);
  // }
  //
  // public void addDpaTypeNonCWD(HLAType dpaType) {
  // if (dpaType == null)
  // return;
  // dpaLocusTypesNonCWD.add(dpaType);
  // }
  //
  // public Set<SeroType> getDrbLocus() {
  // return drbLocus;
  // }
  //
  // public Set<SeroType> getDqaLocus() {
  // return dqaLocus;
  // }
  //
  // public Set<SeroType> getDqbLocus() {
  // return dqbLocus;
  // }
  //
  // public Set<SeroType> getDpaLocus() {
  // return dpaLocus;
  // }
  //
  // public Set<HLAType> getDpbLocusTypes() {
  // return dpbLocusTypes;
  // }
  //
  // public List<HLAType> getDr51Locus() {
  // return dr51Locus;
  // }
  //
  // public List<HLAType> getDr52Locus() {
  // return dr52Locus;
  // }
  //
  // public List<HLAType> getDr53Locus() {
  // return dr53Locus;
  // }
  //
  // public Set<HLAType> getFinalDPBTypes() {
  // Set<HLAType> dpbTypes = new HashSet<>();
  // for (HLAType dpbType1 : dpbLocusTypes) {
  // String dpbType = dpbType1.specString();
  // if (!Strings.isNullOrEmpty(dpbType) && dpbType.matches(".*\\d.*")) {
  // HLAType tmpDPB1 = new HLAType(HLALocus.DPB1, dpbType);
  // if (tmpDPB1.spec().size() > 2) {
  // tmpDPB1 = new HLAType(HLALocus.DPB1,
  // new int[] {tmpDPB1.spec().get(0), tmpDPB1.spec().get(1)});
  // }
  // dpbTypes.add(tmpDPB1);
  // }
  // }
  // return dpbTypes;
  // }
  //
  // public LocusData getLocusData(HLALocus locus) {
  // switch (locus) {
  // case DQA1:
  // return new LocusData(SeroLocus.DQA, HLALocus.DQA1);
  // case DQB1:
  // return new LocusData(SeroLocus.DQB, HLALocus.DQB1);
  // case DPA1:
  // return new LocusData(SeroLocus.DPA, HLALocus.DPA1);
  // case DPB1:
  // return new LocusData(SeroLocus.DPB, HLALocus.DPB1);
  // default:
  // throw new IllegalArgumentException("Invalid locus: " + locus);
  // }
  // }
  // }
  //
  // /** @return A table of the highest-probability haplotypes for each ethnicity */
  // private Multimap<RaceGroup, Haplotype> buildHaplotypes(
  // List<Multimap<Strand, HLAType>> typesByLocus) {
  // Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity = new HashMap<>();
  // Multimap<RaceGroup, Haplotype> haplotypesByEthnicity =
  // MultimapBuilder.enumKeys(RaceGroup.class).arrayListValues().build();
  //
  // List<Multimap<Strand, HLAType>> presentTypesByLocus =
  // typesByLocus.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
  // presentTypesByLocus.forEach(this::pruneUnknown);
  // presentTypesByLocus.forEach(this::condenseGroups);
  //
  // if (!presentTypesByLocus.isEmpty()) {
  // // Recursively generate the set of all possible haplotype pairs
  // generateHaplotypePairs(maxScorePairsByEthnicity, presentTypesByLocus);
  // for (Entry<RaceGroup, ScoredHaplotypes> entry : maxScorePairsByEthnicity.entrySet()) {
  // for (Haplotype h : entry.getValue()) {
  // haplotypesByEthnicity.put(entry.getKey(), h);
  // }
  // }
  // }
  //
  // return haplotypesByEthnicity;
  // }
  //
  // /**
  // * Recursive entry point for generating all possible {@link Haplotype} pairs.
  // *
  // * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
  // * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles
  // for
  // * that strand.
  // */
  // private void generateHaplotypePairs(Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
  // List<Multimap<Strand, HLAType>> typesByLocus) {
  // // Overview:
  // // 1. Recurse through strand 1 sets - for each locus, record the possible complementary options
  // // 2. At the terminal strand 1 step, start recursing through the possible strand 2 options
  // // 3. At the terminal strand 2 step, create a ScoredHaplotype for the pair
  //
  // generateStrandOneHaplotypes(maxScorePairsByEthnicity, typesByLocus, new ArrayList<>(),
  // new ArrayList<>(), 0);
  // }
  //
  // /**
  // * Recursively generate all possible haplotypes for the "first" strand.
  // *
  // * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
  // * @param typesByLocus List of mappings, one per locus, of {@link Strand} to possible alleles
  // for
  // * that strand.
  // * @param currentHaplotypeAlleles Current alleles of the first haplotype
  // * @param strandTwoOptionsByLocus List of allele options, by locus, to populate for
  // complementary
  // * haplotype generation.
  // * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
  // */
  // private void generateStrandOneHaplotypes(
  // Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity,
  // List<Multimap<Strand, HLAType>> typesByLocus, List<HLAType> currentHaplotypeAlleles,
  // List<List<HLAType>> strandTwoOptionsByLocus, int locusIndex) {
  // if (locusIndex == typesByLocus.size()) {
  // // Terminal step - we now have one haplotype; recursively generate the second
  // Haplotype firstHaplotype = new Haplotype(currentHaplotypeAlleles);
  // generateStrandTwoHaplotypes(maxScorePairsByEthnicity, firstHaplotype, strandTwoOptionsByLocus,
  // new ArrayList<>(), 0);
  // } else {
  // // Recursive step -
  // Multimap<Strand, HLAType> currentLocus = typesByLocus.get(locusIndex);
  //
  // // The strand notations are arbitrary. So at each locus we need to consider each possible
  // // alignment of the alleles - including whether they are heterozygous or homozygous.
  // // However at the first locus we do not need to consider Strand1 + Strand2 AND
  // // Strand2 + Strand1, as the resulting haplotype pairs would mirror each other.
  // Set<Strand> firstStrandsSet =
  // locusIndex == 0 ? ImmutableSet.of(Strand.FIRST) : currentLocus.keySet();
  //
  // // Build the combinations of strand1 + strand2 alleles at this locus
  // for (Strand strandOne : firstStrandsSet) {
  // List<HLAType> firstStrandTypes = Lists.newArrayList(currentLocus.get(strandOne));
  // List<HLAType> secondStrandTypes = Lists.newArrayList(currentLocus.get(strandOne.flip()));
  // if (secondStrandTypes.isEmpty()) {
  // secondStrandTypes = Lists.newArrayList(firstStrandTypes);
  // }
  //
  // // sorts in descending order, notice h2's weight is found first
  // Comparator<HLAType> c = ((h1, h2) -> {
  // return Double.compare(CommonWellDocumented.getEquivStatus(h2).getWeight(),
  // CommonWellDocumented.getEquivStatus(h1).getWeight());
  // });
  // firstStrandTypes.sort(c);
  // secondStrandTypes.sort(c);
  //
  // double bestCWD;
  // int i;
  //
  // if (!firstStrandTypes.isEmpty()) {
  // bestCWD = CommonWellDocumented.getEquivStatus(firstStrandTypes.get(0)).getWeight();
  // i = 1;
  // while (i < firstStrandTypes.size() && CommonWellDocumented
  // .getEquivStatus(firstStrandTypes.get(i)).getWeight() == bestCWD) {
  // i++;
  // }
  // for (int j = firstStrandTypes.size() - 1; j >= i; j--) {
  // firstStrandTypes.remove(j);
  // }
  // }
  //
  // if (!secondStrandTypes.isEmpty()) {
  // bestCWD = CommonWellDocumented.getEquivStatus(secondStrandTypes.get(0)).getWeight();
  // i = 1;
  // while (i < secondStrandTypes.size() && CommonWellDocumented
  // .getEquivStatus(secondStrandTypes.get(i)).getWeight() == bestCWD) {
  // i++;
  // }
  // for (int j = secondStrandTypes.size() - 1; j >= i; j--) {
  // secondStrandTypes.remove(j);
  // }
  // }
  //
  // // set up the second strand options to iterate over after the first haplotype is built
  // setOrAdd(strandTwoOptionsByLocus, secondStrandTypes, locusIndex);
  //
  // // Try each possible strand one allele
  // for (HLAType currentType : firstStrandTypes) {
  // setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);
  //
  // // Recurse to the next locus
  // generateStrandOneHaplotypes(maxScorePairsByEthnicity, typesByLocus,
  // currentHaplotypeAlleles, strandTwoOptionsByLocus, locusIndex + 1);
  // }
  // }
  // }
  // }
  //
  // /**
  // * Recursively generate all possible haplotypes for the "second" strand.
  // *
  // * @param maxScorePairsByEthnicity Collection to populate with possible haplotype pairs
  // * @param firstHaplotype The fixed, complementary haplotype
  // * @param strandTwoOptionsByLocus List of allele options, by locus, to use when generating
  // * haplotypes
  // * @param currentHaplotypeAlleles Current alleles of the second haplotype
  // * @param locusIndex Current locus index in the {@code strandTwoOptionsByLocus} list
  // */
  // private void generateStrandTwoHaplotypes(
  // Map<RaceGroup, ScoredHaplotypes> maxScorePairsByEthnicity, Haplotype firstHaplotype,
  // List<List<HLAType>> strandTwoOptionsByLocus, List<HLAType> currentHaplotypeAlleles,
  // int locusIndex) {
  // if (locusIndex == strandTwoOptionsByLocus.size()) {
  // // Terminal step - we now have two haplotypes so we score them and compare
  // final Haplotype e2 = new Haplotype(currentHaplotypeAlleles);
  // ScoredHaplotypes scored = new ScoredHaplotypes(ImmutableList.of(firstHaplotype, e2));
  //
  // Set<ScoredHaplotypes> maxSet = Sets.newHashSet(maxScorePairsByEthnicity.values());
  // // performance hack - shortcut if all the currently tracked max scores are the same
  // int sz = maxSet.size();
  // if (sz == 1) {
  // // doesn't matter which we use, all RaceGroups point to the same ScoredHaplotypes
  // if (comparators.get(RaceGroup.AFA).compare(maxSet.iterator().next(), scored) < 0) {
  // for (RaceGroup ethnicity : RaceGroup.values()) {
  // maxScorePairsByEthnicity.put(ethnicity, scored);
  // }
  // }
  // } else {
  // for (RaceGroup ethnicity : RaceGroup.values()) {
  // // if the map doesn't have a value yet
  // // or if the current value is less than the new value
  // // put the value into the map
  // if (!maxScorePairsByEthnicity.containsKey(ethnicity) || comparators.get(ethnicity)
  // .compare(maxScorePairsByEthnicity.get(ethnicity), scored) < 0) {
  // maxScorePairsByEthnicity.put(ethnicity, scored);
  // }
  // }
  // }
  //
  // } else {
  // // Recursive step - iterate through the possible alleles for this locus, building up the
  // // current haplotype
  // for (HLAType currentType : strandTwoOptionsByLocus.get(locusIndex)) {
  // setOrAdd(currentHaplotypeAlleles, currentType, locusIndex);
  // generateStrandTwoHaplotypes(maxScorePairsByEthnicity, firstHaplotype,
  // strandTwoOptionsByLocus, currentHaplotypeAlleles, locusIndex + 1);
  // }
  // }
  // }
  //
  // /** Replace all HLA types with their groups (condensing equivalent alleles) */
  // private void condenseGroups(Multimap<Strand, HLAType> typesForStrand) {
  // for (Strand strand : typesForStrand.keySet()) {
  // Set<HLAType> condensed = new HashSet<>();
  // Collection<HLAType> uncondensed = typesForStrand.get(strand);
  //
  // for (HLAType hlaType : uncondensed) {
  // condensed.add(AlleleGroups.getGGroup(hlaType));
  // }
  //
  // // NB: making the values empty will break this loop by removing the key from the multimap :D
  // typesForStrand.replaceValues(strand, condensed);
  // }
  //
  // // Homozygous
  // if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
  // typesForStrand.removeAll(Strand.SECOND);
  // }
  // }
  //
  // /** Filter out {@link Status#UNKNOWN} types and eliminate redundant strands */
  // private void pruneUnknown(Multimap<Strand, HLAType> typesForStrand) {
  // // Homozygous
  // if (Objects.equals(typesForStrand.get(Strand.FIRST), typesForStrand.get(Strand.SECOND))) {
  // typesForStrand.removeAll(Strand.SECOND);
  // }
  //
  // // Sort out our types by CWD status
  // for (Strand strand : typesForStrand.keySet()) {
  // Multimap<Status, HLAType> typesByStatus =
  // MultimapBuilder.enumKeys(Status.class).hashSetValues().build();
  // Collection<HLAType> values = typesForStrand.get(strand);
  // values.forEach(t -> typesByStatus.put(CommonWellDocumented.getEquivStatus(t), t));
  //
  // Set<HLAType> cwdTypes = new HashSet<>();
  // for (Status s : Status.values()) {
  // if (s != Status.UNKNOWN) {
  // cwdTypes.addAll(typesByStatus.get(s));
  // }
  // }
  //
  // // If we have any common or well-documented types, drop all unknown
  // if (!cwdTypes.isEmpty()) {
  // // NB: making the values empty will break this loop by removing the key from the multimap :D
  // typesForStrand.replaceValues(strand, cwdTypes);
  // }
  // }
  // }
  //
  //
  // public static class AllelePairings {
  //
  // private Multimap<String, String> map = HashMultimap.create();
  //
  // /**
  // * @param a1
  // * @param a2
  // */
  // public void addPairing(String a1, String a2) {
  // map.put(a1, a2);
  // map.put(a2, a1);
  // }
  //
  // public Set<String> getAlleleKeys() {
  // return map.keySet();
  // }
  //
  // public Collection<String> getValidPairings(String allele) {
  // return map.get(allele);
  // }
  //
  // public boolean isValidPairing(String a1, String a2) {
  // return map.containsEntry(a1, a2);
  // }
  //
  // public Collection<String> getMatchingAlleles(String pattern) {
  // final RabinKarp rabinKarp = new RabinKarp(pattern);
  // return map.keySet().stream().filter(s -> rabinKarp.search(s) < s.length())
  // .collect(Collectors.toSet());
  // }
  //
  // }
  //
  // public static class TypePair implements Comparable<TypePair> {
  // private final HLAType hlaType;
  // private final SeroType seroType;
  //
  // public TypePair(HLAType hlaType, SeroType seroType) {
  // this.hlaType = hlaType;
  // this.seroType = seroType;
  // }
  //
  // /**
  // * @return the hlaType
  // */
  // public HLAType getHlaType() {
  // return hlaType;
  // }
  //
  // /**
  // * @return the seroType
  // */
  // public SeroType getSeroType() {
  // return seroType;
  // }
  //
  // @Override
  // public String toString() {
  // return seroType.specString() + " [" + hlaType.specString() + " - "
  // + CommonWellDocumented.getStatus(getHlaType()) + "]";
  // }
  //
  // @Override
  // public int compareTo(TypePair o) {
  // int c = seroType.compareTo(o.seroType);
  // if (c != 0)
  // return c;
  // return hlaType.compareTo(o.hlaType);
  // }
  //
  // }
  //
  // /** Helper wrapper class to cache the scores for haplotypes */
  // private static class ScoredHaplotypes extends ArrayList<Haplotype> {
  // private static final long serialVersionUID = 3780864438450985328L;
  // private static final int NO_MISSING_WEIGHT = 10;
  // private final Map<RaceGroup, Double> scoresByEthnicity = new HashMap<>();
  //
  // private ScoredHaplotypes(Collection<Haplotype> initialHaplotypes) {
  // super();
  // BigDecimal cwdScore1 = BigDecimal.ZERO;
  //
  // for (Haplotype haplotype : initialHaplotypes) {
  // add(haplotype);
  //
  // for (RaceGroup e : RaceGroup.values()) {
  // BigDecimal f;
  // if (!frequencyTable.contains(haplotype, e)) {
  // f = HaplotypeFrequencies.getFrequency(e, haplotype);
  // frequencyTable.put(haplotype, e, f);
  // }
  // }
  //
  // for (HLAType allele : haplotype.getTypes()) {
  // cwdScore1 = cwdScore1
  // .add(new BigDecimal(CommonWellDocumented.getEquivStatus(allele).getWeight()));
  // }
  // }
  // BigDecimal cwdScore = cwdScore1;
  //
  // // TODO - document walk-through
  // scoresByEthnicity
  // // calculate the score for each ethnicity
  // .putAll(Arrays.stream(RaceGroup.values()).collect(Collectors.toMap(e -> e, e -> {
  //
  // int noMissingCount = 0;
  //
  // // starting from 1
  // BigDecimal frequency = new BigDecimal(1.0);
  //
  // for (Haplotype haplotype : this) {
  // BigDecimal f = frequencyTable.get(haplotype, e);
  //
  // if (f.compareTo(BigDecimal.ZERO) > 0) {
  // frequency = frequency.multiply(f);
  // noMissingCount++;
  // }
  // }
  //
  // BigDecimal weights =
  // cwdScore.add(BigDecimal.valueOf(NO_MISSING_WEIGHT * noMissingCount));
  //
  // double s = weights.add(frequency).doubleValue();
  //
  // return s;
  // })));
  // }
  //
  // @Override
  // public String toString() {
  // return super.toString() + " - " + scoresByEthnicity.toString();
  // }
  //
  // /**
  // * @return A weighted score for this ethnicity, prioritizing haplotypes without missing
  // * frequencies.
  // */
  // public double getScore(RaceGroup ethnicity) {
  // return scoresByEthnicity.get(ethnicity);
  // }
  //
  // public int compareTo(ScoredHaplotypes o, RaceGroup e) {
  // // Prefer larger frequencies for this ethnicity
  // int c = Double.compare(getScore(e), o.getScore(e));
  // Iterator<Haplotype> myIterator = iterator();
  // Iterator<Haplotype> otherIterator = o.iterator();
  // // Fall back to the haplotypes themselves
  // while (myIterator.hasNext() && otherIterator.hasNext() && c == 0) {
  // c = myIterator.next().compareTo(otherIterator.next());
  // }
  // return c;
  // }
  // }
  //
  // /**
  // * {@link Comparator} to sort collections of {@link Haplotype}s based on their frequency and the
  // * CWD status of their alleles.
  // */
  // private static class EthnicityHaplotypeComp implements Comparator<ScoredHaplotypes> {
  // private RaceGroup ethnicity;
  //
  // private EthnicityHaplotypeComp(RaceGroup e) {
  // this.ethnicity = e;
  // }
  //
  // @Override
  // public int compare(ScoredHaplotypes o1, ScoredHaplotypes o2) {
  //
  // int result = o1.compareTo(o2, ethnicity);
  //
  // if (result == 0) {
  // // If the scores are the same, we compare the unique HLATypes between these two
  // List<HLAType> t1 = makeList(o1);
  // List<HLAType> t2 = makeList(o2);
  // removeOverlapAndSort(t1, t2);
  // for (int i = 0; i < t1.size() && i < t2.size(); i++) {
  // result += t2.get(i).compareTo(t1.get(i));
  // }
  // }
  // return result;
  // }
  //
  // /**
  // * Modify the two input sets to remove any overlap between them, and sort them both before
  // * returning.
  // */
  // private void removeOverlapAndSort(List<HLAType> t1, List<HLAType> t2) {
  // Set<HLAType> overlap = new HashSet<>(t1);
  // overlap.retainAll(t2);
  // t1.removeAll(overlap);
  // t2.removeAll(overlap);
  // Collections.sort(t1);
  // Collections.sort(t2);
  // }
  //
  // /**
  // * Helper method to convert a {@link Haplotype} collection to a sorted list of the alleles
  // * contained in that haplotype.
  // */
  // private List<HLAType> makeList(Collection<Haplotype> haplotypes) {
  // List<HLAType> sorted = new ArrayList<>();
  // haplotypes.forEach(haplotype -> {
  // haplotype.getTypes().forEach(allele -> {
  // sorted.add(allele);
  // });
  // });
  // Collections.sort(sorted);
  // return sorted;
  // }
  // }
  //
  // public Set<HLALocus> getNonCWDLoci() {
  // return nonCWDLoci;
  // }
}
