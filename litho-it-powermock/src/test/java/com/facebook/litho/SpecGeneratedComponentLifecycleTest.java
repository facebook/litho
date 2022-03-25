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
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.Component.MountType;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeFactory;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

/** Tests {@link Component} */
@PrepareForTest({
  DiffNode.class,
  Layout.class,
  InternalNodeUtils.class,
})
@PowerMockIgnore({
  "org.mockito.*",
  "org.robolectric.*",
  "androidx.*",
  "android.*",
  "com.facebook.yoga.*"
})
@RunWith(LithoTestRunner.class)
public class SpecGeneratedComponentLifecycleTest {

  private static final String KEY = "globalKey";
  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int A_HEIGHT = 11;
  private static final int A_WIDTH = 12;
  private int mNestedTreeWidthSpec;
  private int mNestedTreeHeightSpec;
  private static final int NODE_LIST_SIZE = 100;

  private LithoLayoutResult mResult;
  private LithoNode mNode;
  private YogaNode mYogaNode;
  private DiffNode mDiffNode;
  private ComponentContext mContext;
  private ComponentTree mComponentTree;
  private boolean mPreviousOnErrorConfig;
  private LayoutStateContext mLayoutStateContext;
  private final List<LithoNode> mNodes = new ArrayList<>(NODE_LIST_SIZE);
  private final List<NestedTreeHolder> mInputOnlyNestedTreeHolders =
      new ArrayList<>(NODE_LIST_SIZE);
  private int mNextInternalNode;
  private int mNextInputOnlyNestedTreeHolder;

  private LithoNode getNextInternalNode() {
    if (mNextInternalNode >= NODE_LIST_SIZE) {
      throw new IllegalStateException("Increase NODE_LIST_SIZE");
    }

    return mNodes.get(mNextInternalNode++);
  }

  private NestedTreeHolder getNextInputOnlyNestedTreeHolder() {
    if (mNextInputOnlyNestedTreeHolder >= NODE_LIST_SIZE) {
      throw new IllegalStateException("Increase NODE_LIST_SIZE");
    }

    return mInputOnlyNestedTreeHolders.get(mNextInputOnlyNestedTreeHolder++);
  }

  private void createSpyNodes() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    for (int i = 0, size = NODE_LIST_SIZE; i < size; i++) {
      mNodes.add(spy(new LithoNode(c)));
      mInputOnlyNestedTreeHolders.add(spy(new NestedTreeHolder(c, null)));
    }
  }

  private void clearSpyNodes() {
    mInputOnlyNestedTreeHolders.clear();
    mNodes.clear();
    mNextInternalNode = 0;
    mNextInputOnlyNestedTreeHolder = 0;
  }

  @Before
  public void setUp() {
    createSpyNodes();
    mockStatic(Layout.class);

    try {
      whenNew(LithoNode.class)
          .withArguments((ComponentContext) any())
          .thenAnswer(
              new Answer<LithoNode>() {
                @Override
                public LithoNode answer(InvocationOnMock invocation) throws Throwable {
                  final ComponentContext c = (ComponentContext) invocation.getArguments()[0];
                  final LithoNode node = getNextInternalNode();
                  Whitebox.setInternalState(node, "mContext", c.getAndroidContext());
                  Whitebox.setInternalState(node, "mScopedComponentInfos", new ArrayList<>(2));

                  return node;
                }
              });
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      whenNew(NestedTreeHolder.class)
          .withArguments((ComponentContext) any(), (TreeProps) any())
          .thenAnswer(
              new Answer<NestedTreeHolder>() {
                @Override
                public NestedTreeHolder answer(InvocationOnMock invocation) throws Throwable {
                  final ComponentContext c = (ComponentContext) invocation.getArguments()[0];
                  final TreeProps props = (TreeProps) invocation.getArguments()[1];
                  final NestedTreeHolder node = getNextInputOnlyNestedTreeHolder();
                  Whitebox.setInternalState(node, "mContext", c.getAndroidContext());
                  Whitebox.setInternalState(node, "mScopedComponentInfos", new ArrayList<>(2));
                  Whitebox.setInternalState(node, "mPendingTreeProps", TreeProps.copy(props));

                  return node;
                }
              });
    } catch (Exception e) {
      e.printStackTrace();
    }

    when(Layout.create(
            (LayoutStateContext) any(),
            (ComponentContext) any(),
            anyInt(),
            anyInt(),
            (Component) any(),
            anyBoolean(),
            anyBoolean(),
            (String) any()))
        .thenCallRealMethod();
    when(Layout.create(
            (LayoutStateContext) any(),
            (ComponentContext) any(),
            (Component) any(),
            anyBoolean(),
            (String) any()))
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
    mNode = mock(LithoNode.class);
    mResult = mock(LithoLayoutResult.class);
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
    when(mResult.getNode()).thenReturn(mNode);
    when(mLayoutStateContext.getComponentTree()).thenReturn(mComponentTree);
    mNestedTreeWidthSpec = SizeSpec.makeSizeSpec(400, SizeSpec.EXACTLY);
    mNestedTreeHeightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
  }

  @After
  public void after() {
    clearSpyNodes();
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCannotMeasure() {
    TestBaseComponent component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component);

    verify(component).onCreateLayout((ComponentContext) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithLayoutSpecCanMeasure() {
    TestBaseComponent component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component);

    verify(component).onCreateLayout((ComponentContext) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCannotMeasure() {
    TestBaseComponent component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component);

    verify(component).onCreateLayout((ComponentContext) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutWithNullComponentWithMountSpecCanMeasure() {
    TestBaseComponent component = setUpSpyLayoutSpecWithNullLayout();
    Layout.create(mLayoutStateContext, mContext, component);

    verify(component).onCreateLayout((ComponentContext) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCannotMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, false /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithMountSpecCanMeasure() {
    Component component =
        setUpSpyComponentForCreateLayout(true /* isMountSpec */, true /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component).onPrepare(scopedContext);
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCannotMeasure() {
    TestBaseComponent component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);
    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    verify(component).onCreateLayout(scopedContext);
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCannotMeasure() {
    TestBaseComponent component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, false /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    verify(component).onCreateLayout(scopedContext);
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndResolveNestedTreeWithLayoutSpecCanMeasure() {
    TestBaseComponent component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    LithoNode node =
        Layout.create(
            mLayoutStateContext,
            mContext,
            mNestedTreeWidthSpec,
            mNestedTreeHeightSpec,
            component,
            true,
            false,
            null);
    final ComponentContext scopedContext =
        node.getComponentContextAt(getComponentKeys(node).indexOf("$" + KEY));
    verify(component)
        .onCreateLayoutWithSizeSpec(scopedContext, mNestedTreeWidthSpec, mNestedTreeHeightSpec);
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node, never()).setMeasureFunction((YogaMeasureFunction) any());
  }

  @Test
  public void testCreateLayoutAndDontResolveNestedTreeWithLayoutSpecCanMeasure() {
    TestBaseComponent component =
        setUpSpyComponentForCreateLayout(false /* isMountSpec */, true /* canMeasure */);
    LithoNode node = Layout.create(mLayoutStateContext, mContext, component);

    verify(component, never()).onCreateLayout((ComponentContext) any());
    verify(component, never())
        .onCreateLayoutWithSizeSpec((ComponentContext) any(), anyInt(), anyInt());
    // verify(node).appendComponent(component, "$" + KEY, null);
    verify(node).setMeasureFunction((YogaMeasureFunction) any());
    verify(component, never()).onPrepare((ComponentContext) any());
  }

  static List<String> getComponentKeys(LithoNode node) {
    List<ScopedComponentInfo> infos = node.getScopedComponentInfos();
    List<String> keys = new ArrayList<>(infos.size());

    for (ScopedComponentInfo info : infos) {
      keys.add(info.getContext().getGlobalKey());
    }

    return keys;
  }

  private TestBaseComponent setUpSpyComponentForCreateLayout(
      boolean isMountSpec, boolean canMeasure) {

    return new SpyComponentBuilder()
        .isMountSpec(isMountSpec)
        .setNode(mNode)
        .canMeasure(canMeasure)
        .build(mContext);
  }

  private TestBaseComponent setUpSpyLayoutSpecWithNullLayout() {
    TestBaseComponent component =
        spy(new TestBaseComponent(false, MountType.NONE, null, mPreviousOnErrorConfig));

    return component;
  }

  private YogaMeasureFunction getMeasureFunction(Component component) {
    when(mNode.getTailComponent()).thenReturn(component);
    when(mNode.getTailComponentKey()).thenReturn("$" + KEY);

    return Component.sMeasureFunction;
  }

  private static TestBaseComponent createSpyComponent(
      ComponentContext context, TestBaseComponent component) {
    TestBaseComponent spy = spy(component);
    when(spy.makeShallowCopy()).thenReturn(spy);
    return spy;
  }

  @OkToExtend
  static class TestBaseComponent extends SpecGeneratedComponent {

    private final boolean mCanMeasure;
    private final MountType mMountType;
    private final LithoNode mNode;
    private final boolean mHasState;

    TestBaseComponent(boolean canMeasure, MountType mountType, LithoNode node, boolean hasState) {
      super("TestBaseComponent");
      mCanMeasure = canMeasure;
      mMountType = mountType;
      mNode = node;
      mHasState = hasState;
      setKey(KEY);
    }

    @Override
    public boolean isEquivalentProps(Component other, boolean shouldCompareCommonProps) {
      return this == other;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return mNode == null ? null : super.onCreateLayout(c);
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
  }

  static class SpyComponentBuilder {
    private boolean mCanMeasure = false;
    private MountType mMountType = MountType.NONE;
    private LithoNode mNode = null;
    private boolean mHasState = false;

    SpyComponentBuilder canMeasure(boolean canMeasure) {
      this.mCanMeasure = canMeasure;
      return this;
    }

    SpyComponentBuilder isMountSpec(boolean isMountSpec) {
      this.mMountType = isMountSpec ? MountType.DRAWABLE : MountType.NONE;
      return this;
    }

    SpyComponentBuilder setNode(LithoNode node) {
      this.mNode = node;
      return this;
    }

    SpyComponentBuilder hasState(boolean hasState) {
      this.mHasState = hasState;
      return this;
    }

    TestBaseComponent build(ComponentContext context) {
      return createSpyComponent(
          context, new TestBaseComponent(mCanMeasure, mMountType, mNode, mHasState));
    }
  }

  private static class TestMountSpecWithEmptyOnMeasure extends TestBaseComponent {

    TestMountSpecWithEmptyOnMeasure(LithoNode node) {
      super(true, MountType.DRAWABLE, node, false);
    }

    @Override
    protected void onMeasure(
        ComponentContext c,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        InterStagePropsContainer interStagePropsContainer) {}
  }

  private static class TestMountSpecSettingSizesInOnMeasure
      extends TestMountSpecWithEmptyOnMeasure {

    TestMountSpecSettingSizesInOnMeasure(LithoNode node) {
      super(node);
    }

    @Override
    protected void onMeasure(
        ComponentContext context,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        InterStagePropsContainer interStagePropsContainer) {
      size.width = A_WIDTH;
      size.height = A_HEIGHT;
    }
  }
}
