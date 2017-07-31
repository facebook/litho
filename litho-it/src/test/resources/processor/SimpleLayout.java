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
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;

public final class SimpleLayout extends ComponentLifecycle {
  private static SimpleLayout sInstance = null;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool = new Pools.SynchronizedPool<Builder>(2);

  private SimpleLayout() {
  }

  public static synchronized SimpleLayout get() {
    if (sInstance == null) {
      sInstance = new SimpleLayout();
    }
    return sInstance;
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext context, Component _abstractImpl) {
    SimpleLayoutImpl _impl = (SimpleLayoutImpl) _abstractImpl;
    ComponentLayout _result = (ComponentLayout) SimpleLayoutSpec.onCreateLayout(
        (ComponentContext) context);
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
    builder.init(context, defStyleAttr, defStyleRes, new SimpleLayoutImpl());
    return builder;
  }

  private static class SimpleLayoutImpl extends Component<SimpleLayout> implements Cloneable {
    private SimpleLayoutImpl() {
      super(get());
    }

    @Override
    public String getSimpleName() {
      return "SimpleLayout";
    }

    @Override
    public boolean isEquivalentTo(Component<?> other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      SimpleLayoutImpl simpleLayoutImpl = (SimpleLayoutImpl) other;
      if (this.getId() == simpleLayoutImpl.getId()) {
        return true;
      }
      return true;
    }
  }

  public static class Builder extends Component.Builder<SimpleLayout, Builder> {
    SimpleLayoutImpl mSimpleLayoutImpl;

    ComponentContext mContext;

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        SimpleLayoutImpl simpleLayoutImpl) {
      super.init(context, defStyleAttr, defStyleRes, simpleLayoutImpl);
      mSimpleLayoutImpl = simpleLayoutImpl;
      mContext = context;
    }

    public Builder key(String key) {
      super.setKey(key);
      return this;
    }

    @Override
    public Component<SimpleLayout> build() {
      SimpleLayoutImpl simpleLayoutImpl = mSimpleLayoutImpl;
      release();
      return simpleLayoutImpl;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleLayoutImpl = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
