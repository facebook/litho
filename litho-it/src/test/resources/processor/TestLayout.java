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

package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Diff;
import com.facebook.litho.ErrorEvent;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.EventTrigger;
import com.facebook.litho.EventTriggerTarget;
import com.facebook.litho.EventTriggersContainer;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.Output;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @prop-required child com.facebook.litho.Component
 * @prop-required prop1 int
 * @prop-optional prop2 boolean
 * @prop-required prop3 java.lang.Object
 * @prop-required prop4 char[]
 * @prop-required prop5 char
 * @prop-required prop6 long
 * @see com.facebook.litho.processor.integration.resources.TestLayoutSpec
 */
@TargetApi(17)
public final class TestLayout<S extends View> extends Component implements TestTag {
  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private TestLayoutStateContainer mStateContainer;

  private TestLayoutRenderData mPreviousRenderData;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  Component child;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  int prop1;

  @Prop(
      resType = ResType.NONE,
      optional = true
  )
  boolean prop2 = TestLayoutSpec.prop2;

  @Nullable
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
      optional = false
  )
  char prop5;

  @Prop(
      resType = ResType.NONE,
      optional = false
  )
  long prop6;

  TestTreeProp treeProp;

  EventHandler testEventHandler;

  EventTrigger onClickEventTriggerTrigger;

  private TestLayout() {
    super("TestLayout");
    mStateContainer = new TestLayoutStateContainer();
  }

  @Override
  protected ComponentLifecycle.StateContainer getStateContainer() {
    return mStateContainer;
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    TestLayout testLayoutRef = (TestLayout) other;
    if (this.getId() == testLayoutRef.getId()) {
      return true;
    }
    if (child != null ? !child.isEquivalentTo(testLayoutRef.child) : testLayoutRef.child != null) {
      return false;
    }
    if (prop1 != testLayoutRef.prop1) {
      return false;
    }
    if (prop2 != testLayoutRef.prop2) {
      return false;
    }
    if (prop3 != null ? !prop3.equals(testLayoutRef.prop3) : testLayoutRef.prop3 != null) {
      return false;
    }
    if (!Arrays.equals(prop4, testLayoutRef.prop4)) {
      return false;
    }
    if (prop5 != testLayoutRef.prop5) {
      return false;
    }
    if (prop6 != testLayoutRef.prop6) {
      return false;
    }
    if (mStateContainer.state1 != testLayoutRef.mStateContainer.state1) {
      return false;
    }
    if (mStateContainer.state2 != null ? !mStateContainer.state2.equals(testLayoutRef.mStateContainer.state2) : testLayoutRef.mStateContainer.state2 != null) {
      return false;
    }
    if (mStateContainer.state3 != testLayoutRef.mStateContainer.state3) {
      return false;
    }
    if (treeProp != null ? !treeProp.equals(testLayoutRef.treeProp) : testLayoutRef.treeProp != null) {
      return false;
    }
    return true;
  }

  private UpdateCurrentStateStateUpdate createUpdateCurrentStateStateUpdate(int someParam) {
    return new UpdateCurrentStateStateUpdate(someParam);
  }

  @Override
  public TestLayout makeShallowCopy() {
    TestLayout component = (TestLayout) super.makeShallowCopy();
    component.child = component.child != null ? component.child.makeShallowCopy() : null;
    component.mStateContainer = new TestLayoutStateContainer();
    return component;
  }

  @Override
  protected void populateTreeProps(TreeProps treeProps) {
    if (treeProps == null) {
      return;
    }
    treeProp = treeProps.get(com.facebook.litho.processor.integration.resources.TestTreeProp.class);
  }

  @Override
  protected TreeProps getTreePropsForChildren(ComponentContext c, TreeProps parentTreeProps) {
    final TreeProps childTreeProps = TreeProps.copy(parentTreeProps);
    childTreeProps.put(com.facebook.litho.processor.integration.resources.TestTreeProp.class, TestLayoutSpec.onCreateFeedPrefetcherProp(
        (ComponentContext) c,
        prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c) {
    Output<Boolean> prop2Tmp = acquireOutput();
    Output<Object> prop3Tmp = acquireOutput();
    TestLayoutSpec.onLoadStyle((ComponentContext) c, prop2Tmp, prop3Tmp);
    if (prop2Tmp.get() != null) {
      prop2 = prop2Tmp.get();
    }
    releaseOutput(prop2Tmp);
    if (prop3Tmp.get() != null) {
      prop3 = prop3Tmp.get();
    }
    releaseOutput(prop3Tmp);
  }

  @Override
  protected void createInitialState(ComponentContext c) {
    StateValue<S> state2 = new StateValue<>();
    TestLayoutSpec.createInitialState((ComponentContext) c, (int) prop1, state2);
    if (state2.get() != null) {
      mStateContainer.state2 = state2.get();
    }
  }

  @Override
  protected Component onCreateLayout(ComponentContext context) {
    Component _result =
        (Component)
            TestLayoutSpec.onCreateLayout(
                (ComponentContext) context,
                (boolean) prop2,
                (Object) prop3,
                (char[]) prop4,
                (long) mStateContainer.state1,
                (S) mStateContainer.state2,
                (int) mStateContainer.state3,
                (TestTreeProp) treeProp,
                (Component) child);
    return _result;
  }

  @Override
  protected void onError(ComponentContext c, Exception e) {
    TestLayoutSpec.onError((ComponentContext) c, (Exception) e);
  }

  @Override
  protected Transition onCreateTransition(ComponentContext c) {
    Diff<Integer> _state3Diff =
        acquireDiff(
            mPreviousRenderData == null ? null : mPreviousRenderData.state3,
            mStateContainer.state3);
    Transition _result =
        (Transition)
            TestLayoutSpec.onCreateTransition(
                (ComponentContext) c, (Object) prop3, (long) mStateContainer.state1, _state3Diff);
    releaseDiff(_state3Diff);
    return _result;
  }

  public static EventHandler getTestEventHandler(ComponentContext context) {
    if (context.getComponentScope() == null) {
      return null;
    }
    return ((TestLayout) context.getComponentScope()).testEventHandler;
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

  private void testLayoutEvent(
      HasEventDispatcher _abstract, ComponentContext c, View view, int param1) {
    TestLayout _ref = (TestLayout) _abstract;
    TestLayoutSpec.testLayoutEvent(
        c,
        view,
        param1,
        (Object) _ref.prop3,
        (char) _ref.prop5,
        (long) _ref.mStateContainer.state1);
  }

  private void __internalOnErrorHandler(
      HasEventDispatcher _abstract, ComponentContext c, Exception exception) {
    TestLayout _ref = (TestLayout) _abstract;
    onError(c, exception);
  }

  public static EventHandler<ClickEvent> testLayoutEvent(ComponentContext c, int param1) {
    return newEventHandler(
        c,
        1328162206,
        new Object[] {
          c, param1,
        });
  }

  public static EventHandler<ErrorEvent> __internalOnErrorHandler(ComponentContext c) {
    return newEventHandler(
        c,
        -1048037474,
        new Object[] {
          c,
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
      case -1048037474:
        {
          ErrorEvent _event = (ErrorEvent) eventState;
          __internalOnErrorHandler(
              eventHandler.mHasEventDispatcher,
              (ComponentContext) eventHandler.params[0],
              (Exception) _event.exception);
        return null;
      }
      default:
        return null;
    }
  }

  public static EventTrigger onClickEventTriggerTrigger(ComponentContext c, String key) {
    int methodId = -1670292499;
    return newEventTrigger(c, key, methodId);
  }

  private void onClickEventTrigger(EventTriggerTarget _abstract, View view) {
    TestLayout _ref = (TestLayout) _abstract;
    TestLayoutSpec.onClickEventTrigger((ComponentContext) _ref.getScopedContext(), view);
  }

  public static void onClickEventTrigger(ComponentContext c, String key, View view) {
    int methodId = -1670292499;
    EventTrigger trigger = getEventTrigger(c, methodId, key);
    if (trigger == null) {
      return;
    }
    ClickEvent _eventState = new ClickEvent();
    _eventState.view = view;
    trigger.dispatchOnTrigger(_eventState, new Object[] {});
  }

  public static void onClickEventTrigger(EventTrigger trigger, View view) {
    ClickEvent _eventState = new ClickEvent();
    _eventState.view = view;
    trigger.dispatchOnTrigger(_eventState, new Object[] {});
  }

  static void onClickEventTrigger(ComponentContext c, View view) {
    TestLayout component = (TestLayout) c.getComponentScope();
    component.onClickEventTrigger(
        (EventTriggerTarget) component,
        view);
  }

  @Override
  public Object acceptTriggerEvent(
      final EventTrigger eventTrigger, final Object eventState, final Object[] params) {
    int id = eventTrigger.mId;
    switch(id) {
      case -1670292499: {
        ClickEvent _event = (ClickEvent) eventState;
          onClickEventTrigger(eventTrigger.mTriggerTarget, _event.view);
        return null;
      }
      default:
        return null;
    }
  }

  @Override
  public void recordEventTrigger(EventTriggersContainer container) {
    if (onClickEventTriggerTrigger != null) {
      onClickEventTriggerTrigger.mTriggerTarget = this;
      container.recordEventTrigger(onClickEventTriggerTrigger);
    }
  }

  @Override
  protected boolean hasState() {
    return true;
  }

  @Override
  protected void transferState(
      ComponentContext context, ComponentLifecycle.StateContainer _prevStateContainer) {
    TestLayoutStateContainer prevStateContainer = (TestLayoutStateContainer) _prevStateContainer;
    mStateContainer.state1 = prevStateContainer.state1;
    mStateContainer.state2 = prevStateContainer.state2;
    mStateContainer.state3 = prevStateContainer.state3;
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateSync(_stateUpdate);
  }

  protected static void lazyUpdateState1(ComponentContext c, final long lazyUpdateValue) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    ComponentLifecycle.StateUpdate _stateUpdate =
        new ComponentLifecycle.StateUpdate() {
          @Override
          public void updateState(
              ComponentLifecycle.StateContainer _stateContainer, Component newComponent) {
            TestLayout newComponentStateUpdate = (TestLayout) newComponent;
            StateValue<Long> state1 = new StateValue<Long>();
            state1.set(lazyUpdateValue);
            newComponentStateUpdate.mStateContainer.state1 = state1.get();
          }
        };
    c.updateStateLazy(_stateUpdate);
  }

  @Override
  protected boolean needsPreviousRenderData() {
    return true;
  }

  @Override
  protected ComponentLifecycle.RenderData recordRenderData(ComponentLifecycle.RenderData toRecycle) {
    TestLayoutRenderData renderInfo =
        toRecycle != null ? (TestLayoutRenderData) toRecycle : new TestLayoutRenderData();
    renderInfo.record(this);
    return renderInfo;
  }

  @Override
  protected void applyPreviousRenderData(ComponentLifecycle.RenderData previousRenderData) {
    if (previousRenderData == null) {
      mPreviousRenderData = null;
      return;
    }
    if (mPreviousRenderData == null) {
      mPreviousRenderData = new TestLayoutRenderData();
    }
    TestLayoutRenderData infoImpl = (TestLayoutRenderData) previousRenderData;
    mPreviousRenderData.copy(infoImpl);
  }

  public static <S extends View> Builder<S> create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static <S extends View> Builder<S> create(
      ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    TestLayout instance = new TestLayout();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  @VisibleForTesting(
      otherwise = 2
  )
  static class TestLayoutStateContainer<S extends View> implements ComponentLifecycle.StateContainer {
    @State
    long state1;

    @State
    S state2;

    @State
    int state3;
  }

  private static class TestLayoutRenderData<S extends View> implements ComponentLifecycle.RenderData {
    @State
    int state3;

    void copy(TestLayoutRenderData info) {
      state3 = info.state3;
    }

    void record(TestLayout component) {
      state3 = component.mStateContainer.state3;
    }
  }

  private static class UpdateCurrentStateStateUpdate implements ComponentLifecycle.StateUpdate {
    private int mSomeParam;

    UpdateCurrentStateStateUpdate(int someParam) {
      mSomeParam = someParam;
    }

    @Override
    public void updateState(
        ComponentLifecycle.StateContainer _stateContainer, Component newComponent) {
      TestLayoutStateContainer stateContainer = (TestLayoutStateContainer) _stateContainer;
      TestLayout newComponentStateUpdate = (TestLayout) newComponent;
      StateValue<Long> state1 = new StateValue<Long>();
      state1.set(stateContainer.state1);
      TestLayoutSpec.updateCurrentState(state1,mSomeParam);
      newComponentStateUpdate.mStateContainer.state1 = state1.get();
    }
  }

  public static class Builder<S extends View> extends Component.Builder<Builder<S>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"child", "prop1", "prop3", "prop4", "prop5", "prop6"};

    private static final int REQUIRED_PROPS_COUNT = 6;

    TestLayout mTestLayout;

    ComponentContext mContext;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, TestLayout testLayoutRef) {
      super.init(context, defStyleAttr, defStyleRes, testLayoutRef);
      mTestLayout = testLayoutRef;
      mContext = context;
      mRequired.clear();
    }

    public Builder<S> child(Component child) {
      this.mTestLayout.child = child == null ? null : child.makeShallowCopy();
      mRequired.set(0);
      return this;
    }

    public Builder<S> child(Component.Builder<?> childBuilder) {
      this.mTestLayout.child = childBuilder.build();
      mRequired.set(0);
      return this;
    }

    public Builder<S> prop1(int prop1) {
      this.mTestLayout.prop1 = prop1;
      mRequired.set(1);
      return this;
    }

    public Builder<S> prop2(boolean prop2) {
      this.mTestLayout.prop2 = prop2;
      return this;
    }

    public Builder<S> prop3(@Nullable Object prop3) {
      this.mTestLayout.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder<S> prop4(char[] prop4) {
      this.mTestLayout.prop4 = prop4;
      mRequired.set(3);
      return this;
    }

    public Builder<S> prop5(char prop5) {
      this.mTestLayout.prop5 = prop5;
      mRequired.set(4);
      return this;
    }

    public Builder<S> prop6(long prop6) {
      this.mTestLayout.prop6 = prop6;
      mRequired.set(5);
      return this;
    }

    public Builder<S> testEventHandler(EventHandler testEventHandler) {
      this.mTestLayout.testEventHandler = testEventHandler;
      return this;
    }

    public Builder<S> onClickEventTriggerTrigger(EventTrigger onClickEventTriggerTrigger) {
      this.mTestLayout.onClickEventTriggerTrigger = onClickEventTriggerTrigger;
      return this;
    }

    private void onClickEventTriggerTrigger(String key) {
      com.facebook.litho.EventTrigger onClickEventTriggerTrigger = this.mTestLayout.onClickEventTriggerTrigger;
      if (onClickEventTriggerTrigger == null) {
        onClickEventTriggerTrigger = TestLayout.onClickEventTriggerTrigger(this.mContext, key);
      }
      onClickEventTriggerTrigger(onClickEventTriggerTrigger);
    }

    @Override
    public Builder<S> key(String key) {
      super.key(key);
      onClickEventTriggerTrigger(key);
      return this;
    }

    @Override
    public Builder<S> getThis() {
      return this;
    }

    @Override
    public TestLayout build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      TestLayout testLayoutRef = mTestLayout;
      release();
      return testLayoutRef;
    }

    @Override
    protected void release() {
      super.release();
      mTestLayout = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
