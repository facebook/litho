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

package com.facebook.samples.litho.kotlin.animations.transitions

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.TypedValue
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flexboxParams
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.margin
import com.facebook.litho.flexbox.width
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.yoga.YogaAlign
import java.util.Arrays

private const val TRANSITION_KEY_TEXT = "key"
private const val TRANSITION_KEY2_TEXT = "key2"

class TransitionsComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
                .animate(AnimatedProperties.X),
            Transition.create(TRANSITION_KEY_TEXT).animate(AnimatedProperties.ALPHA),
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY2_TEXT)
                .animate(AnimatedProperties.HEIGHT, AnimatedProperties.WIDTH)))

    val alphaValue = useState { 1f }
    val shouldExpand = useState { false }
    val toRight = useState { true }
    return Column(
        style =
            Style.width(200.dp).alpha(alphaValue.value).onClick {
              toRight.update(!toRight.value)
              alphaValue.update(if (alphaValue.value == 1f) 0.5f else 1f)
              shouldExpand.update(!shouldExpand.value)
            }) {
      child(
          flexboxParams(
              alignSelf = if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START) {
            Column(
                style =
                    Style.width(50.dp)
                        .height(50.dp)
                        .margin(all = 5.dp)
                        .transitionKey(TRANSITION_KEY_TEXT, Transition.TransitionKeyType.GLOBAL)
                        .background(buildRoundedRect(context, Color.parseColor("#666699"), 8)))
          })
      child(
          Column(
              style =
                  Style.width(if (shouldExpand.value) 75.dp else 50.dp)
                      .height(if (shouldExpand.value) 75.dp else 50.dp)
                      .margin(all = 5.dp)
                      .transitionKey(TRANSITION_KEY2_TEXT, Transition.TransitionKeyType.GLOBAL)
                      .background(buildRoundedRect(context, Color.parseColor("#ba7bb5"), 8))))
    }
  }

  private fun buildRoundedRect(c: ComponentContext, color: Int, cornerRadiusDp: Int): Drawable {
    val cornerRadiusPx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp.toFloat(), c.resources.displayMetrics)

    val radii = FloatArray(8)
    Arrays.fill(radii, cornerRadiusPx)
    val roundedRectShape = RoundRectShape(radii, null, radii)

    return ShapeDrawable(roundedRectShape).also { it.paint.color = color }
  }
}
