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

import android.os.Build;
import android.os.Trace;
import com.facebook.litho.config.ComponentsConfiguration;

public class DefaultComponentsSystrace implements ComponentsSystrace.Systrace {

  @Override
  public void beginSection(String name) {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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

  @Override
  public boolean isTracing() {
    return ComponentsConfiguration.IS_INTERNAL_BUILD
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
  }
}
