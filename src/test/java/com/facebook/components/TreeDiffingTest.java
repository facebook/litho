/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.util.SparseArrayCompat;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestSizeDependentComponent;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PrepareForTest(Component.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class TreeDiffingTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private int mUnspecifiedSpec;

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mUnspecifiedSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void testDiffTreeDisabled() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        false,
        null);

    // Check diff tree is null.
    assertNull(layoutState.getDiffTree());
  }

  @Test
  public void testDiffTreeEnabled() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        null);

    // Check diff tree is not null and consistent.
    DiffNode node = layoutState.getDiffTree();
    assertNotNull(node);
    assertEquals(countNodes(node), 4);
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
        YogaConstants.UNDEFINED,
        YogaConstants.UNDEFINED);

    assertTrue(YogaMeasureOutput.getHeight(output) == (int) diffNode.getLastMeasuredHeight());
    assertTrue(YogaMeasureOutput.getWidth(output) == (int) diffNode.getLastMeasuredWidth());
  }

  @Test
  public void tesLastConstraints() {
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DiffNode();
    diffNode.setLastWidthSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY));
    diffNode.setLastHeightSpec(SizeSpec.makeSizeSpec(5, SizeSpec.EXACTLY));
    diffNode.setLastMeasuredWidth(10f);
    diffNode.setLastMeasuredHeight(5f);
    diffNode.setComponent(component);

    node.setCachedMeasuresValid(true);
    node.setDiffNode(diffNode);

    long output = measureInternalNode(node, 10f, 5f);

    assertTrue(YogaMeasureOutput.getHeight(output) == (int) diffNode.getLastMeasuredHeight());
    assertTrue(YogaMeasureOutput.getWidth(output) == (int) diffNode.getLastMeasuredWidth());

    int lastWidthSpec = node.getLastWidthSpec();
    int lastHeightSpec = node.getLastHeightSpec();

    assertTrue(SizeSpec.getMode(lastWidthSpec) == SizeSpec.EXACTLY);
    assertTrue(SizeSpec.getMode(lastHeightSpec) == SizeSpec.EXACTLY);
    assertTrue(SizeSpec.getSize(lastWidthSpec) == 10);
    assertTrue(SizeSpec.getSize(lastHeightSpec) == 5);
  }

  @Test
  public void measureAndCreateDiffNode() {
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    long output = measureInternalNode(
        node,
        YogaConstants.UNDEFINED,
        YogaConstants.UNDEFINED);

    node.setCachedMeasuresValid(false);
    DiffNode diffNode = LayoutState.createDiffNode(node, null);
    assertTrue(YogaMeasureOutput.getHeight(output) == (int) diffNode.getLastMeasuredHeight());
    assertTrue(YogaMeasureOutput.getWidth(output) == (int) diffNode.getLastMeasuredWidth());
  }

  @Test
  public void testCachedMeasures() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState prevLayoutState = LayoutState.calculate(
        mContext,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
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
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState prevLayoutState = LayoutState.calculate(
        mContext,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
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
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState prevLayoutState = LayoutState.calculate(
        mContext,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        null);

    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component2,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        prevLayoutState.getDiffTree());

    assertEquals(prevLayoutState.getMountableOutputCount(), layoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertEquals(
          prevLayoutState.getMountableOutputAt(i).getId(),
          layoutState.getMountableOutputAt(i).getId());
    }
  }

  @Test
  public void testLayoutOutputPartialReuse() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState prevLayoutState = LayoutState.calculate(
        mContext,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        null);
    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component2,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        prevLayoutState.getDiffTree());

    assertNotEquals(
        prevLayoutState.getMountableOutputCount(),
        layoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertEquals(
          prevLayoutState.getMountableOutputAt(i).getId(),
          layoutState.getMountableOutputAt(i).getId());
    }
  }

  private void assertCachedMeasurementsNotDefined(InternalNode node) {
    assertFalse(node.areCachedMeasuresValid());
  }

  private void checkAllComponentsHaveMeasureCache(InternalNode node) {
    if (node.getComponent() != null && node.getComponent().getLifecycle().canMeasure()) {
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
    assertEquals(hostHolder.getMountItemAt(0), mountItem);
    assertEquals(hostHolder.getMountItemAt(1), mountItem1);
    assertEquals(hostHolder.getMountItemAt(2), mountItem2);
    hostHolder.moveItem(mountItem, 0, 2);
    hostHolder.moveItem(mountItem2, 2, 0);
    assertEquals(hostHolder.getMountItemAt(0), mountItem2);
    assertEquals(hostHolder.getMountItemAt(1), mountItem1);
    assertEquals(hostHolder.getMountItemAt(2), mountItem);
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
    assertEquals(hostHolder.getMountItemAt(0), mountItem);
    assertEquals(hostHolder.getMountItemAt(1), mountItem1);
    assertEquals(hostHolder.getMountItemAt(2), mountItem2);
    hostHolder.moveItem(mountItem2, 2, 0);
    assertEquals(hostHolder.getMountItemAt(0), mountItem2);
    assertEquals(hostHolder.getMountItemAt(1), mountItem1);

    assertEquals(
        ((SparseArrayCompat<MountItem>) Whitebox
            .getInternalState(hostHolder, "mScrapMountItemsArray")).size(),
        1);

    hostHolder.unmount(0, mountItem);

    assertEquals(
        ((SparseArrayCompat<MountItem>) Whitebox
            .getInternalState(hostHolder, "mMountItems")).size(),
        2);
    assertNull(Whitebox.getInternalState(hostHolder, "mScrapMountItemsArray"));
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

