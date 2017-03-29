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

