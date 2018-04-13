/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import com.facebook.litho.config.ComponentsConfiguration;

/**
 * This is intended as a hook into {@code android.os.Trace}, but allows you to provide your own
 * functionality.  Use it as
 * <p>
 *   {@code
 *      ComponentsSystrace.beginSection("tag");
 *      ...
 *      ComponentsSystrace.endSection();
 *   }
 * </p>
 *
 * As a default, it simply calls {@code android.os.Trace} (see {@link DefaultComponentsSystrace}).
 * You may supply your own with {@link ComponentsSystrace#provide(Systrace)}.
 */
public class ComponentsSystrace {

  private static volatile Systrace sInstance = null;

  public interface Systrace {
    void beginSection(String name);
    void endSection();
    boolean isTracing();
  }

  private ComponentsSystrace() {
  }

  public static void provide(Systrace instance) {
    sInstance = instance;
  }

  public static void beginSection(String name) {
    getInstance().beginSection(name);
  }

  public static void endSection() {
    getInstance().endSection();
  }

  public static boolean isTracing() {
    return getInstance().isTracing();
  }

  private static Systrace getInstance() {
    if (sInstance == null) {
      synchronized (ComponentsSystrace.class) {
        if (sInstance == null) {
          sInstance = new DefaultComponentsSystrace();
        }
      }
    }
    return sInstance;
  }
}
