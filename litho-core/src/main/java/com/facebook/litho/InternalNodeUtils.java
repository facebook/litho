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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class InternalNodeUtils {

  static InternalNode create(ComponentContext context) {
    NodeConfig.InternalNodeFactory factory = NodeConfig.sInternalNodeFactory;
    if (factory != null) {
      return factory.create(context);
    } else {
      return context.isInputOnlyInternalNodeEnabled()
          ? new InputOnlyInternalNode<>(context)
          : new DefaultInternalNode(context);
    }
  }

  static InternalNode.NestedTreeHolder createNestedTreeHolder(
      final ComponentContext context, final @Nullable TreeProps props) {
    NodeConfig.InternalNodeFactory factory = NodeConfig.sInternalNodeFactory;
    if (factory != null) {
      return factory.createNestedTreeHolder(context, props);
    } else {
      return context.isInputOnlyInternalNodeEnabled()
          ? new InputOnlyNestedTreeHolder(context, props)
          : new DefaultNestedTreeHolder(context, props);
    }
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  static boolean hasValidLayoutDirectionInNestedTree(
      NestedTreeHolderResult holder, LithoLayoutResult nestedTree) {
    return nestedTree.getInternalNode().isLayoutDirectionInherit()
        || (nestedTree.getResolvedLayoutDirection() == holder.getResolvedLayoutDirection());
  }
}
