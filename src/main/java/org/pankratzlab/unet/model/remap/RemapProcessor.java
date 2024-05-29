package org.pankratzlab.unet.model.remap;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;

public interface RemapProcessor {

  public Pair<Set<TypePair>, Set<TypePair>> processRemapping(HLALocus locus,
      ValidationModelBuilder builder) throws CancellationException;

}