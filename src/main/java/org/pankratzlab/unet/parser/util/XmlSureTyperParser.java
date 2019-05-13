/*
 * package org.pankratzlab.unet.parser;
 * 
 * import java.io.File; import java.util.HashMap; import
 * org.pankratzlab.unet.model.ValidationModelBuilder; import
 * com.google.common.collect.ArrayListMultimap; import com.google.common.collect.ImmutableMap;
 * 
 * public class XmlSureTyperParser {
 * 
 * // Following are the Haplotype key-values used for the temporary haplotype map. // These strings
 * occur in multiple locations... Consider refactoring // the code base to use a set of common
 * symbols to denote these strings. private static final String HAPLOTYPE_C = "HLA-C"; private
 * static final String HAPLOTYPE_B = "HLA-B"; private static final String HAPLOTYPE_DRB1 =
 * "HLA-DRB1"; private static final String HAPLOTYPE_DQB1 = "HLA-DQB1"; private static final String
 * HAPLOTYPE_DRB345 = "HLA-DRB345";
 * 
 * private static ImmutableMap<String, TypeSetter> metadataMap;
 * 
 * public static void parseTypes(ValidationModelBuilder builder, Document doc) { if (null ==
 * metadataMap) { init(); }
 * 
 * Map<String, Multimap<Strand, HLAType>> haplotypeMap = new HashMap<>();
 * haplotypeMap.put(HAPLOTYPE_B, ArrayListMultimap.create()); haplotypeMap.put(HAPLOTYPE_C,
 * ArrayListMultimap.create()); haplotypeMap.put(HAPLOTYPE_DRB1, ArrayListMultimap.create());
 * haplotypeMap.put(HAPLOTYPE_DQB1, ArrayListMultimap.create()); haplotypeMap.put(HAPLOTYPE_DRB345,
 * ArrayListMultimap.create());
 * 
 * Element root = doc.getElementsByTag().get(0); Elements elementsByTag = ...; for (Element e :
 * elementsByTag) { // ... Fill this body with code, don't throw into method! }
 * 
 * }
 * 
 *//**
    * Initialize the XmlSureTyperParser object attributes. For the moment, it initializes the
    * metadataMap with
    */
/*
 * private static void init() { Builder<String, TypeSetter> setterBuilder = ImmutableMap.builder();
 * setterBuilder.put(HLA_A, new TypeSetter("A", ValidationModelBuilder::a));
 * setterBuilder.put(HLA_B, new TypeSetter("B", ValidationModelBuilder::b));
 * setterBuilder.put(HLA_C, new TypeSetter("Cw", ValidationModelBuilder::c));
 * setterBuilder.put(HLA_DRB1, new TypeSetter("DR", ValidationModelBuilder::drb));
 * setterBuilder.put(HLA_DQA1, new TypeSetter("DQA1*", ValidationModelBuilder::dqa));
 * setterBuilder.put(HLA_DQB1, new TypeSetter("DQ", ValidationModelBuilder::dqb));
 * setterBuilder.put(HLA_DRB3, new TypeSetter("DRB3*", ValidationModelBuilder::dr52));
 * setterBuilder.put(HLA_DRB4, new TypeSetter("DRB4*", ValidationModelBuilder::dr53));
 * setterBuilder.put(HLA_DRB5, new TypeSetter("DRB5*", ValidationModelBuilder::dr51));
 * setterBuilder.put(HLA_DRB345, new TypeSetter("DR", XmlSureTyperParser::noOp));
 * setterBuilder.put(HLA_DPA1, new TypeSetter("DPA1*", XmlSureTyperParser::noOp));
 * setterBuilder.put(HLA_DPB1, new TypeSetter("DPB1*", XmlSureTyperParser::dpb));
 * setterBuilder.put(HLA_BW, new TypeSetter("", XmlSureTyperParser::decodeBw)); metadataMap =
 * setterBuilder.build(); }
 * 
 *//**
    * Method to be used for calling the correct {@link ValidationModelBuilder} method when a Bw
    * value is encountered in the token stream.
    * 
    * @param builder ValidationModelBuilder to be called if the value matches some particular
    *        value(s).
    * @param value String of the token to be processed.
    */
/*
 * private static void decodeBw(ValidationModelBuilder builder, String value) { switch (value) {
 * case "Bw4": builder.bw4(true); break; case "Bw6": builder.bw6(true); break; } }
 * 
 *//**
    * Method to be used by the TypeSetter object when an unsupported token is happened upon.
    * 
    * @param builder ValidationModelBuilder object to be ignored.
    * @param value String to be ignored.
    *//*
       * private static void noOp(ValidationModelBuilder builder, String value) { // Do nothing as
       * per the method name. } }
       * 
       */
