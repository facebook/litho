/*
 * Copyright 2018-present Facebook, Inc.
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

/** Helper class to access Litho internals for benchmark testing. */
public class BenchmarkTestHelper {
  private static final int[] LAYOUT_SIZE_OUT = new int[2];

  public static void calculateLayoutState(
      ComponentContext c, Component component, int widthSizeSpec, int heightSizeSpec) {
    LayoutState.calculate(
        c, component, -1, widthSizeSpec, heightSizeSpec, LayoutState.CalculateLayoutSource.TEST);
  }

  public static LithoView createAndMeasureLithoView(
      ComponentContext c, Component component, int widthSpec, int heightSpec) {
    final ComponentTree componentTree = ComponentTree.create(c, component).build();
    final LithoView lithoView = new LithoView(c);
    lithoView.setComponentTree(componentTree);
    lithoView.onAttachedToWindow();
    lithoView.measure(widthSpec, heightSpec);
    // This is really lithoView#layout without the mount
    componentTree.measure(widthSpec, heightSpec, LAYOUT_SIZE_OUT, false);
    return lithoView;
  }

  public static void mountLithoView(LithoView lithoView) {
    lithoView.getComponentTree().mountComponent(null, true);
  }

  public static void unmountLithoView(LithoView lithoView) {
    lithoView.unmountAllItems();
  }

  public static void prepareComponent(ComponentContext c, Component component) {
    component.onPrepare(c);
  }

  public static void bindComponent(ComponentContext c, Component component, Object content) {
    component.bind(c, content);
  }

  public static void unbindComponent(ComponentContext c, Component component, Object content) {
    component.unbind(c, content);
  }

  public static void ensureMountSpec(Component component) {
    if (!Component.isMountSpec(component)) {
      throw new IllegalStateException("Bind benchmark test is for MountSpec component only!");
    }
  }

  public static Object getMountContent(ComponentContext c, Component component) {
    return ComponentsPools.acquireMountContent(c.getAndroidContext(), component);
  }
}
