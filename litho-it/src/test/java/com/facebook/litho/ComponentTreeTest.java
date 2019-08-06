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

import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import android.os.Looper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeTest {

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
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext)
        .build();

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));

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
    Assert.assertNull(getLithoView(componentTree));
    Assert.assertFalse(isAttached(componentTree));

    // The component input should be the one we passed in
    Assert.assertSame(
        mComponent,
        Whitebox.getInternalState(componentTree, "mRoot"));
  }

  private void postSizeSpecChecks(
      ComponentTree componentTree,
      String layoutStateVariableName) {
    postSizeSpecChecks(
        componentTree,
        layoutStateVariableName,
        mWidthSpec,
        mHeightSpec);
  }

  private void postSizeSpecChecks(
      ComponentTree componentTree,
      String layoutStateVariableName,
      int widthSpec,
      int heightSpec) {
    // Spec specified in create

    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue();
    assertThat((int) getInternalState(componentTree, "mWidthSpec"))
        .isEqualTo(widthSpec);

    assertThat((int) getInternalState(componentTree, "mHeightSpec"))
        .isEqualTo(heightSpec);

    LayoutState mainThreadLayoutState = getInternalState(
        componentTree, "mMainThreadLayoutState");

    LayoutState backgroundLayoutState = getInternalState(
        componentTree, "mBackgroundLayoutState");

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
    assertThat(layoutState.isCompatibleComponentAndSpec(
        mComponent.getId(),
        widthSpec,
        heightSpec)).isTrue();
  }

  @Test
  public void testCreate() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .build();

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
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .build();
    componentTree.setSizeSpec(mWidthSpec, mHeightSpec);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testSetSizeSpecAsync() {
    ComponentTree componentTree =
        create(mContext, mComponent)
            .build();
    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    // Only fields changed but no layout is done yet.

    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue();
    assertThat((int) getInternalState(componentTree, "mWidthSpec"))
        .isEqualTo(mWidthSpec);
    assertThat((int) getInternalState(componentTree, "mHeightSpec"))
        .isEqualTo(mHeightSpec);
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

  @Test
  public void testSetSizeSpecAsyncThenSyncBeforeRunningTask() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .build();

    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    componentTree.setSizeSpec(mWidthSpec2, mHeightSpec2);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(
        componentTree,
        "mBackgroundLayoutState",
        mWidthSpec2,
        mHeightSpec2);
  }

  @Test
  public void testSetSizeSpecAsyncThenSyncAfterRunningTask() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .build();
    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    componentTree.setSizeSpec(mWidthSpec2, mHeightSpec2);

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(
        componentTree,
        "mBackgroundLayoutState",
        mWidthSpec2,
        mHeightSpec2);
  }

  @Test
  public void testSetSizeSpecWithOutput() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .build();

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
    ComponentTree componentTree =
        create(mContext, mComponent)
            .build();

    Size size = new Size();

    componentTree.setSizeSpec(
        makeSizeSpec(100, AT_MOST),
        makeSizeSpec(100, AT_MOST),
        size);

    assertEquals(100, size.width, 0.0);
    assertEquals(100, size.height, 0.0);

    LayoutState firstLayoutState = componentTree.getBackgroundLayoutState();
    assertThat(firstLayoutState).isNotNull();

    componentTree.setSizeSpec(
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        size);

    assertEquals(100, size.width, 0.0);
    assertEquals(100, size.height, 0.0);

    assertThat(componentTree.getBackgroundLayoutState()).isEqualTo(firstLayoutState);
  }

  @Test
  public void testSetCompatibleSizeSpecWithDifferentRoot() {
    ComponentTree componentTree =
        create(mContext, mComponent)
            .build();

    Size size = new Size();

    componentTree.setSizeSpec(
        makeSizeSpec(100, AT_MOST),
        makeSizeSpec(100, AT_MOST),
        size);

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

    assertThat(componentTree.getBackgroundLayoutState()).isNull();

    componentTree.measure(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), new int[2], true);

    final ComponentContext c =
        getInternalState(componentTree.getMainThreadLayoutState(), "mContext");
    assertThat(c.getTreeProps()).isNotNull();
    assertThat(c.getTreeProps().get(Object.class)).isEqualTo(treeProps.get(Object.class));
  }

  @Test
  public void testSetInput() {
    Component component = TestLayoutComponent.create(mContext)
        .build();

    ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .build();

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
    Component component1 = TestDrawableComponent.create(mContext)
        .build();
    ComponentTree componentTree1 = ComponentTree.create(
        mContext,
        component1)
        .build();

    Component component2 = TestDrawableComponent.create(mContext)
        .build();
    ComponentTree componentTree2 = ComponentTree.create(
        mContext,
        component2)
        .build();

    Assert.assertNull(getLithoView(componentTree1));
    Assert.assertNull(getLithoView(componentTree2));

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree1);

    Assert.assertNotNull(getLithoView(componentTree1));
    Assert.assertNull(getLithoView(componentTree2));

    lithoView.setComponentTree(componentTree2);

    Assert.assertNull(getLithoView(componentTree1));
    Assert.assertNotNull(getLithoView(componentTree2));
  }

  @Test
  public void testComponentTreeReleaseClearsView() {
    Component component = TestDrawableComponent.create(mContext)
        .build();
    ComponentTree componentTree = create(
        mContext,
        component)
        .build();

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);

    assertThat(componentTree).isEqualTo(lithoView.getComponentTree());

    componentTree.release();

    assertThat(lithoView.getComponentTree()).isNull();
  }

  @Test
  public void testSetTreeToTwoViewsBothAttached() {
    Component component = TestDrawableComponent.create(mContext)
        .build();

    ComponentTree componentTree = ComponentTree.create(
        mContext,
        component)
        .build();

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
    Component component = TestDrawableComponent.create(mContext)
        .build();

    ComponentTree componentTree = create(
        mContext,
        component)
        .build();

    // Attach first view.
    LithoView lithoView1 = new LithoView(mContext);
    lithoView1.setComponentTree(componentTree);

    assertThat(getLithoView(componentTree)).isEqualTo(lithoView1);
    assertThat(getComponentTree(lithoView1)).isEqualTo(componentTree);

    // Attach second view.
    LithoView lithoView2 = new LithoView(mContext);

    Assert.assertNull(getComponentTree(lithoView2));

    lithoView2.setComponentTree(componentTree);

    assertThat(getLithoView(componentTree)).isEqualTo(lithoView2);
    assertThat(getComponentTree(lithoView2)).isEqualTo(componentTree);

    Assert.assertNull(getComponentTree(lithoView1));
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

  @Test
  public void testMeasureWithIncompatibleSetRootAsyncBeforeStart() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);
    componentTree.attach();

    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    // Since the layout thread hasn't started the async layout, we know it will capture the updated
    // size specs when it does run

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(mComponent.getId()))
        .isTrue();

    runOnBackgroundThreadSync(
        new Runnable() {
          @Override
          public void run() {
            mLayoutThreadShadowLooper.runToEndOfTasks();
          }
        });
    ShadowLooper.runUiThreadTasks();

    // Once the async layout finishes, the main thread should have the updated layout.

    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
  }

  @Test
  public void testMeasureWithIncompatibleSetRootAsyncThatFinishes() {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);
    componentTree.attach();

    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    Component newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    componentTree.setRootAsync(newComponent);

    runOnBackgroundThreadSync(
        new Runnable() {
          @Override
          public void run() {
            // "Commit" layout (it will fail since it doesn't have compatible size specs)
            mLayoutThreadShadowLooper.runToEndOfTasks();
          }
        });

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not be used once it completes");
  }

  @Test
  public void testMeasureWithIncompatibleSetRootAsync() throws InterruptedException {
    ComponentTree componentTree = ComponentTree.create(mContext, mComponent).build();
    componentTree.setLithoView(new LithoView(mContext));

    int widthSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec1 = SizeSpec.makeSizeSpec(1000, SizeSpec.AT_MOST);
    int widthSpec2 = SizeSpec.makeSizeSpec(1000, SizeSpec.EXACTLY);
    int heightSpec2 = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);
    componentTree.attach();

    componentTree.measure(widthSpec1, heightSpec1, new int[2], false);

    final CountDownLatch unblockAsyncPrepare = new CountDownLatch(1);
    final CountDownLatch onAsyncPrepareStart = new CountDownLatch(1);
    TestDrawableComponent newComponent = TestDrawableComponent.create(mContext).color(1234).build();
    newComponent.setTestComponentListener(
        new TestDrawableComponent.TestComponentListener() {
          @Override
          public void onPrepare() {
            // We only want to block/wait for the component instance that is created async
            if (ThreadUtils.isMainThread()) {
              return;
            }

            onAsyncPrepareStart.countDown();

            try {
              if (!unblockAsyncPrepare.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for prepare to unblock!");
              }
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        });

    componentTree.setRootAsync(newComponent);

    final CountDownLatch asyncLayoutFinish =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                mLayoutThreadShadowLooper.runToEndOfTasks();
              }
            });

    if (!onAsyncPrepareStart.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout. We expect the main
    // thread to determine that this async layout will not be correct and that it needs to compute
    // one in measure

    componentTree.measure(widthSpec2, heightSpec2, new int[2], false);

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue()
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs");

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure

    unblockAsyncPrepare.countDown();
    if (!asyncLayoutFinish.await(5, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout!");
    }

    assertThat(componentTree.getRoot()).isEqualTo(newComponent);
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue();
    assertThat(componentTree.getMainThreadLayoutState().isForComponentId(newComponent.getId()))
        .isTrue();
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

  private static LithoView getLithoView(ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mLithoView");
  }

  private static boolean isAttached(ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mIsAttached");
  }

  private static ComponentTree getComponentTree(LithoView lithoView) {
    return Whitebox.getInternalState(lithoView, "mComponentTree");
  }

  // TODO(T37885964): Fix me
  //@Test
  public void testCreateOneLayoutStateFuture() {
    MyTestComponent root1 = new MyTestComponent("MyTestComponent");
    root1.testId = 1;

    ThreadPoolLayoutHandler handler =
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1)
            .layoutThreadHandler(handler)
            .build();

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
            layoutStateFuture.runAndGet();
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
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1)
            .layoutThreadHandler(handler)
            .build();

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
                componentTree.calculateLayoutState(
                    mContext,
                    root3,
                    mWidthSpec,
                    mHeightSpec,
                    true,
                    null,
                    null,
                    LayoutState.CalculateLayoutSource.TEST,
                    null);

                // At this point, the current thread is unblocked after waiting for the first to
                // finish layout.
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
        new ThreadPoolLayoutHandler(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    ComponentTree componentTree =
        ComponentTree.create(mContext, root1)
            .layoutThreadHandler(handler)
            .build();

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
                componentTree.calculateLayoutState(
                    mContext,
                    root3,
                    mWidthSpec,
                    mHeightSpec,
                    true,
                    null,
                    null,
                    LayoutState.CalculateLayoutSource.TEST,
                    null);

                // At this point, the current thread is unblocked after waiting for the first to
                // finish layout.
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
      throw new IllegalArgumentException("Failed to invoke hasSizeSpec on ComponentTree for: "+e);
    }
  }

  private static void runOnBackgroundThreadSync(final Runnable runnable) {
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

    try {
      assertThat(latch.await(5000, TimeUnit.MILLISECONDS)).isTrue();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
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
