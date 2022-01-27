/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import androidx.annotation.Nullable;
import com.facebook.yoga.YogaNode;

/**
 * This is an output only {@link NestedTreeHolderResult}; this is created by a {@link
 * NestedTreeHolder}.
 */
public class NestedTreeHolderResult extends LithoLayoutResult {

  @Nullable LithoLayoutResult mNestedTree;

  public NestedTreeHolderResult(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final NestedTreeHolder internalNode,
      final YogaNode yogaNode,
      final LithoLayoutResult parent) {
    super(layoutStateContext, c, internalNode, yogaNode, parent);
  }

  @Override
  public NestedTreeHolder getNode() {
    return (NestedTreeHolder) super.getNode();
  }

  public @Nullable LithoLayoutResult getNestedResult() {
    return mNestedTree;
  }

  public void setNestedResult(@Nullable LithoLayoutResult tree) {
    mNestedTree = tree;
    if (tree != null) {
      tree.setParent(this);
    }
  }
}
