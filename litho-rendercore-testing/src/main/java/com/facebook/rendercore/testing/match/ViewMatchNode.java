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

package com.facebook.rendercore.testing.match;

import static com.facebook.rendercore.testing.ViewAssertions.MatchAssertionBuilder.viewToString;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.rendercore.RootHost;
import java.util.ArrayList;
import org.assertj.core.api.Java6Assertions;

/**
 * This class can be used to make assertions about the structure and configuration of a View *
 * hierarchy. A ViewMatchNode is used to assert things about a given View, and its child *
 * ViewMatchNodes can be used to match against children of the that View.
 *
 * <p>In addition to generic {@link #prop} matchers from the MatchNode parent class, you can match
 * against layout and children.
 *
 * <pre>
 *   Example:
 *
 *   ViewAssertions.assertThat(rootView)
 *      .matches(
 *          ViewMatchNode.forType(ViewGroup.class)
 *              .bounds(0, 0, 500, 122)
 *              .child(
 *                  ViewMatchNode.forType(ViewGroup.class)
 *                      .padding(8, 8, 8, 8)
 *                      .prop(
 *                          "background",
 *                          MatchNode.forType(ColorDrawable.class).prop("color", Color.WHITE))
 *                      .child(
 *                          ViewMatchNode.forType(TextView.class)
 *                              .prop("text", "My title"))
 *                      .child(
 *                          ViewMatchNode.forType(TextView.class)
 *                              .prop("text", "My subtitle"))));
 * </pre>
 */
public class ViewMatchNode extends MatchNode {

  public static ViewMatchNode forType(Class type) {
    return new ViewMatchNode(type);
  }

  private final ArrayList<ViewMatchNode> mChildren = new ArrayList<>();
  private Rect mBounds;
  private Rect mPadding;
  private Rect mAbsoluteBounds;

  private ViewMatchNode(Class type) {
    super(type);
  }

  public ViewMatchNode bounds(int x, int y, int width, int height) {
    mBounds = new Rect(x, y, width, height);
    return this;
  }

  public ViewMatchNode padding(int left, int top, int right, int bottom) {
    mPadding = new Rect(left, top, right, bottom);
    return this;
  }

  public ViewMatchNode absoluteBounds(int x, int y, int width, int height) {
    mAbsoluteBounds = new Rect(x, y, width, height);
    return this;
  }

  @Override
  public <T> ViewMatchNode prop(String name, T value) {
    super.prop(name, value);
    return this;
  }

  public ViewMatchNode child(ViewMatchNode node) {
    mChildren.add(node);
    return this;
  }

  @Override
  public void assertMatchesImpl(Object o, DebugTraceContext debugContext) {
    View view = (View) o;

    if (mBounds != null) {
      Java6Assertions.assertThat(
              new Rect(view.getLeft(), view.getTop(), view.getWidth(), view.getHeight()))
          .describedAs(getDescription("Bounds on " + viewToString(view)))
          .isEqualTo(mBounds);
    }

    if (mAbsoluteBounds != null) {
      final Rect bounds = getAbsoluteBounds(view);
      Java6Assertions.assertThat(bounds)
          .describedAs(getDescription("Absolute bounds on " + viewToString(view)))
          .isEqualTo(mAbsoluteBounds);
    }

    if (mPadding != null) {
      Java6Assertions.assertThat(
              new Rect(
                  view.getPaddingLeft(),
                  view.getPaddingTop(),
                  view.getPaddingRight(),
                  view.getPaddingBottom()))
          .describedAs(getDescription("Padding on " + viewToString(view)))
          .isEqualTo(mPadding);
    }

    if (!(view instanceof ViewGroup)) {
      Java6Assertions.assertThat(mChildren)
          .describedAs(getDescription("Child count on " + viewToString(view)))
          .isEmpty();
    } else {
      final ViewGroup viewGroup = (ViewGroup) view;
      Java6Assertions.assertThat(viewGroup.getChildCount())
          .describedAs(getDescription("Child count on " + viewToString(view)))
          .isEqualTo(mChildren.size());
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        mChildren.get(i).assertMatches(viewGroup.getChildAt(i), debugContext);
      }
    }
  }

  public static Rect getAbsoluteBounds(View target) {
    View parent = (View) target.getParent();
    int x = target.getLeft();
    int y = target.getTop();
    while (parent != null && !(parent instanceof RootHost)) {
      x += parent.getLeft();
      y += parent.getTop();
      parent = (View) parent.getParent();
    }
    return new Rect(x, y, target.getWidth(), target.getHeight());
  }
}
