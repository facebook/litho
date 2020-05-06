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
import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;

import android.os.Looper;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoStatsRule;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class ComponentTreeTest {

  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();
  public @Rule LithoStatsRule mLithoStatsRule = new LithoStatsRule();

  private int mWidthSpec;
  private int mWidthSpec2;
  private int mHeightSpec;
  private int mHeightSpec2;

  private Component mComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private RootWrapperComponentFactory mOldWrapperConfig;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(getApplicationContext());
    mComponent = TestDrawableComponent.create(mContext).build();

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));

    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mWidthSpec2 = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);
    mHeightSpec2 = makeSizeSpec(42, EXACTLY);
  }

  @Before
  public void saveConfig() {
    mOldWrapperConfig = ErrorBoundariesConfiguration.rootWrapperComponentFactory;
  }

  @After
  public void restoreConfig() {
    ErrorBoundariesConfiguration.rootWrapperComponentFactory = mOldWrapperConfig;
  }

  @After
  public void tearDown() {
    // Clear pending tasks in case test failed
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  private void creationCommonChecks(ComponentTree componentTree) {
    // Not view or attached yet
    Assert.assertNull(componentTree.getLithoView());
    Assert.assertFalse(isAttached(componentTree));

    // The component input should be the one we passed in
    Assert.assertSame(mComponent, Whitebox.getInternalState(componentTree, "mRoot"));
  }

  private void postSizeSpecChecks(ComponentTree componentTree, String layoutStateVariableName) {
    postSizeSpecChecks(componentTree, layoutStateVariableName, mWidthSpec, mHeightSpec);
  }

  private void postSizeSpecChecks(
      ComponentTree componentTree, String layoutStateVariableName, int widthSpec, int heightSpec) {
    // Spec specified in create

    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue();
    assertThat((int) getInternalState(componentTree, "mWidthSpec")).isEqualTo(widthSpec);

    assertThat((int) getInternalState(componentTree, "mHeightSpec")).isEqualTo(heightSpec);

    LayoutState mainThreadLayoutState = getInternalState(componentTree, "mMainThreadLayoutState");

    LayoutState backgroundLayoutState = getInternalState(componentTree, "mBackgroundLayoutState");

    LayoutState layoutState = null;
    LayoutState nullLayoutState = null;
    if ("mMainThreadLayoutState".equals(layoutStateVariableName)) {
      layoutState = mainThreadLayoutState;
      nullLayoutState = backgroundLayoutState;
    } else if ("mBackgroundLayoutState".equals(layoutStateVariableName)) {
      layoutState = backgroundLayoutState;
      nullLayoutState = mainThreadLayoutState;
    } else {
      fail("Incorrect variable name: " + layoutStateVariableName);
    }

    Assert.assertNull(nullLayoutState);
    assertThat(layoutState.isCompatibleComponentAndSpec(mComponent.getId(), widthSpec, heightSpec))
        .isTrue();
  }

  @Test
  public void testCreate() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();

    creationCommonChecks(componentTree);

    // Both the main thread and the background layout state shouldn't be calculated yet.
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mMainThreadLayoutState"));
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mBackgroundLayoutState"));

    Assert.assertFalse(componentTreeHasSizeSpec(componentTree));
  }

  @Test
  public void testCreate_ContextIsNotScoped() {
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, Row.create(mContext).build());
    ComponentTree componentTree = ComponentTree.create(scopedContext, mComponent).build();

    ComponentContext c = Whitebox.getInternalState(componentTree, "mContext");
    Assert.assertNull(c.getComponentScope());
  }

  @Test
  public void testSetSizeSpec() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testSetSizeSpecAsync() {
    ComponentTree componentTree = create(mContext, mComponent).build();
    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    // Only fields changed but no layout is done yet.

    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue();
    assertThat((int) getInternalState(componentTree, "mWidthSpec")).isEqualTo(mWidthSpec);
    assertThat((int) getInternalState(componentTree, "mHeightSpec")).isEqualTo(mHeightSpec);
    Assert.assertNull(getInternalState(componentTree, "mMainThreadLayoutState"));
    Assert.assertNull(getInternalState(componentTree, "mBackgroundLayoutState"));

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runOneTask();

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testLayoutState_ContextIsNotScoped() {
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, Row.create(mContext).build());
    Component root = Column.create(scopedContext).build();

    ComponentTree componentTree = ComponentTree.create(scopedContext, root).build();

    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    mLayoutThreadShadowLooper.runOneTask();

    LayoutState layoutState = getInternalState(componentTree, "mBackgroundLayoutState");
    ComponentContext c = getInternalState(componentTree, "mContext");
    assertThat(c).isNotEqualTo(scopedContext);
    Assert.assertNull(c.getComponentScope());
    assertThat(layoutState.getRootComponent().getScopedContext()).isNotEqualTo(scopedContext);
  }

  private static class MeasureListener implements ComponentTree.MeasureListener {
    int mWidth = 0;
    int mHeight = 0;

    @Override
    public void onSetRootAndSizeSpec(
        int layoutVersion, int width, int height, boolean stateUpdate) {
      mWidth = width;
      mHeight = height;
    }
  }

  @Test
  public void testRacyLayouts() {
    final CountDownLatch asyncLatch = new CountDownLatch(1);
    final CountDownLatch syncLatch = new CountDownLatch(1);
    final CountDownLatch endOfTest = new CountDownLatch(1);

    final int widthSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
    final int asyncWidthSpec = SizeSpec.makeSizeSpec(200, EXACTLY);
    final int asyncHeightSpec = SizeSpec.makeSizeSpec(200, EXACTLY);

    final ComponentTree componentTree = ComponentTree.create(mContext).build();
    final ComponentTree innerComponentTree = ComponentTree.create(mContext).build();
    final MeasureListener measureListener = new MeasureListener();
    innerComponentTree.addMeasureListener(measureListener);

    Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            innerComponentTree.setVersionedRootAndSizeSpec(
                new InlineLayoutSpec() {}, widthSpec, heightSpec, null, null, c.getLayoutVersion());
            return super.onCreateLayout(c);
          }
        };

    Component asyncComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            // TODO
            try {
              syncLatch.countDown();
              asyncLatch.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            innerComponentTree.setVersionedRootAndSizeSpec(
                new InlineLayoutSpec() {},
                asyncWidthSpec,
                asyncHeightSpec,
                null,
                null,
                c.getLayoutVersion());
            return super.onCreateLayout(c);
          }
        };

    new Thread() {
      @Override
      public void run() {
        componentTree.setRootAndSizeSpec(asyncComponent, 0, 0);
        endOfTest.countDown();
      }
    }.start();

    try {
      syncLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    componentTree.setRootAndSizeSpec(component, 0, 0);
    assertEquals(measureListener.mWidth, 100);
    assertEquals(measureListener.mHeight, 100);

    asyncLatch.countDown();

    componentTree.setRootAsync(component);

    try {
      endOfTest.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // Verify your stuff at this point
    assertEquals(measureListener.mWidth, 100);
    assertEquals(measureListener.mHeight, 100);
  }

  @Test
  public void testSetSizeSpecAsyncThenSyncBeforeRunningTask() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();

    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    componentTree.setSizeSpec(mWidthSpec2, mHeightSpec2);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState", mWidthSpec2, mHeightSpec2);
  }

  @Test
  public void testSetRootSynchThenAsyncThenSync() {
    ComponentTree componentTree = ComponentTree.create(mContext).build();
    componentTree.setRootAndSizeSpec(
        TestDrawableComponent.create(mContext).measuredWidth(200).measuredHeight(200).build(),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    TestDrawableComponent newComponent =
        TestDrawableComponent.create(mContext).measuredWidth(100).measuredHeight(100).build();

    componentTree.setRootAndSizeSpecAsync(
        newComponent,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    Size size = new Size();
    componentTree.setRootAndSizeSpec(
        newComponent,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        size);

    assertThat(size.width).isEqualTo(100);
    assertThat(size.height).isEqualTo(100);
  }

  @Test
  public void testSetSizeSpecAsyncThenSyncAfterRunningTask() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    componentTree.setSizeSpec(mWidthSpec2, mHeightSpec2);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState", mWidthSpec2, mHeightSpec2);
  }

  @Test
  public void testSetSizeSpecWithOutput() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();

    Size size = new Size();

    componentTree.setSizeSpec(mWidthSpec, mHeightSpec, size);

    assertEquals(SizeSpec.getSize(mWidthSpec), size.width, 0.0);
    assertEquals(SizeSpec.getSize(mHeightSpec), size.height, 0.0);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testSetSizeSpecWithOutputWhenAttachedToViewWithSameSpec() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    Size size = new Size();
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    componentTree.setSizeSpec(mWidthSpec, mHeightSpec, size);

    assertEquals(SizeSpec.getSize(mWidthSpec), size.width, 0.0);
    assertEquals(SizeSpec.getSize(mHeightSpec), size.height, 0.0);

    assertThat(componentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isTrue();
    assertThat(componentTree.getRoot()).isEqualTo(mComponent);
  }

  @Test
  public void testSetSizeSpecWithOutputWhenAttachedToViewWithNewSpec() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    Size size = new Size();
    componentTree.measure(mWidthSpec2, mHeightSpec2, new int[2], false);
    componentTree.attach();

    componentTree.setSizeSpec(mWidthSpec, mHeightSpec, size);

    assertEquals(SizeSpec.getSize(mWidthSpec), size.width, 0.0);
    assertEquals(SizeSpec.getSize(mHeightSpec), size.height, 0.0);

    assertThat(componentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isTrue();
    assertThat(componentTree.getRoot()).isEqualTo(mComponent);
  }

  @Test
  public void testSetCompatibleSizeSpec() {
    ComponentTree componentTree = create(mContext, mComponent).build();

    Size size = new Size();

    componentTree.setSizeSpec(makeSizeSpec(100, AT_MOST), makeSizeSpec(100, AT_MOST), size);

    assertEquals(100, size.width, 0.0);
    assertEquals(100, size.height, 0.0);

    LayoutState firstLayoutState = componentTree.getBackgroundLayoutState();
    assertThat(firstLayoutState).isNotNull();

    componentTree.setSizeSpec(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), size);

    assertEquals(100, size.width, 0.0);
    assertEquals(100, size.height, 0.0);

    assertThat(componentTree.getBackgroundLayoutState()).isEqualTo(firstLayoutState);
  }

  @Test
  public void testSetCompatibleSizeSpecWithDifferentRoot() {
    ComponentTree componentTree = create(mContext, mComponent).build();

    Size size = new Size();

    componentTree.setSizeSpec(makeSizeSpec(100, AT_MOST), makeSizeSpec(100, AT_MOST), size);

    assertEquals(100, size.width, 0.0);
    assertEquals(100, size.height, 0.0);

    LayoutState firstLayoutState = componentTree.getBackgroundLayoutState();
    assertThat(firstLayoutState).isNotNull();

    componentTree.setRootAndSizeSpec(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        size);

    assertNotEquals(firstLayoutState, componentTree.getBackgroundLayoutState());
  }

  @Test
  public void testSetRootAndSizeSpecWithTreeProps() {
    ComponentTree componentTree = create(mContext, mComponent).build();

    final Size size = new Size();
    final TreeProps treeProps = new TreeProps();
    treeProps.put(Object.class, "hello world");

    componentTree.setRootAndSizeSpec(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        size,
        treeProps);

    final ComponentContext c =
        getInternalState(componentTree.getBackgroundLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isSameAs(treeProps);
  }

  @Test
  public void testDefaultInitialisationAndSetRoot() {
    ComponentTree componentTree = create(mContext).build();
    componentTree.setLithoView(new LithoView(mContext));
    componentTree.attach();

    assertThat(componentTree.getRoot()).isNotNull();

    componentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);

    assertThat(componentTree.getRoot()).isEqualTo(mComponent);
  }

  @Test
  public void testSetRootWithTreePropsThenMeasure() {
    ComponentTree componentTree = create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));
    componentTree.attach();

    final TreeProps treeProps = new TreeProps();
    treeProps.put(Object.class, "hello world");

    componentTree.setRootAndSizeSpecAsync(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps);

    assertThat(componentTree.getCommittedLayoutState()).isNull();

    componentTree.measure(
        makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), new int[2], false);

    final ComponentContext c =
        getInternalState(componentTree.getMainThreadLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isNotNull();
    assertThat(c.getTreeProps().get(Object.class)).isEqualTo(treeProps.get(Object.class));
  }

  @Test
  public void testSetRootWithTreePropsThenSetSizeSpec() {
    ComponentTree componentTree = create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));
    componentTree.attach();

    final TreeProps treeProps = new TreeProps();
    treeProps.put(Object.class, "hello world");

    componentTree.setRootAndSizeSpecAsync(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps);

    assertThat(componentTree.getCommittedLayoutState()).isNull();

    componentTree.setSizeSpec(makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY));

    final ComponentContext c =
        getInternalState(componentTree.getMainThreadLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isNotNull();
    assertThat(c.getTreeProps().get(Object.class)).isEqualTo(treeProps.get(Object.class));
  }

  @Test
  public void testSetRootWithTreePropsThenSetNewRoot() {
    ComponentTree componentTree = create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));
    componentTree.attach();

    final TreeProps treeProps = new TreeProps();
    treeProps.put(Object.class, "hello world");

    componentTree.setRootAndSizeSpecAsync(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps);

    assertThat(componentTree.getCommittedLayoutState()).isNull();

    componentTree.setRootAndSizeSpec(
        TestDrawableComponent.create(mContext).build(),
        makeSizeSpec(200, EXACTLY),
        makeSizeSpec(200, EXACTLY));

    final ComponentContext c =
        getInternalState(componentTree.getMainThreadLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isNotNull();
    assertThat(c.getTreeProps().get(Object.class)).isEqualTo(treeProps.get(Object.class));
  }

  @Test
  public void testSetRootWithTreePropsThenUpdateState() {
    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    ComponentTree componentTree =
        create(mContext, SimpleStateUpdateEmulator.create(mContext).caller(caller).build()).build();
    componentTree.setLithoView(new LithoView(mContext));
    componentTree.attach();

    componentTree.setRootAndSizeSpec(
        SimpleStateUpdateEmulator.create(mContext).caller(caller).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        new Size());

    final TreeProps treeProps = new TreeProps();
    treeProps.put(Object.class, "hello world");

    componentTree.setRootAndSizeSpecAsync(
        SimpleStateUpdateEmulator.create(mContext).caller(caller).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps);

    caller.increment();
    ShadowLooper.runUiThreadTasks();

    final ComponentContext c =
        getInternalState(componentTree.getMainThreadLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isNotNull();
    assertThat(c.getTreeProps().get(Object.class)).isEqualTo(treeProps.get(Object.class));
  }

  @Test
  public void testSetInput() {
    Component component = TestLayoutComponent.create(mContext).build();

    ComponentTree componentTree = ComponentTree.create(mContext, component).build();

    componentTree.setRoot(mComponent);

    creationCommonChecks(componentTree);
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mMainThreadLayoutState"));
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mBackgroundLayoutState"));

    componentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testRootWrapperComponent() {
    final Component component = TestLayoutComponent.create(mContext).build();
    final Component wrapperComponent = TestLayoutComponent.create(mContext).build();

    ErrorBoundariesConfiguration.rootWrapperComponentFactory =
        new RootWrapperComponentFactory() {
          @Override
          public Component createWrapper(ComponentContext c, Component root) {
            return wrapperComponent;
          }
        };

    ComponentTree componentTree = ComponentTree.create(mContext, component).build();

    componentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);
    assertThat(componentTree.getRoot()).isEqualTo(wrapperComponent);
  }

  @Test
  public void testSetComponentFromView() {
    Component component1 = TestDrawableComponent.create(mContext).build();
    ComponentTree componentTree1 = ComponentTree.create(mContext, component1).build();

    Component component2 = TestDrawableComponent.create(mContext).build();
    ComponentTree componentTree2 = ComponentTree.create(mContext, component2).build();

    Assert.assertNull(componentTree1.getLithoView());
    Assert.assertNull(componentTree2.getLithoView());

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree1);

    Assert.assertNotNull(componentTree1.getLithoView());
    Assert.assertNull(componentTree2.getLithoView());

    lithoView.setComponentTree(componentTree2);

    Assert.assertNull(componentTree1.getLithoView());
    Assert.assertNotNull(componentTree2.getLithoView());
  }

  @Test
  public void testComponentTreeReleaseClearsView() {
    Component component = TestDrawableComponent.create(mContext).build();
    ComponentTree componentTree = create(mContext, component).build();

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);

    assertThat(componentTree).isEqualTo(lithoView.getComponentTree());

    componentTree.release();

    assertThat(lithoView.getComponentTree()).isNull();
  }

  @Test
  public void testSetTreeToTwoViewsBothAttached() {
    Component component = TestDrawableComponent.create(mContext).build();

    ComponentTree componentTree = ComponentTree.create(mContext, component).build();

    // Attach first view.
    LithoView lithoView1 = new LithoView(mContext);
    lithoView1.setComponentTree(componentTree);
    lithoView1.onAttachedToWindow();

    // Attach second view.
    LithoView lithoView2 = new LithoView(mContext);
    lithoView2.onAttachedToWindow();

    // Set the component that is already mounted on the first view, on the second attached view.
    // This should be ok.
    lithoView2.setComponentTree(componentTree);
  }

  @Test
  public void testSettingNewViewToTree() {
    Component component = TestDrawableComponent.create(mContext).build();

    ComponentTree componentTree = create(mContext, component).build();

    // Attach first view.
    LithoView lithoView1 = new LithoView(mContext);
    lithoView1.setComponentTree(componentTree);

    assertThat(componentTree.getLithoView()).isEqualTo(lithoView1);
    assertThat(lithoView1.getComponentTree()).isEqualTo(componentTree);

    // Attach second view.
    LithoView lithoView2 = new LithoView(mContext);

    Assert.assertNull(lithoView2.getComponentTree());

    lithoView2.setComponentTree(componentTree);

    assertThat(componentTree.getLithoView()).isEqualTo(lithoView2);
    assertThat(lithoView2.getComponentTree()).isEqualTo(componentTree);

    Assert.assertNull(lithoView1.getComponentTree());
  }

  @Test
  public void testSetRootAsyncFollowedByMeasureDoesntComputeSyncLayout() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);
    assertThat(componentTree.getRoot()).isEqualTo(newComponent);

    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);

    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(mComponent.getId()))
        .isTrue();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
  }

  @Test
  public void testSetRootAsyncFollowedByNonCompatibleMeasureComputesSyncLayout() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);

    componentTree.measure(mWidthSpec2, mHeightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(mWidthSpec2, mHeightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();

    // Clear tasks
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs that both the original component and the new component are incompatible with.
   */
  @Test
  public void testSetRootAsyncWithIncompatibleMeasureBeforeStart() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    // Since the layout thread hasn't started the async layout, we know it will capture the updated
    // size specs when it does run

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .describedAs(
            "The old component spec is not compatible so we should do a sync layout with the new root.")
        .isTrue();

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    ShadowLooper.runUiThreadTasks();

    // Once the async layout finishes, the main thread should have the updated layout.

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(0);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout and one layout after measure. The async layout shouldn't happen.")
        .isEqualTo(2);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs that both the original component and the new component are compatible with.
   */
  @Test
  public void testSetRootAsyncWithCompatibleMeasureBeforeStart() {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    componentTree.setRootAsync(newComponent);

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    // Since the layout thread hasn't started the async layout, we know it will capture the updated
    // size specs when it does run

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(oldComponent.getId()))
        .describedAs("The old component spec is compatible so we shouldn't do a sync layout.")
        .isTrue();

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    ShadowLooper.runUiThreadTasks();

    // Once the async layout finishes, the main thread should have the updated layout.

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("We expect one initial layout and the async layout.")
        .isEqualTo(2);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs the original component is compatible with but the new component is incompatible with.
   */
  @Test
  public void
      testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutBeforeStart() {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    componentTree.setRootAsync(newComponent);

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(oldComponent.getId()))
        .describedAs("The old component spec is compatible so we shouldn't do a sync layout.")
        .isTrue();

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    ShadowLooper.runUiThreadTasks();

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(500);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout and one layout after measure. The async layout shouldn't happen.")
        .isEqualTo(2);
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are incompatible with.
   */
  @Test
  public void testSetRootAsyncWithIncompatibleMeasureAfterFinish() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not be used once it completes");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(0);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  public void testSetRootAsyncWithCompatibleMeasureAfterFinish() {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    componentTree.setRootAsync(newComponent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should promote the committed layout to the UI thread in measure.");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs("We expect one initial layout and the async layout.")
        .isEqualTo(2);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  public void
      testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutAfterFinish() {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    componentTree.setRootAsync(newComponent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread will calculate a new layout synchronously because the background layout isn't compatible.");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(500);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout from measure.")
        .isEqualTo(3);
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs both the original component and the new
   * component are incompatible with.
   */
  @Test
  public void testSetRootAsyncWithIncompatibleMeasureDuringLayout() throws InterruptedException {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockInPrepare.setDoNotBlockOnThisThread();
    TestDrawableComponent newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    newComponent.setTestComponentListener(blockInPrepare);

    componentTree.setRootAsync(newComponent);

    final CountDownLatch asyncLayoutFinish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                mLayoutThreadShadowLooper.runToEndOfTasks();
              }
            });

    blockInPrepare.awaitPrepareStart();

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout. We expect the main
    // thread to determine that this async layout will not be correct and that it needs to compute
    // one in measure

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(0);

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure

    blockInPrepare.allowPrepareToComplete();
    if (!asyncLayoutFinish.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    ShadowLooper.runUiThreadTasks();

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(0);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  public void testSetRootAsyncWithCompatibleMeasureDuringLayout() throws InterruptedException {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockInPrepare.setDoNotBlockOnThisThread();
    TestDrawableComponent newComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    newComponent.setTestComponentListener(blockInPrepare);

    componentTree.setRootAsync(newComponent);

    final CountDownLatch asyncLayoutFinish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                mLayoutThreadShadowLooper.runToEndOfTasks();
              }
            });

    blockInPrepare.awaitPrepareStart();

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout. We expect the main
    // thread to determine that this async layout will not be correct and that it needs to compute
    // one in measure

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(100);

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure

    blockInPrepare.allowPrepareToComplete();
    if (!asyncLayoutFinish.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    ShadowLooper.runUiThreadTasks();

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(100);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3);
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs that the original component is compatible with
   * but and the new component isn't.
   */
  @Test
  public void
      testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutDuringLayout()
          throws InterruptedException {
    Component oldComponent =
        TestDrawableComponent.create(mContext).widthPx(100).heightPx(100).color(1234).build();
    ComponentTree componentTree = ComponentTree.create(mContext, oldComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(500, SizeSpec.AT_MOST);

    componentTree.attach();
    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockInPrepare.setDoNotBlockOnThisThread();
    TestDrawableComponent newComponent =
        TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    newComponent.setTestComponentListener(blockInPrepare);

    componentTree.setRootAsync(newComponent);

    final CountDownLatch asyncLayoutFinish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                mLayoutThreadShadowLooper.runToEndOfTasks();
              }
            });

    blockInPrepare.awaitPrepareStart();

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout. We expect the main
    // thread to determine that this async layout will not be correct and that it needs to compute
    // one in measure

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue();
    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs");
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(500);

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure

    blockInPrepare.allowPrepareToComplete();
    if (!asyncLayoutFinish.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    ShadowLooper.runUiThreadTasks();

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(500);

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout from measure.")
        .isEqualTo(3);
  }

  @Test
  public void testMeasureWithUpdateStateThatCompletesFirst() throws InterruptedException {
    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    TestDrawableComponent blockingComponent =
        TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    Component rootComponent =
        Column.create(mContext)
            .child(SimpleStateUpdateEmulator.create(mContext).caller(caller).tagPrefix("counter:"))
            .child(blockingComponent)
            .minHeightPx(100)
            .build();
    LithoView lithoView = new LithoView(mContext);
    ComponentTree componentTree =
        ComponentTree.create(mContext, rootComponent).isReconciliationEnabled(true).build();
    lithoView.setComponentTree(componentTree);

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int heightSpec1 = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int heightSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);

    componentTree.attach();
    lithoView.measure(widthSpec1, heightSpec1);

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockingComponent.setTestComponentListener(blockInPrepare);

    // This is necessary because we end up posting the synchronous state update to the layout looper
    componentTree
        .getLayoutThreadHandler()
        .post(
            new Runnable() {
              @Override
              public void run() {
                blockInPrepare.setDoNotBlockOnThisThread();
              }
            },
            "test");

    final CountDownLatch asyncStateUpdate =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                blockInPrepare.awaitPrepareStart();
                blockInPrepare.setDoNotBlockOnThisThread();
                caller.increment();
                mBackgroundLayoutLooperRule.runToEndOfTasksSync();
                blockInPrepare.allowPrepareToComplete();
              }
            });

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse();
    lithoView.measure(widthSpec2, heightSpec2);
    if (!asyncStateUpdate.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }
    ShadowLooper.runUiThreadTasks();

    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    // We now want to assert that we are the right size and also the state update was applied.
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(1000);
    assertThat(lithoView.getHeight()).isEqualTo(1000);
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0).getTag()).isEqualTo("counter:2");

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one layout during setup, one from measure that will be thrown away and one from the state update.")
        .isEqualTo(3);
  }

  @Test
  public void testUpdateStateWithMeasureThatStartsBeforeUpdateStateCompletes()
      throws InterruptedException {
    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    TestDrawableComponent blockingComponent =
        TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    Component rootComponent =
        Column.create(mContext)
            .child(SimpleStateUpdateEmulator.create(mContext).caller(caller).tagPrefix("counter:"))
            .child(blockingComponent)
            .minHeightPx(100)
            .build();
    LithoView lithoView = new LithoView(mContext);

    // We need to turn reconciliation off so that the BlockInPrepare always executes
    ComponentTree componentTree =
        ComponentTree.create(mContext, rootComponent).isReconciliationEnabled(false).build();
    lithoView.setComponentTree(componentTree);

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int heightSpec1 = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int heightSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);

    componentTree.attach();
    lithoView.measure(widthSpec1, heightSpec1);

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockInPrepare.setDoNotBlockOnThisThread();
    blockingComponent.setTestComponentListener(blockInPrepare);

    final CountDownLatch asyncStateUpdate =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                caller.increment();

                // The sync state update will be posted to the layout thread
                mBackgroundLayoutLooperRule.runToEndOfTasksSync();
              }
            });

    blockInPrepare.awaitPrepareStart();
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse();
    lithoView.measure(widthSpec2, heightSpec2);
    blockInPrepare.allowPrepareToComplete();

    if (!asyncStateUpdate.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }
    ShadowLooper.runUiThreadTasks();

    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    // We now want to assert that we are the right size and also the state update was applied.
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().getHeight()).isEqualTo(1000);
    assertThat(lithoView.getHeight()).isEqualTo(1000);
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0).getTag()).isEqualTo("counter:2");

    assertThat(mLithoStatsRule.getComponentCalculateLayoutCount())
        .describedAs(
            "We expect one layout during setup, one from measure and one from the state update that will be thrown away.")
        .isEqualTo(3);
  }

  @Test
  public void testLayoutStateNotCommittedTwiceWithLayoutStateFutures() throws InterruptedException {
    TestDrawableComponent component =
        TestDrawableComponent.create(mContext).flexGrow(1).color(1234).build();
    int widthSpec = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    ComponentTree componentTree = ComponentTree.create(mContext, component).build();
    componentTree.setLithoView(new LithoView(mContext));

    // It's called a measure listener, but it is invoked every time a new layout is committed.
    AtomicInteger commitCount = new AtomicInteger(0);
    componentTree.addMeasureListener(
        new ComponentTree.MeasureListener() {
          @Override
          public void onSetRootAndSizeSpec(
              int layoutVersion, int width, int height, boolean stateUpdate) {
            commitCount.incrementAndGet();
          }
        });
    componentTree.attach();

    final TestDrawableComponent.BlockInPrepareComponentListener blockInPrepare =
        new TestDrawableComponent.BlockInPrepareComponentListener();
    blockInPrepare.setDoNotBlockOnThisThread();
    component.setTestComponentListener(blockInPrepare);

    final CountDownLatch asyncLayout1Finish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setSizeSpec(widthSpec, heightSpec, new Size());
              }
            });

    blockInPrepare.awaitPrepareStart();

    final CountDownLatch asyncLayout2Finish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setSizeSpec(widthSpec, heightSpec, new Size());
              }
            });

    ComponentTree.LayoutStateFuture future = componentTree.getLayoutStateFutures().get(0);

    int timeSpentWaiting = 0;
    while (future.getWaitingCount() != 2 && timeSpentWaiting < 5000) {
      Thread.sleep(10);
      timeSpentWaiting += 10;
    }

    assertThat(future.getWaitingCount())
        .describedAs("Make sure the second thread is waiting on the first Future")
        .isEqualTo(2);

    blockInPrepare.allowPrepareToComplete();
    if (!asyncLayout1Finish.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    blockInPrepare.allowPrepareToComplete();
    if (!asyncLayout2Finish.await(5, TimeUnit.SECONDS)) {
      // Timing out here probably means we didn't use LayoutStateFutures and we are blocking on the
      // "block in prepare" semaphore.
      throw new RuntimeException("Timeout!");
    }

    assertThat(componentTree.getRoot()).isEqualTo(component);
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue();
    assertThat(commitCount.get()).isEqualTo(1);
  }

  @Test
  public void testSetRootAfterRelease() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();

    componentTree.release();

    // Verify we don't crash
    componentTree.setRoot(TestDrawableComponent.create(mContext).build());
  }

  @Test
  public void testCachedValues() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    assertThat(componentTree.getCachedValue("key1")).isNull();
    componentTree.putCachedValue("key1", "value1");
    assertThat(componentTree.getCachedValue("key1")).isEqualTo("value1");
    assertThat(componentTree.getCachedValue("key2")).isNull();
  }

  private static boolean isAttached(ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mIsAttached");
  }

  // TODO(T37885964): Fix me
  @Test
  @Ignore
  public void testCreateOneLayoutStateFuture() {
    MyTestComponent root1 = new MyTestComponent("MyTestComponent");
    root1.testId = 1;

    ThreadPoolLayoutHandler handler =
        ThreadPoolLayoutHandler.getNewInstance(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1).layoutThreadHandler(handler).build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    final CountDownLatch unlockWaitingOnCreateLayout = new CountDownLatch(1);

    MyTestComponent root2 = new MyTestComponent("MyTestComponent");
    root2.testId = 2;
    root2.unlockWaitingOnCreateLayout = unlockWaitingOnCreateLayout;

    componentTree.setRootAsync(root2);

    try {
      unlockWaitingOnCreateLayout.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertEquals(1, componentTree.getLayoutStateFutures().size());
    ComponentTree.LayoutStateFuture layoutStateFuture =
        componentTree.getLayoutStateFutures().get(0);

    handler.post(
        new Runnable() {
          @Override
          public void run() {
            assertEquals(1, layoutStateFuture.getWaitingCount());
            layoutStateFuture.runAndGet(LayoutState.CalculateLayoutSource.SET_ROOT_ASYNC);
            assertEquals(0, layoutStateFuture.getWaitingCount());
            assertEquals(0, componentTree.getLayoutStateFutures().size());
          }
        },
        "tag");
  }

  @Test
  public void testLayoutStateFutureMainWaitingOnBg() {
    MyTestComponent root1 = new MyTestComponent("MyTestComponent");
    root1.testId = 1;

    ThreadPoolLayoutHandler handler =
        ThreadPoolLayoutHandler.getNewInstance(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1).layoutThreadHandler(handler).build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    final CountDownLatch unlockWaitingOnCreateLayout = new CountDownLatch(1);
    final CountDownLatch lockOnCreateLayoutFinish = new CountDownLatch(1);

    MyTestComponent root2 = new MyTestComponent("MyTestComponent");
    root2.testId = 2;
    root2.unlockWaitingOnCreateLayout = unlockWaitingOnCreateLayout;
    root2.lockOnCreateLayoutFinish = lockOnCreateLayoutFinish;

    MyTestComponent root3 = new MyTestComponent("MyTestComponent");
    root3.testId = 2;

    componentTree.setRootAsync(root2);

    // Wait for first thread to get into onCreateLayout
    try {
      unlockWaitingOnCreateLayout.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertEquals(1, componentTree.getLayoutStateFutures().size());

    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setRoot(root3);

                // At this point, the current thread is unblocked after waiting for the first to
                // finish layout.
                // TODO T62608123 This actually never runs
                assertFalse(root3.hasRunLayout);
                assertTrue(root2.hasRunLayout);
              }
            });

    // Schedule second thread to start
    thread.start();

    // Unblock the first thread to continue through onCreateLayout. The second thread will only
    // unblock once the first thread's onCreateLayout finishes
    lockOnCreateLayoutFinish.countDown();
  }

  @Test
  public void testRecalculateDifferentRoots() {
    MyTestComponent root1 = new MyTestComponent("MyTestComponent");
    root1.testId = 1;

    ThreadPoolLayoutHandler handler =
        ThreadPoolLayoutHandler.getNewInstance(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1).layoutThreadHandler(handler).build();

    componentTree.setLithoView(new LithoView(mContext));
    componentTree.measure(mWidthSpec, mHeightSpec, new int[2], false);
    componentTree.attach();

    final CountDownLatch unlockWaitingOnCreateLayout = new CountDownLatch(1);
    final CountDownLatch lockOnCreateLayoutFinish = new CountDownLatch(1);

    MyTestComponent root2 = new MyTestComponent("MyTestComponent");
    root2.testId = 2;
    root2.unlockWaitingOnCreateLayout = unlockWaitingOnCreateLayout;
    root2.lockOnCreateLayoutFinish = lockOnCreateLayoutFinish;

    MyTestComponent root3 = new MyTestComponent("MyTestComponent");
    root3.testId = 2;

    componentTree.setRootAsync(root2);

    // Wait for first thread to get into onCreateLayout
    try {
      unlockWaitingOnCreateLayout.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertEquals(1, componentTree.getLayoutStateFutures().size());

    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setRoot(root3);

                // At this point, the current thread is unblocked after waiting for the first to
                // finish layout.
                // TODO T62608123 This actually never runs
                assertTrue(root3.hasRunLayout);
                assertTrue(root2.hasRunLayout);
              }
            });

    // Schedule second thread to start
    thread.start();

    // Unblock the first thread to continue through onCreateLayout. The second thread will only
    // unblock once the first thread's onCreateLayout finishes
    lockOnCreateLayoutFinish.countDown();
  }

  @Test
  public void testVersioningCalculate() {
    MyTestComponent root1 = new MyTestComponent("MyTestComponent");
    root1.testId = 1;

    ComponentTree componentTree = ComponentTree.create(mContext).build();

    componentTree.setVersionedRootAndSizeSpec(root1, mWidthSpec, mHeightSpec, new Size(), null, 0);

    LayoutState backgroundLayoutState = getInternalState(componentTree, "mBackgroundLayoutState");
    assertEquals(root1.testId, backgroundLayoutState.getRootComponent().getId());

    MyTestComponent root2 = new MyTestComponent("MyTestComponent");
    root2.testId = 2;

    MyTestComponent root3 = new MyTestComponent("MyTestComponent");
    root3.testId = 3;

    componentTree.setVersionedRootAndSizeSpec(root3, mWidthSpec, mHeightSpec, new Size(), null, 2);

    backgroundLayoutState = getInternalState(componentTree, "mBackgroundLayoutState");
    assertEquals(root3.testId, backgroundLayoutState.getRootComponent().getId());

    componentTree.setVersionedRootAndSizeSpec(root2, mWidthSpec, mHeightSpec, new Size(), null, 1);

    backgroundLayoutState = getInternalState(componentTree, "mBackgroundLayoutState");
    assertEquals(root3.testId, backgroundLayoutState.getRootComponent().getId());
  }

  @Test
  public void testAttachFromListenerDoesntCrash() {
    final Component component = TestLayoutComponent.create(mContext).build();
    final LithoView lithoView = new LithoView(mContext);

    final ComponentTree componentTree = ComponentTree.create(mContext, component).build();
    lithoView.setComponentTree(componentTree);

    componentTree.setNewLayoutStateReadyListener(
        new ComponentTree.NewLayoutStateReadyListener() {
          @Override
          public void onNewLayoutStateReady(ComponentTree componentTree) {
            lithoView.onAttachedToWindow();
          }
        });

    componentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);
  }

  @Test
  public void testDetachFromListenerDoesntCrash() {
    final Component component = TestLayoutComponent.create(mContext).build();
    final LithoView lithoView = new LithoView(mContext);

    final ComponentTree componentTree = ComponentTree.create(mContext, component).build();
    lithoView.setComponentTree(componentTree);
    lithoView.onAttachedToWindow();

    componentTree.setNewLayoutStateReadyListener(
        new ComponentTree.NewLayoutStateReadyListener() {
          @Override
          public void onNewLayoutStateReady(ComponentTree componentTree) {
            lithoView.onDetachedFromWindow();
            componentTree.clearLithoView();
          }
        });

    componentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);
  }

  class MyTestComponent extends Component {

    CountDownLatch unlockWaitingOnCreateLayout;
    CountDownLatch lockOnCreateLayoutFinish;
    int testId;
    boolean hasRunLayout;

    protected MyTestComponent(String simpleName) {
      super(simpleName);
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      if (unlockWaitingOnCreateLayout != null) {
        unlockWaitingOnCreateLayout.countDown();
      }

      if (lockOnCreateLayoutFinish != null) {
        try {
          lockOnCreateLayoutFinish.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      hasRunLayout = true;
      return Column.create(c).build();
    }

    @Override
    protected int getId() {
      return testId;
    }
  }

  private static boolean componentTreeHasSizeSpec(ComponentTree componentTree) {
    try {
      boolean hasCssSpec;
      // Need to hold the lock on componentTree here otherwise the invocation of hasCssSpec
      // will fail.
      synchronized (componentTree) {
        hasCssSpec = Whitebox.invokeMethod(componentTree, "hasSizeSpec");
      }
      return hasCssSpec;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to invoke hasSizeSpec on ComponentTree for: " + e);
    }
  }

  private static CountDownLatch runOnBackgroundThread(final Runnable runnable) {
    final CountDownLatch latch = new CountDownLatch(1);

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                runnable.run();
                latch.countDown();
              }
            })
        .start();

    return latch;
  }
}
