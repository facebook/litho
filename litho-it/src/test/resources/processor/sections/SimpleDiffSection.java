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

public final class SimpleDiffSection extends Section {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private SimpleDiffSection() {
    super();
  }

  @Override
  public String getSimpleName() {
    return "SimpleDiffSection";
  }

  @Override
  public boolean isEquivalentTo(Section other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    SimpleDiffSection simpleDiffSectionRef = (SimpleDiffSection) other;
    return true;
  }

  public static Builder create(SectionContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    SimpleDiffSection instance = new SimpleDiffSection();
    builder.init(context, instance);
    return builder;
  }

  @Override
  protected void generateChangeSet(
      SectionContext c, ChangeSet changeSet, Section _prevAbstractImpl, Section _nextAbstractImpl) {
    SimpleDiffSection _prevImpl = (SimpleDiffSection) _prevAbstractImpl;
    SimpleDiffSection _nextImpl = (SimpleDiffSection) _nextAbstractImpl;
    SimpleDiffSectionSpec.onCreateChangeset((SectionContext) c, (ChangeSet) changeSet);
  }

  @Override
  protected boolean isDiffSectionSpec() {
    return true;
  }

  public static class Builder extends Section.Builder<Builder> {
    SimpleDiffSection mSimpleDiffSection;

    SectionContext mContext;

    private void init(SectionContext context, SimpleDiffSection simpleDiffSectionRef) {
      super.init(context, simpleDiffSectionRef);
      mSimpleDiffSection = simpleDiffSectionRef;
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
    public SimpleDiffSection build() {
      SimpleDiffSection simpleDiffSectionRef = mSimpleDiffSection;
      release();
      return simpleDiffSectionRef;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleDiffSection = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
