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

import static com.facebook.litho.ThreadUtils.assertMainThread;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnDetached;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages dispatching attach/detach events to a set of {@link Attachable}.
 *
 * <p>For Spec Components, this will invoke {@link OnAttached} when a component is attached to the
 * {@link ComponentTree} and {@link OnDetached} when a component is detached from the tree.
 *
 * <p>For Kotlin components, this will handle dispatching callbacks registered with the {@code
 * useEffect} hook.
 */
@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AttachDetachHandler {

  @Nullable private Map<String, Attachable> mAttached;

  private @Nullable LayoutStateContext mLayoutStateContext;

  /**
   * Marks the given Attachables as attached, invoking attach if they weren't already attached. Any
   * Attachables that were attached and are no longer attached will be detached. Note that identity
   * is determined by {@link Attachable#getUniqueId()}.
   */
  @UiThread
  void onAttached(LayoutStateContext layoutStateContext, @Nullable List<Attachable> attachables) {
    assertMainThread();
    if (mAttached == null && attachables == null) {
      return;
    }

    final LayoutStateContext previousLayoutStateContext = mLayoutStateContext;
    mLayoutStateContext = layoutStateContext;

    if (attachables == null) {
      detachAll(
          Preconditions.checkNotNull(previousLayoutStateContext),
          Preconditions.checkNotNull(mAttached));
      mAttached = null;
      return;
    }

    LinkedHashMap<String, Attachable> attachableMap = new LinkedHashMap<>(attachables.size());
    for (Attachable attachable : attachables) {
      attachableMap.put(attachable.getUniqueId(), attachable);
    }

    if (mAttached == null) {
      attachAll(attachableMap);
      mAttached = attachableMap;
      return;
    }

    final LayoutStateContext safeLayoutStateContext =
        Preconditions.checkNotNull(previousLayoutStateContext);
    for (Map.Entry<String, Attachable> attachedEntry : mAttached.entrySet()) {
      if (!attachableMap.containsKey(attachedEntry.getKey())) {
        attachedEntry.getValue().detach(safeLayoutStateContext);
      }
    }

    for (Map.Entry<String, Attachable> attachableEntry : attachableMap.entrySet()) {
      final Attachable existing = mAttached.get(attachableEntry.getKey());
      if (existing == null) {
        attachableEntry.getValue().attach(mLayoutStateContext);
      } else if (existing.shouldUpdate(attachableEntry.getValue())) {
        existing.detach(safeLayoutStateContext);
        attachableEntry.getValue().attach(mLayoutStateContext);
      } else if (!existing.useLegacyUpdateBehavior()) {
        // If the attachable already exists and it doesn't need to update, make sure to use the
        // existing one.
        attachableEntry.setValue(existing);
      }
    }

    mAttached = attachableMap;
  }

  /** Detaches all Attachables currently attached. */
  @UiThread
  void onDetached() {
    assertMainThread();
    if (mAttached == null) {
      return;
    }
    detachAll(Preconditions.checkNotNull(mLayoutStateContext), mAttached);
    mAttached = null;
  }

  private void attachAll(Map<String, Attachable> toAttach) {
    final LayoutStateContext layoutStateContext = Preconditions.checkNotNull(mLayoutStateContext);
    for (Attachable entry : toAttach.values()) {
      entry.attach(layoutStateContext);
    }
  }

  private void detachAll(LayoutStateContext layoutStateContext, Map<String, Attachable> toDetach) {
    for (Attachable entry : toDetach.values()) {
      entry.detach(layoutStateContext);
    }
  }

  @VisibleForTesting
  @Nullable
  Map<String, Attachable> getAttached() {
    return mAttached;
  }
}
