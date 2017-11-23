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
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A {@link Column} that is actually a {@link Component}. This is just in an experimental phase and
 * MUST NOT BE USED!
 */
public final class ColumnExperimental extends Component<ColumnExperimental> {

  @Nullable
  @Prop(optional = true)
  List<Component<?>> children;

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

  private ColumnExperimental() {}

  @Override
  public String getSimpleName() {
    return "ColumnExperimental";
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new ColumnExperimental());
    return builder;
  }

  @Override
  protected boolean hasExperimentalOnCreateLayout() {
    return true;
  }

  protected ComponentLayout resolve(ComponentContext c, Component<?> component) {
    InternalNode node = c.newLayoutBuilder(0, 0).flexDirection(YogaFlexDirection.COLUMN);

    if (component.getCommonProps() != null) {
      component.getCommonProps().copyInto(c, node);
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

    if (((ColumnExperimental) component).children != null) {
      for (Component<?> child : ((ColumnExperimental) component).children) {
        node.child(child);
      }
    }

    return node;
  }

  @Override
  public boolean isEquivalentTo(Component<?> other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    ColumnExperimental columnImpl = (ColumnExperimental) other;
    if (this.getId() == columnImpl.getId()) {
      return true;
    }
    if (children != null ? !children.equals(columnImpl.children) : columnImpl.children != null) {
      return false;
    }
    if (alignItems != null
        ? !alignItems.equals(columnImpl.alignItems)
        : columnImpl.alignItems != null) {
      return false;
    }
    if (alignContent != null
        ? !alignContent.equals(columnImpl.alignContent)
        : columnImpl.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(columnImpl.justifyContent)
        : columnImpl.justifyContent != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.Builder<ColumnExperimental, Builder> {
    ColumnExperimental mColumn;
    ComponentContext mContext;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, ColumnExperimental column) {
      super.init(context, defStyleAttr, defStyleRes, column);
      mColumn = column;
      mContext = context;
    }

    public Builder child(Component<?> child) {
      if (child == null) {
        return this;
      }

      if (this.mColumn.children == null) {
        this.mColumn.children = new ArrayList<>();
      }

      this.mColumn.children.add(child);
      return this;
    }

    public Builder child(Component.Builder<?, ?> child) {
      if (child == null) {
        return this;
      }
      return child(child.build());
    }

    public Builder alignContent(YogaAlign alignContent) {
      this.mColumn.alignContent = alignContent;
      return this;
    }

    public Builder alignItems(YogaAlign alignItems) {
      this.mColumn.alignItems = alignItems;
      return this;
    }

    public Builder justifyContent(YogaJustify justifyContent) {
      this.mColumn.justifyContent = justifyContent;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component<ColumnExperimental> build() {
      ColumnExperimental column = mColumn;
      release();
      return column;
    }

    @Override
    protected void release() {
      super.release();
      mColumn = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
