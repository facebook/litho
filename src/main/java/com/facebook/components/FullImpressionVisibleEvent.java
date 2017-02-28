// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.annotations.Event;

/**
 * Event triggered when a Component enters the Full Impression Range. This happens, for instance in
 * the case of a vertical RecyclerView, when both the top and bottom edges of the component become
 * visible.
 */
@Event
public class FullImpressionVisibleEvent {
}
