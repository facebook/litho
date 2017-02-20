// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.annotations.Event;

/**
 * Event triggered when a Component becomes invisible. This is the same with exiting the Visible
 * Range, the Focused Range and the Full Impression Range. All the code that needs to be executed
 * when a component leaves any of these ranges should be written in the handler for this event.
 */
@Event
public class InvisibleEvent {
}
