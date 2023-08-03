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

package com.facebook.rendercore

import com.facebook.yoga.YogaConfig
import com.facebook.yoga.YogaNode

class DefaultYogaNode(private val yogaProps: YogaProps) : DefaultNode() {

  override fun calculateLayout(
      context: LayoutContext<Any?>,
      widthSpec: Int,
      heightSpec: Int
  ): LayoutResult {
    return YogaLayoutFunction.calculateLayout(
        context,
        this,
        object : YogaLayoutDataProvider<Void?> {
          override fun getYogaConfig(): YogaConfig? = null

          override fun nodeCanMeasure(node: Node<*>?): Boolean = node !is DefaultYogaNode

          override fun getYogaChildren(node: Node<*>): List<Node<*>?> =
              (node as DefaultNode).children

          override fun getYogaRootLayoutParams(root: Node<*>?): YogaRootLayoutParams? = null

          override fun applyYogaPropsFromNode(
              node: Node<*>?,
              context: LayoutContext<Void?>?,
              yogaNode: YogaNode
          ) {
            if (node is DefaultYogaNode) {
              node.yogaProps.applyToNode(context, yogaNode)
            }
            if (node is DefaultNode && node.layoutParams != null) {
              node.layoutParams.applyToNode(context, yogaNode)
            }
          }

          override fun getRenderUnitForNode(
              node: Node<*>?,
              layoutContext: LayoutContext<Void?>?
          ): RenderUnit<*>? = (node as DefaultNode).getRenderUnit(layoutContext)
        },
        widthSpec,
        heightSpec)
  }
}
