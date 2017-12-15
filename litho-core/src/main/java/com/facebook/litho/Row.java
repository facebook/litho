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

/** A {@link Component} that renders its children in a row. */
public final class Row extends Component {

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

  private Row() {}

  @Override
  public String getSimpleName() {
    return "Row";
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new Row());
    return builder;
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext c, Component component) {
    return component;
  }

  @Override
  protected ActualComponentLayout resolve(ComponentContext c, Component component) {
    InternalNode node = c.newLayoutBuilder(0, 0).flexDirection(YogaFlexDirection.ROW);

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

    if (((Row) component).children != null) {
      for (Component child : ((Row) component).children) {
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
    Row row = (Row) other;
    if (this.getId() == row.getId()) {
      return true;
    }
    if (children != null ? !children.equals(row.children) : row.children != null) {
      return false;
    }
    if (alignItems != null ? !alignItems.equals(row.alignItems) : row.alignItems != null) {
      return false;
    }
    if (alignContent != null ? !alignContent.equals(row.alignContent) : row.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(row.justifyContent)
        : row.justifyContent != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.ContainerBuilder<Builder>
      implements ComponentLayout.ContainerBuilder {
    Row mRow;
    ComponentContext mContext;

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes, Row row) {
      super.init(context, defStyleAttr, defStyleRes, row);
      mRow = row;
      mContext = context;
    }

    @Override
    public Builder child(ComponentLayout child) {
      if (child == null) {
        return this;
      }

      return child((Component) child);
    }

    @Override
    public Builder child(ComponentLayout.Builder child) {
      if (child == null) {
        return this;
      }

      return child(child.build());
    }

    @Override
    public Builder child(Component child) {
      if (child == null) {
        return this;
      }

      if (this.mRow.children == null) {
        this.mRow.children = new ArrayList<>();
      }

      this.mRow.children.add(child);
      return this;
    }

    @Override
    public Builder child(Component.Builder<?> child) {
      if (child == null) {
        return this;
      }
      return child(child.build());
    }

    @Override
    public Builder alignContent(YogaAlign alignContent) {
      this.mRow.alignContent = alignContent;
      return this;
    }

    @Override
    public Builder alignItems(YogaAlign alignItems) {
      this.mRow.alignItems = alignItems;
      return this;
    }

    @Override
    public Builder justifyContent(YogaJustify justifyContent) {
      this.mRow.justifyContent = justifyContent;
      return this;
    }

    @Override
    public Builder wrap(YogaWrap wrap) {
      this.mRow.wrap = wrap;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Row build() {
      Row row = mRow;
      release();
      return row;
    }

    @Override
    protected void release() {
      super.release();
      mRow = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
