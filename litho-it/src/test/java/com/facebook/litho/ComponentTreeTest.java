/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.os.Looper;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.powermock.reflect.Whitebox.getInternalState;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeTest {

  private int mWidthSpec;
  private int mWidthSpec2;
  private int mHeightSpec;
  private int mHeightSpec2;

  private Component mComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

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

  private void creationCommonChecks(ComponentTree componentTree) {
    // Not view or attached yet
    Assert.assertNull(getLithoView(componentTree));
    Assert.assertFalse(isAttached(componentTree));

    // No measure spec from view yet.
    Assert.assertFalse(
        (Boolean) Whitebox.getInternalState(componentTree, "mHasViewMeasureSpec"));

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
    assertThat(getInternalState(componentTree, "mWidthSpec")).isEqualTo(widthSpec);

    assertThat(getInternalState(componentTree, "mHeightSpec")).isEqualTo(heightSpec);

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
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();

    creationCommonChecks(componentTree);

    // Both the main thread and the background layout state shouldn't be calculated yet.
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mMainThreadLayoutState"));
    Assert.assertNull(Whitebox.getInternalState(componentTree, "mBackgroundLayoutState"));

    Assert.assertFalse(componentTreeHasSizeSpec(componentTree));
  }

  @Test
  public void testSetSizeSpec() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .incrementalMount(false)
            .layoutDiffing(false)
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
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    componentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);

    // Only fields changed but no layout is done yet.

    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue();
    assertThat(getInternalState(componentTree, "mWidthSpec")).isEqualTo(mWidthSpec);
    assertThat(getInternalState(componentTree, "mHeightSpec")).isEqualTo(mHeightSpec);
    Assert.assertNull(getInternalState(componentTree, "mMainThreadLayoutState"));
    Assert.assertNull(getInternalState(componentTree, "mBackgroundLayoutState"));

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runOneTask();

    // Since this happens post creation, it's not in general safe to update the main thread layout
    // state synchronously, so the result should be in the background layout state
    postSizeSpecChecks(componentTree, "mBackgroundLayoutState");
  }

  @Test
  public void testSetSizeSpecAsyncThenSyncBeforeRunningTask() {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .incrementalMount(false)
            .layoutDiffing(false)
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
            .incrementalMount(false)
            .layoutDiffing(false)
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
            .incrementalMount(false)
            .layoutDiffing(false)
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
  public void testSetCompatibleSizeSpec() {
    ComponentTree componentTree =
        create(mContext, mComponent)
            .incrementalMount(false)
            .layoutDiffing(false)
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
            .incrementalMount(false)
            .layoutDiffing(false)
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
  public void testSetInput() {
    Component component = TestLayoutComponent.create(mContext)
        .build();

    ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
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
  public void testSetComponentFromView() {
    Component component1 = TestDrawableComponent.create(mContext)
        .build();
    ComponentTree componentTree1 = ComponentTree.create(
        mContext,
        component1)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    Component component2 = TestDrawableComponent.create(mContext)
        .build();
    ComponentTree componentTree2 = ComponentTree.create(
        mContext,
        component2)
        .incrementalMount(false)
        .layoutDiffing(false)
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
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);

    assertThat(componentTree).isEqualTo(lithoView.getComponentTree());

    componentTree.release();

    assertThat(lithoView.getComponentTree()).isNull();
  }

  @Test
  public void testsetTreeToTwoViewsBothAttached() {
    Component component = TestDrawableComponent.create(mContext)
        .build();

    ComponentTree componentTree = ComponentTree.create(
        mContext,
        component)
        .incrementalMount(false)
        .layoutDiffing(false)
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
        .incrementalMount(false)
        .layoutDiffing(false)
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

  private static LithoView getLithoView(ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mLithoView");
  }

  private static boolean isAttached(ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mIsAttached");
  }

  private static ComponentTree getComponentTree(LithoView lithoView) {
    return Whitebox.getInternalState(lithoView, "mComponentTree");
  }

  private static boolean componentTreeHasSizeSpec(ComponentTree componentTree) {
    try {
      boolean hasCssSpec;
      // Need to hold the lock on componentTree here otherwise the invocation of hasCssSpec
      // will fail.
      synchronized (componentTree) {
        hasCssSpec = Whitebox.invokeMethod(componentTree, ComponentTree.class, "hasSizeSpec");
      }
      return hasCssSpec;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to invoke hasSizeSpec on ComponentTree for: "+e);
    }
  }
}
