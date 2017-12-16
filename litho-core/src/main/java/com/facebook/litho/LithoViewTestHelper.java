/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.Deque;

/**
 * Helper class to access metadata from {@link LithoView} that is relevant during end to end
 * tests. In order for the data to be collected, {@link
 * ComponentsConfiguration#isEndToEndTestRun} must be enabled.
 */
@DoNotStrip
public class LithoViewTestHelper {

  /**
   * Holds an opaque reference to an {@link InternalNode} without giving the holder any access to
   * it.
   */
  public static final class InternalNodeRef {
    private final InternalNode mInternalNodeRef;

    private InternalNodeRef(InternalNode node) {
      this.mInternalNodeRef = node;
    }
  }

  /**
   * @see #findTestItems(LithoView, String)
   *     <p><strong>Note:</strong> If there is more than one element mounted under the given key,
   *     the last one to render will be returned.
   * @param lithoView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Test item if found, null otherwise.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   */
  @DoNotStrip
  @Nullable
  public static TestItem findTestItem(LithoView lithoView, String testKey) {
    final Deque<TestItem> items = lithoView.findTestItems(testKey);

    return items.isEmpty() ? null : items.getLast();
  }

  /**
   * Finds a {@link TestItem} given a {@link LithoView} based on the test key it was assigned during
   * construction.
   *
   * <p><strong>Example use:</strong>
   *
   * <pre>{@code
   * final LithoView lithoView = ComponentTestHelper.mountComponent(
   *     mContext,
   *     new InlineLayoutSpec() {
   *       @Override
   *       protected ActualComponentLayout onCreateLayout(ComponentContext c) {
   *         return Column.create(c)
   *             .child(
   *                 Column.create(c)
   *                     .child(TestDrawableComponent.create(c))
   *                     .child(TestDrawableComponent.create(c))
   *                     .testKey("mytestkey"))
   *             .build();
   *       }
   *     });
   * final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, "mytestkey");
   *
   * }</pre>
   *
   * @param lithoView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Queue of mounted items in order by mount time.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   */
  @DoNotStrip
  @NonNull
  public static Deque<TestItem> findTestItems(LithoView lithoView, String testKey) {
    return lithoView.findTestItems(testKey);
  }

  /**
   * Provide a nested string representation of a LithoView and its nested
   * components for debugging purposes.
   *
   * @param view A Litho view with mounted components.
   */
  @DoNotStrip
  public static String viewToString(LithoView view) {
    return viewToString(DebugComponent.getRootInstance(view), 0);
  }

  private static String viewToString(@Nullable DebugComponent debugComponent, int depth) {
    if (debugComponent == null) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();

    if (depth > 0) {
      sb.append('\n');
    }

    for (int i = 0; i < depth; i++) {
      sb.append("  ");
    }

    sb.append(debugComponent.getComponent().getSimpleName());

    final Rect bounds = debugComponent.getBounds();
    sb.append('{');

    sb.append(bounds.left);
    sb.append(", ");
    sb.append(bounds.top);
    sb.append(" - ");
    sb.append(bounds.right);
    sb.append(", ");
    sb.append(bounds.bottom);

    final String testKey = debugComponent.getTestKey();
    if (!TextUtils.isEmpty(testKey)) {
      sb.append(String.format(" testKey=\"%s\"", testKey));
    }

    final String textContent = debugComponent.getTextContent();
    if (!TextUtils.isEmpty(textContent)) {
      sb.append(String.format(" text=\"%s\"", textContent));
    }

    final DebugLayoutNode layout = debugComponent.getLayoutNode();
    if (layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');

    for (DebugComponent child : debugComponent.getChildComponents()) {
      sb.append(viewToString(child, depth + 1));
    }

    return sb.toString();
  }

  /**
   * Obtain a reference to a LithoView's internal layout root, if present. This is used to restore a
   * view's root after it has been freed for testing purposes.
   *
   * @see #setRootLayoutRef(LithoView, InternalNodeRef)
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  public static InternalNodeRef getRootLayoutRef(final LithoView view) {
    final ComponentTree componentTree = view.getComponentTree();
    final LayoutState mainThreadLayoutState =
        componentTree != null ? componentTree.getMainThreadLayoutState() : null;
    return mainThreadLayoutState != null
        ? new InternalNodeRef(mainThreadLayoutState.getLayoutRoot())
        : null;
  }

  /**
   * Restore a previously saved root layout reference.
   *
   * @see #getRootLayoutRef(LithoView)
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  @ThreadConfined(ThreadConfined.UI)
  public static void setRootLayoutRef(final LithoView view, final InternalNodeRef rootLayoutNode) {
    final ComponentTree componentTree = view.getComponentTree();
    final LayoutState mainThreadLayoutState =
        componentTree != null ? componentTree.getMainThreadLayoutState() : null;
    if (mainThreadLayoutState != null) {
      mainThreadLayoutState.mLayoutRoot = rootLayoutNode.mInternalNodeRef;
    }
  }
}
