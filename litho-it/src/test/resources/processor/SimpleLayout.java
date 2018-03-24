/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.processor.integration.resources;

import android.support.v4.util.Pools;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

/** @see com.facebook.litho.processor.integration.resources.SimpleLayoutSpec */
public final class SimpleLayout extends Component {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private SimpleLayout() {
    super("SimpleLayout");
  }

  @Override
  public boolean isEquivalentTo(Component other) {
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
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
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
      sBuilderPool.release(this);
    }
  }
}
