/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.rendercore;

import android.os.Build;
import android.os.Trace;
import androidx.annotation.Nullable;

public final class RenderCoreSystrace {

  public interface IRenderCoreSystrace {
    void beginSection(String name, Class value);

    void endSection();

    void beginAsyncSection(String name, Class value, int cookie);

    void endAsyncSection(String name, Class value, int cookie);
  }

  private static volatile IRenderCoreSystrace sInstance = new DefaultTrace();
  private static volatile boolean sHasStarted = false;

  public static void beginSection(String name) {
    sHasStarted = true;
    sInstance.beginSection(name, null);
  }

  public static void beginSection(String name, @Nullable Class value) {
    sInstance.beginSection(name, value);
  }

  public static void endSection() {
    sInstance.endSection();
  }

  public static void beginAsyncSection(String name, int cookie) {
    sInstance.beginAsyncSection(name, null, cookie);
  }

  public static void beginAsyncSection(String name, @Nullable Class value, int cookie) {
    sInstance.beginAsyncSection(name, value, cookie);
  }

  public static void endAsyncSection(String name, int cookie) {
    sInstance.endAsyncSection(name, null, cookie);
  }

  public static void endAsyncSection(String name, @Nullable Class value, int cookie) {
    sInstance.endAsyncSection(name, value, cookie);
  }

  public static void use(IRenderCoreSystrace systraceImpl) {
    if (sHasStarted) {
      // We will not switch the implementation if the trace has already been used in the
      // app lifecycle.
      return;
    }

    sInstance = systraceImpl;
  }

  private static final class DefaultTrace implements IRenderCoreSystrace {

    @Override
    public void beginSection(String name, Class value) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(getName(name, value));
      }
    }

    @Override
    public void endSection() {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.endSection();
      }
    }

    @Override
    public void beginAsyncSection(String name, Class value, int cookie) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Trace.beginAsyncSection(getName(name, value), cookie);
      }
    }

    @Override
    public void endAsyncSection(String name, Class value, int cookie) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Trace.endAsyncSection(getName(name, value), cookie);
      }
    }

    private static String getName(String name, Class value) {
      return value != null ? name + value.getSimpleName() : name;
    }
  }
}
