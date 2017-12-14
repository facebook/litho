/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.Pools;
import com.facebook.litho.annotations.Prop;
import java.util.BitSet;
import javax.annotation.Nullable;

/**
 * Utility class for wrapping an existing {@link Component}. This is useful for adding further
 * {@link CommonProps} to an already created component.
 */
public final class Wrapper extends Component {

  @Nullable @Prop Component delegate;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private Wrapper() {}

  @Override
  public String getSimpleName() {
    return "Wrapper";
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new Wrapper());
    return builder;
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext c, Component component) {
    return component;
  }

  @Override
  protected ActualComponentLayout resolve(ComponentContext c, Component component) {
    if (delegate == null) {
      return ComponentContext.NULL_LAYOUT;
    }

    InternalNode node = (InternalNode) c.newLayoutBuilder(delegate, 0, 0);
    if (component.getCommonProps() != null) {
      component.getCommonProps().copyInto(c, node);
    }

    return node;
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Wrapper wrapper = (Wrapper) other;
    if (this.getId() == wrapper.getId()) {
      return true;
    }
    if (delegate != null ? !delegate.equals(wrapper.delegate) : wrapper.delegate != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.Builder<Builder> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"delegate"};
    private static final int REQUIRED_PROPS_COUNT = 1;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);
    private Wrapper mWrapper;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, Wrapper wrapper) {
      super.init(context, defStyleAttr, defStyleRes, wrapper);
      mWrapper = wrapper;
    }

    public Builder delegate(Component delegate) {
      mRequired.set(0);
      this.mWrapper.delegate = delegate;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Wrapper build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      Wrapper wrapper = mWrapper;
      release();
      return wrapper;
    }

    @Override
    protected void release() {
      super.release();
      mWrapper = null;
      sBuilderPool.release(this);
    }
  }
}
