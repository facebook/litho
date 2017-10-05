/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.integration.resources;

import android.support.v4.util.Pools;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Diff;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import java.util.BitSet;
import java.util.List;

/**
 * @prop-required prop1 java.lang.Integer
 * @prop-optional prop2 java.lang.String
 * @prop-required data java.util.List<T>
 * @prop-required prop3 com.facebook.litho.Component
 */
public final class FullDiffSection<T> extends SectionLifecycle {
  private static FullDiffSection sInstance = null;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  static final Pools.SynchronizedPool<TestEvent> sTestEventPool = new Pools.SynchronizedPool<TestEvent>(2);

  private FullDiffSection() {
  }

  private static synchronized FullDiffSection get() {
    if (sInstance == null) {
      sInstance = new FullDiffSection();
    }
    return sInstance;
  }

  public static <T> Builder<T> create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new FullDiffSectionImpl());
    return builder;
  }

  @Override
  protected void transferState(
      SectionContext context,
      SectionLifecycle.StateContainer prevStateContainer,
      Section component) {
    FullDiffSectionStateContainerImpl prevStateContainerImpl =
        (FullDiffSectionStateContainerImpl) prevStateContainer;
    FullDiffSectionImpl componentImpl = (FullDiffSectionImpl) component;
    componentImpl.mStateContainerImpl.state1 = prevStateContainerImpl.state1;
  }

  protected static void updateStateAsync(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullDiffSection.UpdateStateStateUpdate _stateUpdate =
        ((FullDiffSection.FullDiffSectionImpl) _component).createUpdateStateStateUpdate(param);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateState(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullDiffSection.UpdateStateStateUpdate _stateUpdate = ((FullDiffSection.FullDiffSectionImpl) _component).createUpdateStateStateUpdate(param);
    c.updateState(_stateUpdate);
  }

  public static EventHandler getTestEventHandler(SectionContext context) {
    if (context.getSectionScope() == null) {
      return null;
    }
    return ((FullDiffSection.FullDiffSectionImpl) context.getSectionScope()).testEventHandler;
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

  private void testEvent(HasEventDispatcher _abstractImpl, SectionContext c, View view,
      int someParam) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.testEvent(
        c,
        view,
        someParam);
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
      case -1204074200: {
        ClickEvent _event = (ClickEvent) eventState;
        testEvent(
            eventHandler.mHasEventDispatcher,
            (SectionContext) eventHandler.params[0],
            (View) _event.view,
            (int) eventHandler.params[1]);
        return null;
      }
      default:
        return null;
    }
  }

  @Override
  protected void createInitialState(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    StateValue<Object> state1 = new StateValue<>();
    FullDiffSectionSpec.onCreateInitialState(
        (SectionContext) c,
        (Integer) _impl.prop1,
        state1);
    _impl.mStateContainerImpl.state1 = state1.get();
  }

  @Override
  protected void generateChangeSet(SectionContext c, ChangeSet changeSet, Section _prevAbstractImpl,
      Section _nextAbstractImpl) {
    FullDiffSectionImpl _prevImpl = (FullDiffSectionImpl) _prevAbstractImpl;
    FullDiffSectionImpl _nextImpl = (FullDiffSectionImpl) _nextAbstractImpl;
    Diff<List<T>> data = (Diff) acquireDiff(_prevImpl == null ? null : _prevImpl.data, _nextImpl == null ? null : _nextImpl.data);
    Diff<Component> prop3 = (Diff) acquireDiff(_prevImpl == null ? null : _prevImpl.prop3, _nextImpl == null ? null : _nextImpl.prop3);
    Diff<Object> state1 = (Diff) acquireDiff(_prevImpl == null ? null : _prevImpl.mStateContainerImpl.state1, _nextImpl == null ? null : _nextImpl.mStateContainerImpl.state1);
    FullDiffSectionSpec.onDiff(
        (SectionContext) c,
        (ChangeSet) changeSet,
        data,
        prop3,
        state1);
    releaseDiff(data);
    releaseDiff(prop3);
    releaseDiff(state1);
  }

  @Override
  protected boolean isDiffSectionSpec() {
    return true;
  }

  private String onCreateService(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    String _result = (String) FullDiffSectionSpec.onCreateService(
        (SectionContext) c,
        (String) _impl.prop2);
    return _result;
  }

  @Override
  public void createService(SectionContext context, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    _impl._service = onCreateService(context, _abstractImpl);
  }

  @Override
  protected void transferService(SectionContext c, Section previous, Section next) {
    FullDiffSectionImpl previousSection = (FullDiffSectionImpl) previous;
    FullDiffSectionImpl nextSection = (FullDiffSectionImpl) next;
    nextSection._service = previousSection._service;
  }

  @Override
  protected Object getService(Section section) {
    return ((FullDiffSectionImpl) section)._service;
  }

  @Override
  protected void bindService(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.bindService(
        (SectionContext) c,
        (String) _impl._service);
  }

  @Override
  protected void unbindService(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.unbindService(
        (SectionContext) c,
        (String) _impl._service);
  }

  @Override
  protected void destroyService(SectionContext c, Object service) {
    FullDiffSectionSpec.destroyService(
        (SectionContext) c,
        (String) service);
  }

  @Override
  protected boolean hasDestroyService() {
    return true;
  }

  @Override
  protected void refresh(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.onRefresh(
        (SectionContext) c,
        (String) _impl._service);
  }

  @Override
  protected void dataBound(SectionContext c, Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.onDataBound(
        (SectionContext) c);
  }

  @Override
  protected boolean shouldUpdate(Section _prevAbstractImpl, Section _nextAbstractImpl) {
    FullDiffSectionImpl _prevImpl = (FullDiffSectionImpl) _prevAbstractImpl;
    FullDiffSectionImpl _nextImpl = (FullDiffSectionImpl) _nextAbstractImpl;
    Diff<Integer> prop1 = (Diff) acquireDiff(_prevImpl == null ? null : _prevImpl.prop1, _nextImpl == null ? null : _nextImpl.prop1);
    boolean _result = (boolean) FullDiffSectionSpec.shouldUpdate(
        prop1);
    releaseDiff(prop1);
    return _result;
  }

  @Override
  protected void viewportChanged(SectionContext c, int firstVisibleIndex, int lastVisibleIndex,
      int totalCount, int firstFullyVisibleIndex, int lastFullyVisibleIndex,
      Section _abstractImpl) {
    FullDiffSectionImpl _impl = (FullDiffSectionImpl) _abstractImpl;
    FullDiffSectionSpec.onViewportChanged(
        (SectionContext) c,
        (int) firstVisibleIndex,
        (int) lastVisibleIndex,
        (int) totalCount,
        (int) firstFullyVisibleIndex,
        (int) lastFullyVisibleIndex);
  }

  private static class FullDiffSectionStateContainerImpl<T> implements SectionLifecycle.StateContainer {
    @State
    Object state1;
  }

  static class FullDiffSectionImpl<T> extends Section<FullDiffSection> implements Cloneable {
    FullDiffSectionStateContainerImpl mStateContainerImpl;

    @Prop(
        resType = ResType.NONE,
        optional = false
    )
    Integer prop1;

    @Prop(
        resType = ResType.NONE,
        optional = true
    )
    String prop2;

    @Prop(
        resType = ResType.NONE,
        optional = false
    )
    List<T> data;

    @Prop(
        resType = ResType.NONE,
        optional = false
    )
    Component prop3;

    String _service;

    EventHandler testEventHandler;

    private FullDiffSectionImpl() {
      super(get());
      mStateContainerImpl = new FullDiffSectionStateContainerImpl();
    }

    @Override
    protected SectionLifecycle.StateContainer getStateContainer() {
      return mStateContainerImpl;
    }

    @Override
    public String getSimpleName() {
      return "FullDiffSection";
    }

    @Override
    public boolean isEquivalentTo(Section<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      FullDiffSectionImpl fullDiffSectionImpl = (FullDiffSectionImpl) other;
      if (prop1 != null ? !prop1.equals(fullDiffSectionImpl.prop1) : fullDiffSectionImpl.prop1 != null) {
        return false;
      }
      if (prop2 != null ? !prop2.equals(fullDiffSectionImpl.prop2) : fullDiffSectionImpl.prop2 != null) {
        return false;
      }
      if (data != null ? !data.equals(fullDiffSectionImpl.data) : fullDiffSectionImpl.data != null) {
        return false;
      }
      if (prop3 != null ? !prop3.isEquivalentTo(fullDiffSectionImpl.prop3) : fullDiffSectionImpl.prop3 != null) {
        return false;
      }
      if (mStateContainerImpl.state1 != null ? !mStateContainerImpl.state1.equals(fullDiffSectionImpl.mStateContainerImpl.state1) : fullDiffSectionImpl.mStateContainerImpl.state1 != null) {
        return false;
      }
      return true;
    }

    private UpdateStateStateUpdate createUpdateStateStateUpdate(Object param) {
      return new UpdateStateStateUpdate(param);
    }

    @Override
    public FullDiffSectionImpl makeShallowCopy(boolean deepCopy) {
      FullDiffSectionImpl component = (FullDiffSectionImpl) super.makeShallowCopy(deepCopy);
      component.prop3 = component.prop3 != null ? component.prop3.makeShallowCopy() : null;
      if (!deepCopy) {
        component.mStateContainerImpl = new FullDiffSectionStateContainerImpl();
      }
      return component;
    }
  }

  public static class Builder<T> extends Section.Builder<FullDiffSection, Builder<T>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"prop1", "data", "prop3"};

    private static final int REQUIRED_PROPS_COUNT = 3;

    FullDiffSectionImpl mFullDiffSectionImpl;

    SectionContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(SectionContext context, FullDiffSectionImpl fullDiffSectionImpl) {
      super.init(context, fullDiffSectionImpl);
      mFullDiffSectionImpl = fullDiffSectionImpl;
      mContext = context;
      mRequired.clear();
    }

    public Builder<T> prop1(Integer prop1) {
      this.mFullDiffSectionImpl.prop1 = prop1;
      mRequired.set(0);
      return this;
    }

    public Builder<T> prop2(String prop2) {
      this.mFullDiffSectionImpl.prop2 = prop2;
      return this;
    }

    public Builder<T> data(List<T> data) {
      this.mFullDiffSectionImpl.data = data;
      mRequired.set(1);
      return this;
    }

    public Builder<T> prop3(Component prop3) {
      this.mFullDiffSectionImpl.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop3(Component.Builder<? extends ComponentLifecycle, ?> prop3Builder) {
      this.mFullDiffSectionImpl.prop3 = prop3Builder.build();
      mRequired.set(2);
      return this;
    }

    public Builder<T> testEventHandler(EventHandler testEventHandler) {
      this.mFullDiffSectionImpl.testEventHandler = testEventHandler;
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
    public Section<FullDiffSection> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      FullDiffSectionImpl fullDiffSectionImpl = mFullDiffSectionImpl;
      release();
      return fullDiffSectionImpl;
    }

    @Override
    protected void release() {
      super.release();
      mFullDiffSectionImpl = null;
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
      FullDiffSectionStateContainerImpl stateContainerImpl = (FullDiffSectionStateContainerImpl) stateContainer;
      FullDiffSectionImpl newComponentStateUpdate = (FullDiffSectionImpl) newComponent;
      StateValue<Object> state1 = new StateValue<Object>();
      state1.set(stateContainerImpl.state1);
      FullDiffSectionSpec.updateState(state1, mParam);
      newComponentStateUpdate.mStateContainerImpl.state1 = state1.get();
    }
  }
}


