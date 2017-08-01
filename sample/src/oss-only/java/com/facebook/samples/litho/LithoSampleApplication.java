/*
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho;

import android.app.Application;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.litho.stetho.LithoWebKitInspector;
import com.facebook.soloader.SoLoader;
import com.facebook.stetho.Stetho;

public class LithoSampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Fresco.initialize(this);
    SoLoader.init(this, false);
    Stetho.initialize(
        Stetho.newInitializerBuilder(this)
            .enableWebKitInspector(new LithoWebKitInspector(this))
            .build());
  }
}
