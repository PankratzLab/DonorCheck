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
import java.security.InvalidParameterException;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.select.Elements;
import org.pankratzlab.unet.model.ValidationModelBuilder;

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

  /**
   * Helper method to extract the text value of an element, with a null value if the element is not
   * present or empty
   */
  protected Optional<String> getText(Elements elements) {
    String val = null;
    if (!elements.isEmpty()) {
      String text = elements.get(0).text();
      if (!text.isEmpty()) {
        val = text;
      }
    }

    return Optional.ofNullable(val);
  }

  protected abstract String getDisplayString();

  protected abstract String extensionName();

  protected abstract void doParse(ValidationModelBuilder builder, File file);
}
