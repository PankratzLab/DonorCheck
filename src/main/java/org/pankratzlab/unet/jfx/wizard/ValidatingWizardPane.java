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
package org.pankratzlab.unet.jfx.wizard;

import java.util.Objects;

import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * {@link WizardPane} subclass which binds the {@link Wizard}'s invalid property to a specified
 * {@link ObservableBooleanValue}. This allows control over whether or not the wizard can be
 * advanced/completed.
 *
 * <p>
 * Also updates the {@link Wizard#titleProperty()} based on the current
 * {@link WizardPane#getUserData()}
 */
public class ValidatingWizardPane extends WizardPane {

  private BooleanProperty invalidBinding = null;
  private BooleanProperty autosizeBinding = null;

  public void setAutosize(ObservableBooleanValue binding) {
    if (Objects.isNull(autosizeBinding)) {
      autosizeBinding = new ReadOnlyBooleanWrapper();
      this.sceneProperty().addListener((s, o, n) -> {
        if (this.getScene() == null)
          return;
        if (this.getScene().getWindow() == null)
          return;
        Window w = this.getScene().getWindow();
        if (w instanceof Stage) {
          ((Stage) w).resizableProperty().bindBidirectional(autosizeBinding);
        }
      });
    }

    autosizeBinding.bind(binding);
  }

  public void setInvalidBinding(ObservableBooleanValue binding) {
    if (Objects.isNull(invalidBinding)) {
      invalidBinding = new ReadOnlyBooleanWrapper();
    }

    invalidBinding.bind(binding);

  }

  @Override
  public void onEnteringPage(Wizard wizard) {
    if (Objects.nonNull(invalidBinding)) {
      wizard.invalidProperty().bind(invalidBinding);
    } else {
      // If no binding, not invalid
      wizard.invalidProperty().unbind();
      wizard.setInvalid(false);
    }

    // Notify any listeners that we are active
    fireEvent(new PageActivatedEvent());
  }

}
