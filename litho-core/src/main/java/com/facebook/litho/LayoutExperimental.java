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
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaJustify;
import java.util.BitSet;
import javax.annotation.Nullable;

/**
 * A {@link Layout} that is actually a {@link Component}. This is just in an experimental phase and
 * MUST NOT BE USED!
 */
public final class LayoutExperimental extends Component<LayoutExperimental> {

  @Nullable @Prop Component<?> delegate;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignItems;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignContent;

  @Nullable
  @Prop(optional = true)
  private YogaJustify justifyContent;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);

  private LayoutExperimental() {}

  @Override
  public String getSimpleName() {
    return "LayoutExperimental";
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new LayoutExperimental());
    return builder;
  }

  protected ComponentLayout resolve(ComponentContext c, Component<?> component) {
    InternalNode node = (InternalNode) c.newLayoutBuilder(delegate, 0, 0);
    if (component.getLayoutAttributes() != null) {
      component.getLayoutAttributes().copyInto(c, node);
    }

    if (alignItems != null) {
      node.alignItems(alignItems);
    }

    if (alignContent != null) {
      node.alignContent(alignContent);
    }

    if (justifyContent != null) {
      node.justifyContent(justifyContent);
    }

    return node;
  }

  @Override
  protected boolean hasExperimentalOnCreateLayout() {
    return true;
  }

  @Override
  public boolean isEquivalentTo(Component<?> other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    LayoutExperimental layoutImpl = (LayoutExperimental) other;
    if (this.getId() == layoutImpl.getId()) {
      return true;
    }
    if (delegate != null ? !delegate.equals(layoutImpl.delegate) : layoutImpl.delegate != null) {
      return false;
    }
    if (alignItems != null
        ? !alignItems.equals(layoutImpl.alignItems)
        : layoutImpl.alignItems != null) {
      return false;
    }
    if (alignContent != null
        ? !alignContent.equals(layoutImpl.alignContent)
        : layoutImpl.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(layoutImpl.justifyContent)
        : layoutImpl.justifyContent != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.Builder<LayoutExperimental, Builder> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"delegate"};
    private static final int REQUIRED_PROPS_COUNT = 1;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);
    private LayoutExperimental mLayout;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, LayoutExperimental layout) {
      super.init(context, defStyleAttr, defStyleRes, layout);
      mLayout = layout;
    }

    public Builder delegate(Component<?> delegate) {
      mRequired.set(0);
      this.mLayout.delegate = delegate;
      return this;
    }

    public Builder alignContent(YogaAlign alignContent) {
      this.mLayout.alignContent = alignContent;
      return this;
    }

    public Builder alignItems(YogaAlign alignItems) {
      this.mLayout.alignItems = alignItems;
      return this;
    }

    public Builder justifyContent(YogaJustify justifyContent) {
      this.mLayout.justifyContent = justifyContent;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component<LayoutExperimental> build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      LayoutExperimental layout = mLayout;
      release();
      return layout;
    }

    @Override
    protected void release() {
      super.release();
      mLayout = null;
      sBuilderPool.release(this);
    }
  }
}
