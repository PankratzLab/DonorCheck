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
package org.pankratzlab.unet.hapstats;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.pankratzlab.hla.HLAType;
import org.pankratzlab.unet.hapstats.HaplotypeFreqTable.Haplotype;
import org.pankratzlab.unet.hapstats.HaplotypeFrequency.Ethnicity;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;

public class HaplotypeFreqTable implements Table<HLAType, HLAType, Haplotype> {


  private final Table<HLAType, HLAType, Haplotype> frequencyTable;

  public HaplotypeFreqTable() {
    frequencyTable = HashBasedTable.create();
  }

  public boolean contains(Object rowKey, Object columnKey) {
    return frequencyTable.contains(rowKey, columnKey);
  }


  public boolean containsRow(Object rowKey) {
    return frequencyTable.containsRow(rowKey);
  }


  public boolean containsColumn(Object columnKey) {
    return frequencyTable.containsColumn(columnKey);
  }


  public boolean containsValue(Object value) {
    return frequencyTable.containsValue(value);
  }


  public Haplotype get(Object rowKey, Object columnKey) {
    return frequencyTable.get(rowKey, columnKey);
  }


  public boolean isEmpty() {
    return frequencyTable.isEmpty();
  }


  public int size() {
    return frequencyTable.size();
  }


  public boolean equals(Object obj) {
    return frequencyTable.equals(obj);
  }


  public int hashCode() {
    return frequencyTable.hashCode();
  }


  public void clear() {
    frequencyTable.clear();
  }


  public Haplotype put(HLAType rowKey, HLAType columnKey, Haplotype value) {
    return frequencyTable.put(rowKey, columnKey, value);
  }


  public void putAll(Table<? extends HLAType, ? extends HLAType, ? extends Haplotype> table) {
    frequencyTable.putAll(table);
  }


  public Haplotype remove(Object rowKey, Object columnKey) {
    return frequencyTable.remove(rowKey, columnKey);
  }


  public Map<HLAType, Haplotype> row(HLAType rowKey) {
    return frequencyTable.row(rowKey);
  }


  public Map<HLAType, Haplotype> column(HLAType columnKey) {
    return frequencyTable.column(columnKey);
  }


  public Set<Cell<HLAType, HLAType, Haplotype>> cellSet() {
    return frequencyTable.cellSet();
  }


  public Set<HLAType> rowKeySet() {
    return frequencyTable.rowKeySet();
  }


  public Set<HLAType> columnKeySet() {
    return frequencyTable.columnKeySet();
  }


  public Collection<Haplotype> values() {
    return frequencyTable.values();
  }


  public Map<HLAType, Map<HLAType, Haplotype>> rowMap() {
    return frequencyTable.rowMap();
  }


  public Map<HLAType, Map<HLAType, Haplotype>> columnMap() {
    return frequencyTable.columnMap();
  }


  public static class Haplotype {
    private final ImmutableMap<Ethnicity, Double> frequencyForEthnicity;

    public Haplotype(Map<Ethnicity, Double> frequencyByEth) {
      this.frequencyForEthnicity = ImmutableMap.copyOf(frequencyByEth);
    }

    public Double getFrequencyForEthnicity(Ethnicity e) {
      return frequencyForEthnicity.get(e);
    }

    @Override
    public String toString() {
      return frequencyForEthnicity.toString();
    }
  }
}
