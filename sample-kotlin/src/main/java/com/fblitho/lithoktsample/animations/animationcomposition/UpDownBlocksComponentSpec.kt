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

package com.fblitho.lithoktsample.animations.animationcomposition

import android.graphics.Color
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.Transition.TransitionUnitsBuilder
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaAlign

@LayoutSpec
object UpDownBlocksComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State top: Boolean): Component =
      Row.create(c)
          .heightDip(200f)
          .alignItems(if (top) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
          .child(
              Row.create(c)
                  .heightDip(40f)
                  .flexGrow(1f)
                  .backgroundColor(Color.parseColor("#ee1111"))
                  .transitionKey("red")
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .build())
          .child(
              Row.create(c)
                  .heightDip(40f)
                  .flexGrow(1f)
                  .backgroundColor(Color.parseColor("#1111ee"))
                  .transitionKey("blue")
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .build())
          .child(
              Row.create(c)
                  .heightDip(40f)
                  .flexGrow(1f)
                  .backgroundColor(Color.parseColor("#11ee11"))
                  .transitionKey("green")
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .build())
          .clickHandler(UpDownBlocksComponent.onClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    UpDownBlocksComponent.updateState(c)
  }

  @OnUpdateState
  fun updateState(top: StateValue<Boolean>) {
    top.set(!(top.get()!!))
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition =
      Transition.stagger<TransitionUnitsBuilder>(
          200,
          Transition.create(Transition.TransitionKeyType.GLOBAL, "red").animate(AnimatedProperties.Y),
          Transition.create(Transition.TransitionKeyType.GLOBAL, "blue").animate(AnimatedProperties.Y),
          Transition.create(Transition.TransitionKeyType.GLOBAL, "green").animate(AnimatedProperties.Y))
}
