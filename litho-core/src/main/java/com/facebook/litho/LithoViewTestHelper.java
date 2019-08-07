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

import android.text.TextUtils;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.Deque;

/**
 * Helper class to access metadata from {@link LithoView} that is relevant during end to end tests.
 * In order for the data to be collected, {@link ComponentsConfiguration#isEndToEndTestRun} must be
 * enabled.
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
    DebugComponent root = DebugComponent.getRootInstance(view);
    if (root == null) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();

    int left = view.getLeft();
    int top = view.getTop();
    if (view.getParent() instanceof NestedScrollView) {
      // TODO(T37986749): understand why we need it and not for RecyclerView
      NestedScrollView scrollingParentView = (NestedScrollView) view.getParent();
      left -= scrollingParentView.computeHorizontalScrollOffset();
      top -= scrollingParentView.computeVerticalScrollOffset();
    }
    DebugComponentDescriptionHelper.addViewDescription(left, top, root, sb, embedded);

    int depth = embedded ? getLithoViewDepthInAndroid(view) : 0;
    viewToString(root, sb, embedded, depth);
    return sb.toString();
  }

  /** For E2E tests we remove non-layout components because they break view-hierarchy parsing. */
  private static void viewToString(
      DebugComponent component, StringBuilder sb, boolean embedded, int depth) {
    for (DebugComponent child : component.getChildComponents()) {
      int nextDepth = depth;
      // TODO(T37986749): add unit test for this scenario (need to create non-layout somehow)
      if (!ComponentsConfiguration.isEndToEndTestRun || child.isLayoutNode()) {
        writeNewLineWithIndentByDepth(sb, nextDepth);
        DebugComponentDescriptionHelper.addViewDescription(0, 0, child, sb, embedded);
        nextDepth++;
      }
      viewToString(child, sb, embedded, nextDepth);
    }
  }

  /** calculate the depth on the litho components in general android view hierarchy */
  private static int getLithoViewDepthInAndroid(LithoView view) {
    int depth = 2;
    ViewParent parent = view.getParent();
    while (parent != null) {
      depth++;
      parent = parent.getParent();
    }
    return depth;
  }

  /** Add new line and two-space indent for each level to match android view hierarchy dump */
  private static void writeNewLineWithIndentByDepth(StringBuilder sb, int depth) {
    sb.append("\n");
    for (int i = 0; i <= depth; i++) {
      sb.append("  ");
    }
  }

  public static String toDebugString(@Nullable LithoView lithoView) {
    if (lithoView == null) {
      return "";
    }

    final String debugString = viewToString(lithoView, true);
    return TextUtils.isEmpty(debugString) ? viewBoundsToString(lithoView) : debugString;
  }

  private static String viewBoundsToString(LithoView lithoView) {
    final StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append(lithoView.getLeft());
    sb.append(",");
    sb.append(lithoView.getTop());
    sb.append("-");
    sb.append(lithoView.getRight());
    sb.append(",");
    sb.append(lithoView.getBottom());
    sb.append(")");
    return sb.toString();
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
