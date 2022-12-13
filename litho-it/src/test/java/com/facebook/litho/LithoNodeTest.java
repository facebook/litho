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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.SparseArray;
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
    return new NestedTreeHolder(null);
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
  public void testAddingAllAttributes() {
    final LithoNode node = spy(acquireInternalNode());
    NodeInfo nodeInfo = mock(NodeInfo.class);
    when(node.mutableNodeInfo()).thenReturn(nodeInfo);

    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    final Object viewTag = new Object();
    final SparseArray<Object> viewTags = new SparseArray<>();
    final EventHandler<DispatchPopulateAccessibilityEventEvent>
        dispatchPopulateAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityNodeInfoEvent>
        onInitializeAccessibilityNodeInfoHandler = mock(EventHandler.class);
    final EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnRequestSendAccessibilityEventEvent>
        onRequestSendAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventUncheckedEvent>
        sendAccessibilityEventUncheckedHandler = mock(EventHandler.class);

    NodeInfo toBeAppliedInfo = new NodeInfo();
    toBeAppliedInfo.setClickHandler(clickHandler);
    toBeAppliedInfo.setFocusChangeHandler(focusChangedHandler);
    toBeAppliedInfo.setLongClickHandler(longClickHandler);
    toBeAppliedInfo.setTouchHandler(touchHandler);
    toBeAppliedInfo.setInterceptTouchHandler(interceptTouchHandler);
    toBeAppliedInfo.setFocusable(true);
    toBeAppliedInfo.setSelected(false);
    toBeAppliedInfo.setEnabled(false);
    toBeAppliedInfo.setAccessibilityHeading(false);
    toBeAppliedInfo.setContentDescription("test");
    toBeAppliedInfo.setViewTag(viewTag);
    toBeAppliedInfo.setViewTags(viewTags);
    toBeAppliedInfo.setShadowElevation(60);
    toBeAppliedInfo.setAmbientShadowColor(Color.RED);
    toBeAppliedInfo.setSpotShadowColor(Color.BLUE);
    toBeAppliedInfo.setClipToOutline(false);
    toBeAppliedInfo.setAccessibilityRole(AccessibilityRole.BUTTON);
    toBeAppliedInfo.setAccessibilityRoleDescription("Test Role Description");
    toBeAppliedInfo.setDispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    toBeAppliedInfo.setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    toBeAppliedInfo.setOnInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    toBeAppliedInfo.setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    toBeAppliedInfo.setOnRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    toBeAppliedInfo.setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    toBeAppliedInfo.setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    toBeAppliedInfo.setSendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);

    node.applyNodeInfo(toBeAppliedInfo);

    verify(nodeInfo).setClickHandler(clickHandler);
    verify(nodeInfo).setFocusChangeHandler(focusChangedHandler);
    verify(nodeInfo).setLongClickHandler(longClickHandler);
    verify(nodeInfo).setTouchHandler(touchHandler);
    verify(nodeInfo).setInterceptTouchHandler(interceptTouchHandler);
    verify(nodeInfo).setFocusable(true);
    verify(nodeInfo).setSelected(false);
    verify(nodeInfo).setEnabled(false);
    verify(nodeInfo).setAccessibilityHeading(false);
    verify(nodeInfo).setContentDescription("test");
    verify(nodeInfo).setViewTag(viewTag);
    verify(nodeInfo).setViewTags(viewTags);
    verify(nodeInfo).setShadowElevation(60);
    verify(nodeInfo).setAmbientShadowColor(Color.RED);
    verify(nodeInfo).setSpotShadowColor(Color.BLUE);
    verify(nodeInfo).setClipToOutline(false);
    verify(nodeInfo).setAccessibilityRole(AccessibilityRole.BUTTON);
    verify(nodeInfo).setAccessibilityRoleDescription("Test Role Description");
    verify(nodeInfo)
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    verify(nodeInfo).setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    verify(nodeInfo).setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    verify(nodeInfo).setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    verify(nodeInfo)
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
  }

  @Test
  public void testApplyNodeInfo() {
    final LithoNode node = acquireInternalNode();
    assertThat(node.getNodeInfo() == null).describedAs("Should be NULL at the beginning").isTrue();

    NodeInfo toBeAppliedInfo = new NodeInfo();
    toBeAppliedInfo.setAlpha(0.5f);
    toBeAppliedInfo.setEnabled(true);
    toBeAppliedInfo.setClickable(true);
    node.applyNodeInfo(toBeAppliedInfo);
    assertThat(node.getNodeInfo() == toBeAppliedInfo)
        .describedAs("Should be the same instance as passing")
        .isTrue();
    assertThat(Float.compare(node.getNodeInfo().getAlpha(), 0.5f) == 0)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue();
    assertThat(node.getNodeInfo().getEnabledState() == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue();
    assertThat(node.getNodeInfo().getClickableState() == NodeInfo.CLICKABLE_SET_TRUE)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue();

    NodeInfo toBeAppliedInfo2 = new NodeInfo();
    toBeAppliedInfo2.setScale(1.0f);
    toBeAppliedInfo2.setSelected(true);
    toBeAppliedInfo2.setClickable(false);
    node.applyNodeInfo(toBeAppliedInfo2);
    assertThat(Float.compare(node.getNodeInfo().getAlpha(), 0.5f) == 0)
        .describedAs("Properties should not be overridden")
        .isTrue();
    assertThat(node.getNodeInfo().getEnabledState() == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should not be overridden")
        .isTrue();
    assertThat(node.getNodeInfo().getClickableState() == NodeInfo.CLICKABLE_SET_FALSE)
        .describedAs("Properties should be overridden")
        .isTrue();
    assertThat(Float.compare(node.getNodeInfo().getScale(), 1.0f) == 0)
        .describedAs("Properties should be merged")
        .isTrue();
    assertThat(node.getNodeInfo().getSelectedState() == NodeInfo.SELECTED_SET_TRUE)
        .describedAs("Properties should be merged")
        .isTrue();

    node.applyNodeInfo(null);
    assertThat(node.getNodeInfo() != null)
        .describedAs("Should be the same after merging a NULL object")
        .isTrue();
  }

  @Test
  public void testMutableNodeInfo() {
    final LithoNode node = acquireInternalNode();

    NodeInfo nodeInfo1 = node.mutableNodeInfo();
    assertThat(nodeInfo1 != null).describedAs("Should never return NULL").isTrue();
    assertThat(node.getNodeInfo() == nodeInfo1)
        .describedAs("Should be the same instance as getNodeInfo returned")
        .isTrue();

    NodeInfo nodeInfo2 = node.mutableNodeInfo();
    assertThat(nodeInfo2 == nodeInfo1)
        .describedAs("Should return the same instance no matter how many times it is called")
        .isTrue();

    NodeInfo initNodeIfo = new NodeInfo();
    initNodeIfo.setAlpha(0.5f);
    initNodeIfo.setEnabled(true);
    initNodeIfo.setClickable(true);
    node.applyNodeInfo(initNodeIfo);
    NodeInfo mutableNodeInfo = node.mutableNodeInfo();
    assertThat(initNodeIfo != mutableNodeInfo)
        .describedAs("Should return a copy of the initNodeInfo")
        .isTrue();
    assertThat(Float.compare(mutableNodeInfo.getAlpha(), 0.5f) == 0)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue();
    assertThat(mutableNodeInfo.getEnabledState() == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue();
    assertThat(mutableNodeInfo.getClickableState() == NodeInfo.CLICKABLE_SET_TRUE)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue();

    mutableNodeInfo.setScale(1.0f);
    assertThat(node.getNodeInfo() != null)
        .describedAs("Should be the same state as mutableNodeInfo")
        .isTrue();
    assertThat(Float.compare(node.getNodeInfo().getScale(), 1.0f) == 0)
        .describedAs("Property of the nodeInfo should also be updated")
        .isTrue();
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

    final ResolveStateContext resolveStateContext = c.setRenderStateContextForTests();
    final MeasuredResultCache resultCache = resolveStateContext.getCache();

    final int unspecifiedSizeSpec = makeSizeSpec(0, UNSPECIFIED);
    final int exactSizeSpec = makeSizeSpec(50, EXACTLY);
    final Component textComponent = Text.create(c).textSizePx(16).text("test").build();
    final Size textSize = new Size();
    textComponent.measure(c, exactSizeSpec, unspecifiedSizeSpec, textSize);

    assertThat(resultCache.getCachedResult(textComponent)).isNotNull();
    LithoLayoutResult cachedLayout = resultCache.getCachedResult(textComponent);
    assertThat(cachedLayout).isNotNull();
    assertThat(cachedLayout.getLastWidthSpec()).isEqualTo(exactSizeSpec);
    assertThat(cachedLayout.getLastHeightSpec()).isEqualTo(unspecifiedSizeSpec);

    resultCache.clearCache(textComponent);
    assertThat(resultCache.getCachedResult(textComponent)).isNull();
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayoutLS_measureMightNotCacheInternalNode() {
    final ComponentContext baseContext = new ComponentContext(getApplicationContext());
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());

    final ResolveStateContext resolveStateContext = c.setRenderStateContextForTests();

    final MeasuredResultCache resultCache = resolveStateContext.getCache();

    final int unspecifiedSizeSpec = makeSizeSpec(0, UNSPECIFIED);
    final int exactSizeSpec = makeSizeSpec(50, EXACTLY);
    final Component textComponent = Text.create(c).textSizePx(16).text("test").build();
    final Size textSize = new Size();
    textComponent.measureMightNotCacheInternalNode(c, exactSizeSpec, unspecifiedSizeSpec, textSize);

    assertThat(resultCache.getCachedResult(textComponent)).isNotNull();
    LithoLayoutResult cachedLayout = resultCache.getCachedResult(textComponent);
    assertThat(cachedLayout).isNotNull();
    assertThat(cachedLayout.getLastWidthSpec()).isEqualTo(exactSizeSpec);
    assertThat(cachedLayout.getLastHeightSpec()).isEqualTo(unspecifiedSizeSpec);

    resultCache.clearCache(textComponent);
    assertThat(resultCache.getCachedResult(textComponent)).isNull();
  }

  @Test
  public void testMeasureMightNotCacheInternalNode_ContextWithoutStateHandler_returnsMeasurement() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    c.setRenderStateContextForTests();

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
    c.setRenderStateContextForTests();

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
