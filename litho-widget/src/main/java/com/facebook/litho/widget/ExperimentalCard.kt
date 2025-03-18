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

package com.facebook.litho.widget

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.DynamicValue
import com.facebook.litho.KComponent
import com.facebook.litho.LayerType
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.core.margin
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.useCached
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.layerType
import com.facebook.rendercore.px
import com.facebook.yoga.YogaPositionType
import kotlin.math.ceil

/**
 * A component that renders a given component into a card border with shadow.
 *
 * @property content Component to render inside the card.
 * @property cardBackgroundDrawable Background drawable for the card.
 * @property cardBackgroundTransitionKey TransitionKey for the Background. If set, enables adding a
 *   global transition for animating the card background.
 * @property cardBackgroundColor Background color for the card.
 * @property cardBackgroundColorDv DynamicValue for the Background color for the card.
 * @property clippingColor Color for corner clipping.
 * @property shadowStartColor Start color for shadow drawn underneath the card.
 * @property shadowEndColor End color for shadow drawn underneath the card.
 * @property cornerRadius Corner radius for the card.
 * @property elevation Elevation of the card.
 * @property shadowTopOverride Override of size of shadow at top of card.
 * @property shadowBottomOverride Override of size of shadow at bottom of card.
 * @property shadowLeftOverride Override of size of shadow at left of card.
 * @property shadowRightOverride Override of size of shadow at right of card.
 * @property transparencyEnabled If set, the card will be rendered with transparency. **UNPERFORMANT
 *   WARNING** if you do not need to render your corners transparently please set to `false`. It is
 *   more expensive to perform rounded corners with transparent clipping due to antialiasing
 *   operations. A component that renders a given component into a card border with shadow, and
 *   allows for transparent corners. With `transparencyEnabled = false` [ExperimentalCard] uses
 *   imitation clipped corners that draw in a solid color to mimic the background.
 *   `transparencyEnabled = true` is useful if you are rendering your pill over a gradient or
 *   dynamic background.
 * @property disableClipTopLeft If set, opt out of clipping the top-left corner, elevation will
 *   force to 0 in this case.
 * @property disableClipTopRight If set, opt out of clipping the top-right corner, elevation will
 *   force to 0 in this case.
 * @property disableClipBottomLeft If set, opt out of clipping the bottom-left corner, elevation
 *   will force to 0 in this case.
 * @property disableClipBottomRight If set, opt out of clipping the bottom-right corner, elevation
 *   will force to 0 in this case.
 */
class ExperimentalCard(
    private val content: Component,
    private val cardBackgroundDrawable: Drawable? = null,
    private val cardBackgroundTransitionKey: String? = null,
    @ColorInt private val cardBackgroundColor: Int = Color.WHITE,
    private val cardBackgroundColorDv: DynamicValue<Int>? = null,
    @ColorInt private val clippingColor: Int = UNSET_CLIPPING,
    @ColorInt private val shadowStartColor: Int = DEFAULT_SHADOW_START_COLOR,
    @ColorInt private val shadowEndColor: Int = DEFAULT_SHADOW_END_COLOR,
    private val cornerRadius: Float = UNSET_CORNER_RADIUS,
    private val elevation: Float = UNSET_ELEVATION,
    private val shadowTopOverride: Float = CardShadowDrawable.UNDEFINED,
    private val shadowBottomOverride: Float = CardShadowDrawable.UNDEFINED,
    private val shadowLeftOverride: Float = CardShadowDrawable.UNDEFINED,
    private val shadowRightOverride: Float = CardShadowDrawable.UNDEFINED,
    private val transparencyEnabled: Boolean = false,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false
) : KComponent() {
  override fun ComponentScope.render(): Component {

    val roundedCornerRadius =
        useCached(cornerRadius) {
          if (cornerRadius == UNSET_CORNER_RADIUS) {
            roundedPixels(context.resources, DEFAULT_CORNER_RADIUS_DP)
          } else {
            cornerRadius
          }
        }

    val roundedElevation =
        useCached(elevation) {
          if (elevation == UNSET_ELEVATION) {
            roundedPixels(context.resources, DEFAULT_SHADOW_SIZE_DP)
          } else {
            elevation
          }
        }

    val shadowTop =
        if (shadowTopOverride == CardShadowDrawable.UNDEFINED) {
          CardShadowDrawable.getShadowTop(roundedElevation)
        } else {
          ceil(shadowTopOverride.toDouble()).toInt()
        }
    val shadowBottom =
        if (shadowBottomOverride == CardShadowDrawable.UNDEFINED) {
          CardShadowDrawable.getShadowBottom(roundedElevation)
        } else {
          ceil(shadowBottomOverride.toDouble()).toInt()
        }
    val shadowLeft =
        if (shadowLeftOverride == CardShadowDrawable.UNDEFINED) {
          CardShadowDrawable.getShadowLeft(roundedElevation)
        } else {
          ceil(shadowLeftOverride.toDouble()).toInt()
        }
    val shadowRight =
        if (shadowRightOverride == CardShadowDrawable.UNDEFINED) {
          CardShadowDrawable.getShadowRight(roundedElevation)
        } else {
          ceil(shadowRightOverride.toDouble()).toInt()
        }

    val marginStyle =
        Style.margin(
            left = shadowLeft.px,
            right = shadowRight.px,
            top = if (disableClipTopLeft && disableClipTopRight) 0.px else shadowTop.px,
            bottom = if (disableClipBottomLeft && disableClipBottomRight) 0.px else shadowBottom.px)

    return Column {
      child(
          if (transparencyEnabled) {
            val realClippingColor =
                if (clippingColor == UNSET_CLIPPING) {
                  Color.TRANSPARENT
                } else {
                  clippingColor
                }
            Column(style = marginStyle.backgroundColor(realClippingColor)) {
              child(
                  createTransparencyEnabledCardClip(
                      context,
                      cardBackgroundDrawable,
                      cardBackgroundColor,
                      cardBackgroundColorDv,
                      realClippingColor,
                      cornerRadius,
                      disableClipTopLeft,
                      disableClipTopRight,
                      disableClipBottomLeft,
                      disableClipBottomRight,
                      cardBackgroundTransitionKey))
              child(content)
            }
          } else {
            Column(style = marginStyle) {
              child(
                  Column(
                      style =
                          Style.backgroundColor(cardBackgroundColor)
                              .flex(grow = 1f, shrink = 0f)
                              .positionType(YogaPositionType.ABSOLUTE)
                              .position(all = 0.px)
                              .transitionKey(
                                  context,
                                  cardBackgroundTransitionKey,
                                  Transition.TransitionKeyType.GLOBAL)) {})
              child(content)
              child(
                  createCardClip(
                      if (clippingColor == UNSET_CLIPPING) Color.WHITE else clippingColor,
                      cornerRadius,
                      disableClipTopLeft,
                      disableClipTopRight,
                      disableClipBottomLeft,
                      disableClipBottomRight))
            }
          })

      if (roundedElevation > 0) {
        child(
            ExperimentalCardShadow(
                shadowStartColor = shadowStartColor,
                shadowEndColor = shadowEndColor,
                cornerRadius = roundedCornerRadius,
                shadowSize = roundedElevation,
                hideTopShadow = disableClipTopLeft && disableClipTopRight,
                hideBottomShadow = disableClipBottomLeft && disableClipBottomRight,
                shadowLeftSizeOverride = shadowLeftOverride,
                shadowRightSizeOverride = shadowRightOverride,
                style = Style.positionType(YogaPositionType.ABSOLUTE).position(all = 0.px)))
      }
    }
  }

  private fun createTransparencyEnabledCardClip(
      context: ComponentContext,
      backgroundDrawable: Drawable?,
      backgroundColor: Int,
      backgroundColorDv: DynamicValue<Int>?,
      clippingColor: Int,
      cornerRadius: Float,
      disableClipTopLeft: Boolean,
      disableClipTopRight: Boolean,
      disableClipBottomLeft: Boolean,
      disableClipBottomRight: Boolean,
      cardBackgroundTransitionKey: String?
  ): ExperimentalTransparencyEnabledCardClip {

    val style =
        Style.positionType(YogaPositionType.ABSOLUTE)
            .position(all = 0.px)
            .transitionKey(
                context, cardBackgroundTransitionKey, Transition.TransitionKeyType.GLOBAL)
            .plus(
                if ((disableClipBottomLeft ||
                    disableClipBottomRight ||
                    disableClipTopLeft ||
                    disableClipTopRight) && clippingColor == Color.TRANSPARENT) {
                  // For any of the above enclosing conditions, the TransparencyEnabledDrawable
                  // implementation relies on PorterDuffXfermode which only works on view layer type
                  // software
                  Style.layerType(LayerType.LAYER_TYPE_SOFTWARE, null)
                } else {
                  null
                })

    return ExperimentalTransparencyEnabledCardClip(
        backgroundDrawable = backgroundDrawable,
        cardBackgroundColor = backgroundColor,
        clippingColor = clippingColor,
        cornerRadius = cornerRadius,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        dynamicCardBackgroundColor = backgroundColorDv,
        style = style)
  }

  private fun createCardClip(
      clippingColor: Int,
      cornerRadius: Float,
      disableClipTopLeft: Boolean,
      disableClipTopRight: Boolean,
      disableClipBottomLeft: Boolean,
      disableClipBottomRight: Boolean
  ): ExperimentalCardClip {
    return ExperimentalCardClip(
        clippingColor = clippingColor,
        cornerRadius = cornerRadius,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        style = Style.positionType(YogaPositionType.ABSOLUTE).position(all = 0.px))
  }

  companion object {
    private const val DEFAULT_CORNER_RADIUS_DP: Int = 2
    private const val DEFAULT_SHADOW_SIZE_DP: Int = 2

    private const val DEFAULT_SHADOW_START_COLOR: Int = 0x37000000
    private const val DEFAULT_SHADOW_END_COLOR: Int = 0x03000000

    // Colors are clamped between 0x00000000 and 0xffffffff so this value is safe
    private const val UNSET_CLIPPING: Int = Int.MIN_VALUE

    private const val UNSET_CORNER_RADIUS: Float = -1f
    private const val UNSET_ELEVATION: Float = -1f

    private fun roundedPixels(resources: Resources, dips: Int): Float {
      val scale = resources.displayMetrics.density
      return dips * scale + 0.5f
    }
  }
}
