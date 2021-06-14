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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use {@link Diff}'s of props in
 * any of their lifecycle methods).
 */
public class RenderState {

  private final Map<String, ComponentLifecycle.RenderData> mRenderData = new HashMap<>();
  private final Set<String> mSeenGlobalKeys = new HashSet<>();

  void recordRenderData(
      final LayoutStateContext layoutStateContext,
      final List<Component> components,
      final List<String> componentGlobalKeys) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      recordRenderData(layoutStateContext, components.get(i), componentGlobalKeys.get(i));
    }
    mSeenGlobalKeys.clear();
  }

  void applyPreviousRenderData(List<Component> components, List<String> componentGlobalKeys) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      applyPreviousRenderData(components.get(i), componentGlobalKeys.get(i));
    }
  }

  private void recordRenderData(
      final LayoutStateContext layoutStateContext,
      final Component component,
      final String globalKey) {
    if (!component.needsPreviousRenderData()) {
      throw new RuntimeException(
          "Trying to record previous render data for component that doesn't support it");
    }

    // Sanity check like in StateHandler
    if (mSeenGlobalKeys.contains(globalKey)) {
      // We found two components with the same global key.
      throw new RuntimeException(
          "Cannot record previous render data for "
              + component.getSimpleName()
              + ", found another Component with the same key: "
              + globalKey);
    }
    mSeenGlobalKeys.add(globalKey);

    final ComponentContext scopedContext =
        component.getScopedContext(layoutStateContext, globalKey);
    final ComponentLifecycle.RenderData existingInfo = mRenderData.get(globalKey);
    final ComponentLifecycle.RenderData newInfo =
        component.recordRenderData(scopedContext, existingInfo);

    mRenderData.put(globalKey, newInfo);
  }

  private void applyPreviousRenderData(Component component, String globalKey) {
    if (!component.needsPreviousRenderData()) {
      throw new RuntimeException(
          "Trying to apply previous render data to component that doesn't support it");
    }

    final ComponentLifecycle.RenderData previousRenderData = mRenderData.get(globalKey);
    component.applyPreviousRenderData(previousRenderData);
  }
}
