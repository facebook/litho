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
import com.facebook.litho.EventHandler;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;

public final class SimpleGroupSection extends SectionLifecycle {
  private static SimpleGroupSection sInstance = null;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private SimpleGroupSection() {
  }

  private static synchronized SimpleGroupSection get() {
    if (sInstance == null) {
      sInstance = new SimpleGroupSection();
    }
    return sInstance;
  }

  public static Builder create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new SimpleGroupSectionImpl());
    return builder;
  }

  @Override
  protected Children createChildren(SectionContext c, Section _abstractImpl) {
    SimpleGroupSectionImpl _impl = (SimpleGroupSectionImpl) _abstractImpl;
    Children _result = (Children) SimpleGroupSectionSpec.onCreateChildren((SectionContext) c);
    return _result;
  }

  static class SimpleGroupSectionImpl extends Section<SimpleGroupSection> implements Cloneable {

    private SimpleGroupSectionImpl() {
      super(get());
    }

    @Override
    public String getSimpleName() {
      return "SimpleGroupSection";
    }

    @Override
    public boolean isEquivalentTo(Section<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      SimpleGroupSectionImpl simpleGroupSectionImpl = (SimpleGroupSectionImpl) other;
      return true;
    }
  }

  public static class Builder extends Section.Builder<SimpleGroupSection, Builder> {
    SimpleGroupSectionImpl mSimpleGroupSectionImpl;

    SectionContext mContext;

    private void init(SectionContext context, SimpleGroupSectionImpl simpleGroupSectionImpl) {
      super.init(context, simpleGroupSectionImpl);
      mSimpleGroupSectionImpl = simpleGroupSectionImpl;
      mContext = context;
    }

    @Override
    public Builder key(String key) {
      return super.key(key);
    }

    @Override
    public Builder loadingEventHandler(EventHandler<LoadingEvent> loadingEventHandler) {
      return super.loadingEventHandler(loadingEventHandler);
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Section<SimpleGroupSection> build() {
      SimpleGroupSectionImpl simpleGroupSectionImpl = mSimpleGroupSectionImpl;
      release();
      return simpleGroupSectionImpl;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleGroupSectionImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
