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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeFactory;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.ParameterizedRobolectricTestRunner;

/** Tests {@link ComponentLifecycle} */
@PrepareForTest({
  DiffNode.class,
  Layout.class,
})
@PowerMockIgnore({
  "org.mockito.*",
  "org.robolectric.*",
  "androidx.*",
  "android.*",
  "com.facebook.yoga.*"
})
@RunWith(ParameterizedRobolectricTestRunner.class)
public class ComponentLifecycleTest {

  private static final String KEY = "globalKey";
  private final boolean mUseStatelessComponent;
  private boolean mUseStatelessComponentConfig;
  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int A_HEIGHT = 11;
  private static final int A_WIDTH = 12;
  private int mNestedTreeWidthSpec;
  private int mNestedTreeHeightSpec;

  private DefaultInternalNode mNode;
  private YogaNode mYogaNode;
  private DiffNode mDiffNode;
  private ComponentContext mContext;
  private boolean mPreviousOnErrorConfig;
  private LayoutStateContext mLayoutStateContext;

  @ParameterizedRobolectricTestRunner.Parameters(name = "useStatelessComponent={0}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false}, {true},
        });
  }

  public ComponentLifecycleTest(boolean useStatelessComponent) {
    mUseStatelessComponent = useStatelessComponent;
  }

  @Before
  public void setUp() {
    mUseStatelessComponentConfig = ComponentsConfiguration.useStatelessComponent;
    ComponentsConfiguration.useStatelessComponent = mUseStatelessComponent;
    mockStatic(Layout.class);
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext componentContext) {
            InternalNode layout = spy(new DefaultInternalNode(componentContext));
            YogaNode node = YogaNodeFactory.create();
            node.setData(layout);
            return layout;
          }
        };

    when(Layout.onCreateLayout((ComponentContext) any(), (Component) any())).thenCallRealMethod();
    when(Layout.create(
            (ComponentContext) any(),
            (Component) any(),
            anyBoolean(),
            anyBoolean(),
            (String) any()))
        .thenCallRealMethod();
    when(Layout.create((ComponentContext) any(), (Component) any(), anyBoolean()))
        .thenCallRealMethod();
    when(Layout.create((ComponentContext) any(), (Component) any())).thenCallRealMethod();
    when(Layout.update((ComponentContext) any(), (Component) any(), anyBoolean(), (String) any()))
        .thenCallRealMethod();

    mDiffNode = mock(DiffNode.class);
    mNode = mock(DefaultInternalNode.class);
    mYogaNode = YogaNodeFactory.create();
    mYogaNode.setData(mNode);

    when(mNode.getLastWidthSpec()).thenReturn(-1);
    when(mNode.getDiffNode()).thenReturn(mDiffNode);
    when(mDiffNode.getLastMeasuredWidth()).thenReturn(-1f);
    when(mDiffNode.getLastMeasuredHeight()).thenReturn(-1f);

    ComponentTree componentTree =
        ComponentTree.create(new ComponentContext(getApplicationContext())).build();
    final ComponentContext c = componentTree.getContext();
    c.setLayoutStateContextForTesting();
    mContext = spy(c);
    mLayoutStateContext = c.getLayoutStateContext();
    when(mNode.getContext()).thenReturn(mContext);

    mNestedTreeWidthSpec = SizeSpec.makeSizeSpec(400, SizeSpec.EXACTLY);
    mNestedTreeHeightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
  }

  @After
  public void after() {
    ComponentsConfiguration.useStatelessComponent = mUseStatelessComponentConfig;
    NodeConfig.sInternalNodeFactory = null;
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCannotMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, true);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(node).appendComponent(component, KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(node).appendComponent(component, KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, true);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(node).appendComponent(component, KEY);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(node).appendComponent(component, KEY);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, true);
    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(node).appendComponent(component, KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, false);

    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(node).appendComponent(component, KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    mContext.setWidthSpec(mNestedTreeWidthSpec);
    mContext.setHeightSpec(mNestedTreeHeightSpec);
    InternalNode node = Layout.create(mContext, component, true);
    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);
    verify(component)
        .onCreateLayoutWithSizeSpec(scopedContext, mNestedTreeWidthSpec, mNestedTreeHeightSpec);
    verify(node).appendComponent(component, KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mContext, component, false);

    verify(component, never()).onCreateLayout((ComponentContext) any());
    verify(component, never())
        .onCreateLayoutWithSizeSpec((ComponentContext) any(), anyInt(), anyInt());
    verify(node).appendComponent(component, KEY);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_FirstCall() {
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

    Layout.create(mContext, component, true);
    final ComponentContext scopedContext = component.getScopedContext(mLayoutStateContext, KEY);

    // onShouldCreateLayoutWithNewSizeSpec should not be called the first time
    verify(component, never())
        .onShouldCreateLayoutWithNewSizeSpec((ComponentContext) any(), anyInt(), anyInt());
    verify(component)
        .onCreateLayoutWithSizeSpec(
            scopedContext, scopedContext.getWidthSpec(), scopedContext.getHeightSpec());

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_shouldUseCache() {
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
    when(Layout.create(mContext, holder, 100, 100)).thenCallRealMethod();
    PowerMockito.doReturn(resolved).when(Layout.class);
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());

    // Resolve nested tree for the 1st time.
    InternalNode result = Layout.create(mContext, holder, 100, 100);

    PowerMockito.verifyStatic(Layout.class);

    // Layout should be created and measured.
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());
    Layout.measure((ComponentContext) any(), eq(result), eq(100), eq(100), eq((DiffNode) null));

    /* --- */

    // Return nested tree next time.
    when(holder.getNestedTree()).thenReturn(result);

    // Use previous layout in next call.
    doReturn(true).when(component).canUsePreviousLayout((ComponentContext) any());

    // Call resolve nested tree 2nd time.
    Layout.create(mContext, holder, 100, 100);

    // There should be no new invocation of Layout.create().
    PowerMockito.verifyStatic(Layout.class, times(1));
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());

    // Should only remeasure.
    PowerMockito.verifyStatic(Layout.class, times(1));
    Layout.remeasure(resolved, 100, 100);

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
  }

  @Test
  public void testOnShouldCreateLayoutWithNewSizeSpec_shouldNotUseCache() {
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
    when(Layout.create(mContext, holder, 100, 100)).thenCallRealMethod();
    PowerMockito.doReturn(resolved).when(Layout.class);
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());

    // Call resolve nested tree 1st time.
    InternalNode result = Layout.create(mContext, holder, 100, 100);

    PowerMockito.verifyStatic(Layout.class);

    // Layout should be created and measured.
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());
    Layout.measure((ComponentContext) any(), eq(result), eq(100), eq(100), eq((DiffNode) null));

    /* --- */

    // Return nested tree next time.
    when(holder.getNestedTree()).thenReturn(result);

    // Should use previous layout in next call
    doReturn(false).when(component).canUsePreviousLayout((ComponentContext) any());

    // Call resolve nested tree 2nd time
    Layout.create(mContext, holder, 100, 100);

    // There should be 1 new invocation of Layout.create().
    PowerMockito.verifyStatic(Layout.class, times(2));
    Layout.create((ComponentContext) any(), (Component) any(), eq(true), eq(true), (String) any());

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
  }

  @Test
  public void testOnMeasureNotOverridden() {
    Component component = setUpSpyComponentForCreateLayout(true, true);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    try {
      measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);
      fail("Should have failed without overridden onMeasure() when canMeasure() returns true.");
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
      fail("Should have failed when onMeasure() is empty.");
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
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    YogaMeasureFunction measureFunction = getMeasureFunction(component);

    final int nestedTreeWidth = 20;
    final int nestedTreeHeight = 25;
    InternalNode nestedTree = mock(InternalNode.class);
    when(nestedTree.getWidth()).thenReturn(nestedTreeWidth);
    when(nestedTree.getHeight()).thenReturn(nestedTreeHeight);
    when(Layout.create(eq(mContext), eq(mNode), anyInt(), anyInt())).thenReturn(nestedTree);
    when(mNode.getContext()).thenReturn(mContext);

    long output = measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic(Layout.class);
    Layout.create(eq(mContext), eq(mNode), anyInt(), anyInt());

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
    when(Layout.create(eq(mContext), eq(mNode), anyInt(), anyInt())).thenReturn(nestedTree);
    when(mNode.getContext()).thenReturn(mContext);
    when(mContext.isReconciliationEnabled()).thenReturn(true);
    when(mNode.getParent()).thenReturn(mNode);

    when(mNode.getContext()).thenReturn(mContext);
    long output = measureFunction.measure(mYogaNode, 0, EXACTLY, 0, EXACTLY);

    PowerMockito.verifyStatic(Layout.class);
    Layout.create(eq(mContext), eq(mNode), anyInt(), anyInt());

    assertThat(YogaMeasureOutput.getWidth(output)).isEqualTo(nestedTreeWidth);
    assertThat(YogaMeasureOutput.getHeight(output)).isEqualTo(nestedTreeHeight);
  }

  private Component setUpSpyComponentForCreateLayout(boolean isMountSpec, boolean canMeasure) {

    return new SpyComponentBuilder()
        .isMountSpec(isMountSpec)
        .setNode(mNode)
        .canMeasure(canMeasure)
        .build(mContext);
  }

  private Component setUpSpyLayoutSpecWithNullLayout() {
    Component component =
        spy(
            new TestBaseComponent(
                false,
                MountType.NONE,
                ComponentContext.NULL_LAYOUT,
                false,
                mPreviousOnErrorConfig));

    return component;
  }

  private YogaMeasureFunction getMeasureFunction(Component component) {
    when(mNode.getTailComponent()).thenReturn(component);
    when(mNode.getTailComponentKey()).thenReturn(KEY);

    return ComponentLifecycle.getYogaMeasureFunction(mLayoutStateContext);
  }

  private static Component createSpyComponent(
      ComponentContext context, TestBaseComponent component) {
    Component spy = spy(component);
    when(spy.makeShallowCopy()).thenReturn(spy);
    return spy;
  }

  @OkToExtend
  static class TestBaseComponent extends Component {

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
      return mNode == ComponentContext.NULL_LAYOUT ? null : super.onCreateLayout(c);
    }

    @Override
    protected Component onCreateLayoutWithSizeSpec(
        ComponentContext c, int widthSpec, int heightSpec) {
      return super.onCreateLayoutWithSizeSpec(c, widthSpec, heightSpec);
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

    @Override
    public String getKey() {
      return KEY;
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
