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

package com.facebook.litho.sections.processor.integration.resources;

import android.support.v4.util.Pools;
import com.facebook.litho.EventHandler;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;

/** @see com.facebook.litho.sections.processor.integration.resources.SimpleGroupSectionSpec */
public final class SimpleGroupSection extends Section {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private SimpleGroupSection() {
    super("SimpleGroupSection");
  }

  @Override
  public boolean isEquivalentTo(Section other) {
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
  protected Children createChildren(SectionContext c) {
    Children _result = (Children) SimpleGroupSectionSpec.onCreateChildren((SectionContext) c);
    return _result;
  }

  public static class Builder extends Section.Builder<Builder> {
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
    public SimpleGroupSection build() {
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
