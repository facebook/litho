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

public final class SimpleGroupSection extends Section<SimpleGroupSection> {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private SimpleGroupSection() {
    super();
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
    SimpleGroupSection simpleGroupSectionRef = (SimpleGroupSection) other;
    return true;
  }

  public static Builder create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    SimpleGroupSection instance = new SimpleGroupSection();
    builder.init(context, instance);
    return builder;
  }

  @Override
  protected Children createChildren(SectionContext c, Section _abstract) {
    SimpleGroupSection _ref = (SimpleGroupSection) _abstract;
    Children _result = (Children) SimpleGroupSectionSpec.onCreateChildren((SectionContext) c);
    return _result;
  }

  public static class Builder extends Section.Builder<SimpleGroupSection, Builder> {
    SimpleGroupSection mSimpleGroupSection;

    SectionContext mContext;

    private void init(SectionContext context, SimpleGroupSection simpleGroupSectionRef) {
      super.init(context, simpleGroupSectionRef);
      mSimpleGroupSection = simpleGroupSectionRef;
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
      SimpleGroupSection simpleGroupSectionRef = mSimpleGroupSection;
      release();
      return simpleGroupSectionRef;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleGroupSection = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
