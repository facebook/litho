/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso;

import android.app.Application;
import com.facebook.soloader.SoLoader;

public class InstrumentationApp extends Application {
  @Override
  public void onCreate() {
    super.onCreate();

    SoLoader.init(this, false);
  }
}
