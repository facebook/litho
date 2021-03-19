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
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnDetached;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A handler stores components that have implemented {@link OnAttached} or {@link OnDetached}
 * delegate methods. {@link OnAttached} method is called when a component is attached to the {@link
 * ComponentTree}, in contrast, {@link OnDetached} method is called when a component is detached
 * from the tree.
 */
@ThreadSafe
public class AttachDetachHandler {

  /** A container stores components whose {@link OnAttached} methods are already executed. */
  @Nullable private Map<String, Component> mAttached;

  private @Nullable LayoutStateContext mLayoutStateContext;

  /**
   * Execute {@link OnAttached} method for components in the set of given attachable minus {@link
   * #mAttached}; execute {@link OnDetached} method for components in the set of {@link #mAttached}
   * minus given attachable.
   *
   * @param attachable contains components that have implemented {@link OnAttached} or {@link
   *     OnDetached} delegate methods.
   */
  @UiThread
  void onAttached(
      LayoutStateContext layoutStateContext, @Nullable Map<String, Component> attachable) {
    assertMainThread();
    @Nullable final Map<String, Component> toAttach;
    @Nullable final Map<String, Component> toDetach;
    final LayoutStateContext previousLayoutStateContext;

    toAttach = composeAttach(attachable, mAttached);
    toDetach = composeDetach(attachable, mAttached);
    previousLayoutStateContext = mLayoutStateContext;
    if (attachable != null) {
      mAttached = new LinkedHashMap<>(attachable);
      mLayoutStateContext = layoutStateContext;
    } else {
      mAttached = null;
    }

    if (toDetach != null) {
      for (Map.Entry<String, Component> entry : toDetach.entrySet()) {
        final Component component = entry.getValue();
        final String key = entry.getKey();
        final ComponentContext scopedContext =
            component.getScopedContext(previousLayoutStateContext, key);

        try {
          component.onDetached(scopedContext);
        } catch (Exception e) {
          ComponentUtils.handle(scopedContext, e);
        }
      }
    }

    if (toAttach != null) {
      for (Map.Entry<String, Component> entry : toAttach.entrySet()) {
        final Component component = entry.getValue();
        final String key = entry.getKey();
        final ComponentContext scopedContext = component.getScopedContext(layoutStateContext, key);

        try {
          component.onAttached(scopedContext);
        } catch (Exception e) {
          ComponentUtils.handle(scopedContext, e);
        }
      }
    }
  }

  /**
   * Execute {@link OnDetached} callbacks for components stored in {@link #mAttached}, this method
   * should be called when releasing a {@link ComponentTree}.
   */
  @UiThread
  void onDetached() {
    assertMainThread();
    final List<Component> toDetach;
    final List<String> toDetachKeys;
    if (mAttached == null) {
      return;
    }

    toDetach = new ArrayList<>();
    toDetachKeys = new ArrayList<>();

    for (Map.Entry<String, Component> entry : mAttached.entrySet()) {
      toDetach.add(entry.getValue());
      toDetachKeys.add(entry.getKey());
    }

    mAttached.clear();

    for (int i = 0, size = toDetach.size(); i < size; i++) {
      final Component component = toDetach.get(i);
      final String globalKey = toDetachKeys.get(i);
      final ComponentContext scopedContext =
          component.getScopedContext(mLayoutStateContext, globalKey);

      try {
        component.onDetached(scopedContext);
      } catch (Exception e) {
        ComponentUtils.handle(scopedContext, e);
      }
    }
  }

  @Nullable
  private static Map<String, Component> composeAttach(
      @Nullable Map<String, Component> attachable, @Nullable Map<String, Component> attached) {
    Map<String, Component> toAttach = null;
    if (attachable != null) {
      toAttach = new LinkedHashMap<>(attachable);
      if (attached != null) {
        toAttach.keySet().removeAll(attached.keySet());
      }
    }
    return toAttach;
  }

  @Nullable
  private static Map<String, Component> composeDetach(
      @Nullable Map<String, Component> attachable, @Nullable Map<String, Component> attached) {
    Map<String, Component> toDetach = null;
    if (attached != null) {
      toDetach = new LinkedHashMap<>(attached);
      if (attachable != null) {
        toDetach.keySet().removeAll(attachable.keySet());
      }
    }
    return toDetach;
  }

  @VisibleForTesting
  @Nullable
  Map<String, Component> getAttached() {
    return mAttached;
  }
}
