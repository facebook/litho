// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.soloader.SoLoader;

public class LithoSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);
  }
}
