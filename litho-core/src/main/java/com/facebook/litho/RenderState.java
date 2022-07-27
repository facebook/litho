/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

  private final Map<String, Component.RenderData> mRenderData = new HashMap<>();
  private final Set<String> mSeenGlobalKeys = new HashSet<>();

  void recordRenderData(final List<ScopedComponentInfo> scopedComponentInfos) {
    if (scopedComponentInfos == null) {
      return;
    }

    for (int i = 0, size = scopedComponentInfos.size(); i < size; i++) {
      recordRenderData(scopedComponentInfos.get(i));
    }
    mSeenGlobalKeys.clear();
  }

  void applyPreviousRenderData(List<ScopedComponentInfo> scopedComponentInfos) {
    if (scopedComponentInfos == null) {
      return;
    }

    for (int i = 0, size = scopedComponentInfos.size(); i < size; i++) {
      applyPreviousRenderData(scopedComponentInfos.get(i));
    }
  }

  private void recordRenderData(final ScopedComponentInfo scopedComponentInfo) {
    final Component component = scopedComponentInfo.getComponent();
    final String globalKey = scopedComponentInfo.getContext().getGlobalKey();
    if (!isPreviousRenderDataSupported(component)) {
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

    final ComponentContext scopedContext = scopedComponentInfo.getContext();
    final Component.RenderData existingInfo = mRenderData.get(globalKey);
    final Component.RenderData newInfo =
        ((SpecGeneratedComponent) component).recordRenderData(scopedContext, existingInfo);

    mRenderData.put(globalKey, newInfo);
  }

  private void applyPreviousRenderData(ScopedComponentInfo scopedComponentInfo) {
    final Component component = scopedComponentInfo.getComponent();
    final String globalKey = scopedComponentInfo.getContext().getGlobalKey();
    if (!isPreviousRenderDataSupported(component)) {
      throw new RuntimeException(
          "Trying to apply previous render data to component that doesn't support it");
    }

    final Component.RenderData previousRenderData = mRenderData.get(globalKey);
    ((SpecGeneratedComponent) component).applyPreviousRenderData(previousRenderData);
  }

  private boolean isPreviousRenderDataSupported(Component component) {
    return component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).needsPreviousRenderData();
  }
}
