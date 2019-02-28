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

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** A manager for Litho's hotswap capability. */
public final class HotswapManager {

  @GuardedBy("this")
  @Nullable
  private static ClassLoader sSpecClassLoader;

  @GuardedBy("this")
  private static final Set<ComponentTree> sComponentTrees =
      Collections.newSetFromMap(new WeakHashMap<ComponentTree, Boolean>());

  /**
   * This method should be called in the {@link android.app.Application} to ensure that the correct
   * class loader is set up when we load the Litho spec classes.
   */
  public static synchronized void init(ClassLoader classLoader) {
    sSpecClassLoader = classLoader;
  }

  /** Gets the class loader that should be used to load Litho spec classes. */
  @Nullable
  public static synchronized ClassLoader getClassLoader() {
    if (!ComponentsConfiguration.IS_INTERNAL_BUILD) {
      throw new RuntimeException("Hotswap ClassLoader should only be accessed in debug mode.");
    }

    return sSpecClassLoader;
  }

  /**
   * Adds a {@link ComponentTree} to the manager. This ComponentTree will be informed when hotswap
   * has occurred.
   */
  static synchronized void addComponentTree(ComponentTree componentTree) {
    sComponentTrees.add(componentTree);
  }

  /**
   * Call this method when hotswap occurs in order to inform all {@link ComponentTree}s that they
   * need to update.
   */
  public static synchronized void onHotswap() {
    for (ComponentTree componentTree : sComponentTrees) {
      componentTree.forceMainThreadLayout();
    }
  }
}
