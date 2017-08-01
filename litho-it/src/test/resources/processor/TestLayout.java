package com.facebook.litho.processor.integration.resources;

import android.annotation.TargetApi;
import android.support.v4.util.Pools;
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
import com.facebook.litho.StateValue;
import com.facebook.litho.TransitionSet;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @prop-required prop1 int
 * @prop-required prop6 long
 * @prop-optional prop2 boolean
 * @prop-required prop3 java.lang.Object
 * @prop-required prop4 char[]
 * @prop-required prop5 char
 */
@TargetApi(17)
public final class TestLayout<S extends View> extends ComponentLifecycle {
  private static TestLayout sInstance = null;

  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private TestLayout() {
  }

  public static synchronized TestLayout get() {
    if (sInstance == null) {
      sInstance = new TestLayout();
    }
    return sInstance;
  }

  @Override
  protected void populateTreeProps(Component _abstractImpl, TreeProps treeProps) {
    if (treeProps == null) {
      return;
    }
    final TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    _impl.treeProp = treeProps.get(com.facebook.litho.processor.integration.resources.TestTreeProp.class);
  }

  @Override
  protected TreeProps getTreePropsForChildren(ComponentContext c, Component _abstractImpl,
      TreeProps parentTreeProps) {
    final TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    final TreeProps childTreeProps = TreeProps.copy(parentTreeProps);
    childTreeProps.put(com.facebook.litho.processor.integration.resources.TestTreeProp.class, TestLayoutSpec.onCreateFeedPrefetcherProp(
        (ComponentContext) c,
        (long) _impl.prop6));
    return childTreeProps;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onLoadStyle(ComponentContext c, Component _abstractImpl) {
    TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    Output<Boolean> prop2 = acquireOutput();
    Output<Object> prop3 = acquireOutput();
    TestLayoutSpec.onLoadStyle(
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
    TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    StateValue<S> state2 = new StateValue<>();
    TestLayoutSpec.createInitialState(
        (ComponentContext) c,
        (int) _impl.prop1,
        state2);
    _impl.mStateContainerImpl.state2 = state2.get();
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext context, Component _abstractImpl) {
    TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    ComponentLayout _result = (ComponentLayout) TestLayoutSpec.onCreateLayout(
        (ComponentContext) context,
        (boolean) _impl.prop2,
        (Object) _impl.prop3,
        (char[]) _impl.prop4,
        (long) _impl.mStateContainerImpl.state1,
        (S) _impl.mStateContainerImpl.state2,
        (int) _impl.mStateContainerImpl.state3,
        (TestTreeProp) _impl.treeProp);
    return _result;
  }

  @Override
  protected TransitionSet onCreateTransition(ComponentContext c, Component _abstractImpl) {
    TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    Diff<Integer> _state3Diff = acquireDiff(
        _impl.mPreviousRenderInfo == null ? null : _impl.mPreviousRenderInfo.state3,
        _impl.mStateContainerImpl.state3);
    TransitionSet _result = (TransitionSet) TestLayoutSpec.onCreateTransition(
        (ComponentContext) c,
        (Object) _impl.prop3,
        (long) _impl.mStateContainerImpl.state1,
        _state3Diff);
    releaseDiff(_state3Diff);
    return _result;
  }

  public static EventHandler getTestEventHandler(ComponentContext context) {
    if (context.getComponentScope() == null) {
      return null;
    }
    return ((TestLayout.TestLayoutImpl) context.getComponentScope()).testEventHandler;
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
    TestLayoutImpl _impl = (TestLayoutImpl) _abstractImpl;
    TestLayoutSpec.testLayoutEvent(
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
    TestLayoutStateContainerImpl prevStateContainerImpl = (TestLayoutStateContainerImpl) prevStateContainer;
    TestLayoutImpl componentImpl = (TestLayoutImpl) component;
    componentImpl.mStateContainerImpl.state1 = prevStateContainerImpl.state1;
    componentImpl.mStateContainerImpl.state2 = prevStateContainerImpl.state2;
    componentImpl.mStateContainerImpl.state3 = prevStateContainerImpl.state3;
  }

  protected static void updateCurrentStateAsync(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout.TestLayoutImpl) _component).createUpdateCurrentStateStateUpdate(someParam);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateCurrentState(ComponentContext c, int someParam) {
    Component _component = c.getComponentScope();
    if (_component == null) {
      return;
    }
    TestLayout.UpdateCurrentStateStateUpdate _stateUpdate = ((TestLayout.TestLayoutImpl) _component).createUpdateCurrentStateStateUpdate(someParam);
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
        TestLayout.TestLayoutImpl newComponentStateUpdate = (TestLayout.TestLayoutImpl) newComponent;
        StateValue<Long> state1 = new StateValue<Long>();
        state1.set(lazyUpdateValue);
        newComponentStateUpdate.mStateContainerImpl.state1 = state1.get();
      }

      public boolean isLazyStateUpdate() {
        return true;
      }
    };
    c.updateStateLazy(_stateUpdate);
  }

  @Override
  protected boolean needsPreviousRenderInfo() {
    return true;
  }

  @Override
  protected ComponentLifecycle.RenderInfo recordRenderInfo(Component previousComponent,
      ComponentLifecycle.RenderInfo toRecycle) {
    TestLayoutImpl _impl = (TestLayoutImpl) previousComponent;
    TestLayoutRenderInfo renderInfo = toRecycle != null ?
        (TestLayoutRenderInfo) toRecycle :
        new TestLayoutRenderInfo();
    renderInfo.record(_impl);
    return renderInfo;
  }

  @Override
  protected void applyPreviousRenderInfo(Component component,
      ComponentLifecycle.RenderInfo previousRenderInfo) {
    TestLayoutImpl _impl = (TestLayoutImpl) component;
    if (previousRenderInfo == null) {
      _impl.mPreviousRenderInfo = null;
      return;
    }
    if (_impl.mPreviousRenderInfo == null) {
      _impl.mPreviousRenderInfo = new TestLayoutRenderInfo();
    }
    TestLayoutRenderInfo infoImpl = (TestLayoutRenderInfo) previousRenderInfo;
    _impl.mPreviousRenderInfo.copy(infoImpl);
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new TestLayoutImpl());
    return builder;
  }

  private static class TestLayoutStateContainerImpl<S extends View> implements ComponentLifecycle.StateContainer {
    @State
    long state1;

    @State
    S state2;

    @State
    int state3;
  }

  private static class TestLayoutRenderInfo<S extends View> implements ComponentLifecycle.RenderInfo {
    @State
    int state3;

    void copy(TestLayoutRenderInfo info) {
      state3 = info.state3;
    }

    void record(TestLayoutImpl component) {
      state3 = component.mStateContainerImpl.state3;
    }
  }

  private static class TestLayoutImpl<S extends View> extends Component<TestLayout> implements Cloneable {
    TestLayoutStateContainerImpl mStateContainerImpl;

    TestLayoutRenderInfo mPreviousRenderInfo;

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
        optional = true
    )
    boolean prop2 = TestLayoutSpec.prop2;

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

    TestTreeProp treeProp;

    EventHandler testEventHandler;

    private TestLayoutImpl() {
      super(get());
      mStateContainerImpl = new TestLayoutStateContainerImpl();
    }

    @Override
    protected ComponentLifecycle.StateContainer getStateContainer() {
      return mStateContainerImpl;
    }

    @Override
    public String getSimpleName() {
      return "TestLayout";
    }

    @Override
    public boolean isEquivalentTo(Component<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      TestLayoutImpl testLayoutImpl = (TestLayoutImpl) other;
      if (this.getId() == testLayoutImpl.getId()) {
        return true;
      }
      if (prop1 != testLayoutImpl.prop1) {
        return false;
      }
      if (prop6 != testLayoutImpl.prop6) {
        return false;
      }
      if (prop2 != testLayoutImpl.prop2) {
        return false;
      }
      if (prop3 != null ? !prop3.equals(testLayoutImpl.prop3) : testLayoutImpl.prop3 != null) {
        return false;
      }
      if (!Arrays.equals(prop4, testLayoutImpl.prop4)) {
        return false;
      }
      if (prop5 != testLayoutImpl.prop5) {
        return false;
      }
      if (mStateContainerImpl.state1 != testLayoutImpl.mStateContainerImpl.state1) {
        return false;
      }
      if (mStateContainerImpl.state2 != null ? !mStateContainerImpl.state2.equals(testLayoutImpl.mStateContainerImpl.state2) : testLayoutImpl.mStateContainerImpl.state2 != null) {
        return false;
      }
      if (mStateContainerImpl.state3 != testLayoutImpl.mStateContainerImpl.state3) {
        return false;
      }
      if (treeProp != null ? !treeProp.equals(testLayoutImpl.treeProp) : testLayoutImpl.treeProp != null) {
        return false;
      }
      return true;
    }

    private UpdateCurrentStateStateUpdate createUpdateCurrentStateStateUpdate(int someParam) {
      return new UpdateCurrentStateStateUpdate(someParam);
    }

    @Override
    public TestLayoutImpl makeShallowCopy() {
      TestLayoutImpl component = (TestLayoutImpl) super.makeShallowCopy();
      component.mStateContainerImpl = new TestLayoutStateContainerImpl();
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
      TestLayoutStateContainerImpl stateContainerImpl = (TestLayoutStateContainerImpl) stateContainer;
      TestLayoutImpl newComponentStateUpdate = (TestLayoutImpl) newComponent;
      StateValue<Long> state1 = new StateValue<Long>();
      state1.set(stateContainerImpl.state1);
      TestLayoutSpec.updateCurrentState(state1,mSomeParam);
      newComponentStateUpdate.mStateContainerImpl.state1 = state1.get();
    }

    public boolean isLazyStateUpdate() {
      return false;
    }
  }

  public static class Builder<S extends View> extends Component.Builder<TestLayout, Builder<S>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"prop1", "prop6", "prop3", "prop4", "prop5"};

    private static final int REQUIRED_PROPS_COUNT = 5;

    TestLayoutImpl mTestLayoutImpl;

    ComponentContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        TestLayoutImpl testLayoutImpl) {
      super.init(context, defStyleAttr, defStyleRes, testLayoutImpl);
      mTestLayoutImpl = testLayoutImpl;
      mContext = context;
      mRequired.clear();
    }

    public Builder prop1(int prop1) {
      this.mTestLayoutImpl.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    public Builder prop6(long prop6) {
      this.mTestLayoutImpl.prop6 = prop6;
      mRequired.set(1);
      return this;
    }

    public Builder prop2(boolean prop2) {
      this.mTestLayoutImpl.prop2 = prop2;
      return this;
    }

    public Builder prop3(Object prop3) {
      this.mTestLayoutImpl.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder prop4(char[] prop4) {
      this.mTestLayoutImpl.prop4 = prop4;
      mRequired.set(3);
      return this;
    }

    public Builder prop5(char prop5) {
      this.mTestLayoutImpl.prop5 = prop5;
      mRequired.set(4);
      return this;
    }

    public Builder testEventHandler(EventHandler testEventHandler) {
      this.mTestLayoutImpl.testEventHandler = testEventHandler;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component<TestLayout> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      TestLayoutImpl testLayoutImpl = mTestLayoutImpl;
      release();
      return testLayoutImpl;
    }

    @Override
    protected void release() {
      super.release();
      mTestLayoutImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
