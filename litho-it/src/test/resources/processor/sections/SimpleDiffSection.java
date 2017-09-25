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
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;

public final class SimpleDiffSection extends SectionLifecycle {
  private static SimpleDiffSection sInstance = null;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private SimpleDiffSection() {
  }

  private static synchronized SimpleDiffSection get() {
    if (sInstance == null) {
      sInstance = new SimpleDiffSection();
    }
    return sInstance;
  }

  public static Builder create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new SimpleDiffSectionImpl());
    return builder;
  }

  @Override
  protected void generateChangeSet(
      SectionContext c, ChangeSet changeSet, Section _prevAbstractImpl, Section _nextAbstractImpl) {
    SimpleDiffSectionImpl _prevImpl = (SimpleDiffSectionImpl) _prevAbstractImpl;
    SimpleDiffSectionImpl _nextImpl = (SimpleDiffSectionImpl) _nextAbstractImpl;
    SimpleDiffSectionSpec.onCreateChangeset((SectionContext) c, (ChangeSet) changeSet);
  }

  @Override
  protected boolean isDiffSectionSpec() {
    return true;
  }

  static class SimpleDiffSectionImpl extends Section<SimpleDiffSection> implements Cloneable {
    private SimpleDiffSectionImpl() {
      super(get());
    }

    @Override
    public String getSimpleName() {
      return "SimpleDiffSection";
    }

    @Override
    public boolean isEquivalentTo(Section<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      SimpleDiffSectionImpl simpleDiffSectionImpl = (SimpleDiffSectionImpl) other;
      return true;
    }
  }

  public static class Builder extends Section.Builder<SimpleDiffSection, Builder> {
    SimpleDiffSectionImpl mSimpleDiffSectionImpl;

    SectionContext mContext;

    private void init(SectionContext context, SimpleDiffSectionImpl simpleDiffSectionImpl) {
      super.init(context, simpleDiffSectionImpl);
      mSimpleDiffSectionImpl = simpleDiffSectionImpl;
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
    public Section<SimpleDiffSection> build() {
      SimpleDiffSectionImpl simpleDiffSectionImpl = mSimpleDiffSectionImpl;
      release();
      return simpleDiffSectionImpl;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleDiffSectionImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
