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

import static android.R.drawable.btn_default;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.STATE_DIRTY;
import static com.facebook.litho.LayoutOutput.STATE_UNKNOWN;
import static com.facebook.litho.LayoutOutput.STATE_UPDATED;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LayoutState.calculate;
import static com.facebook.litho.LayoutState.createDiffNode;
import static com.facebook.litho.OutputUnitType.CONTENT;
import static com.facebook.litho.SizeSpec.getMode;
import static com.facebook.litho.SizeSpec.getSize;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static com.facebook.yoga.YogaAlign.FLEX_START;
import static com.facebook.yoga.YogaConstants.UNDEFINED;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static com.facebook.yoga.YogaMeasureOutput.getHeight;
import static com.facebook.yoga.YogaMeasureOutput.getWidth;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.collection.SparseArrayCompat;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaMeasureFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class TreeDiffingTest {

  private static Drawable sRedDrawable;
  private static Drawable sBlackDrawable;
  private static Drawable sTransparentDrawable;

  @Rule public final LithoViewRule mLithoViewRule = new LithoViewRule();

  private int mUnspecifiedSpec;

  @Before
  public void setup() throws Exception {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mUnspecifiedSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    sRedDrawable = ComparableColorDrawable.create(Color.RED);
    sBlackDrawable = ComparableColorDrawable.create(Color.BLACK);
    sTransparentDrawable = ComparableColorDrawable.create(TRANSPARENT);
  }

  @Test
  public void testDiffTreeDisabled() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mLithoViewRule.getComponentTree().getContext(),
            component,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check diff tree is null.
    assertThat(layoutState.getDiffTree()).isNull();
  }

  @Test
  public void testDiffTreeEnabled() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is not null and consistent.
    DiffNode node = layoutState.getDiffTree();
    assertThat(node).isNotNull();
    assertThat(4).isEqualTo(countNodes(node));
  }

  private static int countNodes(DiffNode node) {
    int sum = 1;
    for (int i = 0; i < node.getChildCount(); i++) {
      sum += countNodes(node.getChildAt(i));
    }

    return sum;
  }

  private DefaultInternalNode createInternalNodeForMeasurableComponent(Component component) {
    ComponentContext context = new ComponentContext(mLithoViewRule.getContext());
    context.setLayoutStateContext(LayoutStateContext.getTestInstance(context));
    component.setScopedContext(context);
    final ComponentContext c = new ComponentContext(context);
    DefaultInternalNode node =
        (DefaultInternalNode) Layout.create(context.getLayoutStateContext(), c, component);
    node.setLayoutStateContextRecursively(context.getLayoutStateContext());
    return node;
  }

  private long measureInternalNode(
      DefaultInternalNode node, float widthConstranint, float heightConstraint) {

    final YogaMeasureFunction measureFunc =
        Whitebox.getInternalState(node.getYogaNode(), "mMeasureFunction");

    return measureFunc.measure(
        node.getYogaNode(), widthConstranint, EXACTLY, heightConstraint, EXACTLY);
  }

  @Test
  public void testCachedMeasureFunction() {
    final Component component =
        TestDrawableComponent.create(mLithoViewRule.getComponentTree().getContext())
            .key("global_key")
            .build();

    DefaultInternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DefaultDiffNode();
    diffNode.setLastHeightSpec(mUnspecifiedSpec);
    diffNode.setLastWidthSpec(mUnspecifiedSpec);
    diffNode.setLastMeasuredWidth(10);
    diffNode.setLastMeasuredHeight(5);
    diffNode.setComponent(component, "global_key");

    node.setCachedMeasuresValid(true);
    node.setDiffNode(diffNode);

    long output = measureInternalNode(node, UNDEFINED, UNDEFINED);

    assertThat(getHeight(output) == (int) diffNode.getLastMeasuredHeight()).isTrue();
    assertThat(getWidth(output) == (int) diffNode.getLastMeasuredWidth()).isTrue();
  }

  @Test
  public void tesLastConstraints() {
    final Component component =
        TestDrawableComponent.create(mLithoViewRule.getComponentTree().getContext())
            .key("global_key")
            .build();

    DefaultInternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DefaultDiffNode();
    diffNode.setLastWidthSpec(makeSizeSpec(10, SizeSpec.EXACTLY));
    diffNode.setLastHeightSpec(makeSizeSpec(5, SizeSpec.EXACTLY));
    diffNode.setLastMeasuredWidth(10f);
    diffNode.setLastMeasuredHeight(5f);
    diffNode.setComponent(component, "global_key");

    node.setCachedMeasuresValid(true);
    node.setDiffNode(diffNode);

    long output = measureInternalNode(node, 10f, 5f);

    assertThat(getHeight(output) == (int) diffNode.getLastMeasuredHeight()).isTrue();
    assertThat(getWidth(output) == (int) diffNode.getLastMeasuredWidth()).isTrue();

    int lastWidthSpec = node.getLastWidthSpec();
    int lastHeightSpec = node.getLastHeightSpec();

    assertThat(getMode(lastWidthSpec) == SizeSpec.EXACTLY).isTrue();
    assertThat(getMode(lastHeightSpec) == SizeSpec.EXACTLY).isTrue();
    assertThat(getSize(lastWidthSpec) == 10).isTrue();
    assertThat(getSize(lastHeightSpec) == 5).isTrue();
  }

  @Test
  public void measureAndCreateDiffNode() {
    final Component component =
        TestDrawableComponent.create(mLithoViewRule.getComponentTree().getContext()).build();

    DefaultInternalNode node = createInternalNodeForMeasurableComponent(component);
    long output = measureInternalNode(node, UNDEFINED, UNDEFINED);

    node.setCachedMeasuresValid(false);
    DiffNode diffNode = createDiffNode(node, node, null);
    assertThat(getHeight(output) == (int) diffNode.getLastMeasuredHeight()).isTrue();
    assertThat(getWidth(output) == (int) diffNode.getLastMeasuredWidth()).isTrue();
  }

  @Test
  public void testCachedMeasures() {
    final Component component1 = new TestLayoutSpec(false);
    final Component component2 = new TestLayoutSpec(false);

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is consistent.
    DiffNode node = prevLayoutState.getDiffTree();
    final LayoutStateContext prev = prevLayoutState.getLayoutStateContext();

    ComponentContext c = mLithoViewRule.getComponentTree().getContext();
    LithoLayoutResult layoutTreeRoot =
        Layout.measure(
            c.getLayoutStateContext(),
            c,
            createInternalNodeForMeasurableComponent(component2),
            SizeSpec.UNSPECIFIED,
            SizeSpec.UNSPECIFIED,
            null,
            prev,
            node);
    Layout.applyDiffNodeToUnchangedNodes(
        c.getLayoutStateContext(),
        layoutTreeRoot,
        true,
        prevLayoutState.getLayoutStateContext(),
        node);
    checkAllComponentsHaveMeasureCache(layoutTreeRoot);
  }

  @Test
  public void testPartiallyCachedMeasures() {
    final Component component1 = new TestLayoutSpec(false);
    final Component component2 = new TestLayoutSpec(true);

    final ComponentContext c = mLithoViewRule.getContext();
    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is consistent.
    DiffNode node = prevLayoutState.getDiffTree();
    final LayoutStateContext prev = prevLayoutState.getLayoutStateContext();

    LithoLayoutResult layoutTreeRoot =
        Layout.measure(
            c.getLayoutStateContext(),
            c,
            createInternalNodeForMeasurableComponent(component2),
            SizeSpec.UNSPECIFIED,
            SizeSpec.UNSPECIFIED,
            null,
            prev,
            node);
    Layout.applyDiffNodeToUnchangedNodes(
        c.getLayoutStateContext(),
        layoutTreeRoot,
        true,
        prevLayoutState.getLayoutStateContext(),
        node);
    LithoLayoutResult child_1 = layoutTreeRoot.getChildAt(0);
    assertCachedMeasurementsDefined(child_1);

    LithoLayoutResult child_2 = layoutTreeRoot.getChildAt(1);
    assertCachedMeasurementsNotDefined(child_2);
    LithoLayoutResult child_3 = child_2.getChildAt(0);
    assertCachedMeasurementsDefined(child_3);

    LithoLayoutResult child_4 = layoutTreeRoot.getChildAt(2);
    assertCachedMeasurementsNotDefined(child_4);
  }

  @Test
  public void testLayoutOutputReuse() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertThat(layoutState.getMountableOutputCount())
        .isEqualTo(prevLayoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getRenderUnit().getId())
          .isEqualTo(prevLayoutState.getMountableOutputAt(i).getRenderUnit().getId());
    }
  }

  @Test
  public void testLayoutOutputPartialReuse() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);
    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component2,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertNotEquals(
        prevLayoutState.getMountableOutputCount(), layoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getRenderUnit().getId())
          .describedAs("Output " + i)
          .isEqualTo(prevLayoutState.getMountableOutputAt(i).getRenderUnit().getId());
    }
  }

  private void assertCachedMeasurementsNotDefined(LithoLayoutResult node) {
    assertThat(node.areCachedMeasuresValid()).isFalse();
  }

  private void checkAllComponentsHaveMeasureCache(LithoLayoutResult node) {
    if (node.getInternalNode().getTailComponent() != null
        && node.getInternalNode().getTailComponent().canMeasure()) {
      assertCachedMeasurementsDefined(node);
    }
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      checkAllComponentsHaveMeasureCache(node.getChildAt(i));
    }
  }

  @Test
  public void testComponentHostMoveItem() {
    ComponentContext c = mLithoViewRule.getComponentTree().getContext();
    ComponentHost hostHolder = new ComponentHost(c);

    MountItem mountItem = mock(MountItem.class);
    when(mountItem.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem1 = mock(MountItem.class);
    when(mountItem1.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem2 = mock(MountItem.class);
    when(mountItem2.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    hostHolder.mount(0, mountItem, new Rect());
    hostHolder.mount(1, mountItem1, new Rect());
    hostHolder.mount(2, mountItem2, new Rect());
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(2));
    hostHolder.moveItem(mountItem, 0, 2);
    hostHolder.moveItem(mountItem2, 2, 0);
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(2));
  }

  @Test
  public void testComponentHostMoveItemPartial() {
    ComponentContext c = mLithoViewRule.getComponentTree().getContext();
    ComponentHost hostHolder = new ComponentHost(c);

    MountItem mountItem = mock(MountItem.class);
    when(mountItem.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem1 = mock(MountItem.class);
    when(mountItem1.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem2 = mock(MountItem.class);
    when(mountItem2.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    hostHolder.mount(0, mountItem, new Rect());
    hostHolder.mount(1, mountItem1, new Rect());
    hostHolder.mount(2, mountItem2, new Rect());
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(2));
    hostHolder.moveItem(mountItem2, 2, 0);
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));

    assertThat(1)
        .isEqualTo(
            ((SparseArrayCompat<MountItem>) getInternalState(hostHolder, "mScrapMountItemsArray"))
                .size());

    hostHolder.unmount(0, mountItem);

    assertThat(2)
        .isEqualTo(
            ((SparseArrayCompat<MountItem>) getInternalState(hostHolder, "mMountItems")).size());
    assertThat((Object) getInternalState(hostHolder, "mScrapMountItemsArray")).isNull();
  }

  @Test
  public void testLayoutOutputUpdateState() {
    ComponentContext c = mLithoViewRule.getContext();
    final Component firstComponent = TestDrawableComponent.create(c).color(Color.BLACK).build();
    final Component secondComponent = TestDrawableComponent.create(c).color(Color.BLACK).build();
    final Component thirdComponent = TestDrawableComponent.create(c).color(Color.WHITE).build();

    mLithoViewRule.setRoot(firstComponent).attachToWindow();

    mLithoViewRule.setRootAndSizeSpec(
        firstComponent,
        SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, LayoutOutput.STATE_UNKNOWN);

    mLithoViewRule.setRoot(secondComponent);
    LayoutState secondState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(secondState, LayoutOutput.STATE_UPDATED);

    mLithoViewRule.setRoot(thirdComponent);
    LayoutState thirdState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(thirdState, LayoutOutput.STATE_DIRTY);
  }

  @Test
  public void testLayoutOutputUpdateStateWithBackground() {
    final Component component1 = new TestLayoutSpecBgState(false);
    final Component component2 = new TestLayoutSpecBgState(false);
    final Component component3 = new TestLayoutSpecBgState(true);

    mLithoViewRule.setRoot(component1).attachToWindow();

    mLithoViewRule.setRootAndSizeSpec(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, STATE_UNKNOWN);

    mLithoViewRule.setRoot(component2);
    LayoutState secondState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(secondState.getMountableOutputCount()).isEqualTo(4);

    mLithoViewRule.setRoot(component3);
    LayoutState thirdState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(thirdState.getMountableOutputCount()).isEqualTo(4);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
  }

  // This test covers the same case with the foreground since the code path is the same!
  @Test
  public void testLayoutOutputUpdateStateWithBackgroundInWithLayout() {
    final Component component1 = new TestLayoutSpecInnerState(false);
    final Component component2 = new TestLayoutSpecInnerState(false);
    final Component component3 = new TestLayoutSpecInnerState(true);

    mLithoViewRule.setRoot(component1).attachToWindow();

    mLithoViewRule.setRootAndSizeSpec(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getLayoutOutput(state.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);

    mLithoViewRule.setRoot(component2);
    LayoutState secondState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getLayoutOutput(secondState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);
    assertThat(getLayoutOutput(secondState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);

    mLithoViewRule.setRoot(component3);
    LayoutState thirdState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
  }

  @Test
  public void testLayoutOutputUpdateStateIdClash() {
    final Component component1 = new TestLayoutWithStateIdClash(false);
    final Component component2 = new TestLayoutWithStateIdClash(true);

    mLithoViewRule.setRoot(component1).attachToWindow();

    mLithoViewRule.setRootAndSizeSpec(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, STATE_UNKNOWN);

    mLithoViewRule.setRoot(component2);
    LayoutState secondState = mLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(6).isEqualTo(secondState.getMountableOutputCount());
    assertThat(STATE_DIRTY)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(0)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(1)).getUpdateState());
    assertThat(STATE_UPDATED)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(2)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(3)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(4)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getLayoutOutput(secondState.getMountableOutputAt(5)).getUpdateState());
  }

  @Test
  public void testDiffTreeUsedIfRootMeasureSpecsAreDifferentButChildHasSame() {
    ComponentContext c = mLithoViewRule.getComponentTree().getContext();
    final TestComponent component = TestDrawableComponent.create(c).color(BLACK).build();

    final Component layoutComponent = new TestSimpleContainerLayout2(component);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            c,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(c).color(BLACK).build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout2(secondComponent);

    calculateLayoutStateWithDiffing(
        c,
        secondLayoutComponent,
        makeSizeSpec(100, SizeSpec.EXACTLY),
        makeSizeSpec(90, SizeSpec.EXACTLY),
        firstLayoutState);

    assertThat(secondComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testDiffTreeUsedIfMeasureSpecsAreSame() {
    ComponentContext c = mLithoViewRule.getComponentTree().getContext();
    final TestComponent component = TestDrawableComponent.create(c).color(BLACK).build();

    final Component layoutComponent = new TestSimpleContainerLayout(component, 0);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            c,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(c).color(BLACK).build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout(secondComponent, 0);

    calculateLayoutStateWithDiffing(
        c,
        secondLayoutComponent,
        makeSizeSpec(100, SizeSpec.EXACTLY),
        makeSizeSpec(100, SizeSpec.EXACTLY),
        firstLayoutState);

    assertThat(secondComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForNestedTreeComponentDelegateWithUndefinedSize() {
    final Component component1 = new TestNestedTreeDelegateWithUndefinedSizeLayout();
    final Component component2 = new TestNestedTreeDelegateWithUndefinedSizeLayout();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevNestedRoot =
        (TestComponent) getLayoutOutput(prevLayoutState.getMountableOutputAt(2)).getComponent();
    assertThat(prevNestedRoot.wasMeasureCalled()).isTrue();

    TestComponent nestedRoot =
        (TestComponent) getLayoutOutput(layoutState.getMountableOutputAt(2)).getComponent();
    assertThat(nestedRoot.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForNestedTreeComponentWithUndefinedSize() {
    final Component component1 = new TestUndefinedSizeLayout();
    final Component component2 = new TestUndefinedSizeLayout();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevMainTreeLeaf =
        (TestComponent) getLayoutOutput(prevLayoutState.getMountableOutputAt(1)).getComponent();
    assertThat(prevMainTreeLeaf.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf1 =
        (TestComponent) getLayoutOutput(prevLayoutState.getMountableOutputAt(3)).getComponent();
    assertThat(prevNestedLeaf1.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf2 =
        (TestComponent) getLayoutOutput(prevLayoutState.getMountableOutputAt(4)).getComponent();
    assertThat(prevNestedLeaf2.wasMeasureCalled()).isTrue();

    TestComponent mainTreeLeaf =
        (TestComponent) getLayoutOutput(layoutState.getMountableOutputAt(1)).getComponent();
    assertThat(mainTreeLeaf.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf1 =
        (TestComponent) getLayoutOutput(layoutState.getMountableOutputAt(3)).getComponent();
    assertThat(nestedLeaf1.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf2 =
        (TestComponent) getLayoutOutput(layoutState.getMountableOutputAt(4)).getComponent();
    assertThat(nestedLeaf2.wasMeasureCalled()).isFalse();
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }

  private static LayoutState calculateLayoutState(
      ComponentContext context, Component component, int widthSpec, int heightSpec) {
    return calculate(
        context,
        component,
        null,
        -1,
        widthSpec,
        heightSpec,
        -1,
        false,
        null,
        LayoutState.CalculateLayoutSource.TEST,
        null);
  }

  private static LayoutState calculateLayoutStateWithDiffing(
      ComponentContext context,
      Component component,
      int widthSpec,
      int heightSpec,
      LayoutState previousLayoutState) {
    return calculate(
        context,
        component,
        null,
        -1,
        widthSpec,
        heightSpec,
        -1,
        true,
        previousLayoutState,
        LayoutState.CalculateLayoutSource.TEST,
        null);
  }

  private static void assertOutputsState(
      LayoutState layoutState, @LayoutOutput.UpdateState int state) {
    assertThat(STATE_DIRTY)
        .isEqualTo(getLayoutOutput(layoutState.getMountableOutputAt(0)).getUpdateState());
    for (int i = 1; i < layoutState.getMountableOutputCount(); i++) {
      LayoutOutput output = getLayoutOutput(layoutState.getMountableOutputAt(i));
      assertThat(state).isEqualTo(output.getUpdateState());
    }
  }

  private static void assertCachedMeasurementsDefined(LithoLayoutResult node) {
    float diffHeight = node.getDiffNode() == null ? -1 : node.getDiffNode().getLastMeasuredHeight();
    float diffWidth = node.getDiffNode() == null ? -1 : node.getDiffNode().getLastMeasuredWidth();
    assertThat(diffHeight != -1).isTrue();
    assertThat(diffWidth != -1).isTrue();
    assertThat(node.areCachedMeasuresValid()).isTrue();
  }

  private static class TestLayoutSpec extends InlineLayoutSpec {
    private final boolean mAddThirdChild;

    TestLayoutSpec(boolean addThirdChild) {
      super();
      this.mAddThirdChild = addThirdChild;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return Column.create(c)
          .child(TestDrawableComponent.create(c))
          .child(Column.create(c).child(TestDrawableComponent.create(c)))
          .child(mAddThirdChild ? TestDrawableComponent.create(c) : null)
          .build();
    }
  }

  private static RenderTreeNode createNode(final Component component) {
    LayoutOutput output =
        new LayoutOutput(
            0,
            component,
            CONTENT,
            null,
            null,
            new Rect(),
            0,
            0,
            0,
            0,
            0,
            0,
            LayoutOutput.STATE_UNKNOWN,
            null);
    return LayoutOutput.create(output, new Rect(), null, null);
  }

  private static class TestSimpleContainerLayout extends InlineLayoutSpec {
    private final Component mComponent;
    private final int mHorizontalPadding;

    TestSimpleContainerLayout(Component component, int horizontalPadding) {
      super();
      mComponent = component;
      mHorizontalPadding = horizontalPadding;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c).paddingPx(HORIZONTAL, mHorizontalPadding).child(mComponent).build();
    }
  }

  private static class TestSimpleContainerLayout2 extends InlineLayoutSpec {
    private final Component mComponent;

    TestSimpleContainerLayout2(Component component) {
      super();
      mComponent = component;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .alignItems(FLEX_START)
          .child(Wrapper.create(c).delegate(mComponent).heightPx(50))
          .build();
    }
  }

  private static class TestLayoutSpecInnerState extends InlineLayoutSpec {
    private final boolean mChangeChildDrawable;

    TestLayoutSpecInnerState(boolean changeChildDrawable) {
      super();
      mChangeChildDrawable = changeChildDrawable;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(sRedDrawable)
          .foregroundRes(btn_default)
          .child(
              TestDrawableComponent.create(c)
                  .background(mChangeChildDrawable ? sRedDrawable : sBlackDrawable))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  private static class TestLayoutSpecBgState extends InlineLayoutSpec {
    private final boolean mChangeBg;

    TestLayoutSpecBgState(boolean changeBg) {
      super();
      mChangeBg = changeBg;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(mChangeBg ? sBlackDrawable : sRedDrawable)
          .foreground(sTransparentDrawable)
          .child(TestDrawableComponent.create(c))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  private static class TestUndefinedSizeLayout extends InlineLayoutSpec {
    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .paddingPx(ALL, 2)
          .child(TestDrawableComponent.create(c, true, true, false))
          .child(
              TestSizeDependentComponent.create(c)
                  .setDelegate(false)
                  .flexShrink(0)
                  .marginPx(ALL, 11))
          .build();
    }
  }

  private static class TestNestedTreeDelegateWithUndefinedSizeLayout extends InlineLayoutSpec {
    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .paddingPx(ALL, 2)
          .child(TestSizeDependentComponent.create(c).setDelegate(true).marginPx(ALL, 11))
          .build();
    }
  }

  private static class TestLayoutWithStateIdClash extends InlineLayoutSpec {
    private final boolean mAddChild;

    TestLayoutWithStateIdClash(boolean addChild) {
      super();
      mAddChild = addChild;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .child(
              create(c)
                  .wrapInView()
                  .child(TestDrawableComponent.create(c))
                  .child(mAddChild ? TestDrawableComponent.create(c) : null))
          .child(create(c).wrapInView().child(TestDrawableComponent.create(c)))
          .build();
    }
  }
}
