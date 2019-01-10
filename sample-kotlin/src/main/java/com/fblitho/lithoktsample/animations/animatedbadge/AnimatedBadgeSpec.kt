/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fblitho.lithoktsample.animations.animatedbadge

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.TypedValue
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.Transition.BaseTransitionUnitsBuilder
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import java.util.Arrays

@LayoutSpec
object AnimatedBadgeSpec {

  private const val ANIMATION_DURATION = 300
  private val ANIMATOR = Transition.timing(ANIMATION_DURATION)

  private const val TRANSITION_KEY_TEXT = "text"

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State state: Int): Component {
    val expanded1 = state == 1 || state == 2
    val expanded2 = state == 2 || state == 3
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 8f)
        .child(Row.create(c).marginDip(YogaEdge.TOP, 8f).child(buildComment1(c, expanded1)))
        .child(Row.create(c).marginDip(YogaEdge.TOP, 16f).child(buildComment2(c, expanded2)))
        .clickHandler(AnimatedBadge.onClick(c))
        .build()
  }

  private fun buildComment1(c: ComponentContext, expanded: Boolean): Component =
      Column.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .child(
              Row.create(c)
                  .alignItems(YogaAlign.CENTER)
                  .child(
                      Text.create(c)
                          .textSizeSp(16f)
                          .textStyle(Typeface.BOLD)
                          .text("Cristobal Castilla"))
                  .child(
                      Row.create(c)
                          .marginDip(YogaEdge.LEFT, 8f)
                          .paddingDip(YogaEdge.ALL, 3f)
                          .alignItems(YogaAlign.CENTER)
                          .child(
                              Column.create(c)
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(buildRoundedRect(c, -0x48b5, 9)))
                          .child(
                              if (!expanded) {
                                null
                              } else {
                                Text.create(c)
                                    // still need transition keys for appear/disappear animations
                                    .transitionKey(TRANSITION_KEY_TEXT)
                                    .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                                    // need this to prevent the global key of "+1" Text from changing
                                    .key("text")
                                    .marginDip(YogaEdge.LEFT, 8f)
                                    .clipToBounds(true)
                                    .textSizeDip(12f)
                                    .text("Top Follower")
                              }
                          )
                          .child(
                              Text.create(c)
                                  .marginDip(YogaEdge.LEFT, 8f)
                                  .marginDip(YogaEdge.RIGHT, 4f)
                                  .textSizeDip(12f)
                                  .textColor(Color.BLUE)
                                  .text("+1"))
                          .background(buildRoundedRect(c, Color.WHITE, 12))))
          .child(Text.create(c).textSizeSp(18f).text("So awesome!"))
          .background(buildRoundedRect(c, -0x222223, 20))
          .build()

  private fun buildComment2(c: ComponentContext, expanded: Boolean): Component =
      Column.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .child(
              Row.create(c)
                  .alignItems(YogaAlign.CENTER)
                  .child(
                      Text.create(c)
                          .textSizeSp(16f)
                          .textStyle(Typeface.BOLD)
                          .text("Cristobal Castilla"))
                  .child(
                      Row.create(c)
                          .widthDip((if (expanded) 48 else 24).toFloat())
                          .marginDip(YogaEdge.LEFT, 8f)
                          .paddingDip(YogaEdge.ALL, 3f)
                          .alignItems(YogaAlign.CENTER)
                          .child(
                              Column.create(c)
                                  .positionType(YogaPositionType.ABSOLUTE)
                                  .positionDip(YogaEdge.LEFT, (if (expanded) 27 else 3).toFloat())
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(buildRoundedRect(c, 0xFFB2CFE5.toInt(), 9)))
                          .child(
                              Column.create(c)
                                  .positionType(YogaPositionType.ABSOLUTE)
                                  .positionDip(YogaEdge.LEFT, (if (expanded) 15 else 3).toFloat())
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(buildRoundedRect(c, 0xFF4B8C61.toInt(), 9)))
                          .child(
                              Column.create(c)
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(buildRoundedRect(c, 0xFFFFB74B.toInt(), 9)))
                          .background(buildRoundedRect(c, Color.WHITE, 12))))
          .child(Text.create(c).textSizeSp(18f).text("So awesome!"))
          .background(buildRoundedRect(c, 0xFFDDDDDD.toInt(), 20))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    AnimatedBadge.updateState(c)
  }

  @OnUpdateState
  fun updateState(state: StateValue<Int>) {
    state.set((state.get()!! + 1) % 4)
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition =
      Transition.parallel<BaseTransitionUnitsBuilder>(
          Transition.allLayout().animator(ANIMATOR),
          Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
              .animate(AnimatedProperties.WIDTH)
              .appearFrom(0f)
              .disappearTo(0f)
              .animator(ANIMATOR),
          Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
              .animate(AnimatedProperties.ALPHA)
              .appearFrom(0f)
              .disappearTo(0f)
              .animator(ANIMATOR))

  private fun buildRoundedRect(c: ComponentContext, color: Int, cornerRadiusDp: Int): Drawable {
    val cornerRadiusPx = TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cornerRadiusDp.toFloat(),
            c.resources.displayMetrics
        )

    val radii = FloatArray(8)
    Arrays.fill(radii, cornerRadiusPx)
    val roundedRectShape = RoundRectShape(radii, null, radii)

    return ShapeDrawable(roundedRectShape).also {
      it.paint.color = color
    }
  }
}
