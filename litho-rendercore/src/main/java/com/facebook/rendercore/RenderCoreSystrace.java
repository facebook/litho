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

public final class RenderCoreSystrace {

  public interface IRenderCoreSystrace {
    void beginSection(String name);

    void endSection();
  }

  private static volatile IRenderCoreSystrace sInstance = new DefaultTrace();
  private static volatile boolean sHasStarted = false;

  public static void beginSection(String name) {
    sHasStarted = true;
    sInstance.beginSection(name);
  }

  public static void endSection() {
    sInstance.endSection();
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
    public void beginSection(String name) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(name);
      }
    }

    @Override
    public void endSection() {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.endSection();
      }
    }
  }
}
