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

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event indicating that a wizard page has been activated
 */
public class PageActivatedEvent extends Event {

  private static final long serialVersionUID = 7366449581681992672L;

  public static final EventType<PageActivatedEvent> PAGE_ACTIVE =
      new EventType<PageActivatedEvent>(Event.ANY, "APP CLOSING");

  public PageActivatedEvent() {
    this(PAGE_ACTIVE);
  }

  public PageActivatedEvent(EventType<PageActivatedEvent> eventType) {
    this(null, null, eventType);
  }

  public PageActivatedEvent(Object source, EventTarget target,
      EventType<PageActivatedEvent> eventType) {
    super(source, target, eventType);
  }
}
