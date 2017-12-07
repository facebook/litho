/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.facebook.litho.ComponentLifecycle.MountType;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
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

/**
 * Tests {@link ComponentLifecycle}
 */
@PrepareForTest({
    InternalNode.class,
    DiffNode.class,
    LayoutState.class,
    ComponentsPools.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class ComponentLifecycleTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int A_HEIGHT = 11;
  private static final int A_WIDTH = 12;
  private int mNestedTreeWidthSpec;
  private int mNestedTreeHeightSpec;

  private InternalNode mNode;
  private DiffNode mDiffNode;
  private ComponentContext mContext;
  private Component mComponentWithNullLayout;

  @Before
  public void setUp() {
    mDiffNode = mock(DiffNode.class);
    mNode = mock(InternalNode.class);
    final YogaNode cssNode = new YogaNode();
    cssNode.setData(mNode);
    mNode.mYogaNode = cssNode;

    mockStatic(ComponentsPools.class);

    when(mNode.getLastWidthSpec()).thenReturn(-1);
    when(mNode.getDiffNode()).thenReturn(mDiffNode);
    when(mDiffNode.getLastMeasuredWidth()).thenReturn(-1f);
    when(mDiffNode.getLastMeasuredHeight()).thenReturn(-1f);
    when(ComponentsPools.acquireInternalNode(any(ComponentContext.class))).thenReturn(mNode);

    mockStatic(LayoutState.class);
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponentWithNullLayout = new InlineLayoutSpec() {
      @Override protected ComponentLayout onCreateLayout(ComponentContext c) { return null; }
    };
    mNestedTreeWidthSpec = SizeSpec.makeSizeSpec(400, SizeSpec.EXACTLY);
    mNestedTreeHeightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, mComponentWithNullLayout, false);

    verify(component).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(component).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, mComponentWithNullLayout, false);

    verify(component).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(component).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, mComponentWithNullLayout, false);

    verify(component).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(component).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, mComponentWithNullLayout, false);

    verify(component).onCreateLayout(mContext, mComponentWithNullLayout);
    verify(mNode).appendComponent(mComponentWithNullLayout);
    verify(component).onPrepare(mContext, mComponentWithNullLayout);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, component, true);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, component, false);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, component, true);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, component, false);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, component, true);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, component, false);

    verify(component).onCreateLayout(mContext, component);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    mContext.setWidthSpec(mNestedTreeWidthSpec);
    mContext.setHeightSpec(mNestedTreeHeightSpec);
    component.createLayout(mContext, component, true);

    verify(component).onCreateLayoutWithSizeSpec(
        mContext,
        mNestedTreeWidthSpec,
        mNestedTreeHeightSpec,
        component);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext, component);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, component, false);

    PowerMockito.verifyStatic();
    // Calling here to verify static call.
    ComponentsPools.acquireInternalNode(mContext);
    verify(component, never()).onCreateLayout(
        any(ComponentContext.class),
        any(Component.class));
    verify(component, never()).onCreateLayoutWithSizeSpec(
        any(ComponentContext.class),
        anyInt(),
        anyInt(),
        any(Component.class));
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component, never())
        .onPrepare(any(ComponentContext.class), any(Component.class));
  }

  @Test
  public void testOnMeasureNotOverriden() {
    Component component = setUpSpyComponentForCreateLayout(true, true);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    try {
      measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);
      fail();
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("canMeasure()");
    }
  }

  @Test
  public void testMountSpecYogaMeasureOutputNotSet() {
    Component component = new TestMountSpecWithEmptyOnMeasure(mNode);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    try {
      measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);
      fail();
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("MeasureOutput not set");
    }
  }

  @Test
  public void testMountSpecYogaMeasureOutputSet() {
    Component component = new TestMountSpecSettingSizesInOnMeasure(mNode);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    long output = measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(A_WIDTH);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(A_HEIGHT);
  }

  @Test
  public void testLayoutSpecMeasureResolveNestedTree() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */, true /* canMeasure */);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    final int nestedTreeWidth = 20;
    final int nestedTreeHeight = 25;
    InternalNode nestedTree = mock(InternalNode.class);
    when(nestedTree.getWidth()).thenReturn(nestedTreeWidth);
    when(nestedTree.getHeight()).thenReturn(nestedTreeHeight);

    when(LayoutState.resolveNestedTree(eq(mNode), anyInt(), anyInt())).thenReturn(nestedTree);

    long output = measureFunction.measure(mNode.mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic();
    LayoutState.resolveNestedTree(eq(mNode), anyInt(), anyInt());

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(nestedTreeWidth);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(nestedTreeHeight);
  }

  private Component setUpSpyComponentForCreateLayout(
      boolean isMountSpec,
      boolean canMeasure) {
    Component component = new TestBaseComponent(
        canMeasure,
        isMountSpec ? MountType.DRAWABLE : MountType.NONE,
        mNode);

    return spy(component);
  }

  private YogaMeasureFunction getMeasureFunction(Component component) {
    when(mNode.getRootComponent()).thenReturn(component);

    return Whitebox.getInternalState(ComponentLifecycle.class, "sMeasureFunction");
  }

  private static class TestBaseComponent extends Component {

    private final boolean mCanMeasure;
    private final MountType mMountType;
    private final InternalNode mNode;

    TestBaseComponent(boolean canMeasure, MountType mountType, InternalNode node) {
      mCanMeasure = canMeasure;
      mMountType = mountType;
      mNode = node;
    }

    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component input) {
      return mNode;
    }

    @Override
    protected ComponentLayout onCreateLayoutWithSizeSpec(
        ComponentContext c, int widthSpec, int heightSpec, Component component) {
      return mNode;
    }

    @Override
    protected boolean canMeasure() {
      return mCanMeasure;
    }

    @Override
    public MountType getMountType() {
      return mMountType;
    }

    @Override
    public String getSimpleName() {
      return "TestLifecycleComponentBase";
    }
  }

  private static class TestMountSpecWithEmptyOnMeasure extends TestBaseComponent {

    TestMountSpecWithEmptyOnMeasure(InternalNode node) {
      super(true, MountType.DRAWABLE, node);
    }

    @Override
    protected void onMeasure(
        ComponentContext c,
        ActualComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Component component) {}
  }

  private static class TestMountSpecSettingSizesInOnMeasure
      extends TestMountSpecWithEmptyOnMeasure {

    TestMountSpecSettingSizesInOnMeasure(InternalNode node) {
      super(node);
    }

    @Override
    protected void onMeasure(
        ComponentContext context,
        ActualComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Component component) {
      size.width = A_WIDTH;
      size.height = A_HEIGHT;
    }
  }
}
