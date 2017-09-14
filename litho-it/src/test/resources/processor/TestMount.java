/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.processor.integration.resources;

import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
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
public final class TestMount<S extends View> extends ComponentLifecycle {
  private static TestMount sInstance = null;

  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private TestMount() {
  }

  private static synchronized TestMount get() {
    if (sInstance == null) {
      sInstance = new TestMount();
    }
    return sInstance;
  }

  @Override
  protected void populateTreeProps(Component _abstractImpl, TreeProps treeProps) {
    if (treeProps == null) {
      return;
    }
    final TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    _impl.treeProp = treeProps.get(com.facebook.litho.processor.integration.resources.TestTreeProp.class);
  }

  @Override
  protected TreeProps getTreePropsForChildren(ComponentContext c, Component _abstractImpl,
      TreeProps parentTreeProps) {
    final TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    final TreeProps childTreeProps = TreeProps.copy(parentTreeProps);
    childTreeProps.put(com.facebook.litho.processor.integration.resources.TestTreeProp.class, TestMountSpec.onCreateFeedPrefetcherProp(
        (ComponentContext) c,
        (long) _impl.prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    Output<Boolean> prop2 = acquireOutput();
    Output<Object> prop3 = acquireOutput();
    TestMountSpec.onLoadStyle(
        (ComponentContext) c,
        prop2,
        prop3);
    if (prop2.get() != null) {
      _impl.prop2 = prop2.get();
    }
    releaseOutput(prop2);
    if (prop3.get() != null) {
      _impl.prop3 = prop3.get();
    }
    releaseOutput(prop3);
  }

  @Override
  protected void createInitialState(ComponentContext c, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    StateValue<S> state2 = new StateValue<>();
    TestMountSpec.createInitialState(
        (ComponentContext) c,
        (int) _impl.prop1,
        state2);
    if (state2.get() != null) {
      _impl.mStateContainerImpl.state2 = state2.get();
    }
  }

  @Override
  protected void onMeasure(ComponentContext context, ComponentLayout layout, int widthSpec,
      int heightSpec, Size size, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    Output<Long> measureOutput = acquireOutput();
    TestMountSpec.onMeasure(
        (ComponentContext) context,
        (ComponentLayout) layout,
        (int) widthSpec,
        (int) heightSpec,
        (Size) size,
        measureOutput);
    _impl.measureOutput = measureOutput.get();
    releaseOutput(measureOutput);
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout,
      Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    Output<Integer> boundsDefinedOutput = acquireOutput();
    TestMountSpec.onBoundsDefined(
        (ComponentContext) c,
        (ComponentLayout) layout,
        (Object) _impl.prop3,
        (char[]) _impl.prop4,
        (Long) _impl.measureOutput,
        boundsDefinedOutput);
    _impl.boundsDefinedOutput = boundsDefinedOutput.get();
    releaseOutput(boundsDefinedOutput);
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    Object _result = (Object) TestMountSpec.onCreateMountContent(
        (ComponentContext) c);
    return _result;
  }

  @Override
  protected void onMount(ComponentContext c, Object v, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    TestMountSpec.onMount(
        (ComponentContext) c,
        (Drawable) v,
        (boolean) _impl.prop2,
        (long) _impl.mStateContainerImpl.state1,
        (S) _impl.mStateContainerImpl.state2,
        (Long) _impl.measureOutput,
        (TestTreeProp) _impl.treeProp);
  }

  @Override
  protected void onUnmount(ComponentContext c, Object v, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    TestMountSpec.onUnmount(
        (ComponentContext) c,
        (Drawable) v,
        (long) _impl.prop8);
  }

  @Override
  protected void onPopulateAccessibilityNode(AccessibilityNodeInfoCompat node,
      Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    TestMountSpec.onPopulateAccessibilityNode(
        (AccessibilityNodeInfoCompat) node,
        (CharSequence) _impl.prop7);
  }

  @Override
  public boolean implementsAccessibility() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodesCount(Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    int _result = (int) TestMountSpec.getExtraAccessibilityNodesCount(
        (int) _impl.prop1,
        (CharSequence) _impl.prop7,
        (Integer) _impl.boundsDefinedOutput);
    return _result;
  }

  @Override
  protected void onPopulateExtraAccessibilityNode(AccessibilityNodeInfoCompat node,
      int extraNodeIndex, int componentBoundsLeft, int componentBoundsTop,
      Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    TestMountSpec.onPopulateExtraAccessibilityNode(
        (AccessibilityNodeInfoCompat) node,
        (int) extraNodeIndex,
        (int) componentBoundsLeft,
        (int) componentBoundsTop,
        (Object) _impl.prop3,
        (CharSequence) _impl.prop7,
        (Integer) _impl.boundsDefinedOutput);
  }

  @Override
  public boolean implementsExtraAccessibilityNodes() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodeAt(int x, int y, Component _abstractImpl) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    int _result = (int) TestMountSpec.getExtraAccessibilityNodeAt(
        (int) x,
        (int) y,
        (Object) _impl.prop3,
        (CharSequence) _impl.prop7,
        (Integer) _impl.boundsDefinedOutput);
    return _result;
  }

  @Override
  protected boolean shouldUpdate(Component _prevAbstractImpl, Component _nextAbstractImpl) {
    TestMountImpl _prevImpl = (TestMountImpl) _prevAbstractImpl;
    TestMountImpl _nextImpl = (TestMountImpl) _nextAbstractImpl;
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
    return ((TestMount.TestMountImpl) context.getComponentScope()).testEventHandler;
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

  private void testLayoutEvent(HasEventDispatcher _abstractImpl, ComponentContext c, View view,
      int param1) {
    TestMountImpl _impl = (TestMountImpl) _abstractImpl;
    TestMountSpec.testLayoutEvent(
        c,
        view,
        param1,
        (Object) _impl.prop3,
        (char) _impl.prop5,
        (long) _impl.mStateContainerImpl.state1);
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
      ComponentLifecycle.StateContainer prevStateContainer, Component component) {
    TestMountStateContainerImpl prevStateContainerImpl = (TestMountStateContainerImpl) prevStateContainer;
    TestMountImpl componentImpl = (TestMountImpl) component;
    componentImpl.mStateContainerImpl.state1 = prevStateContainerImpl.state1;
    componentImpl.mStateContainerImpl.state2 = prevStateContainerImpl.state2;
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestMount.UpdateCurrentStateStateUpdate _stateUpdate = ((TestMount.TestMountImpl) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestMount.UpdateCurrentStateStateUpdate _stateUpdate = ((TestMount.TestMountImpl) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateState(_stateUpdate);
  }

  protected static void lazyUpdateState1(ComponentContext c, final long lazyUpdateValue) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    ComponentLifecycle.StateUpdate _stateUpdate = new ComponentLifecycle.StateUpdate() {
      public void updateState(ComponentLifecycle.StateContainer stateContainer,
          Component newComponent) {
        TestMount.TestMountImpl newComponentStateUpdate = (TestMount.TestMountImpl) newComponent;
        StateValue<Long> state1 = new StateValue<Long>();
        state1.set(lazyUpdateValue);
        newComponentStateUpdate.mStateContainerImpl.state1 = state1.get();
      }
    };
    c.updateStateLazy(_stateUpdate);
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new TestMountImpl());
    return builder;
  }

  private static class TestMountStateContainerImpl<S extends View> implements ComponentLifecycle.StateContainer {
    @State
    long state1;

    @State
    S state2;
  }

  static class TestMountImpl<S extends View> extends Component<TestMount> implements Cloneable {
    TestMountStateContainerImpl mStateContainerImpl;

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

    private TestMountImpl() {
      super(get());
      mStateContainerImpl = new TestMountStateContainerImpl();
    }

    @Override
    protected ComponentLifecycle.StateContainer getStateContainer() {
      return mStateContainerImpl;
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
      TestMountImpl testMountImpl = (TestMountImpl) other;
      if (this.getId() == testMountImpl.getId()) {
        return true;
      }
      if (prop1 != testMountImpl.prop1) {
        return false;
      }
      if (prop6 != testMountImpl.prop6) {
        return false;
      }
      if (prop3 != null ? !prop3.equals(testMountImpl.prop3) : testMountImpl.prop3 != null) {
        return false;
      }
      if (!Arrays.equals(prop4, testMountImpl.prop4)) {
        return false;
      }
      if (prop2 != testMountImpl.prop2) {
        return false;
      }
      if (prop8 != testMountImpl.prop8) {
        return false;
      }
      if (prop7 != null ? !prop7.equals(testMountImpl.prop7) : testMountImpl.prop7 != null) {
        return false;
      }
      if (prop5 != testMountImpl.prop5) {
        return false;
      }
      if (mStateContainerImpl.state1 != testMountImpl.mStateContainerImpl.state1) {
        return false;
      }
      if (mStateContainerImpl.state2 != null ? !mStateContainerImpl.state2.equals(testMountImpl.mStateContainerImpl.state2) : testMountImpl.mStateContainerImpl.state2 != null) {
        return false;
      }
      if (treeProp != null ? !treeProp.equals(testMountImpl.treeProp) : testMountImpl.treeProp != null) {
        return false;
      }
      return true;
    }

    @Override
    protected void copyInterStageImpl(Component<TestMount> impl) {
      TestMountImpl testMountImpl = (TestMountImpl) impl;
      measureOutput = testMountImpl.measureOutput;
      boundsDefinedOutput = testMountImpl.boundsDefinedOutput;
    }

    private UpdateCurrentStateStateUpdate createUpdateCurrentStateStateUpdate(int someParam) {
      return new UpdateCurrentStateStateUpdate(someParam);
    }

    @Override
    public TestMountImpl makeShallowCopy() {
      TestMountImpl component = (TestMountImpl) super.makeShallowCopy();
      component.measureOutput = null;
      component.boundsDefinedOutput = null;
      component.mStateContainerImpl = new TestMountStateContainerImpl();
      return component;
    }
  }

  private static class UpdateCurrentStateStateUpdate implements ComponentLifecycle.StateUpdate {
    private int mSomeParam;

    UpdateCurrentStateStateUpdate(int someParam) {
      mSomeParam = someParam;
    }

    public void updateState(ComponentLifecycle.StateContainer stateContainer,
        Component newComponent) {
      TestMountStateContainerImpl stateContainerImpl = (TestMountStateContainerImpl) stateContainer;
      TestMountImpl newComponentStateUpdate = (TestMountImpl) newComponent;
      StateValue<Long> state1 = new StateValue<Long>();
      state1.set(stateContainerImpl.state1);
      TestMountSpec.updateCurrentState(state1,mSomeParam);
      newComponentStateUpdate.mStateContainerImpl.state1 = state1.get();
    }
  }

  public static class Builder<S extends View> extends Component.Builder<TestMount, Builder<S>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"prop1", "prop6", "prop3", "prop4", "prop8", "prop7", "prop5"};

    private static final int REQUIRED_PROPS_COUNT = 7;

    TestMountImpl mTestMountImpl;

    ComponentContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        TestMountImpl testMountImpl) {
      super.init(context, defStyleAttr, defStyleRes, testMountImpl);
      mTestMountImpl = testMountImpl;
      mContext = context;
      mRequired.clear();
    }

    public Builder prop1(int prop1) {
      this.mTestMountImpl.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    public Builder prop6(long prop6) {
      this.mTestMountImpl.prop6 = prop6;
      mRequired.set(1);
      return this;
    }

    public Builder prop3(Object prop3) {
      this.mTestMountImpl.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder prop4(char[] prop4) {
      this.mTestMountImpl.prop4 = prop4;
      mRequired.set(3);
      return this;
    }

    public Builder prop2(boolean prop2) {
      this.mTestMountImpl.prop2 = prop2;
      return this;
    }

    public Builder prop8(long prop8) {
      this.mTestMountImpl.prop8 = prop8;
      mRequired.set(4);
      return this;
    }

    public Builder prop7(CharSequence prop7) {
      this.mTestMountImpl.prop7 = prop7;
      mRequired.set(5);
      return this;
    }

    public Builder prop7Res(@StringRes int resId) {
      final CharSequence res = resolveStringRes(resId);
      this.mTestMountImpl.prop7 = res;
      mRequired.set(5);
      return this;
    }

    public Builder prop7Res(@StringRes int resId, Object... formatArgs) {
      final CharSequence res = resolveStringRes(resId, formatArgs);
      this.mTestMountImpl.prop7 = res;
      mRequired.set(5);
      return this;
    }

    public Builder prop7Attr(@AttrRes int attrResId, @StringRes int defResId) {
      final CharSequence res = resolveStringAttr(attrResId, defResId);
      this.mTestMountImpl.prop7 = res;
      mRequired.set(5);
      return this;
    }

    public Builder prop7Attr(@AttrRes int attrResId) {
      final CharSequence res = resolveStringAttr(attrResId, 0);
      this.mTestMountImpl.prop7 = res;
      mRequired.set(5);
      return this;
    }

    public Builder prop5(char prop5) {
      this.mTestMountImpl.prop5 = prop5;
      mRequired.set(6);
      return this;
    }

    public Builder testEventHandler(EventHandler testEventHandler) {
      this.mTestMountImpl.testEventHandler = testEventHandler;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component<TestMount> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      TestMountImpl testMountImpl = mTestMountImpl;
      release();
      return testMountImpl;
    }

    @Override
    protected void release() {
      super.release();
      mTestMountImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
