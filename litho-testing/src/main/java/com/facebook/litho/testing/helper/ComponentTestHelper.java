/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.testing.helper;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.LithoView;
import com.facebook.litho.TestComponentTree;
import com.facebook.litho.TreeProps;
import com.facebook.litho.UnfocusedVisibleEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.subcomponents.SubComponent;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.shadows.ShadowLooper;

/**
 * Helper class to simplify testing of components.
 *
 * <p>Allows simple and short creation of views that are created and mounted in a similar way to how
 * they are in real apps.
 */
public final class ComponentTestHelper {

  private ComponentTestHelper() {}

  /**
   * Mount a component into a component view.
   *
   * @param component The component builder to mount
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(Component.Builder component) {
    return mountComponent(getContext(component), component.build());
  }

  /**
   * Mount a component into a component view.
   *
   * @param component The component builder to mount
   * @param incrementalMountEnabled States whether incremental mount is enabled
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      Component.Builder component, boolean incrementalMountEnabled) {
    ComponentContext context = getContext(component);
    return mountComponent(
        context, new LithoView(context), component.build(), incrementalMountEnabled, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param component The component to mount
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(ComponentContext context, Component component) {
    return mountComponent(context, component, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param component The component to mount
   * @param incrementalMountEnabled States whether incremental mount is enabled
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      ComponentContext context, Component component, boolean incrementalMountEnabled) {
    return mountComponent(
        context, new LithoView(context), component, incrementalMountEnabled, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param component The component to mount
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      ComponentContext context, Component component, int width, int height) {
    return mountComponent(context, new LithoView(context), component, width, height);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param lithoView The view to mount the component into
   * @param component The component to mount
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      ComponentContext context, LithoView lithoView, Component component) {
    return mountComponent(context, lithoView, component, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param lithoView The view to mount the component into
   * @param component The component to mount
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      ComponentContext context, LithoView lithoView, Component component, int width, int height) {
    return mountComponent(context, lithoView, component, false, width, height);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param lithoView The view to mount the component into
   * @param component The component to mount
   * @param incrementalMountEnabled States whether incremental mount is enabled
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView mountComponent(
      ComponentContext context,
      LithoView lithoView,
      Component component,
      boolean incrementalMountEnabled,
      int width,
      int height) {
    return mountComponent(
        lithoView,
        ComponentTree.create(context, component)
            .incrementalMount(incrementalMountEnabled)
            .layoutDiffing(false)
            .build(),
        makeMeasureSpec(width, EXACTLY),
        makeMeasureSpec(height, EXACTLY));
  }

  /**
   * Mount a component tree into a component view.
   *
   * @param lithoView The view to mount the component tree into
   * @param componentTree The component tree to mount
   * @return A LithoView with the component tree mounted in it.
   */
  public static LithoView mountComponent(LithoView lithoView, ComponentTree componentTree) {
    return mountComponent(
        lithoView, componentTree, makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));
  }

  /**
   * Mount a component tree into a component view.
   *
   * @param lithoView The view to mount the component tree into
   * @param componentTree The component tree to mount
   * @param widthSpec The width spec used to measure the resulting view
   * @param heightSpec The height spec used to measure the resulting view
   * @return A LithoView with the component tree mounted in it.
   */
  public static LithoView mountComponent(
      LithoView lithoView, ComponentTree componentTree, int widthSpec, int heightSpec) {
    final boolean addParent = lithoView.getParent() == null;
    final ViewGroup parent =
        new ViewGroup(lithoView.getContext()) {
          @Override
          protected void onLayout(boolean changed, int l, int t, int r, int b) {}
        };

    if (addParent) {
      parent.addView(lithoView);
    }

    lithoView.setComponentTree(componentTree);

    try {
      Whitebox.invokeMethod(lithoView, "onAttach");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    lithoView.measure(widthSpec, heightSpec);

    if (addParent) {
      parent.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    }

    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    return lithoView;
  }

  /**
   * Unmounts a component tree from a component view.
   *
   * @param lithoView the view to unmount
   */
  public static void unmountComponent(LithoView lithoView) {
    if (!lithoView.isIncrementalMountEnabled()) {
      throw new IllegalArgumentException(
          "In order to test unmounting a Component, it needs to be mounted with "
              + "incremental mount enabled. Please use a mountComponent() variation that "
              + "accepts an incrementalMountEnabled argument");
    }

    // Unmounting the component by running incremental mount to a Rect that we certain won't
    // contain the component.
    Rect rect = new Rect(99999, 99999, 999999, 999999);
    lithoView.performIncrementalMount(rect, true);
  }

  /**
   * Unbinds a component tree from a component view.
   *
   * @param lithoView The view to unbind.
   */
  public static void unbindComponent(LithoView lithoView) {
    try {
      Whitebox.invokeMethod(lithoView, "onDetach");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the subcomponents of a component
   *
   * @param component The component builder which to get the subcomponents of
   * @return The subcomponents of the given component
   */
  public static List<SubComponent> getSubComponents(Component.Builder component) {
    return getSubComponents(getContext(component), component.build());
  }

  /**
   * Get the subcomponents of a component
   *
   * @param context A components context
   * @param component The component which to get the subcomponents of
   * @return The subcomponents of the given component
   */
  public static List<SubComponent> getSubComponents(ComponentContext context, Component component) {
    return getSubComponents(
        context, component, makeMeasureSpec(1000, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
  }

  /**
   * Get the subcomponents of a component
   *
   * @param component The component which to get the subcomponents of
   * @param widthSpec The width to measure the component with
   * @param heightSpec The height to measure the component with
   * @return The subcomponents of the given component
   */
  public static List<SubComponent> getSubComponents(
      Component.Builder component, int widthSpec, int heightSpec) {
    return getSubComponents(getContext(component), component.build(), widthSpec, heightSpec);
  }

  /**
   * Get the subcomponents of a component
   *
   * @param context A components context
   * @param component The component which to get the subcomponents of
   * @param widthSpec The width to measure the component with
   * @param heightSpec The height to measure the component with
   * @return The subcomponents of the given component
   */
  public static List<SubComponent> getSubComponents(
      ComponentContext context, Component component, int widthSpec, int heightSpec) {
    final TestComponentTree componentTree =
        TestComponentTree.create(context, component).incrementalMount(false).build();

    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(componentTree);

    lithoView.measure(widthSpec, heightSpec);
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    final List<Component> components = componentTree.getSubComponents();
    final List<SubComponent> subComponents = new ArrayList<>(components.size());
    for (Component lifecycle : components) {
      subComponents.add(SubComponent.of(lifecycle));
    }

    return subComponents;
  }

  /**
   * Returns the first subComponent of type class.
   *
   * @param component The component builder which to get the subcomponent from
   * @param componentClass the class type of the requested sub component
   * @return The first instance of subComponent of type Class or null if none is present.
   */
  public static <T extends Component> Component getSubComponent(
      Component.Builder component, Class<T> componentClass) {
    List<SubComponent> subComponents = getSubComponents(component);

    for (SubComponent subComponent : subComponents) {
      if (subComponent.getComponentType().equals(componentClass)) {
        return subComponent.getComponent();
      }
    }

    return null;
  }

  /**
   * Measure and layout a component view.
   *
   * @param view The component view to measure and layout
   */
  public static void measureAndLayout(View view) {
    view.measure(makeMeasureSpec(1000, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
  }

  private static ComponentContext getContext(Component.Builder builder) {
    return Whitebox.getInternalState(builder, "mContext");
  }

  /**
   * Mounts the component & triggers the visibility event. Requires that the component supports
   * incremental mounting.
   *
   * <p>{@link com.facebook.litho.VisibleEvent}
   *
   * @param context A components context
   * @param onVisibleHandler SpecificComponent.onVisible(component)
   * @param component The component builder which to get the subcomponent from
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView dispatchOnVisibleEvent(
      ComponentContext context, EventHandler onVisibleHandler, Component component) {
    return dispatchVisibilityEvent(context, onVisibleHandler, new VisibleEvent(), component);
  }

  /**
   * Mounts the component & triggers the focused visibility event. Requires that the component
   * supports incremental mounting.
   *
   * <p>{@link com.facebook.litho.FocusedVisibleEvent}
   *
   * @param context A components context
   * @param onFocusedVisibleHandler SpecificComponent.onFocusedVisible(component)
   * @param component The component builder which to get the subcomponent from
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView dispatchOnFocusedVisibleEvent(
      ComponentContext context, EventHandler onFocusedVisibleHandler, Component component) {
    return dispatchVisibilityEvent(
        context, onFocusedVisibleHandler, new FocusedVisibleEvent(), component);
  }

  /**
   * Mounts the component & triggers the invisible event. Requires that the component supports
   * incremental mounting.
   *
   * <p>{@link com.facebook.litho.InvisibleEvent}
   *
   * @param context A components context
   * @param onInvisibleHandler SpecificComponent.onInvisible(component)
   * @param component The component builder which to get the subcomponent from
   * @return A LithoView with the component mounted in it.
   */
  public static LithoView dispatchOnInvisibleEvent(
      ComponentContext context, EventHandler onInvisibleHandler, Component component) {
    return dispatchVisibilityEvent(context, onInvisibleHandler, new InvisibleEvent(), component);
  }

  private static LithoView dispatchVisibilityEvent(
      ComponentContext context,
      EventHandler eventHandler,
      Object eventInstance,
      Component component) {
    LithoView lithoView = new LithoView(context);
    FrameLayout parent = new FrameLayout(context.getAndroidContext());

    parent.addView(lithoView);

    mountComponent(context, lithoView, component, true, 100, 100);

    lithoView.performIncrementalMount();

    eventHandler.mHasEventDispatcher = component;

    try {
      Whitebox.invokeMethod(
          component.getEventDispatcher(), "dispatchOnEvent", eventHandler, eventInstance);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return lithoView;
  }

  /**
   * Triggers a Litho visibility event
   *
   * <p>The event needs to be one of {@link VisibleEvent}, {@link InvisibleEvent}, {@link
   * FocusedVisibleEvent}, {@link UnfocusedVisibleEvent}, or {@link FullImpressionVisibleEvent}.
   *
   * @param lithoView the view to invoke the event on its component tree
   * @param visibilityClass the type of the event to invoke
   * @return true if a handler for this event type was found
   */
  public static boolean triggerVisibilityEvent(LithoView lithoView, Class<?> visibilityClass) {
    return VisibilityEventsHelper.triggerVisibilityEvent(
        lithoView.getComponentTree(), visibilityClass);
  }

  /**
   * Sets a TreeProp that will be visible to all Components which are created from the given Context
   * (unless a child overwrites its).
   */
  public static void setTreeProp(ComponentContext context, Class propClass, Object prop) {
    TreeProps treeProps;
    try {
      treeProps = Whitebox.invokeMethod(context, "getTreeProps");
      if (treeProps == null) {
        treeProps = new TreeProps();
        Whitebox.invokeMethod(context, "setTreeProps", treeProps);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    treeProps.put(propClass, prop);
  }

  /** Access the default layout thread looper for testing purposes only. */
  public static Looper getDefaultLayoutThreadLooper() {
    return (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper");
  }

  /** Access the shadow of the default layout thread looper for testing purposes only. */
  public static ShadowLooper getDefaultLayoutThreadShadowLooper() throws Exception {
    return shadowOf(getDefaultLayoutThreadLooper());
  }
}
