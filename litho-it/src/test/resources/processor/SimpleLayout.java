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

package com.facebook.litho.processor.integration.resources;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;

/** @see com.facebook.litho.processor.integration.resources.SimpleLayoutSpec */
public final class SimpleLayout extends Component {
  private SimpleLayout() {
    super("SimpleLayout");
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (ComponentsConfiguration.useNewIsEquivalentToInLayoutSpec) {
      return super.isEquivalentTo(other);
    }
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    SimpleLayout simpleLayoutRef = (SimpleLayout) other;
    if (this.getId() == simpleLayoutRef.getId()) {
      return true;
    }
    return true;
  }

  @Override
  protected Component onCreateLayout(ComponentContext context) {
    Component _result = (Component) SimpleLayoutSpec.onCreateLayout((ComponentContext) context);
    return _result;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    final Builder builder = new Builder();
    SimpleLayout instance = new SimpleLayout();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  public static class Builder extends Component.Builder<Builder> {
    SimpleLayout mSimpleLayout;

    ComponentContext mContext;

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        SimpleLayout simpleLayoutRef) {
      super.init(context, defStyleAttr, defStyleRes, simpleLayoutRef);
      mSimpleLayout = simpleLayoutRef;
      mContext = context;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public SimpleLayout build() {
      SimpleLayout simpleLayoutRef = mSimpleLayout;
      release();
      return simpleLayoutRef;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleLayout = null;
      mContext = null;
    }
  }
}
