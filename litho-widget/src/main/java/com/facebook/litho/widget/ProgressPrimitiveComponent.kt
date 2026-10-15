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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.R
import com.facebook.litho.Style
import com.facebook.litho.core.androidStyle
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.useCached
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.utils.withEqualDimensions

/**
 * Renders an infinitely spinning progress bar.
 *
 * @param indeterminateDrawable Drawable to be shown to show progress.
 * @param color Tint color for the drawable.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 *   resource that supplies defaults for the progress bar, such as its
 *   `android:indeterminateDrawable`.
 * @param defStyleRes A style resource that supplies defaults for the progress bar, such as its
 *   `android:indeterminateDrawable`. Used only when [indeterminateDrawable] is not provided.
 */
class ProgressPrimitiveComponent(
    @param:ColorInt private val color: Int = Color.TRANSPARENT,
    private val indeterminateDrawable: Drawable? = null,
    private val style: Style? = null,
    @param:AttrRes private val defStyleAttr: Int = 0,
    @param:StyleRes private val defStyleRes: Int = 0,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val resolvedIndeterminateDrawable =
        useCached(indeterminateDrawable, defStyleAttr, defStyleRes) {
          indeterminateDrawable
              ?: if (defStyleAttr != 0 || defStyleRes != 0) {
                getStyledIndeterminateDrawable(androidContext, defStyleAttr, defStyleRes)
              } else {
                null
              }
        }
    return LithoPrimitive(
        layoutBehavior = ProgressLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> ProgressView(context) }) {
              bind(resolvedIndeterminateDrawable, color) { content ->
                val defaultIndeterminateDrawable = content.indeterminateDrawable

                resolvedIndeterminateDrawable?.let { content.indeterminateDrawable = it }
                content.indeterminateDrawable?.let {
                  if (color != Color.TRANSPARENT) {
                    content.indeterminateDrawable.mutate().colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            color,
                            BlendModeCompat.MODULATE,
                        )
                  }
                }
                onUnbind {
                  // restore the color first, since it acts on the indeterminateDrawable
                  if (color != Color.TRANSPARENT && content.indeterminateDrawable != null) {
                    content.indeterminateDrawable.mutate().clearColorFilter()
                  }

                  content.indeterminateDrawable = defaultIndeterminateDrawable
                }
              }
            },
        style = Style.androidStyle(defStyleAttr, defStyleRes) + style,
    )
  }

  companion object {
    const val DEFAULT_SIZE: Int = 50

    /**
     * Java-compatible factory method that accepts pixel values for dimensions. Use this method when
     * calling from Java code with pixel values (e.g., from .widthPx() or .heightPx() calls).
     *
     * @param color Tint color for the [indeterminateDrawable].
     * @param indeterminateDrawable Drawable to be shown to indicate progress.
     * @param widthPx Width in pixels, or null to not set.
     * @param heightPx Height in pixels, or null to not set.
     * @param style Additional style to apply, or null for no additional style.
     * @param defStyleAttr Theme attribute referencing a style with progress bar defaults.
     * @param defStyleRes Style resource supplying progress bar defaults (e.g.
     *   `android:indeterminateDrawable`); used only when [indeterminateDrawable] is null.
     */
    @JvmStatic
    @JvmOverloads
    @SinceKotlin("1000.0") // make unavailable to Kotlin
    fun create(
        @ColorInt color: Int = Color.TRANSPARENT,
        indeterminateDrawable: Drawable? = null,
        widthPx: Int? = null,
        heightPx: Int? = null,
        style: Style? = null,
        @AttrRes defStyleAttr: Int = 0,
        @StyleRes defStyleRes: Int = 0,
    ): ProgressPrimitiveComponent {
      val dimensionStyle =
          when {
            widthPx != null && heightPx != null -> Style.width(widthPx.px).height(heightPx.px)
            widthPx != null -> Style.width(widthPx.px)
            heightPx != null -> Style.height(heightPx.px)
            else -> null
          }
      val finalStyle =
          when {
            dimensionStyle != null && style != null -> dimensionStyle + style
            dimensionStyle != null -> dimensionStyle
            style != null -> style
            else -> null
          }
      return ProgressPrimitiveComponent(
          color = color,
          indeterminateDrawable = indeterminateDrawable,
          style = finalStyle,
          defStyleAttr = defStyleAttr,
          defStyleRes = defStyleRes,
      )
    }

    /**
     * Java-compatible factory method that accepts dp values for dimensions. Use this method when
     * calling from Java code with dp values (e.g., from .widthDip() or .heightDip() calls).
     *
     * @param color Tint color for the [indeterminateDrawable].
     * @param indeterminateDrawable Drawable to be shown to indicate progress.
     * @param widthDp Width in dp, or null to not set.
     * @param heightDp Height in dp, or null to not set.
     * @param style Additional style to apply, or null for no additional style.
     * @param defStyleAttr Theme attribute referencing a style with progress bar defaults.
     * @param defStyleRes Style resource supplying progress bar defaults (e.g.
     *   `android:indeterminateDrawable`); used only when [indeterminateDrawable] is null.
     */
    @JvmStatic
    @JvmOverloads
    @SinceKotlin("1000.0") // make unavailable to Kotlin
    fun createDp(
        @ColorInt color: Int = Color.TRANSPARENT,
        indeterminateDrawable: Drawable? = null,
        widthDp: Float? = null,
        heightDp: Float? = null,
        style: Style? = null,
        @AttrRes defStyleAttr: Int = 0,
        @StyleRes defStyleRes: Int = 0,
    ): ProgressPrimitiveComponent {
      val dimensionStyle =
          when {
            widthDp != null && heightDp != null -> Style.width(widthDp.dp).height(heightDp.dp)
            widthDp != null -> Style.width(widthDp.dp)
            heightDp != null -> Style.height(heightDp.dp)
            else -> null
          }
      val finalStyle =
          when {
            dimensionStyle != null && style != null -> dimensionStyle + style
            dimensionStyle != null -> dimensionStyle
            style != null -> style
            else -> null
          }
      return ProgressPrimitiveComponent(
          color = color,
          indeterminateDrawable = indeterminateDrawable,
          style = finalStyle,
          defStyleAttr = defStyleAttr,
          defStyleRes = defStyleRes,
      )
    }

    private fun getStyledIndeterminateDrawable(
        context: Context,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int,
    ): Drawable? {
      val a = context.obtainStyledAttributes(null, R.styleable.Progress, defStyleAttr, defStyleRes)
      return try {
        val resId = a.getResourceId(R.styleable.Progress_android_indeterminateDrawable, 0)
        if (resId != 0) ContextCompat.getDrawable(context, resId) else null
      } finally {
        a.recycle()
      }
    }
  }
}

internal object ProgressLayoutBehavior : LayoutBehavior {

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
              IntSize(
                  ProgressPrimitiveComponent.DEFAULT_SIZE,
                  ProgressPrimitiveComponent.DEFAULT_SIZE,
              )
            } else {
              IntSize.withEqualDimensions(sizeConstraints)
            },
    )
  }
}
