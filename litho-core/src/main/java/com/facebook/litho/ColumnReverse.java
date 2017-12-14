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
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** A {@link Component} that renders its children in a reversed column. */
public final class ColumnReverse extends Component {

  @Nullable
  @Prop(optional = true)
  List<Component> children;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignItems;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignContent;

  @Nullable
  @Prop(optional = true)
  private YogaJustify justifyContent;

  @Nullable
  @Prop(optional = true)
  private YogaWrap wrap;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private ColumnReverse() {}

  @Override
  public String getSimpleName() {
    return "ColumnReverse";
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new ColumnReverse());
    return builder;
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext c, Component component) {
    return component;
  }

  @Override
  protected ActualComponentLayout resolve(ComponentContext c, Component component) {
    InternalNode node = c.newLayoutBuilder(0, 0).flexDirection(YogaFlexDirection.COLUMN_REVERSE);

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

    if (wrap != null) {
      node.wrap(wrap);
    }

    if (((ColumnReverse) component).children != null) {
      for (Component child : ((ColumnReverse) component).children) {
        node.child(child);
      }
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
    ColumnReverse columnReverse = (ColumnReverse) other;
    if (this.getId() == columnReverse.getId()) {
      return true;
    }
    if (children != null
        ? !children.equals(columnReverse.children)
        : columnReverse.children != null) {
      return false;
    }
    if (alignItems != null
        ? !alignItems.equals(columnReverse.alignItems)
        : columnReverse.alignItems != null) {
      return false;
    }
    if (alignContent != null
        ? !alignContent.equals(columnReverse.alignContent)
        : columnReverse.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(columnReverse.justifyContent)
        : columnReverse.justifyContent != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.Builder<Builder>
      implements ComponentLayout.ContainerBuilder {
    ColumnReverse mColumnReverse;
    ComponentContext mContext;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, ColumnReverse columnReverse) {
      super.init(context, defStyleAttr, defStyleRes, columnReverse);
      mColumnReverse = columnReverse;
      mContext = context;
    }

    public Builder child(Component child) {
      if (child == null) {
        return this;
      }

      if (this.mColumnReverse.children == null) {
        this.mColumnReverse.children = new ArrayList<>();
      }

      this.mColumnReverse.children.add(child);
      return this;
    }

    public Builder child(Component.Builder<?> child) {
      if (child == null) {
        return this;
      }
      return child(child.build());
    }

    public Builder child(ComponentLayout child) {
      if (child == null) {
        return this;
      }

      return child((Component) child);
    }

    public Builder child(ComponentLayout.Builder child) {
      if (child == null) {
        return this;
      }

      return child(child.build());
    }

    public Builder alignContent(YogaAlign alignContent) {
      this.mColumnReverse.alignContent = alignContent;
      return this;
    }

    public Builder alignItems(YogaAlign alignItems) {
      this.mColumnReverse.alignItems = alignItems;
      return this;
    }

    public Builder justifyContent(YogaJustify justifyContent) {
      this.mColumnReverse.justifyContent = justifyContent;
      return this;
    }

    public Builder wrap(YogaWrap wrap) {
      this.mColumnReverse.wrap = wrap;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public ColumnReverse build() {
      ColumnReverse columnReverse = mColumnReverse;
      release();
      return columnReverse;
    }

    @Override
    protected void release() {
      super.release();
      mColumnReverse = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}

