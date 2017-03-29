/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;

import org.powermock.reflect.Whitebox;
import org.robolectric.shadows.ShadowLooper;

/**
 * Helper for writing state update unit tests.
 */
public class StateUpdatesTestHelper {

  public interface StateUpdater {
    void performStateUpdate(ComponentContext context);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   *   on the component and return the updated view.
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param layoutThreadShadowLooper shadow looper to post messages to the main thread
   * @return the updated ComponentView after the state update was applied
   */
  public static ComponentView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper layoutThreadShadowLooper) throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        stateUpdater,
        layoutThreadShadowLooper,
        false);
  }

  /**
   * Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   *   on the component and return the updated view with the option to incrementally mount.
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param layoutThreadShadowLooper shadow looper to post messages to the main thread
   * @param incrementalMountEnabled whether or not to enable incremental mount for the component
   * @return the updated ComponentView after the state update was applied
   */
  public static ComponentView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper layoutThreadShadowLooper,
      boolean incrementalMountEnabled) throws Exception {
    return getViewAfterStateUpdate(
        context,
        component,
        stateUpdater,
        new ShadowLooper[]{layoutThreadShadowLooper},
        incrementalMountEnabled);
  }

  /**
   *  Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   *   on the component and return the updated view with the option to incrementally mount.
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param loopers shadow loopers to post messages to the main thread, run in the same order they
   * are specified
   * @param incrementalMountEnabled whether or not to enable incremental mount for the component
   * @return the updated ComponentView after the state update was applied
   */
  public static ComponentView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper[] loopers,
      boolean incrementalMountEnabled) throws Exception {
    // This is for working around component immutability, to be able to retrieve the updated
    // instance of the component.
    Whitebox.invokeMethod(component, "setKey", "bogusKeyForTest");
    final ComponentTree componentTree = ComponentTree.create(context, component)
        .incrementalMount(incrementalMountEnabled)
        .build();

