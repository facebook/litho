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

package com.facebook.litho.testing.state;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import org.robolectric.shadows.ShadowLooper;

/** Helper for writing state update unit tests. */
public final class StateUpdatesTestHelper {

  private StateUpdatesTestHelper() {}

  @FunctionalInterface
  public interface StateUpdater {
    void performStateUpdate(ComponentContext context);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   * on the component and return the updated view.
   *
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param layoutThreadShadowLooper shadow looper to post messages to the main thread
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper layoutThreadShadowLooper)
      throws Exception {
    return getViewAfterStateUpdate(
        context, component, stateUpdater, layoutThreadShadowLooper, false, false);
  }

  /**
   * Returns a LithoView after all outstanding asynchronous state updates are performed.
   *
   * @param context context
   * @param component the component to update
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(ComponentContext context, Component component)
      throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        new StateUpdater() {
          @Override
          public void performStateUpdate(ComponentContext ignored) {}
        },
        ComponentTestHelper.getDefaultLayoutThreadShadowLooper(),
        false,
        false);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   * on the component and return the updated view.
   *
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(
      ComponentContext context, Component component, StateUpdater stateUpdater) throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        stateUpdater,
        ComponentTestHelper.getDefaultLayoutThreadShadowLooper(),
        false,
        false);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   * on the component and return the updated view with the option to incrementally mount.
   *
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param incrementalMountEnabled whether or not to enable incremental mount for the component
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      boolean incrementalMountEnabled,
      boolean visibilityProcessingEnabled)
      throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        stateUpdater,
        new ShadowLooper[] {ComponentTestHelper.getDefaultLayoutThreadShadowLooper()},
        incrementalMountEnabled,
        visibilityProcessingEnabled);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   * on the component and return the updated view with the option to incrementally mount.
   *
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param layoutThreadShadowLooper shadow looper to post messages to the main thread
   * @param incrementalMountEnabled whether or not to enable incremental mount for the component
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper layoutThreadShadowLooper,
      boolean incrementalMountEnabled,
      boolean visibilityProcessingEnabled)
      throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        stateUpdater,
        new ShadowLooper[] {layoutThreadShadowLooper},
        incrementalMountEnabled,
        visibilityProcessingEnabled);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   * on the component and return the updated view with the option to incrementally mount.
   *
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param loopers shadow loopers to post messages to the main thread, run in the same order they
   *     are specified
   * @param incrementalMountEnabled whether or not to enable incremental mount for the component
   * @return the updated LithoView after the state update was applied
   */
  public static LithoView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper[] loopers,
      boolean incrementalMountEnabled,
      boolean visibilityProcessingEnabled)
      throws Exception {
    // This is for working around component immutability, to be able to retrieve the updated
    // instance of the component.
    Whitebox.invokeMethod(component, "setKey", "bogusKeyForTest");
    final ComponentTree componentTree =
        ComponentTree.create(context, component)
            .incrementalMount(incrementalMountEnabled)
            .visibilityProcessing(visibilityProcessingEnabled)
            .layoutDiffing(false)
            .build();

    final LithoView lithoView = new LithoView(context);
    ComponentTestHelper.mountComponent(lithoView, componentTree);

    Whitebox.setInternalState(component, "mGlobalKey", "bogusKeyForTest");
    Whitebox.setInternalState(component, "mId", 457282882);

    Whitebox.setInternalState(context, "mComponentScope", component);
    Whitebox.setInternalState(context, "mComponentTree", componentTree);

    final LithoViewTestHelper.InternalNodeRef rootLayoutNode =
        LithoViewTestHelper.getRootLayoutRef(lithoView);

    stateUpdater.performStateUpdate(context);
    for (ShadowLooper looper : loopers) {
      looper.runToEndOfTasks();
    }

    LithoViewTestHelper.setRootLayoutRef(lithoView, rootLayoutNode);
    return ComponentTestHelper.mountComponent(lithoView, componentTree);
  }
}
