// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.os.Build;
import android.os.Trace;

public interface Systrace {

  public static Systrace sInstance = new DefaultTrace();

  void beginSection(String name);

  void endSection();

  class DefaultTrace implements Systrace {

    @Override
    public void beginSection(String name) {
      if (BuildConfig.IS_INTERNAL_BUILD
          && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(name);
      }
    }

    @Override
    public void endSection() {
      if (BuildConfig.IS_INTERNAL_BUILD
          && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.endSection();
      }
    }
  }
}
