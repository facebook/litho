/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.view.View;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.EventHandler;
import com.facebook.litho.TestComponentTree;
import com.facebook.litho.TreeProps;

import org.powermock.reflect.Whitebox;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/**
 * Helper class to simplify testing of components.
 *
 * Allows simple and short creation of views that are created and mounted in a similar way to how
 * they are in real apps.
 */
public final class ComponentTestHelper {

  /**
   * Mount a component into a component view.
   *
   * @param component The component builder to mount
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(Component.Builder component) {
    return mountComponent(getContext(component), component.build());
  }

  /**
   * Mount a component into a component view.
   *
   * @param component The component builder to mount
   * @param incrementalMountEnabled States whether incremental mount is enabled
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(
      Component.Builder component,
      boolean incrementalMountEnabled) {
    ComponentContext context = getContext(component);
    return mountComponent(
        context,
        new ComponentView(context),
        component.build(),
        incrementalMountEnabled,
        100,
        100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param component The component to mount
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(ComponentContext context, Component component) {
    return mountComponent(context, component, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param component The component to mount
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentContext context,
      Component component,
      int width,
      int height) {
    return mountComponent(context, new ComponentView(context), component, width, height);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param componentView The view to mount the component into
   * @param component The component to mount
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentContext context,
      ComponentView componentView,
      Component component) {
    return mountComponent(context, componentView, component, 100, 100);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param componentView The view to mount the component into
   * @param component The component to mount
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentContext context,
      ComponentView componentView,
      Component component,
      int width,
      int height) {
    return mountComponent(
        context,
        componentView,
        component,
        false,
        width,
        height);
  }

  /**
   * Mount a component into a component view.
   *
   * @param context A components context
   * @param componentView The view to mount the component into
   * @param component The component to mount
   * @param incrementalMountEnabled States whether incremental mount is enabled
   * @param width The width of the resulting view
   * @param height The height of the resulting view
   * @return A ComponentView with the component mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentContext context,
      ComponentView componentView,
      Component component,
      boolean incrementalMountEnabled,
      int width,
      int height) {
    return mountComponent(
        componentView,
        ComponentTree.create(context, component)
            .incrementalMount(incrementalMountEnabled)
            .build(),
        makeMeasureSpec(width, EXACTLY),
        makeMeasureSpec(height, EXACTLY));
  }

  /**
   * Mount a component tree into a component view.
   *
   * @param componentView The view to mount the component tree into
   * @param componentTree The component tree to mount
   * @return A ComponentView with the component tree mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentView componentView,
      ComponentTree componentTree) {
    return mountComponent(
        componentView,
        componentTree,
        makeMeasureSpec(100, EXACTLY),
        makeMeasureSpec(100, EXACTLY));
  }

  /**
   * Mount a component tree into a component view.
   *
   * @param componentView The view to mount the component tree into
   * @param componentTree The component tree to mount
   * @param widthSpec The width spec used to measure the resulting view
   * @param heightSpec The height spec used to measure the resulting view
   * @return A ComponentView with the component tree mounted in it.
   */
  public static ComponentView mountComponent(
      ComponentView componentView,
      ComponentTree componentTree,
      int widthSpec,
      int heightSpec) {
    componentView.setComponent(componentTree);

    try {
      Whitebox.invokeMethod(componentView, "onAttach");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    componentView.measure(widthSpec, heightSpec);
    componentView.layout(
        0,
        0,
        componentView.getMeasuredWidth(),
        componentView.getMeasuredHeight());

    return componentView;
  }

  /**
   * Unmounts a component tree from a component view.
   * @param componentView the view to unmount
   */
  public static void unmountComponent(ComponentView componentView) {
    if (!componentView.isIncrementalMountEnabled()) {
      throw new IllegalArgumentException(
          "In order to test unmounting a Component, it needs to be mounted with " +
              "incremental mount enabled. Please use a mountComponent() variation that " +
              "accepts an incrementalMountEnabled argument");
    }

    // Unmounting the component by running incremental mount to a Rect that we certain won't
    // contain the component.
    Rect rect = new Rect(99999, 99999, 999999, 999999);
    componentView.performIncrementalMount(rect);
  }

  /**
   * Unbinds a component tree from a component view.
   *
   * @param componentView The view to unbind.
   */
  public static void unbindComponent(ComponentView componentView) {
    try {
      Whitebox.invokeMethod(componentView, "onDetach");
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
        context,
        component,
        makeMeasureSpec(1000, EXACTLY),
        makeMeasureSpec(0, UNSPECIFIED));
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
      Component.Builder component,
      int widthSpec,
      int heightSpec) {
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
      ComponentContext context,
      Component component,
      int widthSpec,
      int heightSpec) {
    final TestComponentTree componentTree =
        TestComponentTree.create(context, component)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    final ComponentView componentView = new ComponentView(context);
    componentView.setComponent(componentTree);

    componentView.measure(widthSpec, heightSpec);
    componentView.layout(0, 0, componentView.getMeasuredWidth(), componentView.getMeasuredHeight());

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
  public static <T extends ComponentLifecycle> Component<T> getSubComponent(
      Component.Builder component,
      Class<T> componentClass) {
    List<SubComponent> subComponents = getSubComponents(component);

    for (SubComponent subComponent : subComponents) {
      if (subComponent.getComponentType().equals(componentClass)) {
        return (Component<T>) subComponent.getComponent();
      }
    }

    return null;
  }

  /**
   * Measure and layout a component view.
   *
   * @param view The component view to measure and layout
