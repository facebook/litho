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

package com.facebook.samples.litho.kotlin.animations.expandableelement

import ExpandableElementBottomDetail
import ExpandableElementTopDetail
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.onClick

const val TRANSITION_MSG_PARENT = "transition_msg_parent"
const val TRANSITION_TOP_DETAIL = "transition_top_detail"
const val TRANSITION_BOTTOM_DETAIL = "transition_bottom_detail"

class ExpandableElement(
    private val content: Component,
    private val timestamp: String,
    private val seen: Boolean = false
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    val expanded = useState { false }

    useTransition(
        Transition.parallel(
            Transition.allLayout(),
            Transition.create(TRANSITION_MSG_PARENT)
                .animate(AnimatedProperties.HEIGHT)
                .appearFrom(0f),
            Transition.create(TRANSITION_TOP_DETAIL, TRANSITION_BOTTOM_DETAIL)
                .animate(AnimatedProperties.HEIGHT)
                .appearFrom(0f)
                .disappearTo(0f)))

    return Column(
        style =
            Style.padding(top = 8.dp).transitionKey(context, TRANSITION_MSG_PARENT).onClick {
              expanded.update(!expanded.value)
            }) {
      if (expanded.value) {
        child(ExpandableElementTopDetail(timestamp))
      }
      child(
          Column {
            child(content)
            if (expanded.value) {
              child(ExpandableElementBottomDetail(seen))
            }
          })
    }
  }
}
