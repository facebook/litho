// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.widget.LinearComponentBinder;

public class FeedBinder extends LinearComponentBinder {

  private final DataModel[] mDataModels;

  FeedBinder(Context c) {
    super(c, new LinearLayoutManager(c));
    mDataModels = DataModel.SampleData();
    notifyDataSetChanged();
  }

  @Override
  protected int getCount() {
    return mDataModels.length;
  }

  @Override
  public boolean isAsyncLayoutEnabled() {
    return false;
  }

  @Override
  public Component<?> createComponent(ComponentContext c, int position) {
    return FeedItemComponent.create(c)
        .item(mDataModels[position])
        .index(position)
        .build();
  }
}
