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
import org.pankratzlab.hla.HLALocus;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.hla.SeroLocus;
import org.pankratzlab.hla.SeroType;
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
    if (Objects.isNull(aLocus)) {
      aLocus = new LinkedHashSet<>();
    }
    aLocus.add(new SeroType(SeroLocus.A, aType));
    return this;
  }

  public ValidationModelBuilder b(String bType) {
    if (Objects.isNull(bLocus)) {
      bLocus = new LinkedHashSet<>();
    }
    bLocus.add(new SeroType(SeroLocus.B, bType));
    return this;
  }

  public ValidationModelBuilder c(String cType) {
    if (Objects.isNull(cLocus)) {
      cLocus = new LinkedHashSet<>();
    }
    cLocus.add(new SeroType(SeroLocus.C, cType));
    return this;
  }

  public ValidationModelBuilder drb(String drbType) {
    if (Objects.isNull(drbLocus)) {
      drbLocus = new LinkedHashSet<>();
    }
    drbLocus.add(new SeroType(SeroLocus.DRB, drbType));
    return this;
  }

  public ValidationModelBuilder dqb(String dqbType) {
    if (Objects.isNull(dqbLocus)) {
      dqbLocus = new LinkedHashSet<>();
    }
    dqbLocus.add(new SeroType(SeroLocus.DQB, dqbType));
    return this;
  }

  public ValidationModelBuilder dqa(String dqaType) {
    if (Objects.isNull(dqaLocus)) {
      dqaLocus = new LinkedHashSet<>();
    }
    dqaLocus.add(new SeroType(SeroLocus.DQA, dqaType));
    return this;
  }

  public ValidationModelBuilder dpb(String dpbType) {
    if (Objects.isNull(dpbLocus)) {
      dpbLocus = new LinkedHashSet<>();
    }
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

    return new ValidationModel(donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus, dqaLocus,
        dpbLocus, bw4, bw6, dr51, dr52, dr53);
  }

  /**
   * @throws IllegalStateException If the model has not been fully populated, or populated
   *         incorrectly.
   */
  private void ensureValidity() throws IllegalStateException {
    for (Object o : new Object[] {donorId, source, aLocus, bLocus, cLocus, drbLocus, dqbLocus, dqaLocus,
        dpbLocus, bw4, bw6, dr51, dr52, dr53}) {
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
}
