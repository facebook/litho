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

package com.facebook.samples.litho.kotlin.animations.animatedapi

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.TypedValue
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.DynamicValue
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.animated.Animated
import com.facebook.litho.animated.Easing
import com.facebook.litho.animated.SpringConfig
import com.facebook.litho.animated.alpha
import com.facebook.litho.animated.scaleX
import com.facebook.litho.animated.scaleY
import com.facebook.litho.animated.translationY
import com.facebook.litho.animated.useBinding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.margin
import com.facebook.litho.flexbox.width
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.litho.view.wrapInView
import com.facebook.yoga.YogaAlign
import java.util.Arrays

class AnimatedComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val isScaleTransitionComplete = useState { false }
    val isYProgressCompleted = useState { false }
    val alpha = useBinding(0f)
    val alpha2 = useBinding(0f)
    val alpha3 = useBinding(0f)
    val alphaProgress = useBinding(1f)
    val animScale = useBinding(100f)
    val animScale2 = useBinding(110f)
    val scale = useBinding(animScale) { progress -> progress / 100 }
    val scale2 = useBinding(animScale2) { progress -> progress / 100 }
    val yProgress = useBinding(0f)
    val y2Progress = useBinding(150f)

    val animateOn =
        Animated.timing(
            target = y2Progress,
            to = 0f,
            duration = 2000,
            easing = Easing.bezier(0.85f, 0f, 0.15f, 1f))
    val animateOff =
        Animated.timing(
            target = yProgress,
            to = 150f,
            duration = 2000,
            easing = Easing.bezier(0.85f, 0f, 0.15f, 1f),
            onFinish = {
              isYProgressCompleted.update(true)
              alphaProgress.set(if (alphaProgress.get() == 1f) 0.5f else 1f)
            })
    val animDelay =
        Animated.timing(
            target = DynamicValue(0f),
            to = 1f,
            duration = 1000,
            onFinish = { isYProgressCompleted.update(false) })

    val staggerTopAnimation =
        Animated.stagger(
            200,
            Animated.timing(alpha, to = 1f),
            Animated.timing(alpha2, to = 1f),
            Animated.timing(alpha3, to = 1f))

    val blueBlockAnimation = Animated.loop(Animated.sequence(animateOff, animateOn, animDelay))

    val pulseAnimation =
        Animated.loop(
            Animated.delay(
                500,
                Animated.sequence(
                    Animated.spring(
                        animScale,
                        to = 110f,
                        SpringConfig(stiffness = 50f, dampingRatio = 0.5f),
                        onFinish = {
                          isScaleTransitionComplete.update(!isScaleTransitionComplete.value)
                        }),
                    Animated.spring(
                        animScale,
                        to = 100f,
                        SpringConfig(stiffness = 50f, dampingRatio = 0.5f),
                        onFinish = {
                          isScaleTransitionComplete.update(!isScaleTransitionComplete.value)
                        }))))

    return Column(
        children =
            listOf(
                Row(
                    style =
                        Style.onClick {
                          blueBlockAnimation.start()
                          staggerTopAnimation.start()
                          pulseAnimation.start()
                        },
                    children =
                        listOf(
                            Column(
                                style =
                                    Style.width(100.dp)
                                        .height(150.dp)
                                        .margin(all = 5.dp)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#99b3ff"), 8))),
                            Column(
                                style =
                                    Style.width(100.dp)
                                        .height(150.dp)
                                        .margin(all = 5.dp)
                                        .alpha(alpha)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#ffd480"), 8))),
                            Column(
                                style =
                                    Style.width(100.dp)
                                        .height(150.dp)
                                        .margin(all = 5.dp)
                                        .alpha(alpha2)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#e699cc"), 8))),
                            Column(
                                style =
                                    Style.width(100.dp)
                                        .height(150.dp)
                                        .margin(all = 5.dp)
                                        .alpha(alpha3)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#9fdfbf"), 8))),
                        )),
                Row(
                    children =
                        listOf(
                            Row(
                                style =
                                    Style.margin(30.dp)
                                        .width(90.dp)
                                        .height(20.dp)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#999999"), 12)))),
                    style =
                        Style.width(150.dp)
                            .height(70.dp)
                            .margin(all = 50.dp)
                            .alignSelf(YogaAlign.CENTER)
                            .scaleX(
                                if (isScaleTransitionComplete.value) {
                                  scale
                                } else {
                                  scale2
                                })
                            .scaleY(
                                if (isScaleTransitionComplete.value) {
                                  scale
                                } else {
                                  scale2
                                })
                            .background(buildRoundedRect(context, Color.parseColor("#d9d9d9"), 8))),
                Row(
                    style = Style.alignSelf(YogaAlign.CENTER).wrapInView(),
                    children =
                        listOf(
                            Column(
                                style =
                                    Style.width(50.dp)
                                        .height(50.dp)
                                        .margin(all = 5.dp)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#666699"), 8))),
                            Column(
                                style =
                                    Style.width(50.dp)
                                        .height(50.dp)
                                        .margin(all = 5.dp)
                                        .translationY(
                                            if (isYProgressCompleted.value) {
                                              y2Progress
                                            } else {
                                              yProgress
                                            })
                                        .alpha(alphaProgress)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#666699"), 8))),
                            Column(
                                style =
                                    Style.width(50.dp)
                                        .height(50.dp)
                                        .margin(all = 5.dp)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#666699"), 8))),
                            Column(
                                style =
                                    Style.width(50.dp)
                                        .height(50.dp)
                                        .margin(all = 5.dp)
                                        .background(
                                            buildRoundedRect(
                                                context, Color.parseColor("#666699"), 8))),
                        ))))
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
