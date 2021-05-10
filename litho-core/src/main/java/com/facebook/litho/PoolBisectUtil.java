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

package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;

/** A utility to help with bisecting a problem with mount content pooling. See T43440735. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PoolBisectUtil {

  static MountContentPool getPoolForComponent(Component component) {
    if (ComponentsConfiguration.isPoolBisectEnabled
        && shouldDisablePool(component.getSimpleName())) {
      return new DisabledMountContentPool();
    }
    return component.onCreateMountContentPool();
  }

  private static boolean shouldDisablePool(String componentName) {
    final String start = ComponentsConfiguration.disablePoolsStart;
    final String end = ComponentsConfiguration.disablePoolsEnd;

    return componentName.compareToIgnoreCase(start) >= 0
        && componentName.compareToIgnoreCase(end) <= 0;
  }
}
