/*
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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.pankratzlab.hla.type.HLALocus;
import org.pankratzlab.hla.type.HLAType;
import org.pankratzlab.hla.type.SeroLocus;
import org.pankratzlab.hla.type.SeroType;
import com.google.common.collect.ImmutableList;

/**
 * Mutable builder class for creating a {@link ValidationModel}.
 * <p>
 * Use the locus-specific setters to build up the donor typing. Each must be called at least once
 * (for homozygous) and at most twice (for heterozygous). Each requires on the specificty string for
 * the given locus (e.g. "24" for A24).
 * </p>
 */
public class ValidationModelBuilder {

  private String donorId;
  private String source;
  private Set<SeroType> aLocus;
  private Set<SeroType> bLocus;
  private Set<SeroType> cLocus;
  private Set<SeroType> drbLocus;
  private Set<SeroType> dqbLocus;
  private Set<SeroType> dqaLocus;
  private Set<HLAType> dpbLocus;
  private Boolean bw4;
  private Boolean bw6;
  private Boolean dr51;
  private Boolean dr52;
  private Boolean dr53;

  /**
   * @param donorId Unique identifying string for this donor
   */
  public ValidationModelBuilder donorId(String donorId) {
    this.donorId = donorId;
    return this;
  }

  /**
   * @param source Name for the source of this model
   */
  public ValidationModelBuilder source(String source) {
    this.source = source;
    return this;
  }

  public ValidationModelBuilder a(String aType) {
    aLocus = makeIfNull(aLocus);
    addToLocus(aLocus, SeroLocus.DQB, aType);
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    bLocus = makeIfNull(bLocus);
    addToLocus(bLocus, SeroLocus.DQB, bType);
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    cLocus = makeIfNull(cLocus);
    addToLocus(cLocus, SeroLocus.DQB, cType);
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    drbLocus = makeIfNull(drbLocus);
    addToLocus(drbLocus, SeroLocus.DQB, drbType);
    return this;
  }

  public ValidationModelBuilder dqb(String dqbType) {
    dqbLocus = makeIfNull(dqbLocus);
    addToLocus(dqbLocus, SeroLocus.DQB, dqbType);
    return this;
  }

  public ValidationModelBuilder dqa(String dqaType) {
    dqaLocus = makeIfNull(dqaLocus);
    addToLocus(dqaLocus, SeroLocus.DQB, dqaType);
    return this;
  }

  public ValidationModelBuilder dpb(String dpbType) {
    dpbLocus = makeIfNull(dpbLocus);
    dpbLocus.add(new HLAType(HLALocus.DPB1, dpbType));
    return this;
  }

  public ValidationModelBuilder bw4(boolean bw4) {
    this.bw4 = bw4;
    return this;
  }

  public ValidationModelBuilder bw6(boolean bw6) {
    this.bw6 = bw6;
    return this;
  }

  public ValidationModelBuilder dr51(boolean dr51) {
    this.dr51 = dr51;
    return this;
  }

  public ValidationModelBuilder dr52(boolean dr52) {
    this.dr52 = dr52;
    return this;
  }

  public ValidationModelBuilder dr53(boolean dr53) {
    this.dr53 = dr53;
    return this;
  }

  /**
   * @return The immutable {@link ValidationModel} based on the current builder state.
   */
  public ValidationModel build() {
    ensureValidity();

    return new ValidationModel(donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus,
        dqaLocus, dpbLocus, bw4, bw6, dr51, dr52, dr53);
  }

  /**
   * @throws IllegalStateException If the model has not been fully populated, or populated
   *         incorrectly.
   */
  private void ensureValidity() throws IllegalStateException {
    for (Object o : new Object[] {donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus,
        dqaLocus, dpbLocus, bw4, bw6, dr51, dr52, dr53}) {
      if (Objects.isNull(o)) {
        throw new IllegalStateException("ValidationModel incomplete");
      }
    }
    for (Set<?> set : ImmutableList.of(aLocus, bLocus, cLocus, drbLocus, dqbLocus, dqaLocus,
        dpbLocus)) {
      if (set.size() <= 0 || set.size() > 2) {
        throw new IllegalStateException("ValidationModel contains invalid allele count: " + set);
      }
    }
  }

  /**
   * Helper method to build a set if it's null
   */
  private <T> Set<T> makeIfNull(Set<T> locusSet) {
    if (Objects.isNull(locusSet)) {
      locusSet = new LinkedHashSet<>();
    }
    return locusSet;
  }

  /**
   * Helper method to create {@link SeroType}s and add them to a locus's set in a consistent manner
   */
  private void addToLocus(Set<SeroType> locusSet, SeroLocus locus, String typeString) {
    if (typeString.length() > 2) {
      typeString = typeString.substring(0, typeString.length() - 2);
    }
    locusSet.add(new SeroType(locus, typeString));
  }
}
