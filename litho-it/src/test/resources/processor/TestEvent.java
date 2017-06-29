// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.processor.integration.resources;

import android.view.View;

import com.facebook.litho.annotations.Event;

@Event
public class TestEvent {
  public View view;
  public Object object;
}
