/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.Collections;
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
    return extractSubComponents(getMainThreadLayoutState().getDiffTree());
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
        diffNode,
        false /* canPrefetchDisplayLists */,
        false /* canCacheDrawingDisplayLists */,
        true /* clipChildren */);
  }

  private static List<Component> extractSubComponents(DiffNode root) {
    if (root == null) {
      return Collections.emptyList();
    }

    final List<Component> output = new ArrayList<>();

    if (root.getChildCount() == 0) {
      if (root.getComponent() != null && root.getComponent() instanceof TestComponent) {
        TestComponent testSubcomponent = (TestComponent) root.getComponent();
        output.add(testSubcomponent.getWrappedComponent());
      }

      return output;
    }

    for (DiffNode child : root.getChildren()) {
      output.addAll(extractSubComponents(child));
    }

    return output;
  }

  public static class Builder extends ComponentTree.Builder {

    private Builder(ComponentContext context, Component<?> root) {
      super(context, root);
    }

    @Override
    public Builder incrementalMount(boolean isEnabled) {
      return (Builder) super.incrementalMount(isEnabled);
    }

    @Override
    public Builder layoutDiffing(boolean enabled) {
      return (Builder) super.layoutDiffing(enabled);
    }

    @Override
    public Builder layoutThreadLooper(Looper looper) {
      return (Builder) super.layoutThreadLooper(looper);
    }

    @Override
    public Builder layoutLock(Object layoutLock) {
      return (Builder) super.layoutLock(layoutLock);
    }

    @Override
    public TestComponentTree build() {

      return new TestComponentTree(this);
    }
  }
}
