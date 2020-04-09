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

package com.facebook.litho.widget;

import android.content.Context;
import android.widget.FrameLayout;
import androidx.annotation.UiThread;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link MountSpec} implementation to provide width and height information to the wrapped
 * component.
 *
 * <p>Usage: Create a {@link SizeSpecMountWrapperComponentSpec} with a {@link Component} added to
 * it, and it will provide the width and height information through a {@link Size} typed {@link
 * com.facebook.litho.annotations.TreeProp}.
 */
@MountSpec(hasChildLithoViews = true)
public class SizeSpecMountWrapperComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, StateValue<AtomicReference<ComponentTree>> componentTreeRef) {
    componentTreeRef.set(new AtomicReference<ComponentTree>());
    // This is the component tree to be added to the LithoView.
    getOrCreateComponentTree(c, componentTreeRef.get());
  }

  @OnCreateMountContent
  static FrameLayout onCreateMountContent(Context c) {
    // This LithoView will contain the new tree that's created from this point onwards
    // TODO: T59446191 Replace with proper solution. Remove the use of FrameLayout.
    FrameLayout wrapperView = new FrameLayout(c);
    wrapperView.addView(new LithoView(c));
    return wrapperView;
  }

  @UiThread
  @OnMount
  static void onMount(
      ComponentContext c,
      FrameLayout wrapperView,
      @State AtomicReference<ComponentTree> componentTreeRef) {
    ((LithoView) wrapperView.getChildAt(0)).setComponentTree(componentTreeRef.get());
  }

  @UiThread
  @OnUnmount
  static void onUnmount(ComponentContext c, FrameLayout wrapperView) {
    ((LithoView) wrapperView.getChildAt(0)).setComponentTree(null);
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop Component component,
      @State AtomicReference<ComponentTree> componentTreeRef) {
    final ComponentTree componentTree = getOrCreateComponentTree(c, componentTreeRef);
    componentTree.setVersionedRootAndSizeSpec(
        component,
        widthSpec,
        heightSpec,
        size,
        getTreePropWithSize(c, widthSpec, heightSpec),
        c.getLayoutVersion());
    if (size.width < 0 || size.height < 0) {
      // if this happens it means that the componentTree was probably released in the UI Thread so
      // this measurement is not needed.
      size.width = size.height = 0;
    }
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop Component component,
      @State AtomicReference<ComponentTree> componentTreeRef) {
    // the updated width and height is passed down.
    int widthSpec = SizeSpec.makeSizeSpec(layout.getWidth(), SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(layout.getHeight(), SizeSpec.EXACTLY);
    final ComponentTree componentTree = getOrCreateComponentTree(c, componentTreeRef);
    // This check is also done in the setRootAndSizeSpec method, but we need to do this here since
    // it will fail if a ErrorBoundariesConfiguration.rootWrapperComponentFactory was set.
    // TODO: T60426216
    if (!componentTree.hasCompatibleLayout(widthSpec, heightSpec)) {
      componentTree.setVersionedRootAndSizeSpec(
          component,
          widthSpec,
          heightSpec,
          null,
          getTreePropWithSize(c, widthSpec, heightSpec),
          c.getLayoutVersion());
    }
  }

  @OnDetached
  static void onDetached(
      ComponentContext c, @State AtomicReference<ComponentTree> componentTreeRef) {
    if (componentTreeRef.get() != null) {
      // We need to release the component tree here to allow for a proper memory deallocation
      componentTreeRef.get().release();
      componentTreeRef.set(null);
    }
  }

  @UiThread
  @OnBind
  static void onBind(ComponentContext c, FrameLayout wrapperView) {
    ((LithoView) wrapperView.getChildAt(0)).rebind();
  }

  @UiThread
  @OnUnbind
  static void onUnbind(ComponentContext c, FrameLayout wrapperView) {
    ((LithoView) wrapperView.getChildAt(0)).unbind();
  }

  /**
   * Creates a TreeProp with the size information. We need to do this every time we call
   * setRootAndSizeSpec on the new tree.
   *
   * @param c
   * @param widthSpec
   * @param heightSpec
   * @return
   */
  private static TreeProps getTreePropWithSize(ComponentContext c, int widthSpec, int heightSpec) {
    TreeProps tp = c.getTreePropsCopy();
    if (tp == null) {
      tp = new TreeProps();
    }
    tp.put(Size.class, new Size(widthSpec, heightSpec));
    return tp;
  }

  /**
   * We create get a componentTree, we have to create it in case it's been released.
   *
   * @param c
   * @param componentTreeRef
   * @return
   */
  private static ComponentTree getOrCreateComponentTree(
      ComponentContext c, AtomicReference<ComponentTree> componentTreeRef) {
    ComponentTree componentTree = componentTreeRef.get();
    if (componentTree == null || componentTree.isReleased()) {
      componentTree = ComponentTree.create(c).build();
      componentTreeRef.set(componentTree);
    }
    return componentTree;
  }
}
