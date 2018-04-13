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

package com.facebook.litho;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
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
   *       protected ComponentLayout onCreateLayout(ComponentContext c) {
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

  @DoNotStrip
  public static String viewToString(LithoView view) {
    return viewToString(view, false);
  }

  /**
   * Provide a nested string representation of a LithoView and its nested components for debugging
   * purposes.
   *
   * @param view A Litho view with mounted components.
   * @param embedded if the call is embedded in "adb dumpsys activity"
   */
  @DoNotStrip
  public static String viewToString(LithoView view, boolean embedded) {
    int left = 0;
    int top = 0;
    int depth = 0;
    if (embedded) {
      left = view.getLeft();
      top = view.getTop();
      depth = 2;
      ViewParent parent = view.getParent();
      while (parent != null) {
        depth++;
        parent = parent.getParent();
      }
    }
    final StringBuilder sb = new StringBuilder();
    viewToString(left, top, DebugComponent.getRootInstance(view), sb, embedded, depth);
    return sb.toString();
  }

  private static void viewToString(
      int left,
      int top,
      @Nullable DebugComponent debugComponent,
      StringBuilder sb,
      boolean embedded,
      int depth) {
    if (debugComponent == null) {
      return;
    }

    sb.append("litho.");
    sb.append(debugComponent.getComponent().getSimpleName());

    sb.append('{');
    sb.append(Integer.toHexString(debugComponent.hashCode()));
    sb.append(' ');

    final LithoView lithoView = debugComponent.getLithoView();
    final DebugLayoutNode layout = debugComponent.getLayoutNode();
    sb.append(lithoView != null && lithoView.getVisibility() == View.VISIBLE ? "V" : ".");
    sb.append(layout != null && layout.getFocusable() ? "F" : ".");
    sb.append(lithoView != null && lithoView.isEnabled() ? "E" : ".");
    sb.append(".");
    sb.append(lithoView != null && lithoView.isHorizontalScrollBarEnabled() ? "H" : ".");
    sb.append(lithoView != null && lithoView.isVerticalScrollBarEnabled() ? "V" : ".");
    sb.append(layout != null && layout.getClickHandler() != null ? "C" : ".");
    sb.append(". .. ");

    final Rect bounds = debugComponent.getBounds();
    sb.append(left + bounds.left);
    sb.append(",");
    sb.append(top + bounds.top);
    sb.append("-");
    sb.append(left + bounds.right);
    sb.append(",");
    sb.append(top + bounds.bottom);

    final String testKey = debugComponent.getTestKey();
    if (testKey != null && !TextUtils.isEmpty(testKey)) {
      sb.append(String.format(" litho:id/%s", testKey.replace(' ', '_')));
    }

    final String textContent = debugComponent.getTextContent();
    if (textContent != null && !TextUtils.isEmpty(textContent)) {
      sb.append(String.format(" text=\"%s\"", textContent.replace("\n", "").replace("\"", "")));
    }

    if (!embedded && layout != null && layout.getClickHandler() != null) {
      sb.append(" [clickable]");
    }

    sb.append('}');

    for (DebugComponent child : debugComponent.getChildComponents()) {
      sb.append("\n");
      for (int i = 0; i <= depth; i++) {
        sb.append("  ");
      }
      viewToString(0, 0, child, sb, embedded, depth + 1);
    }
  }

  /**
   * Obtain a reference to a LithoView's internal layout root, if present. This is used to restore a
   * view's root after it has been freed for testing purposes.
   *
   * @see #setRootLayoutRef(LithoView, InternalNodeRef)
   */
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
