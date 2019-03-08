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
package org.pankratzlab.unet.hapstats;

/**
 * Enum of 2013 haplotype ethnicities, mapping to {@link RaceGroup} for older sets (which are used
 * in DonorNet still). Taken from
 * <a href="https://frequency.nmdp.org/Frequencies/NMDP%20Population%20Tables%201-2.pdf">NMDP</a>
 */
public enum RaceCode {

  AAFA(RaceGroup.AFA), AFB(RaceGroup.AFA), AINDI(RaceGroup.API), AISC(RaceGroup.NAM), ALANAM(
      RaceGroup.NAM), AMIND(RaceGroup.NAM), CARB(RaceGroup.AFA), CARHIS(RaceGroup.HIS), CARIBI(
          RaceGroup.NAM), EURCAU(RaceGroup.CAU), FILII(RaceGroup.API), HAWI(RaceGroup.API), JAPI(
              RaceGroup.API), KORI(RaceGroup.API), MENAFC(RaceGroup.CAU), MSWHIS(
                  RaceGroup.HIS), NCHI(RaceGroup.API), SCAHIS(RaceGroup.HIS), SCAMB(
                      RaceGroup.AFA), SCSEAI(RaceGroup.API), VIET(RaceGroup.API);

  private final RaceGroup generalRaceGroup;

  private RaceCode(RaceGroup eth) {
    generalRaceGroup = eth;
  }

  public RaceGroup getRaceGroup() {
    return generalRaceGroup;
  }
}
