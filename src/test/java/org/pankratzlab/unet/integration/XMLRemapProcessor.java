package org.pankratzlab.unet.integration;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import org.pankratzlab.unet.model.remap.CancellationException;
import org.pankratzlab.unet.model.remap.RemapProcessor;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

public class XMLRemapProcessor implements RemapProcessor {

  private Table<SourceType, HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remappings =
      HashBasedTable.create();

  public XMLRemapProcessor(String file) {
    try (FileInputStream xmlStream = new FileInputStream(new File(file))) {
      Document parsed = Jsoup.parse(xmlStream, "UTF-8", "http://example.com");
      Elements remapElements = parsed.getElementsByTag("remappings");
      remapElements = remapElements.get(0).getElementsByTag("remap");
      remapElements.forEach(e -> {
        e.getElementsByTag("remapLocus").forEach(locusElement -> {
          // TODO catch missing or malformed SourceType values and report
          SourceType source =
              SourceType.valueOf(locusElement.getElementsByTag("sourceType").get(0).text());
          // TODO catch missing or malformed locus values and report
          HLALocus locus = HLALocus.valueOf(locusElement.getElementsByTag("locus").get(0).text());
          HLAType[] fromAlleles = locusElement.getElementsByTag("fromAllele").stream()
              .map(Element::text).map(HLAType::valueOf).toArray(HLAType[]::new);
          HLAType[] toAlleles = locusElement.getElementsByTag("toAllele").stream()
              .map(Element::text).map(HLAType::valueOf).toArray(HLAType[]::new);
          // TODO catch missing or malformed allele values and report
          remappings.put(source, locus,
              Pair.of(
                  ImmutableSet.of(new TypePair(fromAlleles[0], fromAlleles[0].equivSafe()),
                      new TypePair(fromAlleles[1], fromAlleles[1].equivSafe())),
                  ImmutableSet.of(new TypePair(toAlleles[0], toAlleles[0].equivSafe()),
                      new TypePair(toAlleles[1], toAlleles[1].equivSafe()))));

        });
      });
    } catch (Throwable e) {
      throw new IllegalStateException("Invalid XML file: " + file, e);
    }
  }

  @Override
  public Pair<Set<TypePair>, Set<TypePair>> processRemapping(HLALocus locus,
      ValidationModelBuilder builder) throws CancellationException {
    return remappings.get(builder.getSourceType(), locus);
  }

  public boolean hasRemappings(SourceType sourceType) {
    return remappings.containsRow(sourceType);
  }

}
