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
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.ComponentLifecycle.MountType;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
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

/** Tests {@link ComponentLifecycle} */
@PrepareForTest({
  DefaultInternalNode.class,
  DiffNode.class,
  LayoutState.class,
  InternalNodeUtils.class
})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class ComponentLifecycleTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int A_HEIGHT = 11;
  private static final int A_WIDTH = 12;
  private int mNestedTreeWidthSpec;
  private int mNestedTreeHeightSpec;

  private DefaultInternalNode mNode;
  private YogaNode mYogaNode;
  private DiffNode mDiffNode;
  private ComponentContext mContext;
  private boolean mPreviousOnErrorConfig;

  @Before
  public void setUp() {
    mockStatic(LayoutState.class);
    mockStatic(DefaultInternalNode.class);
    mockStatic(InternalNodeUtils.class);

    mDiffNode = mock(DiffNode.class);
    mNode = mock(DefaultInternalNode.class);
    mYogaNode = YogaNode.create();
    mYogaNode.setData(mNode);

    when(mNode.getLastWidthSpec()).thenReturn(-1);
    when(mNode.getDiffNode()).thenReturn(mDiffNode);
    when(mDiffNode.getLastMeasuredWidth()).thenReturn(-1f);
    when(mDiffNode.getLastMeasuredHeight()).thenReturn(-1f);
    when(InternalNodeUtils.create(any(ComponentContext.class))).thenReturn(mNode);

    StateHandler stateHandler = mock(StateHandler.class);
    mContext = spy(new ComponentContext(RuntimeEnvironment.application, stateHandler));

    mNestedTreeWidthSpec = SizeSpec.makeSizeSpec(400, SizeSpec.EXACTLY);
    mNestedTreeHeightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(component, never()).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(component, never()).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(component, never()).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(component, never()).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, true);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, true);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        true /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, true);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        false /* canMeasure */);
    component.createLayout(mContext, false);

    verify(component).onCreateLayout(mContext);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    mContext.setWidthSpec(mNestedTreeWidthSpec);
    mContext.setHeightSpec(mNestedTreeHeightSpec);
    component.createLayout(mContext, true);

    verify(component).onCreateLayoutWithSizeSpec(
        mContext,
        mNestedTreeWidthSpec,
        mNestedTreeHeightSpec);
    verify(mNode).appendComponent(component);
    verify(mNode, never()).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component).onPrepare(mContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component = setUpSpyComponentForCreateLayout(
        false /* isMountSpec */,
        true /* canMeasure */);
    component.createLayout(mContext, false);

    PowerMockito.verifyStatic();
    // Calling here to verify static call.
    InternalNodeUtils.create(mContext);
    verify(component, never()).onCreateLayout(
        any(ComponentContext.class));
    verify(component, never()).onCreateLayoutWithSizeSpec(
        any(ComponentContext.class),
        anyInt(),
        anyInt());
    verify(mNode).appendComponent(component);
    verify(mNode).setMeasureFunction(any(YogaMeasureFunction.class));
    verify(component, never())
        .onPrepare(any(ComponentContext.class));
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_FirstCall() {
    ComponentsConfiguration.isReconciliationEnabled = true;
    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = true;

    Component component;

    component =
        new SpyComponentBuilder()
            .setNode(mNode)
            .canMeasure(true)
            .isMountSpec(false)
            .hasState(true)
            .isLayoutSpecWithSizeSpecCheck(true)
            .build(mContext);

    component.createLayout(mContext, true);

    // onShouldCreateLayoutWithNewSizeSpec should not be called the first time
    verify(component, never())
        .onShouldCreateLayoutWithNewSizeSpec(any(ComponentContext.class), anyInt(), anyInt());
    verify(component)
        .onCreateLayoutWithSizeSpec(mContext, mContext.getWidthSpec(), mContext.getHeightSpec());

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
    ComponentsConfiguration.isReconciliationEnabled = false;
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_shouldUseCache() {
    ComponentsConfiguration.isReconciliationEnabled = true;
    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = true;

    Component component;
    InternalNode holder = mock(InternalNode.class);
    InternalNode resolved = mock(InternalNode.class);

    component =
        new SpyComponentBuilder()
            .setNode(mNode)
            .canMeasure(true)
            .isMountSpec(false)
            .isLayoutSpecWithSizeSpecCheck(true)
            .hasState(true)
            .build(mContext);

    when(holder.getTailComponent()).thenReturn(component);

    when(LayoutState.resolveNestedTree(mContext, holder, 100, 100)).thenCallRealMethod();
    when(LayoutState.createAndMeasureTreeForComponent(
            mContext, component, 100, 100, holder, null, null, null))
        .thenReturn(resolved);

    // call resolve nested tree 1st time
    InternalNode result = LayoutState.resolveNestedTree(mContext, holder, 100, 100);

    PowerMockito.verifyStatic();

    // it should call create and measure
    LayoutState.createAndMeasureTreeForComponent(mContext, component, 100, 100, holder, null, null, null);

    // should return nested tree next time
    when(holder.getNestedTree()).thenReturn(result);

    // should use previous layout in next call
    doReturn(true).when(component).canUsePreviousLayout(any(ComponentContext.class));

    // call resolve nested tree 1st time
    LayoutState.resolveNestedTree(mContext, holder, 100, 100);

    // no new invocation of create
    PowerMockito.verifyStatic(times(1));
    LayoutState.createAndMeasureTreeForComponent(mContext, component, 100, 100, holder, null, null, null);

    // should only measure
    PowerMockito.verifyStatic(times(1));
    LayoutState.remeasureTree(resolved, 100, 100);

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
    ComponentsConfiguration.isReconciliationEnabled = false;
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_shouldNotUseCache() {
    ComponentsConfiguration.isReconciliationEnabled = true;
    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = true;

    Component component;
    InternalNode holder = mock(InternalNode.class);
    InternalNode resolved = mock(InternalNode.class);

    component =
        new SpyComponentBuilder()
            .setNode(mNode)
            .canMeasure(true)
            .isMountSpec(false)
            .isLayoutSpecWithSizeSpecCheck(true)
            .hasState(true)
            .build(mContext);

    when(holder.getTailComponent()).thenReturn(component);

    when(LayoutState.resolveNestedTree(mContext, holder, 100, 100)).thenCallRealMethod();
    when(LayoutState.createAndMeasureTreeForComponent(
            mContext, component, 100, 100, holder, null, null, null))
        .thenReturn(resolved);

    // call resolve nested tree 1st time
    InternalNode result = LayoutState.resolveNestedTree(mContext, holder, 100, 100);

    PowerMockito.verifyStatic();

    // it should call create and measure
    LayoutState.createAndMeasureTreeForComponent(mContext, component, 100, 100, holder, null, null, null);

    // should return nested tree next time
    when(holder.getNestedTree()).thenReturn(result);

    // should use previous layout in next call
    doReturn(false).when(component).canUsePreviousLayout(any(ComponentContext.class));

    // call resolve nested tree 1st time
    LayoutState.resolveNestedTree(mContext, holder, 100, 100);

    // a new invocation of create
    PowerMockito.verifyStatic(times(2));
    LayoutState.createAndMeasureTreeForComponent(mContext, component, 100, 100, holder, null, null, null);

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
    ComponentsConfiguration.isReconciliationEnabled = false;
  }

  @Test
  public void testOnMeasureNotOverriden() {
    Component component = setUpSpyComponentForCreateLayout(true, true);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    try {
      measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);
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
      measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);
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

    long output = measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);

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
    when(LayoutState.resolveNestedTree(eq(mContext), eq(mNode), anyInt(), anyInt()))
        .thenReturn(nestedTree);
    when(mNode.getContext()).thenReturn(mContext);

    long output = measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic();
    LayoutState.resolveNestedTree(eq(mContext), eq(mNode), anyInt(), anyInt());

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(nestedTreeWidth);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(nestedTreeHeight);
  }

  @Test
  public void testLayoutSpecMeasureResolveNestedTree_withExperiment() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    final int nestedTreeWidth = 20;
    final int nestedTreeHeight = 25;
    InternalNode nestedTree = mock(InternalNode.class);
    when(nestedTree.getWidth()).thenReturn(nestedTreeWidth);
    when(nestedTree.getHeight()).thenReturn(nestedTreeHeight);
    when(LayoutState.resolveNestedTree(eq(mContext), eq(mNode), anyInt(), anyInt()))
        .thenReturn(nestedTree);
    when(mNode.getContext()).thenReturn(mContext);
    when(mContext.isReconciliationEnabled()).thenReturn(true);
    when(mNode.getParent()).thenReturn(mNode);

    when(mNode.getContext()).thenReturn(mContext);
    long output = measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic();
    LayoutState.resolveNestedTree(eq(mContext), eq(mNode), anyInt(), anyInt());

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(nestedTreeWidth);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(nestedTreeHeight);
  }

  private Component setUpSpyComponentForCreateLayout(
      boolean isMountSpec,
      boolean canMeasure) {

    return new SpyComponentBuilder()
        .isMountSpec(isMountSpec)
        .setNode(mNode)
        .canMeasure(canMeasure)
        .build(mContext);
  }

  private Component setUpSpyLayoutSpecWithNullLayout() {
    Component component =
        spy(new TestBaseComponent(false, MountType.NONE, null, false, mPreviousOnErrorConfig));

    when(component.getScopedContext()).thenReturn(mContext);

    return component;
  }

  private YogaMeasureFunction getMeasureFunction(Component component) {
    when(mNode.getTailComponent()).thenReturn(component);

    return Whitebox.getInternalState(ComponentLifecycle.class, "sMeasureFunction");
  }

  private static Component createSpyComponent(
      ComponentContext context, TestBaseComponent component) {
    Component spy = spy(component);
    when(spy.getScopedContext()).thenReturn(context);
    return spy;
  }

  @OkToExtend
  private static class TestBaseComponent extends Component {

    private final boolean mCanMeasure;
    private final MountType mMountType;
    private final InternalNode mNode;
    private final boolean mIsLayoutSpecWithSizeSpecCheck;
    private final boolean mHasState;

    TestBaseComponent(
        boolean canMeasure,
        MountType mountType,
        InternalNode node,
        boolean isLayoutSpecWithSizeSpecCheck,
        boolean hasState) {
      super("TestBaseComponent");
      mCanMeasure = canMeasure;
      mMountType = mountType;
      mNode = node;
      mIsLayoutSpecWithSizeSpecCheck = isLayoutSpecWithSizeSpecCheck;
      mHasState = hasState;
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return this == other;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return this;
    }

    @Override
    protected Component onCreateLayoutWithSizeSpec(
        ComponentContext c, int widthSpec, int heightSpec) {
      return this;
    }

    @Override
    protected ComponentLayout resolve(ComponentContext c) {
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
    protected boolean hasState() {
      return mHasState;
    }

    @Override
    protected boolean isLayoutSpecWithSizeSpecCheck() {
      return mIsLayoutSpecWithSizeSpecCheck;
    }

    @Override
    protected boolean canUsePreviousLayout(ComponentContext context) {
      return super.canUsePreviousLayout(context);
    }
  }

  static class SpyComponentBuilder {
    private boolean mCanMeasure = false;
    private MountType mMountType = MountType.NONE;
    private InternalNode mNode = null;
    private boolean mIsLayoutSpecWithSizeSpecCheck = false;
    private boolean mHasState = false;

    SpyComponentBuilder canMeasure(boolean canMeasure) {
      this.mCanMeasure = canMeasure;
      return this;
    }

    SpyComponentBuilder isMountSpec(boolean isMountSpec) {
      this.mMountType = isMountSpec ? MountType.DRAWABLE : MountType.NONE;
      return this;
    }

    SpyComponentBuilder setNode(InternalNode node) {
      this.mNode = node;
      return this;
    }

    SpyComponentBuilder isLayoutSpecWithSizeSpecCheck(boolean isLayoutSpecWithSizeSpecCheck) {
      this.mIsLayoutSpecWithSizeSpecCheck = isLayoutSpecWithSizeSpecCheck;
      return this;
    }

    SpyComponentBuilder hasState(boolean hasState) {
      this.mHasState = hasState;
      return this;
    }

    Component build(ComponentContext context) {
      return createSpyComponent(
          context,
          new TestBaseComponent(
              mCanMeasure, mMountType, mNode, mIsLayoutSpecWithSizeSpecCheck, mHasState));
    }
  }

  private static class TestMountSpecWithEmptyOnMeasure extends TestBaseComponent {

    TestMountSpecWithEmptyOnMeasure(InternalNode node) {
      super(true, MountType.DRAWABLE, node, false, false);
    }

    @Override
    protected void onMeasure(
        ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {}
  }

  private static class TestMountSpecSettingSizesInOnMeasure
      extends TestMountSpecWithEmptyOnMeasure {

    TestMountSpecSettingSizesInOnMeasure(InternalNode node) {
      super(node);
    }

    @Override
    protected void onMeasure(
        ComponentContext context,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size) {
      size.width = A_WIDTH;
      size.height = A_HEIGHT;
    }
  }
}
