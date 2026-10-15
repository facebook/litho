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

package com.facebook.litho.animation

import android.graphics.drawable.Drawable
import android.view.View
import com.facebook.litho.AnimatableItem
import com.facebook.rendercore.Host
import com.facebook.rendercore.MountItem
import com.facebook.rendercore.MountItemVisitor
import com.facebook.rendercore.transitions.AnimatedRootHost
import com.facebook.rendercore.transitions.TransitionRenderUnit
import com.facebook.rendercore.transitions.TransitionUtils
import com.facebook.rendercore.utils.BoundsUtils
import kotlin.Array
import kotlin.math.roundToInt

/** A convenience class for common View properties applicable to all subclasses of View. */
object AnimatedProperties {

  /**
   * The absolute X-position of a mount content, relative to the root [Host] that is rendering this
   * component tree.
   */
  @JvmField val X: AnimatedProperty = XAnimatedProperty()

  /**
   * The absolute Y-position of a mount content, relative to the root [Host] that is rendering this
   * component tree.
   */
  @JvmField val Y: AnimatedProperty = YAnimatedProperty()

  /**
   * The X-position of a mount content relative to its parent. Use instead of [X] when a child's
   * absolute position is unchanged between renders (e.g., pinned to a screen edge) but its position
   * within the parent changes because the parent resized. [X] would skip the animation (same
   * absolute position), while [RELATIVE_X] detects the change and fires it.
   */
  @JvmField val RELATIVE_X: AnimatedProperty = RelativeXAnimatedProperty()

  /**
   * The Y-position of a mount content relative to its parent. Use instead of [Y] when a child's
   * absolute position is unchanged between renders but its position within the parent changes
   * because the parent resized.
   */
  @JvmField val RELATIVE_Y: AnimatedProperty = RelativeYAnimatedProperty()

  /** The width of a mount content. */
  @JvmField val WIDTH: AnimatedProperty = WidthAnimatedProperty()

  /** The height of a mount content. */
  @JvmField val HEIGHT: AnimatedProperty = HeightAnimatedProperty()

  /** The transparency of a mount content, where 0 is fully invisible and 1 is fully opaque. */
  @JvmField val ALPHA: AnimatedProperty = AlphaAnimatedProperty()

  /**
   * The scale of a mount content: treats both X- and Y-scales as one.
   *
   * The unit for scale is a percentage of the canonical dimensions of this content, with 1 =
   * original size (e.g. .5 = half the width/height, 2 = double the width/height).
   */
  @JvmField val SCALE: AnimatedProperty = ScaleAnimatedProperty()

  /**
   * The width scale factor of a mount content.
   *
   * The unit for scale is a percentage of the canonical width of this content, with 1 = original
   * width (e.g. .5 = half the width, 2 = double the width).
   */
  @JvmField val SCALE_X: AnimatedProperty = ScaleXAnimatedProperty()

  /**
   * The height scale factor of a mount content.
   *
   * The unit for scale is a percentage of the canonical height of this content, with 1 = original
   * height (e.g. .5 = half the height, 2 = double the height).
   */
  @JvmField val SCALE_Y: AnimatedProperty = ScaleYAnimatedProperty()

  /**
   * The rotated degree around the pivot point of a mount content. Increasing the value results in
   * clockwise rotation.
   */
  @JvmField val ROTATION: AnimatedProperty = RotationAnimatedProperty()

  /**
   * The rotated degree around the X-axis of a mount content. Increasing the value results in
   * clockwise rotation.
   */
  @JvmField val ROTATION_X: AnimatedProperty = RotationXAnimatedProperty()

  /**
   * The rotated degree around the Y-axis of a mount content. Increasing the value results in
   * clockwise rotation.
   */
  @JvmField val ROTATION_Y: AnimatedProperty = RotationYAnimatedProperty()

  @JvmField val AUTO_LAYOUT_PROPERTIES: Array<AnimatedProperty> = arrayOf(X, Y, WIDTH, HEIGHT)

  private fun assertIsView(mountContent: Any, property: AnimatedProperty): View {
    if (mountContent !is View) {
      throw RuntimeException(
          "Animating '${property.getName()}' is only supported on Views (got ${mountContent})",
      )
    }
    return mountContent
  }

  /**
   * @return the x or y position of the given view relative to the root [Host] that this
   *   ComponentTree is being rendered in to.
   */
  private fun getPositionRelativeToRootHost(mountContent: View?, getX: Boolean): Float {
    var pos = 0f
    var currentView = mountContent
    while (true) {
      if (currentView == null || currentView.parent !is View) {
        return pos
      }
      if (currentView is Host && currentView is AnimatedRootHost) {
        return pos
      }
      pos += if (getX) currentView.x else currentView.y
      currentView = currentView.parent as View?
    }
  }

  private fun getHostView(drawable: Drawable): View? {
    var _drawable: Drawable? = drawable
    var callback: Drawable.Callback?
    while (true) {
      callback = _drawable?.callback
      if (callback is Drawable) {
        _drawable = callback
      } else if (callback is View) {
        return callback
      } else {
        return null
      }
    }
  }

  private fun getLinkedDrawables(host: Host): List<Drawable>? {
    val collector = LinkedDrawablesCollector()
    host.visitMountItems(collector)
    return collector.drawables
  }

  /**
   * Instantiated per call rather than pooled on this `object`: a shared accumulator would have to
   * be drained on every exit path, including a throwing visit, and would be reachable from every
   * animating host in the process at once.
   */
  private class LinkedDrawablesCollector : MountItemVisitor {
    var drawables: MutableList<Drawable>? = null
      private set

    override fun visit(mountItem: MountItem): Boolean {
      val content = mountItem.content
      val renderUnit = mountItem.renderTreeNode.renderUnit
      if (content is Drawable && renderUnit is TransitionRenderUnit && renderUnit.matchHostBounds) {
        val collected = drawables ?: ArrayList<Drawable>().also { drawables = it }
        collected.add(content)
      }
      return true
    }
  }

  private class XAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "x"

    override fun get(mountContent: Any): Float =
        when {
          mountContent is Host && mountContent is AnimatedRootHost -> (mountContent as Host).x
          mountContent is View -> getPositionRelativeToRootHost(mountContent, true)
          mountContent is Drawable -> {
            val drawable = mountContent
            val parentX = getPositionRelativeToRootHost(getHostView(drawable), true)
            parentX + drawable.bounds.left
          }
          else -> {
            throw UnsupportedOperationException(
                "Getting X from unsupported mount content: $mountContent",
            )
          }
        }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getAbsoluteBounds().left.toFloat()

    override fun set(mountContent: Any, value: Float) {
      when {
        mountContent is Host && mountContent is AnimatedRootHost -> (mountContent as View).x = value
        mountContent is View -> {
          val view = mountContent
          val parentX = getPositionRelativeToRootHost(view.parent as? View, true)
          view.x = value - parentX
        }
        mountContent is Drawable -> {
          val drawable = mountContent
          val parentX = getPositionRelativeToRootHost(getHostView(drawable), true)
          TransitionUtils.applyXYToDrawableForAnimation(
              drawable,
              (value - parentX).toInt(),
              drawable.bounds.top,
          )
        }
        else -> {
          throw UnsupportedOperationException(
              "Setting X on unsupported mount content: $mountContent",
          )
        }
      }
    }

    override fun reset(mountContent: Any) {
      if (mountContent is View) {
        mountContent.translationX = 0f
      } else if (mountContent is Drawable) {
        // No-op: x/y are always properly set for Drawables
      }
    }
  }

  private class YAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "y"

    override fun get(mountContent: Any): Float =
        when {
          mountContent is Host && mountContent is AnimatedRootHost -> (mountContent as Host).y
          mountContent is View -> getPositionRelativeToRootHost(mountContent, false)
          mountContent is Drawable -> {
            val drawable = mountContent
            val parentY = getPositionRelativeToRootHost(getHostView(drawable), false)
            parentY + drawable.bounds.top
          }
          else -> {
            throw UnsupportedOperationException(
                "Getting Y from unsupported mount content: $mountContent",
            )
          }
        }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getAbsoluteBounds().top.toFloat()

    override fun set(mountContent: Any, value: Float) {
      when {
        mountContent is Host && mountContent is AnimatedRootHost -> (mountContent as View).y = value
        mountContent is View -> {
          val view = mountContent
          val parentY = getPositionRelativeToRootHost(view.parent as? View, false)
          view.y = value - parentY
        }
        mountContent is Drawable -> {
          val drawable = mountContent
          val parentY = getPositionRelativeToRootHost(getHostView(drawable), false)
          TransitionUtils.applyXYToDrawableForAnimation(
              drawable,
              drawable.bounds.left,
              (value - parentY).toInt(),
          )
        }
        else -> {
          throw UnsupportedOperationException(
              "Setting Y on unsupported mount content: $mountContent",
          )
        }
      }
    }

    override fun reset(mountContent: Any) {
      if (mountContent is View) {
        mountContent.translationY = 0f
      } else if (mountContent is Drawable) {
        // No-op: x/y are always properly set for Drawables
      }
    }
  }

  private class RelativeXAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "relative_x"

    override fun get(mountContent: Any): Float {
      if (mountContent is View) {
        return mountContent.x
      }
      throw UnsupportedOperationException(
          "Getting relative X from unsupported mount content: $mountContent",
      )
    }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getBounds().left.toFloat()

    override fun set(mountContent: Any, value: Float) {
      if (mountContent is View) {
        mountContent.x = value
        return
      }
      throw UnsupportedOperationException(
          "Setting relative X on unsupported mount content: $mountContent",
      )
    }

    override fun reset(mountContent: Any) {
      if (mountContent is View) {
        mountContent.translationX = 0f
      }
    }
  }

  private class RelativeYAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "relative_y"

    override fun get(mountContent: Any): Float {
      if (mountContent is View) {
        return mountContent.y
      }
      throw UnsupportedOperationException(
          "Getting relative Y from unsupported mount content: $mountContent",
      )
    }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getBounds().top.toFloat()

    override fun set(mountContent: Any, value: Float) {
      if (mountContent is View) {
        mountContent.y = value
        return
      }
      throw UnsupportedOperationException(
          "Setting relative Y on unsupported mount content: $mountContent",
      )
    }

    override fun reset(mountContent: Any) {
      if (mountContent is View) {
        mountContent.translationY = 0f
      }
    }
  }

  private class WidthAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "width"

    override fun get(mountContent: Any): Float =
        if (mountContent is View) {
          mountContent.width.toFloat()
        } else if (mountContent is Drawable) {
          mountContent.bounds.width().toFloat()
        } else {
          throw UnsupportedOperationException(
              "Getting width from unsupported mount content: $mountContent",
          )
        }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getAbsoluteBounds().width().toFloat()

    override fun set(mountContent: Any, value: Float) {
      when {
        mountContent is Host -> {
          val view = mountContent
          if (view is AnimatedRootHost) {
            (view as AnimatedRootHost).setAnimatedWidth(value.toInt())
          } else {
            val left = view.left
            BoundsUtils.applyBoundsToMountContent(
                left,
                view.top,
                (left + value).toInt(),
                view.bottom,
                null,
                view,
                false,
            )
          }
          val animatingDrawables = getLinkedDrawables(view)
          if (animatingDrawables != null) {
            val width = value.toInt()
            val height = view.height
            for (index in animatingDrawables.indices) {
              TransitionUtils.applySizeToDrawableForAnimation(
                  animatingDrawables[index],
                  width,
                  height,
              )
            }
          }
        }
        mountContent is View -> {
          val view = mountContent
          val left = view.left
          val right = (left + value).toInt()
          BoundsUtils.applyBoundsToMountContent(
              left,
              view.top,
              right,
              view.bottom,
              null,
              view,
              false,
          )
        }
        mountContent is Drawable -> {
          val drawable = mountContent
          val width = value.toInt()
          val height = drawable.bounds.height()
          TransitionUtils.applySizeToDrawableForAnimation(drawable, width, height)
        }
        else -> {
          throw UnsupportedOperationException(
              "Setting width on unsupported mount content: $mountContent",
          )
        }
      }
    }

    override fun reset(mountContent: Any) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  }

  private class HeightAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "height"

    override fun get(mountContent: Any): Float =
        if (mountContent is View) {
          mountContent.height.toFloat()
        } else if (mountContent is Drawable) {
          mountContent.bounds.height().toFloat()
        } else {
          throw UnsupportedOperationException(
              "Getting height from unsupported mount content: $mountContent",
          )
        }

    override fun get(animatableItem: AnimatableItem): Float =
        animatableItem.getAbsoluteBounds().height().toFloat()

    override fun set(mountContent: Any, value: Float) {
      when {
        mountContent is Host -> {
          val view = mountContent
          if (view is AnimatedRootHost) {
            (view as AnimatedRootHost).setAnimatedHeight(value.toInt())
          } else {
            val top = view.top
            BoundsUtils.applyBoundsToMountContent(
                view.left,
                top,
                view.right,
                (top + value).toInt(),
                null,
                view,
                false,
            )
          }
          val animatingDrawables = getLinkedDrawables(view)
          if (animatingDrawables != null) {
            val width = view.width
            val height = value.toInt()
            for (index in animatingDrawables.indices) {
              TransitionUtils.applySizeToDrawableForAnimation(
                  animatingDrawables[index],
                  width,
                  height,
              )
            }
          }
        }
        mountContent is View -> {
          val view = mountContent
          val top = view.top
          val bottom = (top + value).toInt()
          BoundsUtils.applyBoundsToMountContent(
              view.left,
              top,
              view.right,
              bottom,
              null,
              view,
              false,
          )
        }
        mountContent is Drawable -> {
          val drawable = mountContent
          val width = drawable.bounds.width()
          val height = value.toInt()
          TransitionUtils.applySizeToDrawableForAnimation(drawable, width, height)
        }
        else -> {
          throw UnsupportedOperationException(
              "Setting height on unsupported mount content: $mountContent",
          )
        }
      }
    }

    override fun reset(mountContent: Any) {
      // No-op: height/width are always properly set at mount time so we don't need to reset it.
    }
  }

  private class AlphaAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "alpha"

    override fun get(mountContent: Any): Float =
        when (mountContent) {
          is View -> mountContent.alpha
          is Drawable -> mountContent.alpha / 255f
          else ->
              throw UnsupportedOperationException(
                  "Tried to get alpha of unsupported mount content: $mountContent",
              )
        }

    override fun get(animatableItem: AnimatableItem): Float =
        if (animatableItem.isAlphaSet()) animatableItem.getAlpha() else 1f

    override fun set(mountContent: Any, value: Float) {
      when (mountContent) {
        is View -> mountContent.alpha = value
        is Drawable -> mountContent.alpha = (value.coerceIn(0f, 1f) * 255).roundToInt()
        else ->
            throw UnsupportedOperationException(
                "Setting alpha on unsupported mount content: $mountContent",
            )
      }
    }

    override fun reset(mountContent: Any) {
      when (mountContent) {
        is Drawable -> mountContent.alpha = 255
        else -> set(mountContent, 1f)
      }
    }
  }

  private class ScaleAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "scale"

    override fun get(mountContent: Any): Float {
      val asView = assertIsView(mountContent, this)
      val scale = asView.scaleX
      if (scale != asView.scaleY) {
        throw RuntimeException("Tried to get scale of view, but scaleX and scaleY are different")
      }
      return scale
    }

    override fun get(animatableItem: AnimatableItem): Float =
        if (animatableItem.isScaleSet()) animatableItem.getScale() else 1f

    override fun set(mountContent: Any, value: Float) {
      val asView = assertIsView(mountContent, this)
      asView.scaleX = value
      asView.scaleY = value
    }

    override fun reset(mountContent: Any) {
      val asView = assertIsView(mountContent, this)
      asView.scaleX = 1f
      asView.scaleY = 1f
    }
  }

  private class ScaleXAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "scale_x"

    override fun get(mountContent: Any): Float = assertIsView(mountContent, this).scaleX

    override fun get(animatableItem: AnimatableItem): Float = 1f

    override fun set(mountContent: Any, value: Float) {
      assertIsView(mountContent, this).scaleX = value
    }

    override fun reset(mountContent: Any) {
      assertIsView(mountContent, this).scaleX = 1f
    }
  }

  private class ScaleYAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "scale_y"

    override fun get(mountContent: Any): Float = assertIsView(mountContent, this).scaleY

    override fun get(animatableItem: AnimatableItem): Float = 1f

    override fun set(mountContent: Any, value: Float) {
      assertIsView(mountContent, this).scaleY = value
    }

    override fun reset(mountContent: Any) {
      assertIsView(mountContent, this).scaleY = 1f
    }
  }

  private class RotationAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "rotation"

    override fun get(mountContent: Any): Float = assertIsView(mountContent, this).rotation

    override fun get(animatableItem: AnimatableItem): Float =
        if (animatableItem.isRotationSet()) animatableItem.getRotation() else 0f

    override fun set(mountContent: Any, value: Float) {
      assertIsView(mountContent, this).rotation = value
    }

    override fun reset(mountContent: Any) {
      assertIsView(mountContent, this).rotation = 0f
    }
  }

  private class RotationXAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "rotationX"

    override fun get(mountContent: Any): Float = assertIsView(mountContent, this).rotationX

    override fun get(animatableItem: AnimatableItem): Float =
        if (animatableItem.isRotationXSet()) animatableItem.getRotationX() else 0f

    override fun set(mountContent: Any, value: Float) {
      assertIsView(mountContent, this).rotationX = value
    }

    override fun reset(mountContent: Any) {
      assertIsView(mountContent, this).rotationX = 0f
    }
  }

  private class RotationYAnimatedProperty : AnimatedProperty {
    override fun getName(): String = "rotationY"

    override fun get(mountContent: Any): Float = assertIsView(mountContent, this).rotationY

    override fun get(animatableItem: AnimatableItem): Float =
        if (animatableItem.isRotationYSet()) animatableItem.getRotationY() else 0f

    override fun set(mountContent: Any, value: Float) {
      assertIsView(mountContent, this).rotationY = value
    }

    override fun reset(mountContent: Any) {
      assertIsView(mountContent, this).rotationY = 0f
    }
  }
}
