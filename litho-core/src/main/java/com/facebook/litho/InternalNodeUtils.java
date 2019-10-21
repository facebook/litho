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

import android.content.res.TypedArray;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;

public class InternalNodeUtils {

  static InternalNode create(ComponentContext context) {
    NodeConfig.InternalNodeFactory factory = NodeConfig.sInternalNodeFactory;
    if (factory != null) {
      return factory.create(context);
    } else {
      return new DefaultInternalNode(context);
    }
  }

  static InternalNode create(
      final ComponentContext c, final @AttrRes int defStyleAttr, final @StyleRes int defStyleRes) {
    final InternalNode node = InternalNodeUtils.create(c);
    applyStyles(node, defStyleAttr, defStyleRes);
    return node;
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  static boolean hasValidLayoutDirectionInNestedTree(InternalNode holder, InternalNode nestedTree) {
    return nestedTree.isLayoutDirectionInherit()
        || (nestedTree.getResolvedLayoutDirection() == holder.getResolvedLayoutDirection());
  }

  static void applyStyles(InternalNode node, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (defStyleAttr != 0 || defStyleRes != 0) {
      ComponentContext c = node.getContext();

      // TODO: (T55170222) Pass the styles through the InternalNode instead of mutating the context.
      c.setDefStyle(defStyleAttr, defStyleRes);

      final TypedArray typedArray =
          c.getAndroidContext()
              .obtainStyledAttributes(null, R.styleable.ComponentLayout, defStyleAttr, defStyleRes);
      node.applyAttributes(typedArray);
      typedArray.recycle();

      // TODO: (T55170222) Not required if styles are passed through the InternalNode.
      c.setDefStyle(0, 0);
    }
  }
}
