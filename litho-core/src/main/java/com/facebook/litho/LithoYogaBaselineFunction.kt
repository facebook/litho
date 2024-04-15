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

package com.facebook.litho

import com.facebook.yoga.YogaBaselineFunction
import com.facebook.yoga.YogaNode

class LithoYogaBaselineFunction : YogaBaselineFunction {

  override fun baseline(cssNode: YogaNode, width: Float, height: Float): Float {
    val result: LithoLayoutResult = cssNode.data as LithoLayoutResult
    val node: LithoNode = result.node
    check(node.tailComponent is SpecGeneratedComponent) {
      ("Trying to call onMeasureBaseline on a non-Spec component: ${node.tailComponent.simpleName}")
    }
    val component = node.tailComponent as SpecGeneratedComponent
    val interStageProps = result.layoutData as InterStagePropsContainer?
    return component
        .onMeasureBaseline(result.context, width.toInt(), height.toInt(), interStageProps)
        .toFloat()
  }
}
