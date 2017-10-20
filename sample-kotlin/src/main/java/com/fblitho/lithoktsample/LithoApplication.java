package com.fblitho.lithoktsample;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.soloader.SoLoader;

/**
 Created by pasqualea on 10/13/17. */

public class LithoApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    Fresco.initialize(this);
    SoLoader.init(this, false);
  }
}
