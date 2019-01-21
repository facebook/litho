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

package com.facebook.samples.litho.animations.animationcomposition;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class UpDownBlocksComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean top) {
    return Row.create(c)
        .heightDip(200)
        .alignItems(top ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey("red")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey("blue")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey("green")
                .build())
        .clickHandler(UpDownBlocksComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    UpDownBlocksComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> top) {
    top.set(!top.get());
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.stagger(
        200,
        Transition.create("red").animate(AnimatedProperties.Y),
        Transition.create("blue").animate(AnimatedProperties.Y),
        Transition.create("green").animate(AnimatedProperties.Y));
  }
}
