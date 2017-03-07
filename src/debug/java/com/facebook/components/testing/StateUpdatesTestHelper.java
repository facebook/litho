// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;

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
        new ShadowLooper[]{layoutThreadShadowLooper});
  }

  /**
   *  Call a state update as specified in {@link StateUpdater#performStateUpdate(ComponentContext)}
   *   on the component and return the updated view.
   * @param context context
   * @param component the component to update
   * @param stateUpdater implementation of {@link StateUpdater} that triggers the state update
   * @param loopers shadow loopers to post messages to the main thread, run in the same order they
   * are specified
   * @return the updated ComponentView after the state update was applied
   */
  public static ComponentView getViewAfterStateUpdate(
      ComponentContext context,
      Component component,
      StateUpdater stateUpdater,
      ShadowLooper[] loopers) throws Exception {
    // This is for working around component immutability, to be able to retrieve the updated
    // instance of the component.
    Whitebox.invokeMethod(component, "setKey", "bogusKeyForTest");
    final ComponentTree componentTree = ComponentTree.create(context, component).build();

    final ComponentView componentView = new ComponentView(context);
    ComponentTestHelper.mountComponent(componentView, componentTree);

    Whitebox.setInternalState(component, "mGlobalKey", "bogusKeyForTest");
    Whitebox.setInternalState(component, "mId", 457282882);

    Whitebox.setInternalState(context, "mComponentScope", component);
    Whitebox.setInternalState(context, "mComponentTree", componentTree);

    stateUpdater.performStateUpdate(context);
    for (ShadowLooper looper : loopers) {
      looper.runToEndOfTasks();
    }

    ComponentTestHelper.mountComponent(componentView, componentTree);
    return componentView;
  }
}
