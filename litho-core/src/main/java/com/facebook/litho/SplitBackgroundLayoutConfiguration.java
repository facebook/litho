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

/**
 * Configures what components can put their children's layout calculations on multiple background
 * threads.
 */
public class SplitBackgroundLayoutConfiguration {

  /** Checks if a component is enabled to split its children layouts on multiple threads. */
  static boolean canSplitChildrenLayouts(ComponentContext c, Component component) {
    return SplitLayoutResolver.isComponentEnabledForSplitting(c, component);
  }

  /** If true, the given component's children layouts will be split on multiple threads. */
  static boolean isSplitLayoutEnabled(Component component) {
    return component.mSplitChildrenLayoutInThreadPool;
  }
}
