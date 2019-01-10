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

import static android.R.drawable.btn_default;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.STATE_DIRTY;
import static com.facebook.litho.LayoutOutput.STATE_UNKNOWN;
import static com.facebook.litho.LayoutOutput.STATE_UPDATED;
import static com.facebook.litho.LayoutState.calculate;
import static com.facebook.litho.LayoutState.createDiffNode;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.getMode;
import static com.facebook.litho.SizeSpec.getSize;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaAlign.FLEX_START;
import static com.facebook.yoga.YogaConstants.UNDEFINED;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static com.facebook.yoga.YogaMeasureOutput.getHeight;
import static com.facebook.yoga.YogaMeasureOutput.getWidth;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.robolectric.RuntimeEnvironment.application;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.util.SparseArrayCompat;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaMeasureFunction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class TreeDiffingTest {

  private static ComparableDrawable sRedDrawable;
  private static ComparableDrawable sBlackDrawable;
  private static ComparableDrawable sTransparentDrawable;

  private int mUnspecifiedSpec;

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
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
            mContext,
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
            mContext,
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

  private InternalNode createInternalNodeForMeasurableComponent(Component component) {
    InternalNode node = LayoutState.createTree(
            component,
            mContext);

    return node;
  }

  private long measureInternalNode(
          InternalNode node,
          float widthConstranint,
          float heightConstraint) {

    final YogaMeasureFunction measureFunc =
            Whitebox.getInternalState(
                    node.mYogaNode,
                    "mMeasureFunction");

    return measureFunc.measure(
            node.mYogaNode,
            widthConstranint,
            EXACTLY,
            heightConstraint,
            EXACTLY);
  }

  @Test
  public void testCachedMeasureFunction() {
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DiffNode();
    diffNode.setLastHeightSpec(mUnspecifiedSpec);
    diffNode.setLastWidthSpec(mUnspecifiedSpec);
    diffNode.setLastMeasuredWidth(10);
    diffNode.setLastMeasuredHeight(5);
    diffNode.setComponent(component);

    node.setCachedMeasuresValid(true);
    node.setDiffNode(diffNode);

    long output = measureInternalNode(
        node,
        UNDEFINED,
        UNDEFINED);

    assertThat(getHeight(output) == (int) diffNode.getLastMeasuredHeight()).isTrue();
    assertThat(getWidth(output) == (int) diffNode.getLastMeasuredWidth()).isTrue();
  }

  @Test
  public void tesLastConstraints() {
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DiffNode();
    diffNode.setLastWidthSpec(makeSizeSpec(10, SizeSpec.EXACTLY));
    diffNode.setLastHeightSpec(makeSizeSpec(5, SizeSpec.EXACTLY));
    diffNode.setLastMeasuredWidth(10f);
    diffNode.setLastMeasuredHeight(5f);
    diffNode.setComponent(component);

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
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    long output = measureInternalNode(
        node,
        UNDEFINED,
        UNDEFINED);

    node.setCachedMeasuresValid(false);
    DiffNode diffNode = createDiffNode(node, null);
    assertThat(getHeight(output) == (int) diffNode.getLastMeasuredHeight()).isTrue();
    assertThat(getWidth(output) == (int) diffNode.getLastMeasuredWidth()).isTrue();
  }

  @Test
  public void testCachedMeasures() {
    final Component component1 = new TestLayoutSpec(false);
    final Component component2 = new TestLayoutSpec(false);

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is consistent.
    DiffNode node = prevLayoutState.getDiffTree();

    InternalNode layoutTreeRoot = LayoutState.createTree(
            component2,
            mContext);
    LayoutState.applyDiffNodeToUnchangedNodes(layoutTreeRoot, node);
    checkAllComponentsHaveMeasureCache(layoutTreeRoot);
  }

  @Test
  public void testPartiallyCachedMeasures() {
    final Component component1 = new TestLayoutSpec(false);
    final Component component2 = new TestLayoutSpec(true);

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is consistent.
    DiffNode node = prevLayoutState.getDiffTree();

    InternalNode layoutTreeRoot = LayoutState.createTree(
            component2,
            mContext);
    LayoutState.applyDiffNodeToUnchangedNodes(layoutTreeRoot, node);
    InternalNode child_1 = (InternalNode) layoutTreeRoot.getChildAt(0);
    assertCachedMeasurementsDefined(child_1);

    InternalNode child_2 = (InternalNode) layoutTreeRoot.getChildAt(1);
    assertCachedMeasurementsNotDefined(child_2);
    InternalNode child_3 = (InternalNode) child_2.getChildAt(0);
    assertCachedMeasurementsDefined(child_3);

    InternalNode child_4 = (InternalNode) layoutTreeRoot.getChildAt(2);
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
            mContext,
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(prevLayoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getId()).isEqualTo(prevLayoutState.getMountableOutputAt(i).getId());
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
            mContext,
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);
    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component2,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertNotEquals(
            prevLayoutState.getMountableOutputCount(),
            layoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getId()).isEqualTo(prevLayoutState.getMountableOutputAt(i).getId());
    }
  }

  private void assertCachedMeasurementsNotDefined(InternalNode node) {
    assertThat(node.areCachedMeasuresValid()).isFalse();
  }

  private void checkAllComponentsHaveMeasureCache(InternalNode node) {
    if (node.getRootComponent() != null && node.getRootComponent().canMeasure()) {
      assertCachedMeasurementsDefined(node);
    }
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      checkAllComponentsHaveMeasureCache((InternalNode) node.getChildAt(i));
    }
  }

  @Test
  public void testComponentHostMoveItem() {
    ComponentHost hostHolder = new ComponentHost(mContext);
    MountItem mountItem = new MountItem();
    MountItem mountItem1 = new MountItem();
    MountItem mountItem2 = new MountItem();
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
    ComponentHost hostHolder = new ComponentHost(mContext);
    MountItem mountItem = new MountItem();
    MountItem mountItem1 = new MountItem();
    MountItem mountItem2 = new MountItem();
    hostHolder.mount(0, mountItem, new Rect());
    hostHolder.mount(1, mountItem1, new Rect());
    hostHolder.mount(2, mountItem2, new Rect());
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(2));
    hostHolder.moveItem(mountItem2, 2, 0);
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));

    assertThat(1).isEqualTo(((SparseArrayCompat<MountItem>)
        getInternalState(hostHolder, "mScrapMountItemsArray")).size());

    hostHolder.unmount(0, mountItem);

    assertThat(2).isEqualTo(((SparseArrayCompat<MountItem>)
        getInternalState(hostHolder, "mMountItems")).size());
    assertThat((Object) getInternalState(hostHolder, "mScrapMountItemsArray"))
        .isNull();
  }

  @Test
  public void testLayoutOutputUpdateState() {
    final Component firstComponent = TestDrawableComponent.create(mContext)
            .color(Color.BLACK)
            .build();
    final Component secondComponent = TestDrawableComponent.create(mContext)
            .color(Color.BLACK)
            .build();
    final Component thirdComponent = TestDrawableComponent.create(mContext)
            .color(Color.WHITE)
            .build();

    ComponentTree componentTree = ComponentTree.create(mContext, firstComponent).build();
    LayoutState state =
        componentTree.calculateLayoutState(
            mContext,
            firstComponent,
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            null,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertOutputsState(state, LayoutOutput.STATE_UNKNOWN);

    LayoutState secondState =
        componentTree.calculateLayoutState(
            mContext,
            secondComponent,
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            state,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertOutputsState(secondState, LayoutOutput.STATE_UPDATED);

    LayoutState thirdState =
        componentTree.calculateLayoutState(
            mContext,
            thirdComponent,
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            secondState,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertOutputsState(thirdState, LayoutOutput.STATE_DIRTY);
  }

  @Test
  public void testLayoutOutputUpdateStateWithBackground() {
    final Component component1 = new TestLayoutSpecBgState(false);
    final Component component2 = new TestLayoutSpecBgState(false);
    final Component component3 = new TestLayoutSpecBgState(true);

    ComponentTree componentTree = ComponentTree.create(mContext, component1)
        .build();
    LayoutState state =
        componentTree.calculateLayoutState(
            mContext,
            component1,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            null,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertOutputsState(state, STATE_UNKNOWN);

    LayoutState secondState =
        componentTree.calculateLayoutState(
            mContext,
            component2,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            state,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(5).isEqualTo(secondState.getMountableOutputCount());
    assertOutputsState(secondState, STATE_UPDATED);

    LayoutState thirdState =
        componentTree.calculateLayoutState(
            mContext,
            component3,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            secondState,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(5).isEqualTo(thirdState.getMountableOutputCount());
    assertThat(thirdState.getMountableOutputAt(1).getUpdateState()).isEqualTo(STATE_DIRTY);
    assertThat(thirdState.getMountableOutputAt(2).getUpdateState()).isEqualTo(STATE_UPDATED);
    assertThat(thirdState.getMountableOutputAt(3).getUpdateState()).isEqualTo(STATE_UPDATED);
    assertThat(thirdState.getMountableOutputAt(4).getUpdateState()).isEqualTo(STATE_UPDATED);
  }

  // This test covers the same case with the foreground since the code path is the same!
  @Test
  public void testLayoutOutputUpdateStateWithBackgroundInWithLayout() {
    final Component component1 = new TestLayoutSpecInnerState(false);
    final Component component2 = new TestLayoutSpecInnerState(false);
    final Component component3 = new TestLayoutSpecInnerState(true);

    ComponentTree componentTree = ComponentTree.create(mContext, component1)
        .build();
    LayoutState state =
        componentTree.calculateLayoutState(
            mContext,
            component1,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            null,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(state.getMountableOutputAt(2).getUpdateState()).isEqualTo(STATE_UNKNOWN);

    LayoutState secondState =
        componentTree.calculateLayoutState(
            mContext,
            component2,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            state,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(secondState.getMountableOutputAt(2).getUpdateState()).isEqualTo(STATE_UPDATED);

    LayoutState thirdState =
        componentTree.calculateLayoutState(
            mContext,
            component3,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            secondState,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(thirdState.getMountableOutputAt(2).getUpdateState()).isEqualTo(STATE_DIRTY);
  }

  @Test
  public void testLayoutOutputUpdateStateIdClash() {
    final Component component1 = new TestLayoutWithStateIdClash(false);
    final Component component2 = new TestLayoutWithStateIdClash(true);

    ComponentTree componentTree = ComponentTree.create(mContext, component1)
        .build();
    LayoutState state =
        componentTree.calculateLayoutState(
            mContext,
            component1,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            null,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertOutputsState(state, STATE_UNKNOWN);

    LayoutState secondState =
        componentTree.calculateLayoutState(
            mContext,
            component2,
            makeSizeSpec(10, SizeSpec.EXACTLY),
            makeSizeSpec(10, SizeSpec.EXACTLY),
            true,
            state,
            null,
            LayoutState.CalculateLayoutSource.TEST,
            null);

    assertThat(6).isEqualTo(secondState.getMountableOutputCount());
    assertThat(STATE_DIRTY).isEqualTo(secondState.getMountableOutputAt(0).getUpdateState());
    assertThat(STATE_UNKNOWN).isEqualTo(secondState.getMountableOutputAt(1).getUpdateState());
    assertThat(STATE_UPDATED).isEqualTo(secondState.getMountableOutputAt(2).getUpdateState());
    assertThat(STATE_UNKNOWN).isEqualTo(secondState.getMountableOutputAt(3).getUpdateState());
    assertThat(STATE_UNKNOWN).isEqualTo(secondState.getMountableOutputAt(4).getUpdateState());
    assertThat(STATE_UNKNOWN).isEqualTo(secondState.getMountableOutputAt(5).getUpdateState());
  }

  @Test
  public void testDiffTreeUsedIfRootMeasureSpecsAreDifferentButChildHasSame() {
    final TestComponent component = TestDrawableComponent.create(mContext)
        .color(BLACK)
        .build();

    final Component layoutComponent = new TestSimpleContainerLayout2(component);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(mContext)
        .color(BLACK)
        .build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout2(secondComponent);

    calculateLayoutStateWithDiffing(
        mContext,
        secondLayoutComponent,
        makeSizeSpec(100, SizeSpec.EXACTLY),
        makeSizeSpec(90, SizeSpec.EXACTLY),
        firstLayoutState);

    assertThat(secondComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testDiffTreeUsedIfMeasureSpecsAreSame() {
    final TestComponent component = TestDrawableComponent.create(mContext)
        .color(BLACK)
        .build();

    final Component layoutComponent = new TestSimpleContainerLayout(component, 0);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(mContext)
        .color(BLACK)
        .build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout(secondComponent, 0);

    calculateLayoutStateWithDiffing(
        mContext,
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
            mContext,
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevNestedRoot =
        (TestComponent) prevLayoutState.getMountableOutputAt(2).getComponent();
    assertThat(prevNestedRoot.wasMeasureCalled()).isTrue();

    TestComponent nestedRoot = (TestComponent) layoutState.getMountableOutputAt(2).getComponent();
    assertThat(nestedRoot.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForNestedTreeComponentWithUndefinedSize() {
    final Component component1 = new TestUndefinedSizeLayout();
    final Component component2 = new TestUndefinedSizeLayout();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mContext,
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevMainTreeLeaf =
        (TestComponent) prevLayoutState.getMountableOutputAt(1).getComponent();
    assertThat(prevMainTreeLeaf.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf1 =
        (TestComponent) prevLayoutState.getMountableOutputAt(3).getComponent();
    assertThat(prevNestedLeaf1.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf2 =
        (TestComponent) prevLayoutState.getMountableOutputAt(4).getComponent();
    assertThat(prevNestedLeaf2.wasMeasureCalled()).isTrue();

    TestComponent mainTreeLeaf = (TestComponent) layoutState.getMountableOutputAt(1).getComponent();
    assertThat(mainTreeLeaf.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf1 = (TestComponent) layoutState.getMountableOutputAt(3).getComponent();
    assertThat(nestedLeaf1.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf2 = (TestComponent) layoutState.getMountableOutputAt(4).getComponent();
    assertThat(nestedLeaf2.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForCachedLayoutSpecWithMeasure() {
    final ComponentContext c = new ComponentContext(application);
    final int widthSpecContainer = makeSizeSpec(300, SizeSpec.EXACTLY);
    final int heightSpec = makeSizeSpec(40, AT_MOST);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent = makeSizeSpec(
        getSize(widthSpecContainer) - horizontalPadding - horizontalPadding,
        SizeSpec.EXACTLY);

    final Component sizeDependentComponentSpy1 = spy(
        TestSizeDependentComponent.create(c)
            .setFixSizes(false)
            .setDelegate(false)
            .build());
    Size sizeOutput1 = new Size();

    sizeDependentComponentSpy1.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput1);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer1 =
        new TestSimpleContainerLayout(sizeDependentComponentSpy1, horizontalPadding);


    final Component sizeDependentComponentSpy2 = spy(
        TestSizeDependentComponent.create(c)
            .setFixSizes(false)
            .setDelegate(false)
            .build());
    Size sizeOutput2 = new Size();
    sizeDependentComponentSpy1.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput2);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer2 =
        new TestSimpleContainerLayout(sizeDependentComponentSpy2, horizontalPadding);

    // Reset the release/clear counts before issuing calculate().
    reset(sizeDependentComponentSpy1);
    doReturn(sizeDependentComponentSpy1).when(sizeDependentComponentSpy1).makeShallowCopy();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mContext, rootContainer1, widthSpecContainer, heightSpec, null);

    // Make sure we reused the cached layout and it wasn't released.
    verify(sizeDependentComponentSpy1, never()).releaseCachedLayout();
    verify(sizeDependentComponentSpy1, times(1)).clearCachedLayout();

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mContext, rootContainer2, widthSpecContainer, heightSpec, prevLayoutState);

    verify(sizeDependentComponentSpy1).makeShallowCopy();

    // Make sure we reused the cached layout and it wasn't released.
    verify(sizeDependentComponentSpy2, never()).releaseCachedLayout();
    verify(sizeDependentComponentSpy2, never()).clearCachedLayout();

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevNestedLeaf1 =
        (TestComponent) prevLayoutState.getMountableOutputAt(2).getComponent();
    assertThat(prevNestedLeaf1.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf2 =
        (TestComponent) prevLayoutState.getMountableOutputAt(3).getComponent();
    assertThat(prevNestedLeaf2.wasMeasureCalled()).isTrue();

    TestComponent nestedLeaf1 = (TestComponent) layoutState.getMountableOutputAt(2).getComponent();
    assertThat(nestedLeaf1.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf2 = (TestComponent) layoutState.getMountableOutputAt(3).getComponent();
    assertThat(nestedLeaf2.wasMeasureCalled()).isFalse();
  }

  private static LayoutState calculateLayoutState(
      ComponentContext context, Component component, int widthSpec, int heightSpec) {
    return calculate(
        context,
        component,
        -1,
        widthSpec,
        heightSpec,
        false,
        null,
        LayoutState.CalculateLayoutSource.TEST,
        null,
        LayoutState.NodeTreePersistenceMode.NONE);
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
        -1,
        widthSpec,
        heightSpec,
        true,
        previousLayoutState,
        LayoutState.CalculateLayoutSource.TEST,
        null,
        LayoutState.NodeTreePersistenceMode.NONE);
  }

  private static void assertOutputsState(
          LayoutState layoutState,
          @LayoutOutput.UpdateState int state) {
    assertThat(STATE_DIRTY).isEqualTo(layoutState.getMountableOutputAt(0).getUpdateState());
    for (int i = 1; i < layoutState.getMountableOutputCount(); i++) {
      LayoutOutput output = layoutState.getMountableOutputAt(i);
      assertThat(state).isEqualTo(output.getUpdateState());
    }
  }

  private static void assertCachedMeasurementsDefined(InternalNode node) {
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
          .child(TestDrawableComponent.create(c, false, true, true, false, false))
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
          .child(
              TestSizeDependentComponent.create(c).setDelegate(true).marginPx(ALL, 11))
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
