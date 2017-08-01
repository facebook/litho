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


package com.facebook.samples.litho.transitionsdemo;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionSet;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class LeftRightBlocksComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @State boolean left) {
    return Column.create(c)
        .alignItems(left ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey("red")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey("blue")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey("green")
                .build())
        .clickHandler(LeftRightBlocksComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    LeftRightBlocksComponent.updateState(c);
  }

  @OnUpdateState
  static void updateState(
      StateValue<Boolean> left) {
    left.set(left.get() == true ? false : true);
  }

  @OnCreateTransition
  static TransitionSet onCreateAutoTransition(
      ComponentContext c) {
    return Transition.createSet(
        Transition.create("red")
            .animate(AnimatedProperties.X),
        Transition.create("blue")
            .animate(AnimatedProperties.X),
        Transition.create("green")
            .animate(AnimatedProperties.X));
  }
}
