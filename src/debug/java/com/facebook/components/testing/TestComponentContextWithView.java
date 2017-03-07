// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing;

import android.content.Context;
import android.view.View;

import com.facebook.components.ComponentContext;

public class TestComponentContextWithView extends ComponentContext {

  private final View mTestView;

  public TestComponentContextWithView(Context c) {
    super(c);
    if (c instanceof TestComponentContextWithView) {
      mTestView = ((TestComponentContextWithView) c).getTestView();
    } else {
      mTestView = new View(c);
    }
  }

  public TestComponentContextWithView(Context context, View view) {
    super(context);
    mTestView = view;
  }

  public View getTestView() {
    return mTestView;
  }
}
