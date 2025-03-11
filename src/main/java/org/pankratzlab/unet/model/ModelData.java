package org.pankratzlab.unet.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.pankratzlab.unet.deprecated.hla.HLALocus;
import org.pankratzlab.unet.deprecated.hla.HLAType;
import org.pankratzlab.unet.model.ValidationModelBuilder.TypePair;
import org.pankratzlab.unet.model.ValidationTable.ValidationKey;
import com.google.common.collect.ImmutableMap;

public class ModelData {
  private final Map<ValidationKey, String> data = new HashMap<>();
  private final List<String> audit;
  private final ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> remaps;
  private ImmutableMap<HLALocus, Set<HLAType>> manuals;

  public ModelData(ValidationModel t) {
    for (ValidationKey k : ValidationKey.values()) {
      data.put(k, k.get(t));
    }
    this.remaps = t.getRemappings();
    this.audit = t.getAuditMessages();
    this.manuals = t.getManuallyAssignedLoci();
  }

  public String get(ValidationKey key) {
    return data.getOrDefault(key, "");
  }

  public ImmutableMap<HLALocus, Pair<Set<TypePair>, Set<TypePair>>> getRemaps() {
    return remaps;
  }

  public List<String> getAudit() {
    return audit;
  }

  public ImmutableMap<HLALocus, Set<HLAType>> getManuallyAssignedLoci() {
    return manuals;
  }

}