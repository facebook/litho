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

import com.facebook.litho.EventHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;

/** @see com.facebook.litho.sections.processor.integration.resources.SimpleDiffSectionSpec */
public final class SimpleDiffSection extends Section {
  private SimpleDiffSection() {
    super("SimpleDiffSection");
  }

  @Override
  public boolean isEquivalentTo(Section other) {
    if (ComponentsConfiguration.useNewIsEquivalentToInSectionSpec) {
      return super.isEquivalentTo(other);
    }
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
    final Builder builder = new Builder();
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
    }
  }
}
