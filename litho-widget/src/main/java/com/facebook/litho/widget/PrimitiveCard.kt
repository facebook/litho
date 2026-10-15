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
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.DynamicValue
import com.facebook.litho.KComponent
import com.facebook.litho.LayerType
import com.facebook.litho.Style
import com.facebook.litho.StyleCompat
import com.facebook.litho.Transition
import com.facebook.litho.core.margin
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.useCached
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.layerType
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

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
 *   allows for transparent corners. With `transparencyEnabled = false` [PrimitiveCard] uses
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
 * @property style Style to apply to the card.
 */
class PrimitiveCard(
    private val content: Component,
    private val cardBackgroundDrawable: Drawable? = null,
    private val cardBackgroundTransitionKey: String? = null,
    @param:ColorInt private val cardBackgroundColor: Int = Color.WHITE,
    private val cardBackgroundColorDv: DynamicValue<Int>? = null,
    @param:ColorInt private val clippingColor: Int = UNSET_CLIPPING,
    @param:ColorInt private val shadowStartColor: Int = DEFAULT_SHADOW_START_COLOR,
    @param:ColorInt private val shadowEndColor: Int = DEFAULT_SHADOW_END_COLOR,
    private val cornerRadius: Dimen? = null,
    private val elevation: Dimen? = null,
    private val shadowTopOverride: Dimen? = null,
    private val shadowBottomOverride: Dimen? = null,
    private val shadowLeftOverride: Dimen? = null,
    private val shadowRightOverride: Dimen? = null,
    private val transparencyEnabled: Boolean = false,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false,
    private val style: Style = Style,
) : KComponent() {
  @Suppress("DEPRECATION")
  override fun ComponentScope.render(): Component {

    val roundedCornerRadius =
        useCached(cornerRadius) {
          cornerRadius?.toPixels()?.toFloat()
              ?: roundedPixels(context.resources, DEFAULT_CORNER_RADIUS_DP)
        }

    val roundedElevation =
        useCached(elevation) {
          elevation?.toPixels()?.toFloat()
              ?: roundedPixels(context.resources, DEFAULT_SHADOW_SIZE_DP)
        }

    val shadowTop =
        if (shadowTopOverride == null) {
          CardShadowDrawable.getShadowTop(roundedElevation)
        } else {
          shadowTopOverride.toPixels()
        }
    val shadowBottom =
        if (shadowBottomOverride == null) {
          CardShadowDrawable.getShadowBottom(roundedElevation)
        } else {
          shadowBottomOverride.toPixels()
        }
    val shadowLeft =
        if (shadowLeftOverride == null) {
          CardShadowDrawable.getShadowLeft(roundedElevation)
        } else {
          shadowLeftOverride.toPixels()
        }
    val shadowRight =
        if (shadowRightOverride == null) {
          CardShadowDrawable.getShadowRight(roundedElevation)
        } else {
          shadowRightOverride.toPixels()
        }

    val marginStyle =
        Style.margin(
            left = shadowLeft.px,
            right = shadowRight.px,
            top = if (disableClipTopLeft && disableClipTopRight) 0.px else shadowTop.px,
            bottom = if (disableClipBottomLeft && disableClipBottomRight) 0.px else shadowBottom.px,
        )

    return Column(style = style) {
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
                      roundedCornerRadius,
                      disableClipTopLeft,
                      disableClipTopRight,
                      disableClipBottomLeft,
                      disableClipBottomRight,
                      cardBackgroundTransitionKey,
                  ),
              )
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
                                  Transition.TransitionKeyType.GLOBAL,
                              ),
                  ) {},
              )
              child(content)
              child(
                  createCardClip(
                      if (clippingColor == UNSET_CLIPPING) Color.WHITE else clippingColor,
                      roundedCornerRadius,
                      disableClipTopLeft,
                      disableClipTopRight,
                      disableClipBottomLeft,
                      disableClipBottomRight,
                  ),
              )
            }
          },
      )

      if (roundedElevation > 0) {
        child(
            CardShadow(
                shadowStartColor = shadowStartColor,
                shadowEndColor = shadowEndColor,
                cornerRadius = roundedCornerRadius,
                shadowSize = roundedElevation,
                hideTopShadow = disableClipTopLeft && disableClipTopRight,
                hideBottomShadow = disableClipBottomLeft && disableClipBottomRight,
                shadowLeftSizeOverride =
                    shadowLeftOverride?.toPixels()?.toFloat() ?: CardShadowDrawable.UNDEFINED,
                shadowRightSizeOverride =
                    shadowRightOverride?.toPixels()?.toFloat() ?: CardShadowDrawable.UNDEFINED,
                style = Style.positionType(YogaPositionType.ABSOLUTE).position(all = 0.px),
            ),
        )
      }
    }
  }

  @Suppress("DEPRECATION")
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
      cardBackgroundTransitionKey: String?,
  ): TransparencyEnabledCardClip {

    val style =
        Style.positionType(YogaPositionType.ABSOLUTE)
            .position(all = 0.px)
            .transitionKey(
                context,
                cardBackgroundTransitionKey,
                Transition.TransitionKeyType.GLOBAL,
            )
            .plus(
                if (
                    (disableClipBottomLeft ||
                        disableClipBottomRight ||
                        disableClipTopLeft ||
                        disableClipTopRight) && clippingColor == Color.TRANSPARENT
                ) {
                  // For any of the above enclosing conditions, the TransparencyEnabledDrawable
                  // implementation relies on PorterDuffXfermode which only works on view layer type
                  // software
                  Style.layerType(LayerType.LAYER_TYPE_SOFTWARE)
                } else {
                  null
                },
            )

    return TransparencyEnabledCardClip(
        backgroundDrawable = backgroundDrawable,
        cardBackgroundColor = backgroundColor,
        clippingColor = clippingColor,
        cornerRadius = cornerRadius,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        dynamicCardBackgroundColor = backgroundColorDv,
        style = style,
    )
  }

  private fun createCardClip(
      clippingColor: Int,
      cornerRadius: Float,
      disableClipTopLeft: Boolean,
      disableClipTopRight: Boolean,
      disableClipBottomLeft: Boolean,
      disableClipBottomRight: Boolean,
  ): CardClip {
    return CardClip(
        clippingColor = clippingColor,
        cornerRadius = cornerRadius,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        style = Style.positionType(YogaPositionType.ABSOLUTE).position(all = 0.px),
    )
  }

  /**
   * Java-friendly Builder for constructing [PrimitiveCard]. Allows setting only the properties you
   * need -- all others use sensible defaults.
   *
   * Kotlin callers should prefer the [PrimitiveCard] constructor with named parameters; the Builder
   * pattern is supported here only so that the `litho_card_spec_to_kotlin_api` codemod can produce
   * idiomatic Java migrations from the legacy Card builder to `PrimitiveCard.create(c,
   * content)...build()`.
   *
   * Two construction paths:
   * - `new PrimitiveCard.Builder(content)` -- for callers that only set already-resolved values
   *   (e.g., `cardBackgroundColor(int)`, `cornerRadiusDp(float)`).
   * - `PrimitiveCard.create(c, content)` -- for callers that want to use `*Res` / `*Attr` overloads
   *   (e.g., `cornerRadiusRes(R.dimen.foo)`, `cardBackgroundColorAttr(R.attr.bar)`). These
   *   overloads require a [ComponentContext] for resource resolution.
   *
   * Usage:
   * ```java
   * PrimitiveCard.create(c, content)
   *     .cardBackgroundColorAttr(R.attr.fds_usage_surface_background)
   *     .cornerRadiusRes(R.dimen.fbui_padding_half_standard)
   *     .elevationDp(8f)
   *     .build();
   * ```
   */
  class Builder(private val content: Component) {
    private var resolverContext: ComponentContext? = null
    private var cardBackgroundDrawable: Drawable? = null
    private var cardBackgroundTransitionKey: String? = null
    @ColorInt private var cardBackgroundColor: Int = Color.WHITE
    private var cardBackgroundColorDv: DynamicValue<Int>? = null
    @ColorInt private var clippingColor: Int = Companion.UNSET_CLIPPING
    @ColorInt private var shadowStartColor: Int = Companion.DEFAULT_SHADOW_START_COLOR
    @ColorInt private var shadowEndColor: Int = Companion.DEFAULT_SHADOW_END_COLOR
    private var cornerRadius: Dimen? = null
    private var elevation: Dimen? = null
    private var shadowTopOverride: Dimen? = null
    private var shadowBottomOverride: Dimen? = null
    private var shadowLeftOverride: Dimen? = null
    private var shadowRightOverride: Dimen? = null
    private var transparencyEnabled: Boolean = false
    private var disableClipTopLeft: Boolean = false
    private var disableClipTopRight: Boolean = false
    private var disableClipBottomLeft: Boolean = false
    private var disableClipBottomRight: Boolean = false
    private var style: Style = Style

    internal fun withContext(c: ComponentContext) = apply { this.resolverContext = c }

    private fun requireContext(method: String): ComponentContext =
        requireNotNull(resolverContext) {
          "PrimitiveCard.Builder.$method requires a ComponentContext. " +
              "Use PrimitiveCard.create(c, content) instead of new PrimitiveCard.Builder(content)."
        }

    fun cardBackgroundDrawable(drawable: Drawable?) = apply {
      this.cardBackgroundDrawable = drawable
    }

    fun cardBackgroundTransitionKey(key: String?) = apply { this.cardBackgroundTransitionKey = key }

    fun cardBackgroundColor(@ColorInt color: Int) = apply { this.cardBackgroundColor = color }

    fun cardBackgroundColorRes(@ColorRes res: Int) = apply {
      this.cardBackgroundColor =
          requireContext("cardBackgroundColorRes").resourceResolver.resolveColorRes(res)
    }

    fun cardBackgroundColorAttr(@AttrRes attr: Int) = apply {
      this.cardBackgroundColor =
          requireContext("cardBackgroundColorAttr").resourceResolver.resolveColorAttr(attr, 0)
    }

    fun cardBackgroundColorDv(dv: DynamicValue<Int>?) = apply { this.cardBackgroundColorDv = dv }

    fun clippingColor(@ColorInt color: Int) = apply { this.clippingColor = color }

    fun clippingColorRes(@ColorRes res: Int) = apply {
      this.clippingColor = requireContext("clippingColorRes").resourceResolver.resolveColorRes(res)
    }

    fun clippingColorAttr(@AttrRes attr: Int) = apply {
      this.clippingColor =
          requireContext("clippingColorAttr").resourceResolver.resolveColorAttr(attr, 0)
    }

    fun shadowStartColor(@ColorInt color: Int) = apply { this.shadowStartColor = color }

    fun shadowStartColorRes(@ColorRes res: Int) = apply {
      this.shadowStartColor =
          requireContext("shadowStartColorRes").resourceResolver.resolveColorRes(res)
    }

    fun shadowStartColorAttr(@AttrRes attr: Int) = apply {
      this.shadowStartColor =
          requireContext("shadowStartColorAttr").resourceResolver.resolveColorAttr(attr, 0)
    }

    fun shadowEndColor(@ColorInt color: Int) = apply { this.shadowEndColor = color }

    fun shadowEndColorRes(@ColorRes res: Int) = apply {
      this.shadowEndColor =
          requireContext("shadowEndColorRes").resourceResolver.resolveColorRes(res)
    }

    fun shadowEndColorAttr(@AttrRes attr: Int) = apply {
      this.shadowEndColor =
          requireContext("shadowEndColorAttr").resourceResolver.resolveColorAttr(attr, 0)
    }

    fun cornerRadiusDp(dp: Float?) = apply { this.cornerRadius = dp?.dp }

    fun cornerRadiusPx(px: Int) = apply { this.cornerRadius = px.px }

    fun cornerRadiusRes(@DimenRes res: Int) = apply {
      this.cornerRadius =
          requireContext("cornerRadiusRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun elevationDp(dp: Float?) = apply { this.elevation = dp?.dp }

    fun elevationPx(px: Int) = apply { this.elevation = px.px }

    fun elevationRes(@DimenRes res: Int) = apply {
      this.elevation = requireContext("elevationRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun shadowTopOverrideDp(dp: Float?) = apply { this.shadowTopOverride = dp?.dp }

    fun shadowTopOverridePx(px: Int?) = apply { this.shadowTopOverride = px?.px }

    fun shadowTopOverrideRes(@DimenRes res: Int) = apply {
      this.shadowTopOverride =
          requireContext("shadowTopOverrideRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun shadowBottomOverrideDp(dp: Float?) = apply { this.shadowBottomOverride = dp?.dp }

    fun shadowBottomOverridePx(px: Int?) = apply { this.shadowBottomOverride = px?.px }

    fun shadowBottomOverrideRes(@DimenRes res: Int) = apply {
      this.shadowBottomOverride =
          requireContext("shadowBottomOverrideRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun shadowLeftOverrideDp(dp: Float?) = apply { this.shadowLeftOverride = dp?.dp }

    fun shadowLeftOverridePx(px: Int?) = apply { this.shadowLeftOverride = px?.px }

    fun shadowLeftOverrideRes(@DimenRes res: Int) = apply {
      this.shadowLeftOverride =
          requireContext("shadowLeftOverrideRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun shadowRightOverrideDp(dp: Float?) = apply { this.shadowRightOverride = dp?.dp }

    fun shadowRightOverridePx(px: Int?) = apply { this.shadowRightOverride = px?.px }

    fun shadowRightOverrideRes(@DimenRes res: Int) = apply {
      this.shadowRightOverride =
          requireContext("shadowRightOverrideRes").resourceResolver.resolveDimenSizeRes(res).px
    }

    fun transparencyEnabled(enabled: Boolean) = apply { this.transparencyEnabled = enabled }

    fun disableClipTopLeft(disable: Boolean) = apply { this.disableClipTopLeft = disable }

    fun disableClipTopRight(disable: Boolean) = apply { this.disableClipTopRight = disable }

    fun disableClipBottomLeft(disable: Boolean) = apply { this.disableClipBottomLeft = disable }

    fun disableClipBottomRight(disable: Boolean) = apply { this.disableClipBottomRight = disable }

    /**
     * Sets the style for the card. This is **additive**: it composes with any styles already
     * accumulated by previous `style()` or convenience-method calls (`marginDp`, `flexShrink`,
     * etc.).
     */
    fun style(style: Style) = apply { this.style = this.style + style }

    fun marginDp(edge: YogaEdge, value: Float) = apply {
      // StyleCompat method is named `marginDip`; semantics are dp.
      this.style = this.style + StyleCompat.marginDip(edge, value).build()
    }

    fun marginPx(edge: YogaEdge, value: Int) = apply {
      this.style = this.style + StyleCompat.marginPx(edge, value).build()
    }

    fun marginRes(edge: YogaEdge, @DimenRes res: Int) = apply {
      val px = requireContext("marginRes").resourceResolver.resolveDimenSizeRes(res)
      this.style = this.style + StyleCompat.marginPx(edge, px).build()
    }

    fun flexShrink(value: Float) = apply {
      this.style = this.style + StyleCompat.flexShrink(value).build()
    }

    fun flexGrow(value: Float) = apply {
      this.style = this.style + StyleCompat.flexGrow(value).build()
    }

    fun build(): PrimitiveCard = PrimitiveCard(
        content = content,
        cardBackgroundDrawable = cardBackgroundDrawable,
        cardBackgroundTransitionKey = cardBackgroundTransitionKey,
        cardBackgroundColor = cardBackgroundColor,
        cardBackgroundColorDv = cardBackgroundColorDv,
        clippingColor = clippingColor,
        shadowStartColor = shadowStartColor,
        shadowEndColor = shadowEndColor,
        cornerRadius = cornerRadius,
        elevation = elevation,
        shadowTopOverride = shadowTopOverride,
        shadowBottomOverride = shadowBottomOverride,
        shadowLeftOverride = shadowLeftOverride,
        shadowRightOverride = shadowRightOverride,
        transparencyEnabled = transparencyEnabled,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        style = style,
    )
  }

  companion object {
    private const val DEFAULT_CORNER_RADIUS_DP: Int = 2
    private const val DEFAULT_SHADOW_SIZE_DP: Int = 2

    private const val DEFAULT_SHADOW_START_COLOR: Int = 0x37000000
    private const val DEFAULT_SHADOW_END_COLOR: Int = 0x03000000

    // Colors are clamped between 0x00000000 and 0xffffffff so this value is safe
    private const val UNSET_CLIPPING: Int = Int.MIN_VALUE

    private fun roundedPixels(resources: Resources, dips: Int): Float {
      val scale = resources.displayMetrics.density
      return dips * scale + 0.5f
    }

    /**
     * Java factory that returns a [Builder] capable of resolving `*Res` / `*Attr` resource IDs.
     * Prefer this over `new PrimitiveCard.Builder(content)` when migrating from the legacy Card
     * builder, since this preserves the same resource-aware ergonomics.
     */
    @JvmStatic
    fun create(c: ComponentContext, content: Component): Builder = Builder(content).withContext(c)

    /**
     * Java-compatible full factory method that accepts Float/Int values for dimensions instead of
     * Dimen inline class. Prefer using [Builder] instead for cleaner call sites.
     *
     * @param cornerRadiusDp Corner radius in dp, or null to use default (2dp)
     * @param elevationDp Elevation in dp, or null to use default (2dp)
     * @param shadowTopOverridePx Override of shadow at top in pixels, or null for default
     * @param shadowBottomOverridePx Override of shadow at bottom in pixels, or null for default
     * @param shadowLeftOverridePx Override of shadow at left in pixels, or null for default
     * @param shadowRightOverridePx Override of shadow at right in pixels, or null for default
     */
    @JvmStatic
    @SinceKotlin("1000.0") // make unavailable to Kotlin
    fun create(
        content: Component,
        cardBackgroundDrawable: Drawable?,
        cardBackgroundTransitionKey: String?,
        @ColorInt cardBackgroundColor: Int,
        cardBackgroundColorDv: DynamicValue<Int>?,
        @ColorInt clippingColor: Int,
        @ColorInt shadowStartColor: Int,
        @ColorInt shadowEndColor: Int,
        cornerRadiusDp: Float?,
        elevationDp: Float?,
        shadowTopOverridePx: Int?,
        shadowBottomOverridePx: Int?,
        shadowLeftOverridePx: Int?,
        shadowRightOverridePx: Int?,
        transparencyEnabled: Boolean,
        disableClipTopLeft: Boolean,
        disableClipTopRight: Boolean,
        disableClipBottomLeft: Boolean,
        disableClipBottomRight: Boolean,
        style: Style?,
    ): PrimitiveCard = PrimitiveCard(
        content = content,
        cardBackgroundDrawable = cardBackgroundDrawable,
        cardBackgroundTransitionKey = cardBackgroundTransitionKey,
        cardBackgroundColor = cardBackgroundColor,
        cardBackgroundColorDv = cardBackgroundColorDv,
        clippingColor = clippingColor,
        shadowStartColor = shadowStartColor,
        shadowEndColor = shadowEndColor,
        cornerRadius = cornerRadiusDp?.dp,
        elevation = elevationDp?.dp,
        shadowTopOverride = shadowTopOverridePx?.px,
        shadowBottomOverride = shadowBottomOverridePx?.px,
        shadowLeftOverride = shadowLeftOverridePx?.px,
        shadowRightOverride = shadowRightOverridePx?.px,
        transparencyEnabled = transparencyEnabled,
        disableClipTopLeft = disableClipTopLeft,
        disableClipTopRight = disableClipTopRight,
        disableClipBottomLeft = disableClipBottomLeft,
        disableClipBottomRight = disableClipBottomRight,
        style = style ?: Style,
    )
  }
}
