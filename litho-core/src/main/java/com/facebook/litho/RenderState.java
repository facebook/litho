/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.support.v4.util.SimpleArrayMap;
import com.facebook.litho.internal.ArraySet;
import java.util.List;

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use {@link Diff}'s of props in
 * any of their lifecycle methods).
 */
public class RenderState {

  private final SimpleArrayMap<String, ComponentLifecycle.RenderData> mRenderData =
      new SimpleArrayMap<>();
  private final ArraySet<String> mSeenGlobalKeys = new ArraySet<>();

  void recordRenderData(List<Component> components) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      recordRenderData(components.get(i));
    }
    mSeenGlobalKeys.clear();
  }

  void applyPreviousRenderData(List<Component> components) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      applyPreviousRenderData(components.get(i));
    }
  }

  private void recordRenderData(Component component) {
    final ComponentLifecycle lifecycle = component;
    if (!lifecycle.needsPreviousRenderData()) {
      throw new RuntimeException(
          "Trying to record previous render data for component that doesn't support it");
    }

    final String key = component.getGlobalKey();

    // Sanity check like in StateHandler
    if (mSeenGlobalKeys.contains(key)) {
      // We found two components with the same global key.
      throw new RuntimeException(
          "Cannot record previous render data for "
              + component.getSimpleName()
              + ", found another Component with the same key: "
              + key);
    }
    mSeenGlobalKeys.add(key);

    final ComponentLifecycle.RenderData existingInfo = mRenderData.get(key);
    final ComponentLifecycle.RenderData newInfo =
        lifecycle.recordRenderData(component, existingInfo);

    mRenderData.put(key, newInfo);
  }

  private void applyPreviousRenderData(Component component) {
    final ComponentLifecycle lifecycle = component;
    if (!lifecycle.needsPreviousRenderData()) {
      throw new RuntimeException(
          "Trying to apply previous render data to component that doesn't support it");
    }

    final String key = component.getGlobalKey();
    ComponentLifecycle.RenderData previousRenderData = mRenderData.get(key);
    lifecycle.applyPreviousRenderData(component, previousRenderData);
  }

  void reset() {
    mRenderData.clear();
    mSeenGlobalKeys.clear();
  }
}
