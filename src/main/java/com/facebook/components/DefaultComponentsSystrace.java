// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.os.Build;
import android.os.Trace;

import com.facebook.components.config.ComponentsConfiguration;

public class DefaultComponentsSystrace implements ComponentsSystrace.Systrace {
  @Override
  public void beginSection(String name) {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD &&
        Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.beginSection(name);
    }
  }

  @Override
  public void endSection() {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.endSection();
    }
  }
}
