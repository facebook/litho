/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

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
  private List<Component> children;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignContent;

  @Nullable
  @Prop(optional = true)
  private YogaAlign alignItems;

  @Nullable
  @Prop(optional = true)
  private YogaJustify justifyContent;

  @Nullable
  @Prop(optional = true)
  private YogaWrap wrap;

  @Prop(optional = true)
  private boolean reverse;

  private final @Nullable String mCustomSimpleName;

  Row(String customSimpleName) {
    mCustomSimpleName = customSimpleName;
  }

  Row(
      @Nullable YogaAlign alignContent,
      @Nullable YogaAlign alignItems,
      @Nullable YogaJustify justifyContent,
      @Nullable YogaWrap wrap,
      boolean reverse) {
    this(alignContent, alignItems, justifyContent, wrap, reverse, null);
  }

  Row(
      @Nullable YogaAlign alignContent,
      @Nullable YogaAlign alignItems,
      @Nullable YogaJustify justifyContent,
      @Nullable YogaWrap wrap,
      boolean reverse,
      @Nullable List<Component> children) {
    mCustomSimpleName = null;
    this.alignContent = alignContent;
    this.alignItems = alignItems;
    this.justifyContent = justifyContent;
    this.wrap = wrap;
    this.reverse = reverse;
    this.children = children;
  }

  @Override
  protected boolean canResolve() {
    return true;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, "Row");
  }

  public static Builder create(ComponentContext context, String simpleName) {
    return create(context, 0, 0, simpleName);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, "Row");
  }

  public static Builder create(
      ComponentContext context, int defStyleAttr, int defStyleRes, String simpleName) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, new Row(simpleName));
    return builder;
  }

  @Override
  protected InternalNode resolve(LayoutStateContext layoutContext, ComponentContext c) {
    InternalNode node =
        InternalNodeUtils.create(c)
            .flexDirection(reverse ? YogaFlexDirection.ROW_REVERSE : YogaFlexDirection.ROW);

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
        if (layoutContext != null && layoutContext.isLayoutReleased()) {
          return ComponentContext.NULL_LAYOUT;
        }

        if (layoutContext != null && layoutContext.isLayoutInterrupted()) {
          node.appendUnresolvedComponent(child);
        } else {
          node.child(layoutContext, c, child);
        }
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
    if (children != null) {
      if (row.children == null || children.size() != row.children.size()) {
        return false;
      }
      for (int i = 0, size = children.size(); i < size; i++) {
        if (!children.get(i).isEquivalentTo(row.children.get(i))) {
          return false;
        }
      }
    } else if (row.children != null) {
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
    if (reverse != row.reverse) {
      return false;
    }
    return true;
  }

  @Override
  public String getSimpleName() {
    return mCustomSimpleName != null ? mCustomSimpleName : "Row";
  }

  public static class Builder extends Component.ContainerBuilder<Builder> {
    Row mRow;
    ComponentContext mContext;

    void init(ComponentContext context, int defStyleAttr, int defStyleRes, Row row) {
      super.init(context, defStyleAttr, defStyleRes, row);
      mRow = row;
      mContext = context;
    }

    @Override
    protected void setComponent(Component component) {
      mRow = (Row) component;
    }

    @Override
    public Builder child(@Nullable Component child) {
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
    public Builder child(@Nullable Component.Builder<?> child) {
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
    public Builder reverse(boolean reverse) {
      this.mRow.reverse = reverse;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Row build() {
      return mRow;
    }
  }
}
