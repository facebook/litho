/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho;

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static com.facebook.yoga.YogaDirection.INHERIT;
import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.drawable.ColorDrawable;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentWithState;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LithoNodeTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private LithoNode acquireInternalNode() {
    final ComponentContext context = mLegacyLithoViewRule.getContext();
    mLegacyLithoViewRule
        .attachToWindow()
        .setRootAndSizeSpecSync(
            Column.create(context).build(),
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
        .measure()
        .layout();

    final LithoLayoutResult root = mLegacyLithoViewRule.getCurrentRootNode();

    return root.getNode();
  }

  private static NestedTreeHolder acquireNestedTreeHolder() {
    final ComponentContext context = new ComponentContext(getApplicationContext());
    context.setLayoutStateContextForTesting();

    return new NestedTreeHolder(context, null);
  }

  private final TestComponentsReporter mComponentsReporter = new TestComponentsReporter();

  @Before
  public void setup() {
    ComponentsReporter.provide(mComponentsReporter);
  }

  @Test
  public void testLayoutDirectionFlag() {
    final LithoNode node = acquireInternalNode();
    node.layoutDirection(INHERIT);
    assertThat(isFlagSet(node, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_LAYOUT_DIRECTION_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testImportantForAccessibilityFlag() {
    final LithoNode node = (LithoNode) acquireInternalNode();
    node.importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertThat(isFlagSet(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testDuplicateParentStateFlag() {
    final LithoNode node = (LithoNode) acquireInternalNode();
    node.duplicateParentState(false);
    assertThat(isFlagSet(node, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testBackgroundFlag() {
    final LithoNode node = (LithoNode) acquireInternalNode();
    node.backgroundColor(0xFFFF0000);
    assertThat(isFlagSet(node, "PFLAG_BACKGROUND_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_BACKGROUND_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testForegroundFlag() {
    final LithoNode node = (LithoNode) acquireInternalNode();
    node.foregroundColor(0xFFFF0000);
    assertThat(isFlagSet(node, "PFLAG_FOREGROUND_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_FOREGROUND_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testTransitionKeyFlag() {
    final LithoNode node = (LithoNode) acquireInternalNode();
    node.transitionKey("key", "");
    assertThat(isFlagSet(node, "PFLAG_TRANSITION_KEY_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_TRANSITION_KEY_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testCopyIntoNodeSetFlags() {
    NestedTreeHolder orig = acquireNestedTreeHolder();
    LithoNode dest = acquireInternalNode();

    orig.importantForAccessibility(0);
    orig.duplicateParentState(true);
    orig.background(new ColorDrawable());
    orig.foreground(null);
    orig.visibleHandler(null);
    orig.focusedHandler(null);
    orig.fullImpressionHandler(null);
    orig.invisibleHandler(null);
    orig.unfocusedHandler(null);
    orig.visibilityChangedHandler(null);

    ((NestedTreeHolder) orig).transferInto(dest);

    assertThat(isFlagSet(dest, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_BACKGROUND_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_FOREGROUND_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_VISIBLE_HANDLER_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_FOCUSED_HANDLER_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_FULL_IMPRESSION_HANDLER_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_INVISIBLE_HANDLER_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_UNFOCUSED_HANDLER_IS_SET")).isTrue();
    assertThat(isFlagSet(dest, "PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET")).isTrue();
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayoutLS_measure() {
    final ComponentContext baseContext = new ComponentContext(getApplicationContext());
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, c.getComponentTree());
    Whitebox.setInternalState(layoutState, "mLayoutStateContext", layoutStateContext);
    c.setLayoutStateContext(layoutStateContext);

    final int unspecifiedSizeSpec = makeSizeSpec(0, UNSPECIFIED);
    final int exactSizeSpec = makeSizeSpec(50, EXACTLY);
    final Component textComponent = Text.create(c).textSizePx(16).text("test").build();
    final Size textSize = new Size();
    textComponent.measure(c, exactSizeSpec, unspecifiedSizeSpec, textSize);

    assertThat(layoutState.getCachedLayout(textComponent)).isNotNull();
    LithoLayoutResult cachedLayout = layoutState.getCachedLayout(textComponent);
    assertThat(cachedLayout).isNotNull();
    assertThat(cachedLayout.getLastWidthSpec()).isEqualTo(exactSizeSpec);
    assertThat(cachedLayout.getLastHeightSpec()).isEqualTo(unspecifiedSizeSpec);

    layoutState.clearCachedLayout(textComponent);
    assertThat(layoutState.getCachedLayout(textComponent)).isNull();
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayoutLS_measureMightNotCacheInternalNode() {
    final ComponentContext baseContext = new ComponentContext(getApplicationContext());
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, c.getComponentTree());
    Whitebox.setInternalState(layoutState, "mLayoutStateContext", layoutStateContext);
    c.setLayoutStateContext(layoutStateContext);

    final int unspecifiedSizeSpec = makeSizeSpec(0, UNSPECIFIED);
    final int exactSizeSpec = makeSizeSpec(50, EXACTLY);
    final Component textComponent = Text.create(c).textSizePx(16).text("test").build();
    final Size textSize = new Size();
    textComponent.measureMightNotCacheInternalNode(c, exactSizeSpec, unspecifiedSizeSpec, textSize);

    assertThat(layoutState.getCachedLayout(textComponent)).isNotNull();
    LithoLayoutResult cachedLayout = layoutState.getCachedLayout(textComponent);
    assertThat(cachedLayout).isNotNull();
    assertThat(cachedLayout.getLastWidthSpec()).isEqualTo(exactSizeSpec);
    assertThat(cachedLayout.getLastHeightSpec()).isEqualTo(unspecifiedSizeSpec);

    layoutState.clearCachedLayout(textComponent);
    assertThat(layoutState.getCachedLayout(textComponent)).isNull();
  }

  @Test
  public void testMeasureMightNotCacheInternalNode_ContextWithoutStateHandler_returnsMeasurement() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext = new LayoutStateContext(layoutState, null);
    c.setLayoutStateContext(layoutStateContext);
    Whitebox.setInternalState(layoutState, "mLayoutStateContext", layoutStateContext);

    final Component component =
        Column.create(c)
            .child(Text.create(c).textSizePx(16).text("test"))
            .child(ComponentWithState.create(c))
            .build();
    final Size textSize = new Size();
    component.measureMightNotCacheInternalNode(
        c, makeSizeSpec(50, EXACTLY), makeSizeSpec(0, UNSPECIFIED), textSize);

    assertThat(textSize.width).isEqualTo(50);
    assertThat(textSize.height).isGreaterThan(0);
  }

  @Test
  public void
      testMeasureMightNotCacheInternalNode_ContextWithoutLayoutStateContextOrStateHandler_returnsMeasurement() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    c.setLayoutStateContext(LayoutStateContext.getTestInstance(c));

    final Component component =
        Column.create(c)
            .child(Text.create(c).textSizePx(16).text("test"))
            .child(ComponentWithState.create(c))
            .build();
    final Size textSize = new Size();
    component.measureMightNotCacheInternalNode(
        c, makeSizeSpec(50, EXACTLY), makeSizeSpec(0, UNSPECIFIED), textSize);

    assertThat(textSize.width).isEqualTo(50);
    assertThat(textSize.height).isGreaterThan(0);
  }

  private static boolean isFlagSet(LithoNode node, String flagName) {
    long flagPosition = Whitebox.getInternalState(LithoNode.class, flagName);
    long flags = Whitebox.getInternalState(node, "mPrivateFlags");

    return ((flags & flagPosition) != 0);
  }

  private static void clearFlag(LithoNode node, String flagName) {
    long flagPosition = Whitebox.getInternalState(LithoNode.class, flagName);
    long flags = Whitebox.getInternalState(node, "mPrivateFlags");
    flags &= ~flagPosition;
    Whitebox.setInternalState(node, "mPrivateFlags", flags);
  }

  private static void assertEmptyFlags(LithoNode node) {
    assertThat(((long) getInternalState(node, "mPrivateFlags")) == 0).isTrue();
  }
}
