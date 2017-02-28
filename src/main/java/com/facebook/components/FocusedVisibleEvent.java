// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.annotations.Event;

/**
 * Event triggered when a Component enters the Focused Range. This happens when either the Component
 * occupies at least half of the viewport or, if the Component is smaller than half of the viewport,
 * when the it is fully visible.
 */
@Event
public class FocusedVisibleEvent {
}
