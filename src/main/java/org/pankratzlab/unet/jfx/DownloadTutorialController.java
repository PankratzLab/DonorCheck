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
package org.pankratzlab.unet.jfx;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;

/**
 * Simple controller for wizard page displaying a download screenshot. Only really necessary to
 * ensure fxml is injected
 */
public class DownloadTutorialController {


  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  void initialize() {

  }

}
