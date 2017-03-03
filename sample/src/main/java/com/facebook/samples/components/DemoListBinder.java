// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import java.util.List;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.widget.LinearComponentBinder;

public class DemoListBinder extends LinearComponentBinder {

  private final List<DemoModel> mDemos;

  DemoListBinder(Context c) {
    super(c, new LinearLayoutManager(c));
    mDemos = Demos.getAll();
    notifyDataSetChanged();
  }

  @Override
  protected int getCount() {
    return mDemos.size();
  }

  @Override
  public boolean isAsyncLayoutEnabled() {
    return false;
  }

  @Override
  public Component<?> createComponent(ComponentContext c, int position) {
    return DemoListItemComponent.create(c)
        .item(mDemos.get(position))
        .build();
  }
}
