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
package org.pankratzlab.unet.parser;

import java.io.File;
import java.util.function.BiConsumer;
import org.pankratzlab.unet.model.ValidationModel;
import org.pankratzlab.unet.model.ValidationModelBuilder;
import org.pankratzlab.unet.model.ValidationTable;

public interface DonorFileParser {

  /**
   * @return Display header when selecting a file
   */
  public String fileChooserHeader();

  /**
   * @return Initial file name when selecting a file
   */
  public String initialName();

  /**
   * @return File filter extension. Used to restrict file types.
   */
  public String extensionFilter();

  /**
   * @return Human-readable display text for extension filter
   */
  public String extensionDescription();

  /**
   * @param fileName Name of the file that was used
   * @return String to display to user when parsing fails
   */
  public String getErrorText();

  /**
   * @return {@link ValidationTable} method to call after parsing the model (e.g.
   *         {@link ValidationTable#setPdfModel} for a PDF input controller
   */
  public BiConsumer<ValidationTable, ValidationModel> setModel();

  /**
   * Populate the given {@link ValidationModelBuilder} based on the File's contents. Only types
   * allowed by the {@link #extension()} filter will be passed here.
   */
  public void parseModel(ValidationModelBuilder builder, File file);
}
