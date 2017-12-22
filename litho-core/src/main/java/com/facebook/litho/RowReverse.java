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

/** A {@link Component} that renders its children in a reversed row. */
public final class RowReverse extends Component {

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

  private RowReverse() {}

  @Override
  public String getSimpleName() {
    return "RowReverse";
  }

  @Override
  boolean isInternalComponent() {
    return true;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new RowReverse());
    return builder;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    return this;
  }

  @Override
  protected ActualComponentLayout resolve(ComponentContext c) {
    InternalNode node = c.newLayoutBuilder(0, 0).flexDirection(YogaFlexDirection.ROW_REVERSE);

    if (getCommonProps() != null) {
      getCommonProps().copyInto(c, node);
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

    if (children != null) {
      for (Component child : children) {
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
    RowReverse rowReverse = (RowReverse) other;
    if (this.getId() == rowReverse.getId()) {
      return true;
    }
    if (children != null ? !children.equals(rowReverse.children) : rowReverse.children != null) {
      return false;
    }
    if (alignItems != null
        ? !alignItems.equals(rowReverse.alignItems)
        : rowReverse.alignItems != null) {
      return false;
    }
    if (alignContent != null
        ? !alignContent.equals(rowReverse.alignContent)
        : rowReverse.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(rowReverse.justifyContent)
        : rowReverse.justifyContent != null) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.ContainerBuilder<Builder>
      implements ComponentLayout.ContainerBuilder {
    RowReverse mRowReverse;
    ComponentContext mContext;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, RowReverse rowReverse) {
      super.init(context, defStyleAttr, defStyleRes, rowReverse);
      mRowReverse = rowReverse;
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

      if (this.mRowReverse.children == null) {
        this.mRowReverse.children = new ArrayList<>();
      }

      this.mRowReverse.children.add(child);
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
      this.mRowReverse.alignContent = alignContent;
      return this;
    }

    @Override
    public Builder alignItems(YogaAlign alignItems) {
      this.mRowReverse.alignItems = alignItems;
      return this;
    }

    @Override
    public Builder justifyContent(YogaJustify justifyContent) {
      this.mRowReverse.justifyContent = justifyContent;
      return this;
    }

    @Override
    public Builder wrap(YogaWrap wrap) {
      this.mRowReverse.wrap = wrap;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public RowReverse build() {
      RowReverse rowReverse = mRowReverse;
      release();
      return rowReverse;
    }

    @Override
    protected void release() {
      super.release();
      mRowReverse = null;
      mContext = null;
      sBuilderPool.release(this);
    }
  }
}
