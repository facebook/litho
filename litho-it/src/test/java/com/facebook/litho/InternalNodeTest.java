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

package com.facebook.litho;

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Layout.createAndMeasureComponent;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.it.R.drawable.background_without_padding;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static com.facebook.yoga.YogaDirection.INHERIT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentWithState;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class InternalNodeTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private final boolean mDefaultUseInputOnlyInternalNodes;

  public InternalNodeTest() {
    ComponentsConfiguration.useStatelessComponent = true;
    mDefaultUseInputOnlyInternalNodes = ComponentsConfiguration.useInputOnlyInternalNodes;
  }

  private InternalNode acquireInternalNode() {
    final ComponentContext context = mLithoViewRule.getContext();
    mLithoViewRule
        .attachToWindow()
        .setRootAndSizeSpec(
            Column.create(context).build(),
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
        .measure()
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    return root.getInternalNode();
  }

  private static InternalNode.NestedTreeHolder acquireNestedTreeHolder() {
    final ComponentContext context = new ComponentContext(getApplicationContext());
    context.setLayoutStateContextForTesting();

    return new InputOnlyNestedTreeHolder(context, null);
  }

  private static LithoLayoutResult acquireInternalNodeWithLogger(ComponentsLogger logger) {
    final ComponentContext context = new ComponentContext(getApplicationContext(), "TEST", logger);
    context.setLayoutStateContextForTesting();

    return createAndMeasureComponent(
            context,
            Column.create(context).build(),
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
        .mResult;
  }

  private final TestComponentsReporter mComponentsReporter = new TestComponentsReporter();

  @Before
  public void setup() {
    ComponentsConfiguration.useInputOnlyInternalNodes = true;
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext componentContext) {
            return new InputOnlyInternalNode<>(componentContext);
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            return new InputOnlyNestedTreeHolder(c, props);
          }
        };
    ComponentsReporter.provide(mComponentsReporter);
  }

  @After
  public void cleanup() {
    NodeConfig.sInternalNodeFactory = null;
    ComponentsConfiguration.useInputOnlyInternalNodes = mDefaultUseInputOnlyInternalNodes;
  }

  @Test
  public void testLayoutDirectionFlag() {
    final InternalNode node = acquireInternalNode();
    node.layoutDirection(INHERIT);
    assertThat(isFlagSet(node, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_LAYOUT_DIRECTION_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testImportantForAccessibilityFlag() {
    final InputOnlyInternalNode node = (InputOnlyInternalNode) acquireInternalNode();
    node.importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertThat(isFlagSet(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testDuplicateParentStateFlag() {
    final InputOnlyInternalNode node = (InputOnlyInternalNode) acquireInternalNode();
    node.duplicateParentState(false);
    assertThat(isFlagSet(node, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testBackgroundFlag() {
    final InputOnlyInternalNode node = (InputOnlyInternalNode) acquireInternalNode();
    node.backgroundColor(0xFFFF0000);
    assertThat(isFlagSet(node, "PFLAG_BACKGROUND_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_BACKGROUND_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testForegroundFlag() {
    final InputOnlyInternalNode node = (InputOnlyInternalNode) acquireInternalNode();
    node.foregroundColor(0xFFFF0000);
    assertThat(isFlagSet(node, "PFLAG_FOREGROUND_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_FOREGROUND_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testTransitionKeyFlag() {
    final InputOnlyInternalNode node = (InputOnlyInternalNode) acquireInternalNode();
    node.transitionKey("key", "");
    assertThat(isFlagSet(node, "PFLAG_TRANSITION_KEY_IS_SET")).isTrue();
    clearFlag(node, "PFLAG_TRANSITION_KEY_IS_SET");
    assertEmptyFlags(node);
  }

  @Test
  public void testCopyIntoNodeSetFlags() {
    InternalNode.NestedTreeHolder orig = acquireNestedTreeHolder();
    InternalNode dest = acquireInternalNode();

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

    ((InputOnlyNestedTreeHolder) orig).transferInto(dest);

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
  public void testPaddingIsNotSetFromDrawable() {
    InternalNode node = acquireInternalNode();

    node.backgroundRes(background_without_padding);

    assertThat(isFlagSet(node, "PFLAG_PADDING_IS_SET")).isFalse();
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayoutLS_measure() {
    final ComponentContext baseContext = new ComponentContext(getApplicationContext());
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateContext(new LayoutStateContext(layoutState, c.getComponentTree()));

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
    c.setLayoutStateContext(new LayoutStateContext(layoutState, c.getComponentTree()));

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
    c.setLayoutStateContext(new LayoutStateContext(layoutState, null));

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

  @Test
  public void testContextSpecificComponentAssertionPasses() {
    acquireInternalNode().assertContextSpecificStyleNotSet();
  }

  @Test
  public void testContextSpecificComponentAssertionFailFormatting() {
    final boolean value = ComponentsConfiguration.isDebugModeEnabled;
    ComponentsConfiguration.isDebugModeEnabled = true;
    final ComponentsLogger componentsLogger = mock(ComponentsLogger.class);
    final PerfEvent perfEvent = mock(PerfEvent.class);
    when(componentsLogger.newPerformanceEvent((ComponentContext) any(), anyInt()))
        .thenReturn(perfEvent);

    final ComponentContext context =
        new ComponentContext(getApplicationContext(), "TEST", componentsLogger);
    context.setLayoutStateContextForTesting();

    InternalNode node = Layout.create(context, Column.create(context).build());

    final LayoutProps editor = node.getDebugLayoutEditor();
    editor.alignSelf(YogaAlign.AUTO);
    editor.flex(1f);

    Layout.measure(context, node, UNSPECIFIED, UNSPECIFIED, null, null);

    assertThat(mComponentsReporter.getLoggedMessages().get(0).second)
        .isEqualTo("You should not set alignSelf, flex to a root layout.");

    ComponentsConfiguration.isDebugModeEnabled = value;
  }

  @Test
  public void testDeepClone() {
    final ComponentContext context = new ComponentContext(getApplicationContext());
    context.setLayoutStateContextForTesting();

    LithoLayoutResult layout =
        createAndMeasureComponent(
                context,
                Column.create(context)
                    .child(Row.create(context).child(Column.create(context)))
                    .child(Column.create(context).child(Row.create(context)))
                    .child(SolidColor.create(context).color(Color.RED))
                    .build(),
                makeSizeSpec(0, UNSPECIFIED),
                makeSizeSpec(0, UNSPECIFIED))
            .mResult;

    InputOnlyInternalNode cloned = (InputOnlyInternalNode) layout.getInternalNode().deepClone();

    assertThat(cloned).isNotNull();

    assertThat(cloned).isNotSameAs(layout);

    assertThat(cloned.getChildCount()).isEqualTo(layout.getChildCount());

    assertThat(cloned.getChildAt(0).getTailComponentKey())
        .isEqualTo(layout.getChildAt(0).getInternalNode().getTailComponentKey());
    assertThat(cloned.getChildAt(1).getTailComponentKey())
        .isEqualTo(layout.getChildAt(1).getInternalNode().getTailComponentKey());
    assertThat(cloned.getChildAt(2).getTailComponentKey())
        .isEqualTo(layout.getChildAt(2).getInternalNode().getTailComponentKey());

    assertThat(cloned.getChildAt(0).getChildAt(0)).isNotSameAs(layout.getChildAt(0).getChildAt(0));
    assertThat(cloned.getChildAt(1).getChildAt(0)).isNotSameAs(layout.getChildAt(1).getChildAt(0));
  }

  private static boolean isFlagSet(InternalNode internalNode, String flagName) {
    long flagPosition = Whitebox.getInternalState(InputOnlyInternalNode.class, flagName);
    long flags = Whitebox.getInternalState(internalNode, "mPrivateFlags");

    return ((flags & flagPosition) != 0);
  }

  private static void clearFlag(InternalNode internalNode, String flagName) {
    long flagPosition = Whitebox.getInternalState(InputOnlyInternalNode.class, flagName);
    long flags = Whitebox.getInternalState(internalNode, "mPrivateFlags");
    flags &= ~flagPosition;
    Whitebox.setInternalState(internalNode, "mPrivateFlags", flags);
  }

  private static void assertEmptyFlags(InternalNode internalNode) {
    assertThat(((long) getInternalState(internalNode, "mPrivateFlags")) == 0).isTrue();
  }
}
