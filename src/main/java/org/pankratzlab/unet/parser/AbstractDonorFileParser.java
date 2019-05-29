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
package org.pankratzlab.unet.parser;

import java.io.File;
import java.security.InvalidParameterException;

import org.apache.commons.io.FilenameUtils;
import org.pankratzlab.unet.model.ValidationModelBuilder;

/** Abstract superclass for {@link DonorFileParser} for common operations. */
public abstract class AbstractDonorFileParser implements DonorFileParser {

  @Override
  public void parseModel(ValidationModelBuilder builder, File file) {
    if (!FilenameUtils.isExtension(file.getName(), extensionName())) {
      throw new InvalidParameterException("Unknown File Type: " + file.getName());
    }

    builder.source(file.getName());

    doParse(builder, file);
  }

  @Override
  public String toString() {
    return getDisplayString();
  }

  /** @return Label to display to users (to select between different file types) */
  protected abstract String getDisplayString();

  /** @return Extension string for checking (e.g. no file separator" */
  protected abstract String extensionName();

  /**
   * Perform the actual builder population from this file. At this point it is verified that the
   * file matches the {@link #extensionName()}.
   */
  protected abstract void doParse(ValidationModelBuilder builder, File file);
}
