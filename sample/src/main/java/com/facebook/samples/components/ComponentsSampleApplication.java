// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import android.app.Application;

import com.facebook.components.ComponentsSystrace;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.soloader.SoLoader;

public class ComponentsSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);
  }
}
