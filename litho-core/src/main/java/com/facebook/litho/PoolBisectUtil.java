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

import androidx.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;

/** A temporary utility to help with bisecting a problem with mount content pooling. See S168698. */
public class PoolBisectUtil {

  static MountContentPool getPoolForComponent(Component component) {
    if (ComponentsConfiguration.isPoolBisectEnabled
        && shouldDisablePool(component.getSimpleName())) {
      return new DisabledMountContentPool();
    }
    return component.onCreateMountContentPool();
  }

  @VisibleForTesting
  static boolean shouldDisablePool(String componentName) {
    if (!ComponentsConfiguration.isPoolBisectEnabled) {
      return false;
    }

    final String start = ComponentsConfiguration.disablePoolsStart;
    final String end = ComponentsConfiguration.disablePoolsEnd;

    return componentName.compareToIgnoreCase(start) >= 0
        && componentName.compareToIgnoreCase(end) <= 0;
  }
}
