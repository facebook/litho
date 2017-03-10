// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.widget.LinearComponentBinder;

public class FeedBinder extends LinearComponentBinder {

  FeedBinder(Context c) {
    super(c, new LinearLayoutManager(c));
  }

  @Override
  protected int getCount() {
    return 32;
  }

  @Override
  public Component<?> createComponent(ComponentContext c, int position) {
    return FeedItem.create(c)
        .build();
  }
}
