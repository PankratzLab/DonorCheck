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
package org.pankratzlab.unet.jfx.wizard;

import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;
import javafx.beans.value.ObservableBooleanValue;

/**
 * {@link WizardPane} subclass which binds the {@link Wizard}'s invalid property to a specified
 * {@link ObservableBooleanValue}. This allows control over whether or not the wizard can be
 * advanced/completed.
 * <p>
 * Also updates the {@link Wizard#titleProperty()} based on the current
 * {@link WizardPane#getUserData()}
 * </p>
 */
public class ValidatingWizardPane extends WizardPane {

  private ObservableBooleanValue invalidBinding;

  public void setInvalidBinding(ObservableBooleanValue invalidBinding) {
    this.invalidBinding = invalidBinding;
  }

  @Override
  public void onEnteringPage(Wizard wizard) {
    if (invalidBinding != null) {
      wizard.invalidProperty().bind(invalidBinding);
    } else {
      // If no binding, not invalid
      wizard.invalidProperty().unbind();
      wizard.setInvalid(false);
    }

    wizard.setTitle(getUserData().toString());
  }

}
