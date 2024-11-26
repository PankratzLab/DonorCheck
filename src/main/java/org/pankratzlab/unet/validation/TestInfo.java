package org.pankratzlab.unet.validation;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.SourceType;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import com.google.common.collect.ImmutableMap;

public class TestInfo {
  public final String label;
  public final String file;
  public final ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remappings;
  public final SourceType sourceType;

  public TestInfo(String label, String file,
      ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remappings,
      SourceType sourceType) {
    this.label = label;
    this.file = file;
    this.remappings = remappings;
    this.sourceType = sourceType;
  }

}