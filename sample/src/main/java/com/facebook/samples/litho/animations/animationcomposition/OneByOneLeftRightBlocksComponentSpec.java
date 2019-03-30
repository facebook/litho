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
import com.facebook.litho.Column;
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
public class OneByOneLeftRightBlocksComponentSpec {

  private static final String TRANSITION_KEY_RED = "red";
  private static final String TRANSITION_KEY_BLUE = "blue";
  private static final String TRANSITION_KEY_GREEN = "green";
  private static final String[] ALL_TRANSITION_KEYS = {
    TRANSITION_KEY_RED, TRANSITION_KEY_BLUE, TRANSITION_KEY_GREEN
  };

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int state) {
    final boolean redLeft = state == 0 || state == 4 || state == 5;
    final boolean blueLeft = state == 0 || state == 1 || state == 5;
    final boolean greenLeft = state == 0 || state == 1 || state == 2;
    return Column.create(c)
        .child(
            Column.create(c)
                .alignItems(redLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#ee1111"))
                        .transitionKey(TRANSITION_KEY_RED)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(blueLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#1111ee"))
                        .transitionKey(TRANSITION_KEY_BLUE)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(greenLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#11ee11"))
                        .transitionKey(TRANSITION_KEY_GREEN)
                        .build()))
        .clickHandler(OneByOneLeftRightBlocksComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    OneByOneLeftRightBlocksComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Integer> state) {
    state.set((state.get() + 1) % 6);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.create(ALL_TRANSITION_KEYS)
        .animate(AnimatedProperties.X, AnimatedProperties.Y, AnimatedProperties.ALPHA);
  }
}
