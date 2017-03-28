// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import android.app.Application;

import com.facebook.soloader.SoLoader;

public class SampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    SoLoader.init(this, false);
  }
}
