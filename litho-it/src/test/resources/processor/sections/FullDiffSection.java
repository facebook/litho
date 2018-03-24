/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.integration.resources;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
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
 * @prop-required data java.util.List<T>
 * @prop-required prop1 java.lang.Integer
 * @prop-optional prop2 java.lang.String
 * @prop-required prop3 com.facebook.litho.Component
 * @see com.facebook.litho.sections.processor.integration.resources.FullDiffSectionSpec
 */
public final class FullDiffSection<T> extends Section implements TestTag {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  static final Pools.SynchronizedPool<TestEvent> sTestEventPool =
      new Pools.SynchronizedPool<TestEvent>(2);

  private FullDiffSectionStateContainer mStateContainer;

  @Prop(resType = ResType.NONE, optional = false)
  List<T> data;

  @Prop(resType = ResType.NONE, optional = false)
  Integer prop1;

  @Prop(resType = ResType.NONE, optional = true)
  String prop2;

  @Prop(resType = ResType.NONE, optional = false)
  Component prop3;

  String _service;

  EventHandler testEventHandler;

  private FullDiffSection() {
    super("FullDiffSection");
    mStateContainer = new FullDiffSectionStateContainer();
  }

  @Override
  protected SectionLifecycle.StateContainer getStateContainer() {
    return mStateContainer;
  }

  @Override
  public boolean isEquivalentTo(Section other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    FullDiffSection fullDiffSectionRef = (FullDiffSection) other;
    if (data != null ? !data.equals(fullDiffSectionRef.data) : fullDiffSectionRef.data != null) {
      return false;
    }
    if (prop1 != null
        ? !prop1.equals(fullDiffSectionRef.prop1)
        : fullDiffSectionRef.prop1 != null) {
      return false;
    }
    if (prop2 != null
        ? !prop2.equals(fullDiffSectionRef.prop2)
        : fullDiffSectionRef.prop2 != null) {
      return false;
    }
    if (prop3 != null
        ? !prop3.isEquivalentTo(fullDiffSectionRef.prop3)
        : fullDiffSectionRef.prop3 != null) {
      return false;
    }
    if (mStateContainer.state1 != null
        ? !mStateContainer.state1.equals(fullDiffSectionRef.mStateContainer.state1)
        : fullDiffSectionRef.mStateContainer.state1 != null) {
      return false;
    }
    return true;
  }

  private UpdateStateStateUpdate createUpdateStateStateUpdate(Object param) {
    return new UpdateStateStateUpdate(param);
  }

  @Override
  public FullDiffSection makeShallowCopy(boolean deepCopy) {
    FullDiffSection component = (FullDiffSection) super.makeShallowCopy(deepCopy);
    component.prop3 = component.prop3 != null ? component.prop3.makeShallowCopy() : null;
    if (!deepCopy) {
      component.mStateContainer = new FullDiffSectionStateContainer();
    }
    return component;
  }

  public static <T> Builder<T> create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    FullDiffSection instance = new FullDiffSection();
    builder.init(context, instance);
    return builder;
  }

  @Override
  protected void transferState(
      SectionContext context, SectionLifecycle.StateContainer _prevStateContainer) {
    FullDiffSectionStateContainer prevStateContainer =
        (FullDiffSectionStateContainer) _prevStateContainer;
    mStateContainer.state1 = prevStateContainer.state1;
  }

  protected static void updateStateAsync(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullDiffSection.UpdateStateStateUpdate _stateUpdate =
        ((FullDiffSection) _component).createUpdateStateStateUpdate(param);
    c.updateStateAsync(_stateUpdate);
  }

  protected static void updateState(SectionContext c, Object param) {
    Section _component = c.getSectionScope();
    if (_component == null) {
      return;
    }
    FullDiffSection.UpdateStateStateUpdate _stateUpdate =
        ((FullDiffSection) _component).createUpdateStateStateUpdate(param);
    c.updateStateSync(_stateUpdate);
  }

  public static EventHandler getTestEventHandler(SectionContext context) {
    if (context.getSectionScope() == null) {
      return null;
    }
    return ((FullDiffSection) context.getSectionScope()).testEventHandler;
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

  private void testEvent(HasEventDispatcher _abstract, SectionContext c, View view, int someParam) {
    FullDiffSection _ref = (FullDiffSection) _abstract;
    FullDiffSectionSpec.testEvent(c, view, someParam);
  }

  public static EventHandler<ClickEvent> testEvent(SectionContext c, int someParam) {
    return newEventHandler(
        c,
        "testEvent",
        -1204074200,
        new Object[] {
          c, someParam,
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
              (View) _event.view,
              (int) eventHandler.params[1]);
          return null;
        }
      default:
        return null;
    }
  }

  @Override
  protected void createInitialState(SectionContext c) {
    StateValue<Object> state1 = new StateValue<>();
    FullDiffSectionSpec.onCreateInitialState((SectionContext) c, (Integer) prop1, state1);
    mStateContainer.state1 = state1.get();
  }

  @Override
  protected void generateChangeSet(
      SectionContext c, ChangeSet changeSet, Section _prevAbstractImpl, Section _nextAbstractImpl) {
    FullDiffSection _prevImpl = (FullDiffSection) _prevAbstractImpl;
    FullDiffSection _nextImpl = (FullDiffSection) _nextAbstractImpl;
    Diff<List<T>> data =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.data,
                _nextImpl == null ? null : _nextImpl.data);
    Diff<Component> prop3 =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.prop3,
                _nextImpl == null ? null : _nextImpl.prop3);
    Diff<Object> state1 =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.mStateContainer.state1,
                _nextImpl == null ? null : _nextImpl.mStateContainer.state1);
    FullDiffSectionSpec.onDiff((SectionContext) c, (ChangeSet) changeSet, data, prop3, state1);
    releaseDiff(data);
    releaseDiff(prop3);
    releaseDiff(state1);
  }

  @Override
  protected boolean isDiffSectionSpec() {
    return true;
  }

  private String onCreateService(SectionContext c) {
    String _result =
        (String) FullDiffSectionSpec.onCreateService((SectionContext) c, (String) prop2);
    return _result;
  }

  @Override
  public void createService(SectionContext context) {
    _service = onCreateService(context);
  }

  @Override
  protected void transferService(SectionContext c, Section previous, Section next) {
    FullDiffSection previousSection = (FullDiffSection) previous;
    FullDiffSection nextSection = (FullDiffSection) next;
    nextSection._service = previousSection._service;
  }

  @Override
  protected Object getService(Section section) {
    return ((FullDiffSection) section)._service;
  }

  @Override
  protected void bindService(SectionContext c) {
    FullDiffSectionSpec.bindService((SectionContext) c, _service);
  }

  @Override
  protected void unbindService(SectionContext c) {
    FullDiffSectionSpec.unbindService((SectionContext) c, _service);
  }

  @Override
  protected void refresh(SectionContext c) {
    FullDiffSectionSpec.onRefresh((SectionContext) c, _service);
  }

  @Override
  protected void dataBound(SectionContext c) {
    FullDiffSectionSpec.onDataBound((SectionContext) c);
  }

  @Override
  protected boolean shouldUpdate(Section _prevAbstractImpl, Section _nextAbstractImpl) {
    FullDiffSection _prevImpl = (FullDiffSection) _prevAbstractImpl;
    FullDiffSection _nextImpl = (FullDiffSection) _nextAbstractImpl;
    Diff<Integer> prop1 =
        (Diff)
            acquireDiff(
                _prevImpl == null ? null : _prevImpl.prop1,
                _nextImpl == null ? null : _nextImpl.prop1);
    boolean _result = (boolean) FullDiffSectionSpec.shouldUpdate(prop1);
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
      int lastFullyVisibleIndex) {
    FullDiffSectionSpec.onViewportChanged(
        (SectionContext) c,
        (int) firstVisibleIndex,
        (int) lastVisibleIndex,
        (int) totalCount,
        (int) firstFullyVisibleIndex,
        (int) lastFullyVisibleIndex);
  }

  @VisibleForTesting(otherwise = 2)
  static class FullDiffSectionStateContainer<T> implements SectionLifecycle.StateContainer {
    @State Object state1;
  }

  public static class Builder<T> extends Section.Builder<Builder<T>> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"data", "prop1", "prop3"};

    private static final int REQUIRED_PROPS_COUNT = 3;

    FullDiffSection mFullDiffSection;

    SectionContext mContext;

    private BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(SectionContext context, FullDiffSection fullDiffSectionRef) {
      super.init(context, fullDiffSectionRef);
      mFullDiffSection = fullDiffSectionRef;
      mContext = context;
      mRequired.clear();
    }

    public Builder<T> data(List<T> data) {
      this.mFullDiffSection.data = data;
      mRequired.set(0);
      return this;
    }

    public Builder<T> prop1(Integer prop1) {
      this.mFullDiffSection.prop1 = prop1;
      mRequired.set(1);
      return this;
    }

    public Builder<T> prop2(String prop2) {
      this.mFullDiffSection.prop2 = prop2;
      return this;
    }

    public Builder<T> prop3(Component prop3) {
      this.mFullDiffSection.prop3 = prop3;
      mRequired.set(2);
      return this;
    }

    public Builder<T> prop3(Component.Builder<?> prop3Builder) {
      this.mFullDiffSection.prop3 = prop3Builder.build();
      mRequired.set(2);
      return this;
    }

    public Builder<T> testEventHandler(EventHandler testEventHandler) {
      this.mFullDiffSection.testEventHandler = testEventHandler;
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
    public FullDiffSection build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      FullDiffSection fullDiffSectionRef = mFullDiffSection;
      release();
      return fullDiffSectionRef;
    }

    @Override
    protected void release() {
      super.release();
      mFullDiffSection = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }

  private static class UpdateStateStateUpdate implements SectionLifecycle.StateUpdate {
    private Object mParam;

    UpdateStateStateUpdate(Object param) {
      mParam = param;
    }

    @Override
    public void updateState(SectionLifecycle.StateContainer _stateContainer, Section newComponent) {
      FullDiffSectionStateContainer stateContainer =
          (FullDiffSectionStateContainer) _stateContainer;
      FullDiffSection newComponentStateUpdate = (FullDiffSection) newComponent;
      StateValue<Object> state1 = new StateValue<Object>();
      state1.set(stateContainer.state1);
      FullDiffSectionSpec.updateState(state1, mParam);
      newComponentStateUpdate.mStateContainer.state1 = state1.get();
    }
  }
}
