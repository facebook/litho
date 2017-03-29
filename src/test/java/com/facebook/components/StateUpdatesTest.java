/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Looper;

import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static com.facebook.litho.ComponentLifecycle.StateUpdate;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(ComponentsTestRunner.class)
public class StateUpdatesTest {
  private static final int LIFECYCLE_TEST_ID = 1;
  private static final int INITIAL_COUNT_STATE_VALUE = 4;

  private int mWidthSpec;
  private int mHeightSpec;

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }

    @Override
    protected boolean hasState() {
      return true;
    }

    @Override
    protected void createInitialState(ComponentContext c, Component component) {
      TestComponent testComponent = (TestComponent) component;
      testComponent.mStateContainer.mCount = INITIAL_COUNT_STATE_VALUE;
    }

    @Override
    protected void transferState(
        ComponentContext c,
        StateContainer stateContainer,
        Component component) {
      TestStateContainer stateContainerImpl = (TestStateContainer) stateContainer;
      TestComponent newTestComponent = (TestComponent) component;
      newTestComponent.mStateContainer.mCount = stateContainerImpl.mCount;
    }
  };

  private static class TestStateUpdate implements StateUpdate {

    @Override
    public void updateState(StateContainer stateContainer, Component component) {
      TestStateContainer stateContainerImpl = (TestStateContainer) stateContainer;
      TestComponent componentImpl = (TestComponent) component;
      System.out.println("1 " + componentImpl.mStateContainer);
      System.out.println("2 " + stateContainerImpl);
      componentImpl.mStateContainer.mCount = stateContainerImpl.mCount + 1;
    }
  }

  static class TestComponent<L extends ComponentLifecycle>
      extends Component<L> implements Cloneable {

    private TestStateContainer mStateContainer;
    private TestComponent shallowCopy;
    private int mId;
    private static final AtomicInteger sIdGenerator = new AtomicInteger(0);

    public TestComponent(L component) {
      super(component);
      mStateContainer = new TestStateContainer();
      mId = sIdGenerator.getAndIncrement();
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }

    int getCount() {
      return mStateContainer.mCount;
    }

    @Override
    public Component makeShallowCopy() {
      return this;
    }

    @Override
    Component makeShallowCopyWithNewId() {
      shallowCopy = (TestComponent) super.makeShallowCopy();
      shallowCopy.mId = sIdGenerator.getAndIncrement();
      return shallowCopy;
    }

    TestComponent getComponentForStateUpdate() {
      if (shallowCopy == null) {
        return this;
      }
      return shallowCopy.getComponentForStateUpdate();
    }

    @Override
    protected int getId() {
      return mId;
    }

    @Override
    protected StateContainer getStateContainer() {
      return mStateContainer;
    }
  }

  static class TestStateContainer implements StateContainer {
    protected int mCount;
  }

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private TestComponent mTestComponent;
  private ComponentTree mComponentTree;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
    mTestComponent = new TestComponent(mLifecycle);

    mComponentTree = ComponentTree.create(mContext, mTestComponent)
        .incrementalMount(false)
        .build();
    final ComponentView componentView = new ComponentView(mContext);
    componentView.setComponent(mComponentTree);
    componentView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(componentView);
  }

  @Test(expected = RuntimeException.class)
  public void testCrashOnSameComponentKey() {
    final Component child1 = new TestComponent(mLifecycle);
    final Component child2 = new TestComponent(mLifecycle);
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(child1)
            .child(child2)
            .build();
      }
    };
    final ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .build();
    final ComponentView componentView = new ComponentView(mContext);
    componentView.setComponent(componentTree);
    componentView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(componentView);
  }

  @Test(expected = RuntimeException.class)
  public void testCrashOnSameComponentKeyNestedContainers() {
    final Component child1 = new TestComponent(mLifecycle);
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(child1))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
