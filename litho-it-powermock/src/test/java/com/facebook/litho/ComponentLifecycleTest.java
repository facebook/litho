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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.Component.MountType;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeFactory;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.ParameterizedRobolectricTestRunner;

/** Tests {@link Component} */
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

  private DefaultLayoutResult mResult;
  private InputOnlyInternalNode mNode;
  private YogaNode mYogaNode;
  private DiffNode mDiffNode;
  private ComponentContext mContext;
  private ComponentTree mComponentTree;
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
            return spy(new InputOnlyInternalNode<>(componentContext));
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            return spy(new InputOnlyNestedTreeHolder(c, props));
          }
        };

    when(Layout.create(
            (LayoutStateContext) any(),
            (ComponentContext) any(),
            (Component) any(),
            anyBoolean(),
            anyBoolean(),
            (String) any()))
        .thenCallRealMethod();
    when(Layout.create(
            (LayoutStateContext) any(), (ComponentContext) any(), (Component) any(), anyBoolean()))
        .thenCallRealMethod();
    when(Layout.create((LayoutStateContext) any(), (ComponentContext) any(), (Component) any()))
        .thenCallRealMethod();
    when(Layout.update(
            (LayoutStateContext) any(),
            (ComponentContext) any(),
            (Component) any(),
            anyBoolean(),
            (String) any()))
        .thenCallRealMethod();

    mDiffNode = mock(DiffNode.class);
    mNode = mock(InputOnlyInternalNode.class);
    mResult = mock(DefaultLayoutResult.class);
    mYogaNode = YogaNodeFactory.create();
    mYogaNode.setData(mNode);

    when(mResult.getLastWidthSpec()).thenReturn(-1);
    when(mResult.getDiffNode()).thenReturn(mDiffNode);
    when(mDiffNode.getLastMeasuredWidth()).thenReturn(-1f);
    when(mDiffNode.getLastMeasuredHeight()).thenReturn(-1f);

    ComponentTree componentTree =
        ComponentTree.create(new ComponentContext(getApplicationContext())).build();
    mComponentTree = spy(componentTree);
    final ComponentContext c = componentTree.getContext();
    mLayoutStateContext = spy(LayoutStateContext.getTestInstance(c));
    c.setLayoutStateContext(mLayoutStateContext);
    mContext = spy(c);
    when(mResult.getInternalNode()).thenReturn(mNode);
    when(mContext.getLayoutStateContext()).thenReturn(mLayoutStateContext);
    when(mLayoutStateContext.getComponentTree()).thenReturn(mComponentTree);
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
    Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    Component component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(component).onCreateLayout(scopedContext);
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, true);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, true);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, true);
    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, false);

    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component).onCreateLayout(scopedContext);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    mContext.setWidthSpec(mNestedTreeWidthSpec);
    mContext.setHeightSpec(mNestedTreeHeightSpec);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, true);
    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);
    verify(component)
        .onCreateLayoutWithSizeSpec(scopedContext, mNestedTreeWidthSpec, mNestedTreeHeightSpec);
    verify(node).appendComponent(component, "$" + KEY);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    InternalNode node = Layout.create(mLayoutStateContext, mContext, component, false);

    verify(component, never()).onCreateLayout((ComponentContext) any());
    verify(component, never())
        .onCreateLayoutWithSizeSpec((ComponentContext) any(), anyInt(), anyInt());
    verify(node).appendComponent(component, "$" + KEY);
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

    Layout.create(mLayoutStateContext, mContext, component, true);
    final ComponentContext scopedContext =
        component.getScopedContext(mLayoutStateContext, "$" + KEY);

    // onShouldCreateLayoutWithNewSizeSpec should not be called the first time
    verify(component, never())
        .onShouldCreateLayoutWithNewSizeSpec((ComponentContext) any(), anyInt(), anyInt());
    verify(component)
        .onCreateLayoutWithSizeSpec(
            scopedContext, scopedContext.getWidthSpec(), scopedContext.getHeightSpec());

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
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
    when(mNode.getTailComponentKey()).thenReturn("$" + KEY);

    return Component.sMeasureFunction;
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
      mCanMeasure = canMeasure;
      mMountType = mountType;
      mNode = node;
      mIsLayoutSpecWithSizeSpecCheck = isLayoutSpecWithSizeSpecCheck;
      mHasState = hasState;
      setKey(KEY);
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
