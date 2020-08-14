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

/** A {@link Component} that renders its children in a column. */
public final class Column extends Component {

  @Nullable
  @Prop(optional = true)
  private List<Component> children;

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

  @Prop(optional = true)
  private boolean reverse;

  Column(String simpleName) {
    super(simpleName);
  }

  @Override
  protected boolean canResolve() {
    return true;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, "Column");
  }

  public static Builder create(ComponentContext context, String simpleName) {
    return create(context, 0, 0, simpleName);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, "Column");
  }

  public static Builder create(
      ComponentContext context, int defStyleAttr, int defStyleRes, String simpleName) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, new Column(simpleName));
    return builder;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    return this;
  }

  @Override
  protected ComponentLayout resolve(ComponentContext c) {
    InternalNode node =
        InternalNodeUtils.create(c)
            .flexDirection(reverse ? YogaFlexDirection.COLUMN_REVERSE : YogaFlexDirection.COLUMN);

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
        if (c.wasLayoutCanceled()) {
          return ComponentContext.NULL_LAYOUT;
        }

        if (c.wasLayoutInterrupted()) {
          node.appendUnresolvedComponent(child);
        } else {
          node.child(child);
        }
      }
    }

    return node;
  }

  @Override
  public boolean isEquivalentTo(Component other, boolean shouldCompareState) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Column column = (Column) other;
    if (this.getId() == column.getId()) {
      return true;
    }
    if (children != null) {
      if (column.children == null || children.size() != column.children.size()) {
        return false;
      }
      for (int i = 0, size = children.size(); i < size; i++) {
        if (!children.get(i).isEquivalentTo(column.children.get(i), shouldCompareState)) {
          return false;
        }
      }
    } else if (column.children != null) {
      return false;
    }
    if (alignItems != null ? !alignItems.equals(column.alignItems) : column.alignItems != null) {
      return false;
    }
    if (alignContent != null
        ? !alignContent.equals(column.alignContent)
        : column.alignContent != null) {
      return false;
    }
    if (justifyContent != null
        ? !justifyContent.equals(column.justifyContent)
        : column.justifyContent != null) {
      return false;
    }
    if (reverse != column.reverse) {
      return false;
    }
    return true;
  }

  public static class Builder extends Component.ContainerBuilder<Builder> {
    Column mColumn;
    ComponentContext mContext;

    void init(ComponentContext context, int defStyleAttr, int defStyleRes, Column column) {
      super.init(context, defStyleAttr, defStyleRes, column);
      mColumn = column;
      mContext = context;
    }

    @Override
    protected void setComponent(Component component) {
      mColumn = (Column) component;
    }

    @Override
    public Builder child(@Nullable Component child) {
      if (child == null) {
        return this;
      }

      if (this.mColumn.children == null) {
        this.mColumn.children = new ArrayList<>();
      }

      this.mColumn.children.add(child);
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
      this.mColumn.alignContent = alignContent;
      return this;
    }

    @Override
    public Builder alignItems(YogaAlign alignItems) {
      this.mColumn.alignItems = alignItems;
      return this;
    }

    @Override
    public Builder justifyContent(YogaJustify justifyContent) {
      this.mColumn.justifyContent = justifyContent;
      return this;
    }

    @Override
    public Builder wrap(YogaWrap wrap) {
      this.mColumn.wrap = wrap;
      return this;
    }

    @Override
    public Builder reverse(boolean reverse) {
      this.mColumn.reverse = reverse;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Column build() {
      return mColumn;
    }
  }
}
