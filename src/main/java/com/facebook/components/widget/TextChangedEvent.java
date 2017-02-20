// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import com.facebook.components.annotations.Event;

/**
 * Event sent by EditText when the text entered by the user changes.
 */
@Event
public class TextChangedEvent {
  public String text;
}
