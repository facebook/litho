/*
 * Copyright 2019-present Facebook, Inc.
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
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
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
import com.facebook.litho.HotswapManager;
import com.facebook.litho.Output;
import com.facebook.litho.StateContainer;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * @prop-required aspectRatio float
 * @prop-required child com.facebook.litho.Component
 * @prop-required focusable boolean
 * @prop-required handler com.facebook.litho.EventHandler<com.facebook.litho.ClickEvent>
 * @prop-optional names java.util.List<java.lang.String>
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
  @Comparable(type = 14)
  private TestLayoutStateContainer mStateContainer;

  private TestLayoutRenderData mPreviousRenderData;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 0)
  float aspectRatio;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 10)
  Component child;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  boolean focusable;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 12)
  EventHandler<ClickEvent> handler;

  @Prop(resType = ResType.STRING, optional = true)
  @Comparable(type = 5)
  List<String> names = TestLayoutSpec.names;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  int prop1;

  @Prop(resType = ResType.NONE, optional = true)
  @Comparable(type = 3)
  boolean prop2 = TestLayoutSpec.prop2;

  @Nullable
  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 13)
  Object prop3;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 2)
  char[] prop4;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  char prop5;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  long prop6;

  @TreeProp
  @Comparable(type = 13)
  TestTreeProp treeProp;

  EventHandler testEventHandler;

  EventTrigger onClickEventTriggerTrigger;

  private TestLayout() {
    super("TestLayout");
    mStateContainer = new TestLayoutStateContainer();
  }

  @Override
  protected StateContainer getStateContainer() {
    return mStateContainer;
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
    final TreeProps childTreeProps = TreeProps.acquire(parentTreeProps);
    childTreeProps.put(com.facebook.litho.processor.integration.resources.TestTreeProp.class, TestLayoutSpec.onCreateFeedPrefetcherProp(
        (ComponentContext) c,
        prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c) {
    Output<Boolean> prop2Tmp = new Output<>();
    Output<Object> prop3Tmp = new Output<>();
    ClassLoader classLoader = HotswapManager.getClassLoader();
    if (classLoader == null) {
      TestLayoutSpec.onLoadStyle(
          (ComponentContext) c, (Output<Boolean>) prop2Tmp, (Output<Object>) prop3Tmp);
    } else {
      Class specClass;
      try {
        specClass =
            classLoader.loadClass(
                "com.facebook.litho.processor.integration.resources.TestLayoutSpec");
      } catch (ClassNotFoundException _e) {
        throw new RuntimeException(_e);
      }
      try {
        final Method method =
            specClass.getDeclaredMethod(
                "onLoadStyle", ComponentContext.class, Output.class, Output.class);
        method.setAccessible(true);
        method.invoke(
            null, (ComponentContext) c, (Output<Boolean>) prop2Tmp, (Output<Object>) prop3Tmp);
      } catch (Exception _e) {
        throw new RuntimeException(_e);
      }
    }
    if (prop2Tmp.get() != null) {
      prop2 = prop2Tmp.get();
    }
    if (prop3Tmp.get() != null) {
      prop3 = prop3Tmp.get();
    }
  }

  @Override
  protected void createInitialState(ComponentContext c) {
    StateValue<S> state2 = new StateValue<>();
    ClassLoader classLoader = HotswapManager.getClassLoader();
    if (classLoader == null) {
      TestLayoutSpec.createInitialState((ComponentContext) c, (int) prop1, (StateValue<S>) state2);
    } else {
      Class specClass;
      try {
        specClass =
            classLoader.loadClass(
                "com.facebook.litho.processor.integration.resources.TestLayoutSpec");
      } catch (ClassNotFoundException _e) {
        throw new RuntimeException(_e);
      }
      try {
        final Method method =
            specClass.getDeclaredMethod(
                "createInitialState", ComponentContext.class, int.class, StateValue.class);
        method.setAccessible(true);
        method.invoke(null, (ComponentContext) c, (int) prop1, (StateValue<S>) state2);
      } catch (Exception _e) {
        throw new RuntimeException(_e);
      }
    }
    if (state2.get() != null) {
      mStateContainer.state2 = state2.get();
    }
  }

  @Override
  protected Component onCreateLayout(ComponentContext context) {
    Component _result;
    ClassLoader classLoader = HotswapManager.getClassLoader();
    if (classLoader == null) {
      _result =
          (Component)
              TestLayoutSpec.onCreateLayout(
                  (ComponentContext) context,
                  (Object) prop3,
                  (char[]) prop4,
                  (EventHandler<ClickEvent>) handler,
                  (Component) child,
                  (boolean) prop2,
                  (List<String>) names,
                  (long) mStateContainer.state1,
                  (S) mStateContainer.state2,
                  (int) mStateContainer.state3,
                  (TestTreeProp) treeProp,
                  (Integer) getCached());
    } else {
      Class specClass;
      try {
        specClass =
            classLoader.loadClass(
                "com.facebook.litho.processor.integration.resources.TestLayoutSpec");
      } catch (ClassNotFoundException _e) {
        throw new RuntimeException(_e);
      }
      try {
        final Method method =
            specClass.getDeclaredMethod(
                "onCreateLayout",
                ComponentContext.class,
                Object.class,
                char[].class,
                EventHandler.class,
                Component.class,
                boolean.class,
                List.class,
                long.class,
                View.class,
                int.class,
                TestTreeProp.class,
                Integer.class);
        method.setAccessible(true);
        _result =
            (Component)
                method.invoke(
                    null,
                    (ComponentContext) context,
                    (Object) prop3,
                    (char[]) prop4,
                    (EventHandler<ClickEvent>) handler,
                    (Component) child,
                    (boolean) prop2,
                    (List<String>) names,
                    (long) mStateContainer.state1,
                    (S) mStateContainer.state2,
                    (int) mStateContainer.state3,
                    (TestTreeProp) treeProp,
                    (Integer) getCached());
      } catch (Exception _e) {
        throw new RuntimeException(_e);
      }
    }
    return _result;
  }

  @Override
  protected void onError(ComponentContext c, Exception e) {
    ClassLoader classLoader = HotswapManager.getClassLoader();
    if (classLoader == null) {
      TestLayoutSpec.onError((ComponentContext) c, (Exception) e);
    } else {
      Class specClass;
      try {
        specClass =
            classLoader.loadClass(
                "com.facebook.litho.processor.integration.resources.TestLayoutSpec");
      } catch (ClassNotFoundException _e) {
        throw new RuntimeException(_e);
      }
      try {
        final Method method =
            specClass.getDeclaredMethod("onError", ComponentContext.class, Exception.class);
        method.setAccessible(true);
        method.invoke(null, (ComponentContext) c, (Exception) e);
      } catch (Exception _e) {
        throw new RuntimeException(_e);
      }
    }
  }

  @Override
  protected Transition onCreateTransition(ComponentContext c) {
    Transition _result;
    Diff<Integer> _state3Diff =
        new Diff<Integer>(
            mPreviousRenderData == null ? null : mPreviousRenderData.state3,
            mStateContainer.state3);
    ClassLoader classLoader = HotswapManager.getClassLoader();
    if (classLoader == null) {
      _result =
          (Transition)
              TestLayoutSpec.onCreateTransition(
                  (ComponentContext) c,
                  (Object) prop3,
                  (long) mStateContainer.state1,
                  (Diff<Integer>) _state3Diff);
    } else {
      Class specClass;
      try {
        specClass =
            classLoader.loadClass(
                "com.facebook.litho.processor.integration.resources.TestLayoutSpec");
      } catch (ClassNotFoundException _e) {
        throw new RuntimeException(_e);
      }
      try {
        final Method method =
            specClass.getDeclaredMethod(
                "onCreateTransition", ComponentContext.class, Object.class, long.class, Diff.class);
        method.setAccessible(true);
        _result =
            (Transition)
                method.invoke(
                    null,
                    (ComponentContext) c,
                    (Object) prop3,
                    (long) mStateContainer.state1,
                    (Diff<Integer>) _state3Diff);
      } catch (Exception _e) {
        throw new RuntimeException(_e);
      }
    }
    return _result;
  }

  public static EventHandler getTestEventHandler(ComponentContext context) {
    if (context.getComponentScope() == null) {
      return null;
    }
    return ((TestLayout) context.getComponentScope()).testEventHandler;
  }

  static void dispatchTestEvent(EventHandler _eventHandler, View view, Object object) {
    final TestEvent _eventState = new TestEvent();
    _eventState.view = view;
    _eventState.object = object;
    EventDispatcher _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher();
    _lifecycle.dispatchOnEvent(_eventHandler, _eventState);
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
        (float) _ref.aspectRatio,
        (boolean) _ref.focusable,
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
      StateContainer _prevStateContainer, StateContainer _nextStateContainer) {
    TestLayoutStateContainer<S> prevStateContainer =
        (TestLayoutStateContainer<S>) _prevStateContainer;
    TestLayoutStateContainer<S> nextStateContainer =
        (TestLayoutStateContainer<S>) _nextStateContainer;
    nextStateContainer.state1 = prevStateContainer.state1;
    nextStateContainer.state2 = prevStateContainer.state2;
    nextStateContainer.state3 = prevStateContainer.state3;
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate =
        ((TestLayout) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate, "TestLayout.updateCurrentState");
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate, "TestLayout.updateCurrentState");
  }

  protected static void updateCurrentStateSync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateSync(_stateUpdate, "TestLayout.updateCurrentState");
  }

  protected static void lazyUpdateState1(ComponentContext c, final long lazyUpdateValue) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    ComponentLifecycle.StateUpdate _stateUpdate =
        new ComponentLifecycle.StateUpdate() {
          @Override
          public void updateState(StateContainer _stateContainer) {
            TestLayoutStateContainer stateContainer = (TestLayoutStateContainer) _stateContainer;
            stateContainer.state1 = lazyUpdateValue;
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
    final Builder builder = new Builder();
    TestLayout instance = new TestLayout();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  private int getCached() {
    ComponentContext c = getScopedContext();
    final CachedInputs inputs = new CachedInputs(prop3, prop5, mStateContainer.state1);
    Integer cached = (Integer) c.getCachedValue(inputs);
    if (cached == null) {
      cached = TestLayoutSpec.onCalculateCached(prop3, prop5, mStateContainer.state1);
      c.putCachedValue(inputs, cached);
    }
    return cached;
  }

  @Override
  protected Component getSimpleNameDelegate() {
    return child;
  }

  @VisibleForTesting(
      otherwise = 2
  )
  static class TestLayoutStateContainer<S extends View> implements StateContainer {
    @State
    @Comparable(type = 3)
    long state1;

    @State
    @Comparable(type = 13)
    S state2;

    @State
    @Comparable(type = 3)
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

  private static class UpdateCurrentStateStateUpdate<S extends View>
      implements ComponentLifecycle.StateUpdate {
    private int mSomeParam;

    UpdateCurrentStateStateUpdate(int someParam) {
      mSomeParam = someParam;
    }

    @Override
    public void updateState(StateContainer _stateContainer) {
      TestLayoutStateContainer<S> stateContainer = (TestLayoutStateContainer<S>) _stateContainer;
      StateValue<Long> state1 = new StateValue<Long>();
      state1.set(stateContainer.state1);
      TestLayoutSpec.updateCurrentState(state1,mSomeParam);
      stateContainer.state1 = state1.get();
    }
  }

  public static class Builder<S extends View> extends Component.Builder<Builder<S>> {
    TestLayout mTestLayout;

    ComponentContext mContext;

    private final String[] REQUIRED_PROPS_NAMES =
        new String[] {
          "aspectRatio",
          "child",
          "focusable",
          "handler",
          "prop1",
          "prop3",
          "prop4",
          "prop5",
          "prop6"
        };

    private final int REQUIRED_PROPS_COUNT = 9;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, TestLayout testLayoutRef) {
      super.init(context, defStyleAttr, defStyleRes, testLayoutRef);
      mTestLayout = testLayoutRef;
      mContext = context;
      mRequired.clear();
    }

    @Override
    public Builder<S> aspectRatio(float aspectRatio) {
      super.aspectRatio(aspectRatio);
      this.mTestLayout.aspectRatio = aspectRatio;
      mRequired.set(0);
      return this;
    }

    public Builder<S> child(Component child) {
      this.mTestLayout.child = child == null ? null : child.makeShallowCopy();
      mRequired.set(1);
      return this;
    }

    public Builder<S> child(Component.Builder<?> childBuilder) {
      this.mTestLayout.child = childBuilder == null ? null : childBuilder.build();
      mRequired.set(1);
      return this;
    }

    @Override
    public Builder<S> focusable(boolean focusable) {
      this.mTestLayout.focusable = focusable;
      mRequired.set(2);
      return this;
    }

    public Builder<S> handler(EventHandler<ClickEvent> handler) {
      this.mTestLayout.handler = handler;
      mRequired.set(3);
      return this;
    }

    public Builder<S> names(List<String> names) {
      if (names == null) {
        return this;
      }
      if (this.mTestLayout.names == null
          || this.mTestLayout.names.isEmpty()
          || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = names;
      } else {
        this.mTestLayout.names.addAll(names);
      }
      return this;
    }

    public Builder<S> name(String name) {
      if (name == null) {
        return this;
      }
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      this.mTestLayout.names.add(name);
      return this;
    }

    public Builder<S> nameRes(@StringRes int resId) {
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      final String res = mResourceResolver.resolveStringRes(resId);
      this.mTestLayout.names.add(res);
      return this;
    }

    public Builder<S> namesRes(List<Integer> resIds) {
      if (resIds == null) {
        return this;
      }
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      for (int i = 0; i < resIds.size(); i++) {
        final String res = mResourceResolver.resolveStringRes(resIds.get(i));
        this.mTestLayout.names.add(res);
      }
      return this;
    }

    public Builder<S> nameRes(@StringRes int resId, Object... formatArgs) {
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      final String res = mResourceResolver.resolveStringRes(resId, formatArgs);
      this.mTestLayout.names.add(res);
      return this;
    }

    public Builder<S> namesRes(@StringRes List<Integer> resIds, Object... formatArgs) {
      if (resIds == null) {
        return this;
      }
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      for (int i = 0; i < resIds.size(); i++) {
        final String res = mResourceResolver.resolveStringRes(resIds.get(i), formatArgs);
        this.mTestLayout.names.add(res);
      }
      return this;
    }

    public Builder<S> nameAttr(@AttrRes int attrResId, @StringRes int defResId) {
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      final String res = mResourceResolver.resolveStringAttr(attrResId, defResId);
      this.mTestLayout.names.add(res);
      return this;
    }

    public Builder<S> nameAttr(@AttrRes int attrResId) {
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      final String res = mResourceResolver.resolveStringAttr(attrResId, 0);
      this.mTestLayout.names.add(res);
      return this;
    }

    public Builder<S> namesAttr(List<Integer> attrResIds, @StringRes int defResId) {
      if (attrResIds == null) {
        return this;
      }
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      for (int i = 0; i < attrResIds.size(); i++) {
        final String res = mResourceResolver.resolveStringAttr(attrResIds.get(i), defResId);
        this.mTestLayout.names.add(res);
      }
      return this;
    }

    public Builder<S> namesAttr(List<Integer> attrResIds) {
      if (attrResIds == null) {
        return this;
      }
      if (this.mTestLayout.names == null || this.mTestLayout.names == TestLayoutSpec.names) {
        this.mTestLayout.names = new ArrayList<String>();
      }
      for (int i = 0; i < attrResIds.size(); i++) {
        final String res = mResourceResolver.resolveStringAttr(attrResIds.get(i), 0);
        this.mTestLayout.names.add(res);
      }
      return this;
    }

    public Builder<S> prop1(int prop1) {
      this.mTestLayout.prop1 = prop1;
      mRequired.set(4);
      return this;
    }

    public Builder<S> prop2(boolean prop2) {
      this.mTestLayout.prop2 = prop2;
      return this;
    }

    public Builder<S> prop3(@Nullable Object prop3) {
      this.mTestLayout.prop3 = prop3;
      mRequired.set(5);
      return this;
    }

    public Builder<S> prop4(char[] prop4) {
      this.mTestLayout.prop4 = prop4;
      mRequired.set(6);
      return this;
    }

    public Builder<S> prop5(char prop5) {
      this.mTestLayout.prop5 = prop5;
      mRequired.set(7);
      return this;
    }

    public Builder<S> prop6(long prop6) {
      this.mTestLayout.prop6 = prop6;
      mRequired.set(8);
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
      return mTestLayout;
    }
  }

  private static class CachedInputs {
    private final Object prop3;

    private final char prop5;

    private final long state1;

    CachedInputs(Object prop3, char prop5, long state1) {
      this.prop3 = prop3;
      this.prop5 = prop5;
      this.state1 = state1;
    }

    @Override
    public int hashCode() {
      return Objects.hash(prop3, prop5, state1);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || !(other instanceof CachedInputs)) {
        return false;
      }
      CachedInputs cachedValueInputs = (CachedInputs) other;
      if (prop3 != null
          ? !prop3.equals(cachedValueInputs.prop3)
          : cachedValueInputs.prop3 != null) {
        return false;
      }
      if (prop5 != cachedValueInputs.prop5) {
        return false;
      }
      if (state1 != cachedValueInputs.state1) {
        return false;
      }
      return true;
    }
  }
}
