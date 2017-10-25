/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.integration.resources;

import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import android.widget.TextView;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Diff;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.StateValue;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import java.util.BitSet;
/**
 * Comment to be copied in generated section
 *
 * @prop-required prop1 int
 * @prop-optional prop2 java.lang.String
 * @prop-required prop3 com.facebook.litho.Component
 * @prop-required prop4 java.lang.String
 */
final class FullGroupSection<T> extends SectionLifecycle {
  private static FullGroupSection sInstance = null;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private FullGroupSection() {
  }

  private static synchronized FullGroupSection get() {
    if (sInstance == null) {
      sInstance = new FullGroupSection();
    }
    return sInstance;
  }

  public static <T> Builder<T> create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new FullGroupSectionImpl());
    return builder;
  }

  @Override
  protected void transferState(
      SectionContext context,
      SectionLifecycle.StateContainer prevStateContainer,
      Section component) {
    FullGroupSectionStateContainerImpl prevStateContainerImpl = (FullGroupSectionStateContainerImpl) prevStateContainer;
    FullGroupSectionImpl componentImpl = (FullGroupSectionImpl) component;
    componentImpl.mStateContainerImpl.state1 = prevStateContainerImpl.state1;
    componentImpl.mStateContainerImpl.state2 = prevStateContainerImpl.state2;
  }

  protected static void updateStateAsync(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullGroupSection.UpdateStateStateUpdate _stateUpdate =
        ((FullGroupSection.FullGroupSectionImpl) _component).createUpdateStateStateUpdate(param);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateState(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullGroupSection.UpdateStateStateUpdate _stateUpdate = ((FullGroupSection.FullGroupSectionImpl) _component).createUpdateStateStateUpdate(param);
    c.updateState(_stateUpdate);
  }

  protected static void lazyUpdateState2(SectionContext c, final Object lazyUpdateValue) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    SectionLifecycle.StateUpdate _stateUpdate =
        new SectionLifecycle.StateUpdate() {
          public void updateState(
              SectionLifecycle.StateContainer stateContainer, Section newComponent) {
            FullGroupSection.FullGroupSectionImpl newComponentStateUpdate =
                (FullGroupSection.FullGroupSectionImpl) newComponent;
            StateValue<Object> state2 = new StateValue<Object>();
            state2.set(lazyUpdateValue);
            newComponentStateUpdate.mStateContainerImpl.state2 = state2.get();
          }
        };
    c.updateStateLazy(_stateUpdate);
  }

  public static EventHandler getTestEventHandler(SectionContext context) {
    if (context.getSectionScope() == null) {
      return null;
    }
    return ((FullGroupSection.FullGroupSectionImpl) context.getSectionScope()).testEventHandler;
  }

  static boolean dispatchTestEvent(EventHandler _eventHandler, Object object) {
    TestEvent _eventState = sTestEventPool.acquire();
    if (_eventState == null) {
      _eventState = new TestEvent();
    }
    _eventState.object = object;
    EventDispatcher _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher();
    boolean result = (boolean) _lifecycle.dispatchOnEvent(_eventHandler, _eventState);
    _eventState.object = null;
    sTestEventPool.release(_eventState);
    return result;
  }

  private void testEvent(
      HasEventDispatcher _abstractImpl, SectionContext c, TextView view, int someParam) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.testEvent(
        c, view, someParam, (Object) _impl.mStateContainerImpl.state2, (String) _impl.prop2);
  }

  public static EventHandler<ClickEvent> testEvent(SectionContext c, int someParam) {
    return newEventHandler(c, "testEvent", -1204074200, new Object[] {
        c,
        someParam,
    });
  }

  @Override
  public Object dispatchOnEvent(final EventHandler eventHandler, final Object eventState) {
    int id = eventHandler.id;
    switch (id) {
      case -1204074200:
        {
          ClickEvent _event = (ClickEvent) eventState;
          testEvent(
              eventHandler.mHasEventDispatcher,
              (SectionContext) eventHandler.params[0],
              (TextView) _event.view,
              (int) eventHandler.params[1]);
          return null;
        }
      default:
        return null;
    }
  }

  @Override
  protected void createInitialState(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    StateValue<T> state1 = new StateValue<>();
    StateValue<Object> state2 = new StateValue<>();
    FullGroupSectionSpec.onCreateInitialState(
        (SectionContext) c, (int) _impl.prop1, state1, state2);
    _impl.mStateContainerImpl.state1 = state1.get();
    _impl.mStateContainerImpl.state2 = state2.get();
  }

  private String onCreateService(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    String _result =
        (String) FullGroupSectionSpec.onCreateService((SectionContext) c, (String) _impl.prop2);
    return _result;
  }

  @Override
  public void createService(SectionContext context, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    _impl._service = onCreateService(context, _abstractImpl);
  }

  @Override
  protected void transferService(SectionContext c, Section previous, Section next) {
    FullGroupSectionImpl previousSection = (FullGroupSectionImpl) previous;
    FullGroupSectionImpl nextSection = (FullGroupSectionImpl) next;
    nextSection._service = previousSection._service;
  }

  @Override
  protected Object getService(Section section) {
    return ((FullGroupSectionImpl) section)._service;
  }

  @Override
  protected Children createChildren(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    Children _result =
        (Children)
            FullGroupSectionSpec.onCreateChildren(
                (SectionContext) c,
                (Component) _impl.prop3,
                (String) _impl.prop4,
                (T) _impl.mStateContainerImpl.state1);
    return _result;
  }

  @Override
  protected void bindService(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.bindService(
        (SectionContext) c,
        (String) _impl._service,
        (int) _impl.prop1,
        (Object) _impl.mStateContainerImpl.state2);
  }

  @Override
  protected void unbindService(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.unbindService(
        (SectionContext) c,
        (String) _impl._service,
        (int) _impl.prop1,
        (Object) _impl.mStateContainerImpl.state2);
  }

  @Override
  protected void refresh(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.onRefresh(
        (SectionContext) c, (String) _impl._service, (String) _impl.prop2);
  }

  @Override
  protected void dataBound(SectionContext c, Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.onDataBound(
        (SectionContext) c, (Component) _impl.prop3, (Object) _impl.mStateContainerImpl.state2);
  }

  @Override
  protected boolean shouldUpdate(Section _prevAbstractImpl, Section _nextAbstractImpl) {
    FullGroupSectionImpl _prevImpl = (FullGroupSectionImpl) _prevAbstractImpl;
    FullGroupSectionImpl _nextImpl = (FullGroupSectionImpl) _nextAbstractImpl;
    Diff<Integer> prop1 =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.prop1,
                _nextImpl == null ? null : _nextImpl.prop1);
    boolean _result = (boolean) FullGroupSectionSpec.shouldUpdate(prop1);
    releaseDiff(prop1);
    return _result;
  }

  @Override
  protected void viewportChanged(
      SectionContext c,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int totalCount,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      Section _abstractImpl) {
    FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    FullGroupSectionSpec.onViewportChanged(
        (SectionContext) c,
        (int) firstVisibleIndex,
        (int) lastVisibleIndex,
        (int) totalCount,
        (int) firstFullyVisibleIndex,
        (int) lastFullyVisibleIndex,
        (T) _impl.mStateContainerImpl.state1,
        (Object) _impl.mStateContainerImpl.state2,
        (int) _impl.prop1,
        (String) _impl.prop2,
        (Component) _impl.prop3);
  }

  @Override
  protected void populateTreeProps(Section _abstractImpl, TreeProps treeProps) {
    if (treeProps == null) {
      return;
    }
    final FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    _impl.treeProp =
        treeProps.get(
            com.facebook.litho.sections.processor.integration.resources.FullGroupSectionSpec
                .TreePropWrapper.class);
  }

  @Override
  protected TreeProps getTreePropsForChildren(
      SectionContext c, Section _abstractImpl, TreeProps parentTreeProps) {
    final FullGroupSectionImpl _impl = (FullGroupSectionImpl) _abstractImpl;
    final TreeProps childTreeProps = TreeProps.copy(parentTreeProps);
    childTreeProps.put(
        com.facebook.litho.sections.processor.integration.resources.FullGroupSectionSpec
            .TreePropWrapper.class,
        FullGroupSectionSpec.onCreateTreeProp(
            (SectionContext) c, (FullGroupSectionSpec.TreePropWrapper) _impl.treeProp));
    return childTreeProps;
  }

  @VisibleForTesting(otherwise = 2) static class FullGroupSectionStateContainerImpl<T> implements SectionLifecycle.StateContainer {
    @State T state1;

    @State Object state2;
  }

  static class FullGroupSectionImpl<T> extends Section<FullGroupSection> implements Cloneable {
    FullGroupSectionStateContainerImpl mStateContainerImpl;

    @Prop(resType = ResType.NONE, optional = false)
    int prop1;

    @Prop(resType = ResType.NONE, optional = true)
    String prop2;

    @Prop(resType = ResType.NONE, optional = false)
    Component prop3;

    @Prop(resType = ResType.STRING, optional = false)
    String prop4;

    FullGroupSectionSpec.TreePropWrapper treeProp;

    String _service;

    EventHandler testEventHandler;

    private FullGroupSectionImpl() {
      super(get());
      mStateContainerImpl = new FullGroupSectionStateContainerImpl();
    }

    @Override
    protected SectionLifecycle.StateContainer getStateContainer() {
      return mStateContainerImpl;
    }

    @Override
    public String getSimpleName() {
      return "FullGroupSection";
    }

    @Override
    public boolean isEquivalentTo(Section<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      FullGroupSectionImpl fullGroupSectionImpl = (FullGroupSectionImpl) other;
      if (prop1 != fullGroupSectionImpl.prop1) {
        return false;
      }
      if (prop2 != null
          ? !prop2.equals(fullGroupSectionImpl.prop2)
          : fullGroupSectionImpl.prop2 != null) {
        return false;
      }
      if (prop3 != null ? !prop3.isEquivalentTo(fullGroupSectionImpl.prop3) : fullGroupSectionImpl.prop3 != null) {
        return false;
      }
      if (prop4 != null
          ? !prop4.equals(fullGroupSectionImpl.prop4)
          : fullGroupSectionImpl.prop4 != null) {
        return false;
      }
      if (mStateContainerImpl.state1 != null
          ? !mStateContainerImpl.state1.equals(fullGroupSectionImpl.mStateContainerImpl.state1)
          : fullGroupSectionImpl.mStateContainerImpl.state1 != null) {
        return false;
      }
      if (mStateContainerImpl.state2 != null ? !mStateContainerImpl.state2.equals(fullGroupSectionImpl.mStateContainerImpl.state2) : fullGroupSectionImpl.mStateContainerImpl.state2 != null) {
        return false;
      }
      if (treeProp != null
          ? !treeProp.equals(fullGroupSectionImpl.treeProp)
          : fullGroupSectionImpl.treeProp != null) {
        return false;
      }
      return true;
    }

    private UpdateStateStateUpdate createUpdateStateStateUpdate(Object param) {
      return new UpdateStateStateUpdate(param);
    }

    @Override
    public FullGroupSectionImpl makeShallowCopy(boolean deepCopy) {
      FullGroupSectionImpl component = (FullGroupSectionImpl) super.makeShallowCopy(deepCopy);
      component.prop3 = component.prop3 != null ? component.prop3.makeShallowCopy() : null;
      if (!deepCopy) {
        component.mStateContainerImpl = new FullGroupSectionStateContainerImpl();
      }
      return component;
    }
  }

  public static class Builder<T> extends Section.Builder<FullGroupSection, Builder<T>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"prop1", "prop3", "prop4"};

    private static final int REQUIRED_PROPS_COUNT = 3;

    FullGroupSectionImpl mFullGroupSectionImpl;

    SectionContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(SectionContext context, FullGroupSectionImpl fullGroupSectionImpl) {
      super.init(context, fullGroupSectionImpl);
      mFullGroupSectionImpl = fullGroupSectionImpl;
      mContext = context;
      mRequired.clear();
    }

    public Builder<T> prop1(int prop1) {
      this.mFullGroupSectionImpl.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    public Builder<T> prop2(String prop2) {
      this.mFullGroupSectionImpl.prop2 = prop2;
      return this;
    }

    public Builder<T> prop3(Component prop3) {
      this.mFullGroupSectionImpl.prop3 = prop3;
      mRequired.set(1);
      return this;
    }

    public Builder<T> prop3(Component.Builder<? extends ComponentLifecycle, ?> prop3Builder) {
      this.mFullGroupSectionImpl.prop3 = prop3Builder.build();
      mRequired.set(1);
      return this;
    }

    public Builder<T> prop4(String prop4) {
      this.mFullGroupSectionImpl.prop4 = prop4;
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop4Res(@StringRes int resId) {
      this.mFullGroupSectionImpl.prop4 = resolveStringRes(resId);
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop4Res(@StringRes int resId, Object... formatArgs) {
      this.mFullGroupSectionImpl.prop4 = resolveStringRes(resId, formatArgs);
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop4Attr(@AttrRes int attrResId, @StringRes int defResId) {
      this.mFullGroupSectionImpl.prop4 = resolveStringAttr(attrResId, defResId);
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop4Attr(@AttrRes int attrResId) {
      this.mFullGroupSectionImpl.prop4 = resolveStringAttr(attrResId, 0);
      mRequired.set(2);
      return this;
    }

    public Builder<T> testEventHandler(EventHandler testEventHandler) {
      this.mFullGroupSectionImpl.testEventHandler = testEventHandler;
      return this;
    }

    @Override
    public Builder<T> key(String key) {
      return super.key(key);
    }

    @Override
    public Builder<T> loadingEventHandler(EventHandler<LoadingEvent> loadingEventHandler) {
      return super.loadingEventHandler(loadingEventHandler);
    }

    @Override
    public Builder<T> getThis() {
      return this;
    }

    @Override
    public Section<FullGroupSection> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      FullGroupSectionImpl fullGroupSectionImpl = mFullGroupSectionImpl;
      release();
      return fullGroupSectionImpl;
    }

    @Override
    protected void release() {
      super.release();
      mFullGroupSectionImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }

  private static class UpdateStateStateUpdate implements SectionLifecycle.StateUpdate {
    private Object mParam;

    UpdateStateStateUpdate(Object param) {
      mParam = param;
    }

    public void updateState(SectionLifecycle.StateContainer stateContainer, Section newComponent) {
      FullGroupSectionStateContainerImpl stateContainerImpl = (FullGroupSectionStateContainerImpl) stateContainer;
      FullGroupSectionImpl newComponentStateUpdate = (FullGroupSectionImpl) newComponent;
      StateValue<Object> state2 = new StateValue<Object>();
      state2.set(stateContainerImpl.state2);
      FullGroupSectionSpec.updateState(state2, mParam);
      newComponentStateUpdate.mStateContainerImpl.state2 = state2.get();
    }
  }
}
