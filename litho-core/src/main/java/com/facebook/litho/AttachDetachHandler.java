/*
 * Copyright 2019-present Facebook, Inc.
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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnDetached;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;

/**
 * A handler stores components that have implemented {@link OnAttached} or {@link OnDetached}
 * delegate methods. {@link OnAttached} method is called when a component is attached to the {@link
 * ComponentTree}, in contrast, {@link OnDetached} method is called when a component is detached
 * from the tree.
 */
@ThreadSafe
public class AttachDetachHandler {

  /** A container stores components whose {@link OnAttached} methods are already executed. */
  @GuardedBy("this")
  @Nullable
  private Map<String, Component> mAttached;

  /**
   * Execute {@link OnAttached} method for components in the set of given attachable minus {@link
   * #mAttached}; execute {@link OnDetached} method for components in the set of {@link #mAttached}
   * minus given attachable.
   *
   * @param attachable contains components that have implemented {@link OnAttached} or {@link
   *     OnDetached} delegate methods.
   */
  void onAttached(@Nullable Map<String, Component> attachable) {
    @Nullable final Map<String, Component> toAttach;
    @Nullable final Map<String, Component> toDetach;
    synchronized (this) {
      toAttach = composeAttach(attachable, mAttached);
      toDetach = composeDetach(attachable, mAttached);

      if (attachable != null) {
        mAttached = new HashMap<>(attachable);
      } else {
        mAttached = null;
      }
    }

    if (toDetach != null) {
      for (Component component : toDetach.values()) {
        component.onDetached(component.getScopedContext());
      }
    }

    if (toAttach != null) {
      for (Component component : toAttach.values()) {
        component.onAttached(component.getScopedContext());
      }
    }
  }

  /**
   * Execute {@link OnDetached} callbacks for components stored in {@link #mAttached}, this method
   * should be called when releasing a {@link ComponentTree}.
   */
  void onDetached() {
    final List<Component> toDetach;
    synchronized (this) {
      if (mAttached == null) {
        return;
      }
      toDetach = new ArrayList<>(mAttached.values());
      mAttached.clear();
    }

    for (int i = 0, size = toDetach.size(); i < size; i++) {
      final Component component = toDetach.get(i);
      component.onDetached(component.getScopedContext());
    }
  }

  @GuardedBy("this")
  @Nullable
  private static Map<String, Component> composeAttach(
      @Nullable Map<String, Component> attachable, @Nullable Map<String, Component> attached) {
    Map<String, Component> toAttach = null;
    if (attachable != null) {
      toAttach = new HashMap<>(attachable);
      if (attached != null) {
        toAttach.keySet().removeAll(attached.keySet());
      }
    }
    return toAttach;
  }

  @GuardedBy("this")
  @Nullable
  private static Map<String, Component> composeDetach(
      @Nullable Map<String, Component> attachable, @Nullable Map<String, Component> attached) {
    Map<String, Component> toDetach = null;
    if (attached != null) {
      toDetach = new HashMap<>(attached);
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
