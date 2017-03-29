/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.List;

import android.os.Looper;
import android.support.annotation.Nullable;

/**
 * A {@link ComponentTree} for testing purposes. Leverages test classes to create component layouts
 * and exposes additional information useful for testing.
 */
public class TestComponentTree extends ComponentTree {

  public static Builder create(ComponentContext context, Component<?> root) {
    return new Builder(context, root);
  }

  private TestComponentTree(ComponentTree.Builder builder) {
    super(builder);
  }

  public List<Component> getSubComponents() {
    final List<Component> subComponents = new ArrayList<>();
    extractSubComponents(getMainThreadLayoutState().getDiffTree(), subComponents);

    return subComponents;
  }

  @Override
  protected LayoutState calculateLayoutState(
      @Nullable Object lock,
      ComponentContext context,
      Component<?> root,
      int widthSpec,
      int heightSpec,
      boolean diffingEnabled,
      @Nullable DiffNode diffNode) {
    return LayoutState.calculate(
        new TestComponentContext(
            ComponentContext.withComponentTree(new TestComponentContext(context), this),
            new StateHandler()),
        root,
        mId,
        widthSpec,
        heightSpec,
        diffingEnabled,
        diffNode);
  }

  private static void extractSubComponents(DiffNode root, List<Component> output) {
    if (root == null) {
      return;
    }

    if (root.getChildCount() == 0) {
      if (root.getComponent() != null && root.getComponent() instanceof TestComponent) {
        TestComponent testSubcomponent = (TestComponent) root.getComponent();
        output.add(testSubcomponent.getWrappedComponent());
      }
