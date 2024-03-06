package org.pankratzlab.unet.parser.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.hapstats.HaplotypeUtils;
import org.pankratzlab.unet.model.Strand;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class XmlSureTyperParser {
  // Root element to cause XMLDonorParser to come here
  public static final String ROOT_ELEMENT = "suretyperhlareport";

  // Attributes used for parsing assigned section and lab results section
  private static final String NAME_TAG = "name";
  private static final String TEST_NAME_TAG = "testName";

  // Attribute values associated to name attribute
  private static final String PATIENT_ID_TAG = "Patient ID";
  private static final String HLA_DRB345 = "HLA-DRB345";
  private static final String HLA_DRB1 = "HLA-DRB1";
  private static final String HLA_DQA1 = "HLA-DQA1";
  private static final String HLA_DPA1 = "HLA-DPA1";
  private static final String HLA_DPB1 = "HLA-DPB1";
  private static final String HLA_DQB1 = "HLA-DQB1";
  private static final String HLA_C = "HLA-C";
  private static final String HLA_B = "HLA-B";
  private static final String HLA_A = "HLA-A";
  private static final String BW = "Bw";

  /**
   * Method to parse XML to ValidationModelBuilder builder to allow for uniformity
   *
   * @param builder ValidationModelBuilder used to mapping attributes from XML to a standard format.
   *        {@link ValidationModel}
   * @param doc Suretyper XML document to be parsed.
   */
  public static void buildModelFromXML(ValidationModelBuilder builder, Document doc) {
    // Parse assigned typing
    builder.donorId(doc.getElementsByAttributeValue(NAME_TAG, PATIENT_ID_TAG).text().toUpperCase());
    Element labAssignmentSection = doc.getElementsByTag("labAssignmentSection").get(0);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_A).text()
        .replaceAll("A", "").split("\\s")).forEach(builder::a);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_B).text()
        .replaceAll("B", "").split("\\s")).forEach(builder::b);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_C).text()
        .replaceAll("Cw", "").split("\\s")).forEach(builder::c);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_DRB1).text()
        .replaceAll("DR", "").split("\\s")).forEach(builder::drb);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_DQB1).text()
        .replaceAll("DQ", "").split("\\s")).forEach(builder::dqb);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_DQA1).text()
        .replaceAll("DQA1\\*", "").split("\\s")).forEach(builder::dqa);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_DPA1).text()
        .replaceAll("DPA1\\*", "").split("\\s")).forEach(builder::dpa);
    Arrays.stream(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, HLA_DPB1).text()
        .replaceAll("DPB1\\*", "").split("\\s")).forEach(builder::dpb);
    builder
        .bw4(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, BW).text().contains("Bw4"));
    builder
        .bw6(labAssignmentSection.getElementsByAttributeValue(NAME_TAG, BW).text().contains("Bw6"));

    // Parse haplotypes
    Map<String, Multimap<Strand, HLAType>> haplotypeMap = new HashMap<>();
    haplotypeMap.put(HLA_B, ArrayListMultimap.create());
    haplotypeMap.put(HLA_C, ArrayListMultimap.create());
    haplotypeMap.put(HLA_DRB1, ArrayListMultimap.create());
    haplotypeMap.put(HLA_DQB1, ArrayListMultimap.create());
    haplotypeMap.put(HLA_DRB345, ArrayListMultimap.create());
    Element haplotypeSection = doc.getElementsByTag("testResultsSection").get(0);
    parseHaplotype(builder,
        haplotypeSection.getElementsByAttributeValue(TEST_NAME_TAG, HLA_B).get(0),
        haplotypeMap.get(HLA_B));
    parseHaplotype(builder,
        haplotypeSection.getElementsByAttributeValue(TEST_NAME_TAG, HLA_C).get(0),
        haplotypeMap.get(HLA_C));
    parseHaplotype(builder,
        haplotypeSection.getElementsByAttributeValue(TEST_NAME_TAG, HLA_DRB1).get(0),
        haplotypeMap.get(HLA_DRB1));
    parseHaplotype(builder,
        haplotypeSection.getElementsByAttributeValue(TEST_NAME_TAG, HLA_DQB1).get(0),
        haplotypeMap.get(HLA_DQB1));
    parseHaplotype(builder,
        haplotypeSection.getElementsByAttributeValue(TEST_NAME_TAG, HLA_DRB345).get(0),
        haplotypeMap.get(HLA_DRB345));

    // Map haplotypes collected to ValidationModelBuilder
    builder.bHaplotype(haplotypeMap.get(HLA_B));
    builder.cHaplotype(haplotypeMap.get(HLA_C));
    builder.dqHaplotype(haplotypeMap.get(HLA_DQB1));
    builder.drHaplotype(haplotypeMap.get(HLA_DRB1));
    builder.dr345Haplotype(haplotypeMap.get(HLA_DRB345));
  }

  /**
   * Helper method to extract correct alleles section from haplotype sections to be parsed
   *
   * @param builder ValidationModelBuilder used to mapping attributes from XML to a standard format.
   *        {@link ValidationModel}
   * @param haplotypeXmlSection Current haplotype section in doc being parsed
   * @param strandMap Multimap created for specific haplotype for parsing alleles onto.
   */
  private static void parseHaplotype(ValidationModelBuilder builder, Element haplotypeXmlSection,
      Multimap<Strand, HLAType> strandMap) {
    boolean flag = false;
    // Check for if there are more than one hlaTestCall section
    // If there is more than one only parse the one that has been manually selected.
    if (haplotypeXmlSection.getElementsByTag("hlaTestCall").size() == 1) {
      String alleles = haplotypeXmlSection.getElementsByTag("alleles").text();
      String[] loci =
          haplotypeXmlSection.getElementsByTag("hlaTestCall").attr("callName").split("\\s+");
      parseAlleles(alleles, builder, strandMap, loci);
    } else if (haplotypeXmlSection.getElementsByTag("hlaTestCall").size() > 1) {
      for (Element e : haplotypeXmlSection.getElementsByTag("hlaTestCall")) {
        if (e.getElementsByTag("footnoteList").text().contains("MANUALLY")) {
          String alleles = e.getElementsByTag("alleles").text();
          String[] loci = e.getElementsByTag("hlaTestCall").attr("callName").split("\\s+");
          parseAlleles(alleles, builder, strandMap, loci);
          flag = true;
        }
      }
      if (flag == false) {
        System.err.println(
            "Error: Could not find manually selected haplotype for: " + haplotypeXmlSection);
      }
    } else {
      System.err.println("Error: can not find haplotype section for: " + haplotypeXmlSection);
    }
  }

  /**
   * Helper method to extract the alleles from XML and map them to Multimap strandMap
   *
   * @param allelesXmlSection Text value from the alleles section of the XML from with in a specific
   *        haplotype
   * @param builder ValidationModelBuilder used to mapping attributes from XML to a standard format.
   *        {@link ValidationModel}
   * @param strandMap Multimap created for specific haplotype for parsing alleles onto.
   * @param loci String array with locus value(s)
   */
  private static void parseAlleles(String allelesXmlSection, ValidationModelBuilder builder,
      Multimap<Strand, HLAType> strandMap, String[] loci) {
    String[] tokens = allelesXmlSection.split("\\s+");
    // flag for being on a new strand.
    boolean newStrand = true;
    int strandIndex = 0;
    String locus = loci[0].split("\\*")[0];
    for (int tokenIndex = 2; tokenIndex < tokens.length; tokenIndex++) {
      String token = tokens[tokenIndex].trim();
      if (!token.contains(":")) {
        // Not an allele specificity
        continue;
      }
      // Check to see if on second strand
      if (loci.length > 1 && token.contains(loci[1]) && !token.contains(loci[0])
          && strandIndex == 0) {
        strandIndex = 1;
        locus = loci[1].split("\\*")[0];
        newStrand = true;
      }
      if ((token.contains(HLALocus.DRB3 + "*") || token.contains(HLALocus.DRB4 + "*")
          || token.contains(HLALocus.DRB5 + "*")) && !token.contains("N") && newStrand) {
        String type = token.substring(token.indexOf("*"), token.indexOf(":"));
        if (token.contains(HLALocus.DRB3 + "*")) {
          builder.dr52(type);
        } else if (token.contains(HLALocus.DRB4 + "*")) {
          builder.dr53(type);
        } else if (token.contains(HLALocus.DRB5 + "*")) {
          builder.dr51(type);
        }
        newStrand = false;
      }

      // double checking that locus is correct.
      if (!token.contains(locus)) {
        System.err.println("Error: locus " + locus + " missing from token " + token);
        throw new IllegalArgumentException();
      }

      // Sanitize the token string
      token = token.replaceAll(locus, "");
      token = token.replaceAll("[^0-9:nN-]", "");

      if (token.isEmpty() || token.equals(":")) {
        // Wasn't actually an allele
        continue;
      }
      HaplotypeUtils.parseAllelesToStrandMap(token, locus, strandIndex, strandMap);
    }
  }
}
