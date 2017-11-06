/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Diff;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @prop-required prop1 int
 * @prop-required prop6 long
 * @prop-required prop3 java.lang.Object
 * @prop-required prop4 char[]
 * @prop-optional prop2 boolean
 * @prop-required prop8 long
 * @prop-required prop7 java.lang.CharSequence
 * @prop-required prop5 char
 */
@TargetApi(17)
public final class TestMount<S extends View> extends Component<TestMount> {
  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private TestMountStateContainer mStateContainer;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  int prop1;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  long prop6;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  Object prop3;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  char[] prop4;

  @Prop(
      resType = ResType.NONE,
      optional = true
  )
  boolean prop2 = TestMountSpec.prop2;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  long prop8;

  @Prop(
      resType = ResType.STRING,
      optional = false
  )
  CharSequence prop7;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  char prop5;

  TestTreeProp treeProp;

  Long measureOutput;

  Integer boundsDefinedOutput;

  EventHandler testEventHandler;

  private TestMount() {
    super();
    mStateContainer = new TestMountStateContainer();
  }

  @Override
  protected ComponentLifecycle.StateContainer getStateContainer() {
    return mStateContainer;
  }

  @Override
  public String getSimpleName() {
    return "TestMount";
  }

  @Override
  public boolean isEquivalentTo(Component<?> other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    TestMount testMountRef = (TestMount) other;
    if (this.getId() == testMountRef.getId()) {
      return true;
    }
    if (prop1 != testMountRef.prop1) {
      return false;
    }
    if (prop6 != testMountRef.prop6) {
      return false;
    }
    if (prop3 != null ? !prop3.equals(testMountRef.prop3) : testMountRef.prop3 != null) {
      return false;
    }
    if (!Arrays.equals(prop4, testMountRef.prop4)) {
      return false;
    }
    if (prop2 != testMountRef.prop2) {
      return false;
    }
    if (prop8 != testMountRef.prop8) {
      return false;
    }
    if (prop7 != null ? !prop7.equals(testMountRef.prop7) : testMountRef.prop7 != null) {
      return false;
    }
    if (prop5 != testMountRef.prop5) {
      return false;
    }
    if (mStateContainer.state1 != testMountRef.mStateContainer.state1) {
      return false;
    }
    if (mStateContainer.state2 != null ? !mStateContainer.state2.equals(testMountRef.mStateContainer.state2) : testMountRef.mStateContainer.state2 != null) {
      return false;
    }
    if (treeProp != null ? !treeProp.equals(testMountRef.treeProp) : testMountRef.treeProp != null) {
      return false;
    }
    return true;
  }

  @Override
  protected void copyInterStageImpl(Component component) {
    TestMount testMountRef = (TestMount) component;
    measureOutput = testMountRef.measureOutput;
    boundsDefinedOutput = testMountRef.boundsDefinedOutput;
  }

  private UpdateCurrentStateStateUpdate createUpdateCurrentStateStateUpdate(int someParam) {
    return new UpdateCurrentStateStateUpdate(someParam);
  }

  @Override
  public TestMount makeShallowCopy() {
    TestMount component = (TestMount) super.makeShallowCopy();
    component.measureOutput = null;
    component.boundsDefinedOutput = null;
    component.mStateContainer = new TestMountStateContainer();
    return component;
  }

  @Override
  protected void populateTreeProps(Component _abstract, TreeProps treeProps) {
    if (treeProps == null) {
      return;
    }
    final TestMount _ref = (TestMount) _abstract;
    _ref.treeProp = treeProps.get(com.facebook.litho.processor.integration.resources.TestTreeProp.class);
  }

  @Override
  protected TreeProps getTreePropsForChildren(ComponentContext c, Component _abstract,
      TreeProps parentTreeProps) {
    final TestMount _ref = (TestMount) _abstract;
    final TreeProps childTreeProps = TreeProps.copy(parentTreeProps);
    childTreeProps.put(com.facebook.litho.processor.integration.resources.TestTreeProp.class, TestMountSpec.onCreateFeedPrefetcherProp(
        (ComponentContext) c,
        (long) _ref.prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    Output<Boolean> prop2 = acquireOutput();
    Output<Object> prop3 = acquireOutput();
    TestMountSpec.onLoadStyle(
        (ComponentContext) c,
        prop2,
        prop3);
    if (prop2.get() != null) {
      _ref.prop2 = prop2.get();
    }
    releaseOutput(prop2);
    if (prop3.get() != null) {
      _ref.prop3 = prop3.get();
    }
    releaseOutput(prop3);
  }

  @Override
  protected void createInitialState(ComponentContext c, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    StateValue<S> state2 = new StateValue<>();
    TestMountSpec.createInitialState(
        (ComponentContext) c,
        (int) _ref.prop1,
        state2);
    if (state2.get() != null) {
      _ref.mStateContainer.state2 = state2.get();
    }
  }

  @Override
  protected void onMeasure(ComponentContext context, ComponentLayout layout, int widthSpec,
      int heightSpec, Size size, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    Output<Long> measureOutput = acquireOutput();
    TestMountSpec.onMeasure(
        (ComponentContext) context,
        (ComponentLayout) layout,
        (int) widthSpec,
        (int) heightSpec,
        (Size) size,
        measureOutput);
    _ref.measureOutput = measureOutput.get();
    releaseOutput(measureOutput);
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout,
      Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    Output<Integer> boundsDefinedOutput = acquireOutput();
    TestMountSpec.onBoundsDefined(
        (ComponentContext) c,
        (ComponentLayout) layout,
        (Object) _ref.prop3,
        (char[]) _ref.prop4,
        (Long) _ref.measureOutput,
        boundsDefinedOutput);
    _ref.boundsDefinedOutput = boundsDefinedOutput.get();
    releaseOutput(boundsDefinedOutput);
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    Object _result = (Object) TestMountSpec.onCreateMountContent(
        (ComponentContext) c);
    return _result;
  }

  @Override
  protected void onMount(ComponentContext c, Object v, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.onMount(
        (ComponentContext) c,
        (Drawable) v,
        (boolean) _ref.prop2,
        (long) _ref.mStateContainer.state1,
        (S) _ref.mStateContainer.state2,
        (Long) _ref.measureOutput,
        (TestTreeProp) _ref.treeProp);
  }

  @Override
  protected void onUnmount(ComponentContext c, Object v, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.onUnmount(
        (ComponentContext) c,
        (Drawable) v,
        (long) _ref.prop8);
  }

  @Override
  protected void onPopulateAccessibilityNode(AccessibilityNodeInfoCompat node,
      Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.onPopulateAccessibilityNode(
        (AccessibilityNodeInfoCompat) node,
        (CharSequence) _ref.prop7);
  }

  @Override
  public boolean implementsAccessibility() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodesCount(Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    int _result = (int) TestMountSpec.getExtraAccessibilityNodesCount(
        (int) _ref.prop1,
        (CharSequence) _ref.prop7,
        (Integer) _ref.boundsDefinedOutput);
    return _result;
  }

  @Override
  protected void onPopulateExtraAccessibilityNode(AccessibilityNodeInfoCompat node,
      int extraNodeIndex, int componentBoundsLeft, int componentBoundsTop,
      Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.onPopulateExtraAccessibilityNode(
        (AccessibilityNodeInfoCompat) node,
        (int) extraNodeIndex,
        (int) componentBoundsLeft,
        (int) componentBoundsTop,
        (Object) _ref.prop3,
        (CharSequence) _ref.prop7,
        (Integer) _ref.boundsDefinedOutput);
  }

  @Override
  public boolean implementsExtraAccessibilityNodes() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodeAt(int x, int y, Component _abstract) {
    TestMount _ref = (TestMount) _abstract;
    int _result = (int) TestMountSpec.getExtraAccessibilityNodeAt(
        (int) x,
        (int) y,
        (Object) _ref.prop3,
        (CharSequence) _ref.prop7,
        (Integer) _ref.boundsDefinedOutput);
    return _result;
  }

  @Override
  protected boolean shouldUpdate(Component _prevAbstractImpl, Component _nextAbstractImpl) {
    TestMount _prevImpl = (TestMount) _prevAbstractImpl;
    TestMount _nextImpl = (TestMount) _nextAbstractImpl;
    Diff<Integer> prop1 =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.prop1,
                _nextImpl == null ? null : _nextImpl.prop1);
    boolean _result = (boolean) TestMountSpec.shouldUpdate(prop1);
    releaseDiff(prop1);
    return _result;
  }

  @Override
  public ComponentLifecycle.MountType getMountType() {
    return ComponentLifecycle.MountType.DRAWABLE;
  }

  @Override
  protected int poolSize() {
    return 15;
  }

  @Override
  protected boolean canPreallocate() {
    return true;
  }

  @Override
  public boolean canMountIncrementally() {
    return true;
  }

  @Override
  public boolean shouldUseDisplayList() {
    return true;
  }

  @Override
  protected boolean isMountSizeDependent() {
    return true;
  }

  @Override
  public boolean callsShouldUpdateOnMount() {
    return true;
  }

  @Override
  public boolean isPureRender() {
    return true;
  }

  public static EventHandler getTestEventHandler(ComponentContext context) {
    if (context.getComponentScope() == null) {
      return null;
    }
    return ((TestMount) context.getComponentScope()).testEventHandler;
  }

  static void dispatchTestEvent(EventHandler _eventHandler, View view, Object object) {
    TestEvent _eventState = sTestEventPool.acquire();
    if (_eventState == null) {
      _eventState = new TestEvent();
    }
    _eventState.view = view;
    _eventState.object = object;
    EventDispatcher _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher();
    _lifecycle.dispatchOnEvent(_eventHandler, _eventState);
    _eventState.view = null;
    _eventState.object = null;
    sTestEventPool.release(_eventState);
  }

  private void testLayoutEvent(HasEventDispatcher _abstract, ComponentContext c, View view,
      int param1) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.testLayoutEvent(
        c,
        view,
        param1,
        (Object) _ref.prop3,
        (char) _ref.prop5,
        (long) _ref.mStateContainer.state1);
  }

  public static EventHandler<ClickEvent> testLayoutEvent(ComponentContext c, int param1) {
    return newEventHandler(c, "testLayoutEvent", 1328162206, new Object[] {
        c,
        param1,
    });
  }

  @Override
  public Object dispatchOnEvent(final EventHandler eventHandler, final Object eventState) {
    int id = eventHandler.id;
    switch (id) {
      case 1328162206: {
        ClickEvent _event = (ClickEvent) eventState;
        testLayoutEvent(
            eventHandler.mHasEventDispatcher,
            (ComponentContext) eventHandler.params[0],
            (View) _event.view,
            (int) eventHandler.params[1]);
        return null;
      }
      default:
        return null;
    }
  }

  @Override
  protected boolean hasState() {
    return true;
  }

  @Override
  protected void transferState(ComponentContext context,
      ComponentLifecycle.StateContainer _prevStateContainer, Component _component) {
    TestMountStateContainer prevStateContainer = (TestMountStateContainer) _prevStateContainer;
    TestMount component = (TestMount) _component;
    component.mStateContainer.state1 = prevStateContainer.state1;
    component.mStateContainer.state2 = prevStateContainer.state2;
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestMount.UpdateCurrentStateStateUpdate _stateUpdate = ((TestMount) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestMount.UpdateCurrentStateStateUpdate _stateUpdate = ((TestMount) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateState(_stateUpdate);
  }

  protected static void lazyUpdateState1(ComponentContext c, final long lazyUpdateValue) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    ComponentLifecycle.StateUpdate _stateUpdate = new ComponentLifecycle.StateUpdate() {
      public void updateState(ComponentLifecycle.StateContainer _stateContainer,
          Component newComponent) {
        TestMount newComponentStateUpdate = (TestMount) newComponent;
        StateValue<Long> state1 = new StateValue<Long>();
        state1.set(lazyUpdateValue);
        newComponentStateUpdate.mStateContainer.state1 = state1.get();
      }
    };
    c.updateStateLazy(_stateUpdate);
  }

  public static <S extends View> Builder<S> create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static <S extends View> Builder<S> create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    TestMount instance = new TestMount();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  @VisibleForTesting( otherwise = 2) static class TestMountStateContainer<S extends View> implements ComponentLifecycle.StateContainer {
    @State
    long state1;

    @State
    S state2;
  }

  private static class UpdateCurrentStateStateUpdate implements ComponentLifecycle.StateUpdate {
    private int mSomeParam;

    UpdateCurrentStateStateUpdate(int someParam) {
      mSomeParam = someParam;
    }

    public void updateState(ComponentLifecycle.StateContainer _stateContainer,
        Component newComponent) {
      TestMountStateContainer stateContainer = (TestMountStateContainer) _stateContainer;
      TestMount newComponentStateUpdate = (TestMount) newComponent;
      StateValue<Long> state1 = new StateValue<Long>();
      state1.set(stateContainer.state1);
      TestMountSpec.updateCurrentState(state1,mSomeParam);
      newComponentStateUpdate.mStateContainer.state1 = state1.get();
    }
  }

  public static class Builder<S extends View> extends Component.Builder<TestMount, Builder<S>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"prop1", "prop6", "prop3", "prop4", "prop8", "prop7", "prop5"};

    private static final int REQUIRED_PROPS_COUNT = 7;

    TestMount mTestMount;

    ComponentContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        TestMount testMountRef) {
      super.init(context, defStyleAttr, defStyleRes, testMountRef);
      mTestMount = testMountRef;
      mContext = context;
      mRequired.clear();
    }

    public Builder<S> prop1(int prop1) {
      this.mTestMount.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    public Builder<S> prop6(long prop6) {
      this.mTestMount.prop6 = prop6;
      mRequired.set(1);
      return this;
    }

    public Builder<S> prop3(Object prop3) {
      this.mTestMount.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder<S> prop4(char[] prop4) {
      this.mTestMount.prop4 = prop4;
      mRequired.set(3);
      return this;
    }

    public Builder<S> prop2(boolean prop2) {
      this.mTestMount.prop2 = prop2;
      return this;
    }

    public Builder<S> prop8(long prop8) {
      this.mTestMount.prop8 = prop8;
      mRequired.set(4);
      return this;
    }

    public Builder<S> prop7(CharSequence prop7) {
      this.mTestMount.prop7 = prop7;
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop7Res(@StringRes int resId) {
      this.mTestMount.prop7 = resolveStringRes(resId);
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop7Res(@StringRes int resId, Object... formatArgs) {
      this.mTestMount.prop7 = resolveStringRes(resId, formatArgs);
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop7Attr(@AttrRes int attrResId, @StringRes int defResId) {
      this.mTestMount.prop7 = resolveStringAttr(attrResId, defResId);
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop7Attr(@AttrRes int attrResId) {
      this.mTestMount.prop7 = resolveStringAttr(attrResId, 0);
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop5(char prop5) {
      this.mTestMount.prop5 = prop5;
      mRequired.set(6);
      return this;
    }

    public Builder<S> testEventHandler(EventHandler testEventHandler) {
      this.mTestMount.testEventHandler = testEventHandler;
      return this;
    }

    @Override
    public Builder<S> getThis() {
      return this;
    }

    @Override
    public Component<TestMount> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      TestMount testMountRef = mTestMount;
      release();
      return testMountRef;
    }

    @Override
    protected void release() {
      super.release();
      mTestMount = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
