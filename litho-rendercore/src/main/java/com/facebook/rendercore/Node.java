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
import androidx.annotation.Px;
import java.util.Map;

public abstract class Node implements Copyable {

  public Node(LayoutFunction layoutFunction, Copyable props) {}

  public Node(LayoutFunction layoutFunction) {}

  /** Represent a function that can calculate a LayoutResult from a Node tree. */
  public interface LayoutFunction {
    LayoutResult calculateLayout(
        final Context context,
        final Node node,
        final int widthSpec,
        final int heightSpec,
        final LayoutCache layoutCache,
        final Map layoutContexts);
  }

  @Override
  public Node makeCopy() {
    return null;
  }

  public interface LayoutResult {

    Node getNode();

    int getChildrenCount();

    LayoutResult getChildAt(int index);

    @Px
    int getXForChildAtIndex(int index);

    @Px
    int getYForChildAtIndex(int index);

    @Px
    int getWidth();

    @Px
    int getHeight();

    @Px
    int getPaddingTop();

    @Px
    int getPaddingRight();

    @Px
    int getPaddingBottom();

    @Px
    int getPaddingLeft();
  }
}
