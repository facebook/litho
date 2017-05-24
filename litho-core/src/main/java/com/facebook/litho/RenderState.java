/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.List;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.internal.ArraySet;

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use {@link Diff}'s of props in
 * any of their lifecycle methods).
 */
public class RenderState {

  private final SimpleArrayMap<String, ComponentLifecycle.RenderInfo> mRenderInfos =
      new SimpleArrayMap<>();
  private final ArraySet<String> mSeenGlobalKeys = new ArraySet<>();

  void recordRenderInfo(List<Component> components) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      recordRenderInfo(components.get(i));
    }
    mSeenGlobalKeys.clear();
  }

  void applyPreviousRenderInfo(List<Component> components) {
    if (components == null) {
      return;
    }

    for (int i = 0, size = components.size(); i < size; i++) {
      applyPreviousRenderInfo(components.get(i));
    }
  }

  private void recordRenderInfo(Component component) {
    final ComponentLifecycle lifecycle = component.getLifecycle();
    if (!lifecycle.needsPreviousRenderInfo()) {
      throw new RuntimeException(
          "Trying to record previous render info for component that doesn't support it");
    }

    final String key = component.getGlobalKey();

    // Sanity check like in StateHandler
    if (mSeenGlobalKeys.contains(key)) {
      // We found two components with the same global key.
      throw new RuntimeException(
          "Cannot record previous render info for " + component.getSimpleName() +
              ", found another Component with the same key: " + key);
    }
    mSeenGlobalKeys.add(key);

    final ComponentLifecycle.RenderInfo existingInfo = mRenderInfos.get(key);
    final ComponentLifecycle.RenderInfo newInfo = lifecycle.recordRenderInfo(
        component,
        existingInfo);

    mRenderInfos.put(key, newInfo);
  }

  private void applyPreviousRenderInfo(Component component) {
    final ComponentLifecycle lifecycle = component.getLifecycle();
    if (!lifecycle.needsPreviousRenderInfo()) {
      throw new RuntimeException(
          "Trying to apply previous render info to component that doesn't support it");
    }

    final String key = component.getGlobalKey();
    ComponentLifecycle.RenderInfo previousRenderInfo = mRenderInfos.get(key);
    lifecycle.applyPreviousRenderInfo(component, previousRenderInfo);
  }

  void reset() {
    mRenderInfos.clear();
    mSeenGlobalKeys.clear();
  }
}
