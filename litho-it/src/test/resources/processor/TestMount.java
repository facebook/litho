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

package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.CommonUtils;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Diff;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.EventTrigger;
import com.facebook.litho.EventTriggerTarget;
import com.facebook.litho.EventTriggersContainer;
import com.facebook.litho.Handle;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.InterStagePropsContainer;
import com.facebook.litho.MountContentPool;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateContainer;
import com.facebook.litho.StateValue;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.annotations.Generated;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropSetter;
import com.facebook.litho.annotations.RequiredProp;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import java.util.Arrays;
import java.util.BitSet;
import javax.annotation.Nullable;

/**
 * @prop-required prop1 int
 * @prop-optional prop2 boolean
 * @prop-required prop3 java.lang.Object
 * @prop-required prop4 char[]
 * @prop-required prop5 char
 * @prop-required prop6 long
 * @prop-required prop7 java.lang.CharSequence
 * @prop-required prop8 long
 * @see com.facebook.litho.processor.integration.resources.TestMountSpec
 */
@TargetApi(17)
@Generated
public final class TestMount<S extends View> extends Component implements TestTag {
  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  int prop1;

  @Prop(resType = ResType.NONE, optional = true)
  @Comparable(type = 3)
  boolean prop2 = TestMountSpec.prop2;

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

  @Nullable
  @Prop(resType = ResType.STRING, optional = false)
  @Comparable(type = 13)
  CharSequence prop7;

  @Prop(resType = ResType.NONE, optional = false)
  @Comparable(type = 3)
  long prop8;

  @TreeProp
  @Comparable(type = 13)
  TestTreeProp treeProp;

  @androidx.annotation.Nullable EventHandler<TestEvent> testEventHandler;

  EventTrigger onClickEventTriggerTrigger;

  private TestMount() {
    super("TestMount");
  }

  private TestMountStateContainer getStateContainerImpl(ComponentContext c) {
    return (TestMountStateContainer) Component.getStateContainer(c, this);
  }

  @Override
  protected TestMountStateContainer createStateContainer() {
    return new TestMountStateContainer();
  }

  @Override
  protected TestMountInterStagePropsContainer createInterStagePropsContainer() {
    return new TestMountInterStagePropsContainer();
  }

  private TestMountInterStagePropsContainer getInterStagePropsContainerImpl(ComponentContext c) {
    return (TestMountInterStagePropsContainer) super.getInterStagePropsContainer(c);
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    TestMount testMountRef = (TestMount) other;
    if (prop1 != testMountRef.prop1) {
      return false;
    }
    if (prop2 != testMountRef.prop2) {
      return false;
    }
    if (prop3 != null ? !prop3.equals(testMountRef.prop3) : testMountRef.prop3 != null) {
      return false;
    }
    if (!Arrays.equals(prop4, testMountRef.prop4)) {
      return false;
    }
    if (prop5 != testMountRef.prop5) {
      return false;
    }
    if (prop6 != testMountRef.prop6) {
      return false;
    }
    if (prop7 != null ? !prop7.equals(testMountRef.prop7) : testMountRef.prop7 != null) {
      return false;
    }
    if (prop8 != testMountRef.prop8) {
      return false;
    }
    if (!useTreePropsFromContext()) {
      if (treeProp != null
          ? !treeProp.equals(testMountRef.treeProp)
          : testMountRef.treeProp != null) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected boolean isEqualivalentTreeProps(ComponentContext current, ComponentContext next) {
    if (current.getParentTreeProp(
                com.facebook.litho.processor.integration.resources.TestTreeProp.class)
            != null
        ? !current
            .getParentTreeProp(
                com.facebook.litho.processor.integration.resources.TestTreeProp.class)
            .equals(
                next.getParentTreeProp(
                    com.facebook.litho.processor.integration.resources.TestTreeProp.class))
        : next.getParentTreeProp(
                com.facebook.litho.processor.integration.resources.TestTreeProp.class)
            != null) {
      return false;
    }

    return true;
  }

  @Override
  protected void copyInterStageImpl(
      InterStagePropsContainer copyIntoInterStagePropsContainer,
      InterStagePropsContainer copyFromInterStagePropsContainer) {
    TestMountInterStagePropsContainer copyIntoInterStagePropsContainer_ref =
        (TestMountInterStagePropsContainer) copyIntoInterStagePropsContainer;
    TestMountInterStagePropsContainer copyFromInterStagePropsContainer_ref =
        (TestMountInterStagePropsContainer) copyFromInterStagePropsContainer;
    copyIntoInterStagePropsContainer_ref.boundsDefinedOutput =
        copyFromInterStagePropsContainer_ref.boundsDefinedOutput;
    copyIntoInterStagePropsContainer_ref.measureOutput =
        copyFromInterStagePropsContainer_ref.measureOutput;
  }

  @Override
  public TestMount makeShallowCopy() {
    TestMount component = (TestMount) super.makeShallowCopy();
    component.setStateContainer(new TestMountStateContainer());
    component.setInterStagePropsContainer(createInterStagePropsContainer());
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
    childTreeProps.put(
        com.facebook.litho.processor.integration.resources.TestTreeProp.class,
        TestMountSpec.onCreateFeedPrefetcherProp((ComponentContext) c, prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c) {
    Output<Boolean> prop2Tmp = new Output<>();
    Output<Object> prop3Tmp = new Output<>();
    TestMountSpec.onLoadStyle(
        (ComponentContext) c, (Output<Boolean>) prop2Tmp, (Output<Object>) prop3Tmp);
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
    TestMountSpec.createInitialState((ComponentContext) c, (int) prop1, (StateValue<S>) state2);
    if (state2.get() != null) {
      getStateContainerImpl(c).state2 = state2.get();
    }
  }

  @Override
  protected void onMeasure(
      ComponentContext context, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    Output<Long> measureOutputTmp = new Output<>();
    TestMountSpec.onMeasure(
        (ComponentContext) context,
        (ComponentLayout) layout,
        (int) widthSpec,
        (int) heightSpec,
        (Size) size,
        (Output<Long>) measureOutputTmp);
    getInterStagePropsContainerImpl(context).measureOutput = measureOutputTmp.get();
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {
    Output<Integer> boundsDefinedOutputTmp = new Output<>();
    TestMountSpec.onBoundsDefined(
        (ComponentContext) c,
        (ComponentLayout) layout,
        (Object) prop3,
        (char[]) prop4,
        (Long) getInterStagePropsContainerImpl(c).measureOutput,
        (Output<Integer>) boundsDefinedOutputTmp);
    getInterStagePropsContainerImpl(c).boundsDefinedOutput = boundsDefinedOutputTmp.get();
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    Object _result;
    _result = (Object) TestMountSpec.onCreateMountContent((Context) c);
    return _result;
  }

  @Override
  protected void onMount(ComponentContext c, Object v) {
    TestMountSpec.onMount(
        (ComponentContext) c,
        (Drawable) v,
        (boolean) prop2,
        (long) getStateContainerImpl(c).state1,
        (S) getStateContainerImpl(c).state2,
        (Long) getInterStagePropsContainerImpl(c).measureOutput,
        (TestTreeProp)
            (useTreePropsFromContext()
                ? c.getParentTreeProp(
                    com.facebook.litho.processor.integration.resources.TestTreeProp.class)
                : treeProp));
  }

  @Override
  protected void onUnmount(ComponentContext c, Object v) {
    TestMountSpec.onUnmount((ComponentContext) c, (Drawable) v, (long) prop8);
  }

  @Override
  protected void onPopulateAccessibilityNode(
      ComponentContext c, View host, AccessibilityNodeInfoCompat node) {
    TestMountSpec.onPopulateAccessibilityNode(
        (ComponentContext) c,
        (View) host,
        (AccessibilityNodeInfoCompat) node,
        (CharSequence) prop7);
  }

  @Override
  public boolean implementsAccessibility() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodesCount(ComponentContext c) {
    int _result;
    _result =
        (int)
            TestMountSpec.getExtraAccessibilityNodesCount(
                (ComponentContext) c,
                (int) prop1,
                (CharSequence) prop7,
                (Integer) getInterStagePropsContainerImpl(c).boundsDefinedOutput);
    return _result;
  }

  @Override
  protected void onPopulateExtraAccessibilityNode(
      ComponentContext c,
      AccessibilityNodeInfoCompat node,
      int extraNodeIndex,
      int componentBoundsLeft,
      int componentBoundsTop) {
    TestMountSpec.onPopulateExtraAccessibilityNode(
        (ComponentContext) c,
        (AccessibilityNodeInfoCompat) node,
        (int) extraNodeIndex,
        (int) componentBoundsLeft,
        (int) componentBoundsTop,
        (Object) prop3,
        (CharSequence) prop7,
        (Integer) getInterStagePropsContainerImpl(c).boundsDefinedOutput);
  }

  @Override
  public boolean implementsExtraAccessibilityNodes() {
    return true;
  }

  @Override
  protected int getExtraAccessibilityNodeAt(ComponentContext c, int x, int y) {
    int _result;
    _result =
        (int)
            TestMountSpec.getExtraAccessibilityNodeAt(
                (ComponentContext) c,
                (int) x,
                (int) y,
                (Object) prop3,
                (CharSequence) prop7,
                (Integer) getInterStagePropsContainerImpl(c).boundsDefinedOutput);
    return _result;
  }

  @Override
  protected boolean shouldUpdate(
      Component _prevAbstractImpl,
      StateContainer _prevStateContainer,
      Component _nextAbstractImpl,
      StateContainer _nextStateContainer) {
    if (!isPureRender()) {
      return true;
    }
    TestMount _prevImpl = (TestMount) _prevAbstractImpl;
    TestMount _nextImpl = (TestMount) _nextAbstractImpl;
    boolean _result;
    Diff<Integer> prop1 =
        new Diff<Integer>(
            _prevImpl == null ? null : _prevImpl.prop1, _nextImpl == null ? null : _nextImpl.prop1);
    _result = (boolean) TestMountSpec.shouldUpdate((Diff<Integer>) prop1);
    return _result;
  }

  @Override
  protected MountContentPool onCreateMountContentPool() {
    MountContentPool _result;
    _result = (MountContentPool) TestMountSpec.onCreateMountContentPool();
    return _result;
  }

  @Override
  public ComponentLifecycle.MountType getMountType() {
    return ComponentLifecycle.MountType.DRAWABLE;
  }

  @Override
  protected int poolSize() {
    return 3;
  }

  @Override
  protected boolean canPreallocate() {
    return true;
  }

  @Override
  public boolean hasChildLithoViews() {
    return true;
  }

  @Override
  protected boolean isMountSizeDependent() {
    return true;
  }

  @Override
  public boolean isPureRender() {
    return true;
  }

  @androidx.annotation.Nullable
  public static EventHandler<TestEvent> getTestEventHandler(ComponentContext context) {
    if (context.getComponentScope() == null) {
      return null;
    }
    return ((TestMount) context.getComponentScope()).testEventHandler;
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
    TestMount _ref = (TestMount) _abstract;
    TestMountStateContainer stateContainer = getStateContainerWithLazyStateUpdatesApplied(c, _ref);
    TestMountSpec.testLayoutEvent(
        c,
        (Object) _ref.prop3,
        (char) _ref.prop5,
        view,
        param1,
        (long) stateContainer.state1,
        (int) _ref.getCached(c));
  }

  public static EventHandler<ClickEvent> testLayoutEvent(ComponentContext c, int param1) {
    return newEventHandler(
        TestMount.class,
        "TestMount",
        c,
        1328162206,
        new Object[] {
          c, param1,
        });
  }

  @Override
  protected Object dispatchOnEventImpl(final EventHandler eventHandler, final Object eventState) {
    int id = eventHandler.id;
    switch (id) {
      case 1328162206:
        {
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
          dispatchErrorEvent(
              (com.facebook.litho.ComponentContext) eventHandler.params[0],
              (com.facebook.litho.ErrorEvent) eventState);
          return null;
        }
      default:
        return null;
    }
  }

  private static EventTrigger<ClickEvent> createOnClickEventTriggerTrigger(
      ComponentContext c, Component component) {
    int methodId = -830639048;
    return newEventTrigger(c, component, methodId);
  }

  /**
   * @deprecated Do not use this method to get a EventTrigger to use later. Instead give the
   *     component a Handle and use {@link #onClickEventTrigger(ComponentContext, Handle)}.
   */
  @Deprecated
  public static EventTrigger<ClickEvent> onClickEventTriggerTrigger(
      ComponentContext c, String key) {
    int methodId = -830639048;
    return newEventTrigger(c, key, methodId);
  }

  private void onClickEventTrigger(ComponentContext c, EventTriggerTarget _abstract, View view) {
    TestMount _ref = (TestMount) _abstract;
    TestMountSpec.onClickEventTrigger(c, view, (Object) _ref.prop3);
  }

  /**
   * This will send the onClickEventTrigger trigger to the component with the given handle. For more
   * information about using triggers, see https://fblitho.com/docs/trigger-events
   */
  public static void onClickEventTrigger(ComponentContext c, Handle handle, View view) {
    int methodId = -830639048;
    EventTrigger trigger = getEventTrigger(c, methodId, handle);
    if (trigger == null) {
      return;
    }
    ClickEvent _eventState = new ClickEvent();
    _eventState.view = view;
    trigger.dispatchOnTrigger(_eventState, new Object[] {});
  }

  /** @deprecated Use {@link #onClickEventTrigger(ComponentContext, Handle)} instead. */
  @Deprecated
  public static void onClickEventTrigger(ComponentContext c, String key, View view) {
    int methodId = -830639048;
    EventTrigger trigger = getEventTrigger(c, methodId, key);
    if (trigger == null) {
      return;
    }
    ClickEvent _eventState = new ClickEvent();
    _eventState.view = view;
    trigger.dispatchOnTrigger(_eventState, new Object[] {});
  }

  /** @deprecated Use {@link #onClickEventTrigger(ComponentContext, Handle)} instead. */
  @Deprecated
  public static void onClickEventTrigger(EventTrigger trigger, View view) {
    ClickEvent _eventState = new ClickEvent();
    _eventState.view = view;
    trigger.dispatchOnTrigger(_eventState, new Object[] {});
  }

  /** @deprecated Use {@link #onClickEventTrigger(ComponentContext, Handle)} instead. */
  @Deprecated
  static void onClickEventTrigger(ComponentContext c, View view) {
    TestMount component = (TestMount) c.getComponentScope();
    component.onClickEventTrigger(c, (EventTriggerTarget) component, view);
  }

  @Override
  protected Object acceptTriggerEventImpl(
      final EventTrigger eventTrigger, final Object eventState, final Object[] params) {
    int id = eventTrigger.mId;
    switch (id) {
      case -830639048:
        {
          ClickEvent _event = (ClickEvent) eventState;
          onClickEventTrigger(
              (com.facebook.litho.ComponentContext) eventTrigger.mComponentContext,
              eventTrigger.mTriggerTarget,
              _event.view);
          return null;
        }
      default:
        return null;
    }
  }

  @Override
  public void recordEventTrigger(ComponentContext c, EventTriggersContainer container) {
    if (onClickEventTriggerTrigger != null) {
      onClickEventTriggerTrigger.mComponentContext = c;
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
    TestMountStateContainer<S> prevStateContainer =
        (TestMountStateContainer<S>) _prevStateContainer;
    TestMountStateContainer<S> nextStateContainer =
        (TestMountStateContainer<S>) _nextStateContainer;
    nextStateContainer.state1 = prevStateContainer.state1;
    nextStateContainer.state2 = prevStateContainer.state2;
  }

  private TestMountStateContainer getStateContainerWithLazyStateUpdatesApplied(
      ComponentContext c, TestMount component) {
    TestMountStateContainer _stateContainer = new TestMountStateContainer();
    transferState(Component.getStateContainer(c, component), _stateContainer);
    c.applyLazyStateUpdatesForContainer(_stateContainer);
    return _stateContainer;
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    StateContainer.StateUpdate _stateUpdate = new StateContainer.StateUpdate(0, someParam);
    c.updateStateAsync(_stateUpdate, "updateState:TestMount.updateCurrentState");
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    StateContainer.StateUpdate _stateUpdate = new StateContainer.StateUpdate(0, someParam);
    c.updateStateAsync(_stateUpdate, "updateState:TestMount.updateCurrentState");
  }

  protected static void updateCurrentStateSync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    StateContainer.StateUpdate _stateUpdate = new StateContainer.StateUpdate(0, someParam);
    c.updateStateSync(_stateUpdate, "updateState:TestMount.updateCurrentState");
  }

  protected static void lazyUpdateState1(ComponentContext c, final long lazyUpdateValue) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    StateContainer.StateUpdate _stateUpdate =
        new StateContainer.StateUpdate(-2147483648, lazyUpdateValue);
    c.updateStateLazy(_stateUpdate);
  }

  public static <S extends View> Builder<S> create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static <S extends View> Builder<S> create(
      ComponentContext context, int defStyleAttr, int defStyleRes) {
    final Builder builder = new Builder();
    TestMount instance = new TestMount();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  private int getCached(ComponentContext c) {
    String globalKey = c.getGlobalKey();
    final CachedInputs inputs =
        new CachedInputs(globalKey, prop3, prop5, getStateContainerImpl(c).state1);
    Integer cached = (Integer) c.getCachedValue(inputs);
    if (cached == null) {
      cached = TestMountSpec.onCalculateCached(prop3, prop5, getStateContainerImpl(c).state1);
      c.putCachedValue(inputs, cached);
    }
    return cached;
  }

  @Generated
  static class TestMountInterStagePropsContainer implements InterStagePropsContainer {
    Integer boundsDefinedOutput;

    Long measureOutput;
  }

  @VisibleForTesting(otherwise = 2)
  @Generated
  static class TestMountStateContainer<S extends View> extends StateContainer {
    @State
    @Comparable(type = 3)
    long state1;

    @State
    @Comparable(type = 13)
    S state2;

    @Override
    public void applyStateUpdate(StateContainer.StateUpdate stateUpdate) {
      StateValue<Long> state1;
      StateValue<S> state2;

      final Object[] params = stateUpdate.params;
      switch (stateUpdate.type) {
        case 0:
          state1 = new StateValue<Long>();
          state1.set(this.state1);
          TestMountSpec.updateCurrentState(state1, (int) params[0]);
          this.state1 = state1.get();
          break;

        case -2147483648:
          this.state1 = (long) params[0];
          break;
      }
    }
  }

  @Generated
  public static final class Builder<S extends View> extends Component.Builder<Builder<S>> {
    TestMount mTestMount;

    ComponentContext mContext;

    private final String[] REQUIRED_PROPS_NAMES =
        new String[] {"prop1", "prop3", "prop4", "prop5", "prop6", "prop7", "prop8"};

    private final int REQUIRED_PROPS_COUNT = 7;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, TestMount testMountRef) {
      super.init(context, defStyleAttr, defStyleRes, testMountRef);
      mTestMount = testMountRef;
      mContext = context;
      mRequired.clear();
    }

    @Override
    protected void setComponent(Component component) {
      mTestMount = (TestMount) component;
    }

    @PropSetter(value = "prop1", required = true)
    @RequiredProp("prop1")
    public Builder<S> prop1(int prop1) {
      this.mTestMount.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    @PropSetter(value = "prop2", required = false)
    public Builder<S> prop2(boolean prop2) {
      this.mTestMount.prop2 = prop2;
      return this;
    }

    @PropSetter(value = "prop3", required = true)
    @RequiredProp("prop3")
    public Builder<S> prop3(Object prop3) {
      this.mTestMount.prop3 = prop3;
      mRequired.set(1);
      return this;
    }

    @PropSetter(value = "prop4", required = true)
    @RequiredProp("prop4")
    public Builder<S> prop4(char[] prop4) {
      this.mTestMount.prop4 = prop4;
      mRequired.set(2);
      return this;
    }

    @PropSetter(value = "prop5", required = true)
    @RequiredProp("prop5")
    public Builder<S> prop5(char prop5) {
      this.mTestMount.prop5 = prop5;
      mRequired.set(3);
      return this;
    }

    @PropSetter(value = "prop6", required = true)
    @RequiredProp("prop6")
    public Builder<S> prop6(long prop6) {
      this.mTestMount.prop6 = prop6;
      mRequired.set(4);
      return this;
    }

    @PropSetter(value = "prop7", required = true)
    @RequiredProp("prop7")
    public Builder<S> prop7(@Nullable CharSequence prop7) {
      this.mTestMount.prop7 = prop7;
      mRequired.set(5);
      return this;
    }

    @PropSetter(value = "prop7", required = true)
    @RequiredProp("prop7")
    public Builder<S> prop7Res(@StringRes int resId) {
      this.mTestMount.prop7 = mResourceResolver.resolveStringRes(resId);
      mRequired.set(5);
      return this;
    }

    @PropSetter(value = "prop7", required = true)
    @RequiredProp("prop7")
    public Builder<S> prop7Res(@StringRes int resId, Object... formatArgs) {
      this.mTestMount.prop7 = mResourceResolver.resolveStringRes(resId, formatArgs);
      mRequired.set(5);
      return this;
    }

    @PropSetter(value = "prop7", required = true)
    @RequiredProp("prop7")
    public Builder<S> prop7Attr(@AttrRes int attrResId, @StringRes int defResId) {
      this.mTestMount.prop7 = mResourceResolver.resolveStringAttr(attrResId, defResId);
      mRequired.set(5);
      return this;
    }

    @PropSetter(value = "prop7", required = true)
    @RequiredProp("prop7")
    public Builder<S> prop7Attr(@AttrRes int attrResId) {
      this.mTestMount.prop7 = mResourceResolver.resolveStringAttr(attrResId, 0);
      mRequired.set(5);
      return this;
    }

    @PropSetter(value = "prop8", required = true)
    @RequiredProp("prop8")
    public Builder<S> prop8(long prop8) {
      this.mTestMount.prop8 = prop8;
      mRequired.set(6);
      return this;
    }

    public Builder<S> testEventHandler(
        @androidx.annotation.Nullable EventHandler<TestEvent> testEventHandler) {
      this.mTestMount.testEventHandler = testEventHandler;
      return this;
    }

    /**
     * @deprecated Do not use this method to set an EventTrigger. Instead, set a Handle on this
     *     Component with .handle() and dispatch the trigger using the variant of
     *     TestMount.onClickEventTriggerTrigger that takes a Handle param.
     */
    @Deprecated
    public Builder<S> onClickEventTriggerTrigger(EventTrigger onClickEventTriggerTrigger) {
      this.mTestMount.onClickEventTriggerTrigger = onClickEventTriggerTrigger;
      return this;
    }

    private void registerOnClickEventTriggerTrigger() {
      com.facebook.litho.EventTrigger onClickEventTriggerTrigger =
          this.mTestMount.onClickEventTriggerTrigger;
      if (onClickEventTriggerTrigger == null) {
        onClickEventTriggerTrigger =
            TestMount.createOnClickEventTriggerTrigger(this.mContext, this.mTestMount);
      }
      onClickEventTriggerTrigger(onClickEventTriggerTrigger);
    }

    private void registerEventTriggers() {
      registerOnClickEventTriggerTrigger();
    }

    @Override
    public Builder<S> getThis() {
      return this;
    }

    @Override
    public TestMount build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      registerEventTriggers();
      return mTestMount;
    }
  }

  @Generated
  private static class CachedInputs {
    private final String globalKey;

    private final Object prop3;

    private final char prop5;

    private final long state1;

    CachedInputs(String globalKey, Object prop3, char prop5, long state1) {
      this.globalKey = globalKey;
      this.prop3 = prop3;
      this.prop5 = prop5;
      this.state1 = state1;
    }

    @Override
    public int hashCode() {
      return CommonUtils.hash(globalKey, prop3, prop5, state1, getClass());
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
      if (!com.facebook.litho.CommonUtils.equals(globalKey, cachedValueInputs.globalKey)) {
        return false;
      }
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
