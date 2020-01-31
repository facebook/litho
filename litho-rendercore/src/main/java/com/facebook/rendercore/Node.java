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

package com.facebook.rendercore;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import java.util.Map;

/**
 * Represents a single node in a RenderCore Tree. A Node has children, base layout information, and
 * whether it needs to be rendered.
 */
public abstract class Node implements Copyable {

  private static final int DEFAULT_SIZE = 4;

  private final LayoutFunction mLayoutFunction;
  private Copyable mProps;
  private Copyable mLayoutParams;
  private RenderUnit mRenderUnit;
  private RenderUnit<? extends Host> mHostRenderUnit;

  public Node(LayoutFunction layoutFunction, Copyable props) {
    mLayoutFunction = layoutFunction;
    mProps = props;
  }

  public Node(LayoutFunction layoutFunction) {
    mLayoutFunction = layoutFunction;
    mProps = null;
  }

  /** @return a RenderUnit that represents the rendering content of this Node. */
  @Nullable
  public final RenderUnit getRenderUnit() {
    return mRenderUnit;
  }

  /** @return the Props associated with this node. */
  @Nullable
  public final Copyable getProps() {
    return mProps;
  }

  public void setProps(Copyable props) {
    mProps = props;
  }

  public final void setRenderUnit(RenderUnit content) {
    mRenderUnit = content;
  }

  /** Sets the View that will wrap this Node and its children RenderUnits. */
  public void setHostRenderUnit(RenderUnit<? extends Host> hostRenderUnit) {
    mHostRenderUnit = hostRenderUnit;
  }

  public RenderUnit<? extends Host> getHostRenderUnit() {
    return mHostRenderUnit;
  }

  /**
   * Calling calculateLayout on this Node will invoke its {@link LayoutFunction} and assign size and
   * positions to itself and its children. It's responsibility of the LayoutFunction to recursively
   * call calculateLayout on the entire tree.
   *
   * @param widthSpec a measure spec for the width in the format of {@link View.MeasureSpec}
   * @param heightSpec a measure spec for the height in the format of {@link View.MeasureSpec}
   */
  public final LayoutResult calculateLayout(
      Context context, int widthSpec, int heightSpec, LayoutCache layoutCache, Map layoutContexts) {
    return mLayoutFunction.calculateLayout(
        context, this, widthSpec, heightSpec, layoutCache, layoutContexts);
  }

  public Copyable getLayoutParams() {
    return mLayoutParams;
  }

  public void setLayoutParams(Copyable layoutParams) {
    mLayoutParams = layoutParams;
  }

  /**
   * Interface to be implemented by a Node with children. This interface can be used by layout
   * functions that need access to the Node's children.
   */
  public interface Container<T extends Node> {
    /** @return the number of children this Node has */
    int getChildrenCount();

    /** @return the child of this node at position index */
    T getChildAt(int index);
  }

  /** Represent a function that can calculate a LayoutResult from a Node tree. */
  public interface LayoutFunction {
    LayoutResult calculateLayout(
        Context context,
        Node node,
        int widthSpec,
        int heightSpec,
        LayoutCache layoutCache,
        Map layoutContexts);
  }

  /**
   * Represents the result of a Layout pass. A LayoutResult has a reference to its originating Node
   * and all the layout information needed to position the content of such Node.
   */
  public interface LayoutResult {

    /** @return the Node which layout this LayoutResult represents. */
    Node getNode();

    /** @return the number of children of this LayoutResult. */
    int getChildrenCount();

    /** @return the LayoutResult for the given child index */
    LayoutResult getChildAt(int index);

    /** @return the resolved X position for the Node */
    @Px
    int getXForChildAtIndex(int index);

    /** @return the resolved Y position for the Node */
    @Px
    int getYForChildAtIndex(int index);

    /** @return the resolved width for the Node */
    @Px
    int getWidth();

    /** @return the resolved height for the Node */
    @Px
    int getHeight();

    /** @return the resolved top padding for the Node */
    @Px
    int getPaddingTop();

    /** @return the resolved right padding for the Node */
    @Px
    int getPaddingRight();

    /** @return the resolved bottom padding for the Node */
    @Px
    int getPaddingBottom();

    /** @return the resolved left padding for the Node */
    @Px
    int getPaddingLeft();
  }

  @Override
  public Node makeCopy() {
    Node clone = null;
    try {
      clone = (Node) super.clone();
      if (mRenderUnit != null) {
        clone.mRenderUnit = mRenderUnit.makeCopy();
      }
      if (mHostRenderUnit != null) {
        clone.mHostRenderUnit = mHostRenderUnit.makeCopy();
      }

      if (mProps != null) {
        clone.mProps = mProps.makeCopy();
      }

      if (mLayoutParams != null) {
        clone.mLayoutParams = mLayoutParams.makeCopy();
      }

      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
