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

package com.facebook.samples.litho.animations.animatedbadge;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.TypedValue;
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
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.Arrays;

@LayoutSpec
public class AnimatedBadgeSpec {
  private static final int ANIMATION_DURATION = 300;
  private static final Transition.TransitionAnimator ANIMATOR =
      Transition.timing(ANIMATION_DURATION);

  private static final String TRANSITION_KEY_OUTER_CONTAINER_1 = "outer_container_1";
  private static final String TRANSITION_KEY_CONTAINER_1 = "container_1";
  private static final String TRANSITION_KEY_TEXT = "text";
  private static final String TRANSITION_KEY_BADGE = "badge";
  private static final String TRANSITION_KEY_OUTER_CONTAINER_2 = "outer_container_2";
  private static final String TRANSITION_KEY_CONTAINER_2 = "container_2";
  private static final String TRANSITION_KEY_BADGE_GREEN = "badge_green";
  private static final String TRANSITION_KEY_BADGE_BLUE = "badge_blue";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int state) {
    final boolean expanded1 = state == 1 || state == 2;
    final boolean expanded2 = state == 2 || state == 3;
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .child(Row.create(c).marginDip(YogaEdge.TOP, 8).child(buildComment1(c, expanded1)))
        .child(Row.create(c).marginDip(YogaEdge.TOP, 16).child(buildComment2(c, expanded2)))
        .clickHandler(AnimatedBadge.onClick(c))
        .build();
  }

  static Component buildComment1(ComponentContext c, boolean expanded) {
    return Column.create(c)
        .transitionKey(TRANSITION_KEY_OUTER_CONTAINER_1)
        .paddingDip(YogaEdge.ALL, 8)
        .child(
            Row.create(c)
                .alignItems(YogaAlign.CENTER)
                .child(
                    Text.create(c)
                        .textSizeSp(16)
                        .textStyle(Typeface.BOLD)
                        .text("Cristobal Castilla"))
                .child(
                    Row.create(c)
                        .transitionKey(TRANSITION_KEY_CONTAINER_1)
                        .marginDip(YogaEdge.LEFT, 8)
                        .paddingDip(YogaEdge.ALL, 3)
                        .alignItems(YogaAlign.CENTER)
                        .child(
                            Column.create(c)
                                .heightDip(18)
                                .widthDip(18)
                                .background(buildRoundedRect(c, 0xFFFFB74B, 9)))
                        .child(
                            !expanded
                                ? null
                                : Text.create(c)
                                    .transitionKey(TRANSITION_KEY_TEXT)
                                    .marginDip(YogaEdge.LEFT, 8)
                                    .clipToBounds(true)
                                    .textSizeDip(12)
                                    .text("Top Follower"))
                        .child(
                            Text.create(c)
                                .transitionKey(TRANSITION_KEY_BADGE)
                                .marginDip(YogaEdge.LEFT, 8)
                                .marginDip(YogaEdge.RIGHT, 4)
                                .textSizeDip(12)
                                .textColor(Color.BLUE)
                                .text("+1"))
                        .background(buildRoundedRect(c, Color.WHITE, 12))))
        .child(Text.create(c).textSizeSp(18).text("So awesome!"))
        .background(buildRoundedRect(c, 0xFFDDDDDD, 20))
        .build();
  }

  static Component buildComment2(ComponentContext c, boolean expanded) {
    return Column.create(c)
        .transitionKey(TRANSITION_KEY_OUTER_CONTAINER_2)
        .paddingDip(YogaEdge.ALL, 8)
        .child(
            Row.create(c)
                .alignItems(YogaAlign.CENTER)
                .child(
                    Text.create(c)
                        .textSizeSp(16)
                        .textStyle(Typeface.BOLD)
                        .text("Cristobal Castilla"))
                .child(
                    Row.create(c)
                        .transitionKey(TRANSITION_KEY_CONTAINER_2)
                        .widthDip(expanded ? 48 : 24)
                        .marginDip(YogaEdge.LEFT, 8)
                        .paddingDip(YogaEdge.ALL, 3)
                        .alignItems(YogaAlign.CENTER)
                        .child(
                            Column.create(c)
                                .transitionKey(TRANSITION_KEY_BADGE_BLUE)
                                .positionType(YogaPositionType.ABSOLUTE)
                                .positionDip(YogaEdge.LEFT, expanded ? 27 : 3)
                                .heightDip(18)
                                .widthDip(18)
                                .background(buildRoundedRect(c, 0xFFB2CFE5, 9)))
                        .child(
                            Column.create(c)
                                .transitionKey(TRANSITION_KEY_BADGE_GREEN)
                                .positionType(YogaPositionType.ABSOLUTE)
                                .positionDip(YogaEdge.LEFT, expanded ? 15 : 3)
                                .heightDip(18)
                                .widthDip(18)
                                .background(buildRoundedRect(c, 0xFF4B8C61, 9)))
                        .child(
                            Column.create(c)
                                .heightDip(18)
                                .widthDip(18)
                                .background(buildRoundedRect(c, 0xFFFFB74B, 9)))
                        .background(buildRoundedRect(c, Color.WHITE, 12))))
        .child(Text.create(c).textSizeSp(18).text("So awesome!"))
        .background(buildRoundedRect(c, 0xFFDDDDDD, 20))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    AnimatedBadge.updateState(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Integer> state) {
    state.set((state.get() + 1) % 4);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create(
                TRANSITION_KEY_CONTAINER_1,
                TRANSITION_KEY_OUTER_CONTAINER_1,
                TRANSITION_KEY_CONTAINER_2,
                TRANSITION_KEY_OUTER_CONTAINER_2)
            .animate(AnimatedProperties.WIDTH)
            .animator(ANIMATOR),
        Transition.create(
                TRANSITION_KEY_BADGE, TRANSITION_KEY_BADGE_GREEN, TRANSITION_KEY_BADGE_BLUE)
            .animate(AnimatedProperties.X)
            .animator(ANIMATOR),
        Transition.create(TRANSITION_KEY_TEXT)
            .animate(AnimatedProperties.WIDTH)
            .appearFrom(0f)
            .disappearTo(0f)
            .animator(ANIMATOR),
        Transition.create(TRANSITION_KEY_TEXT)
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0f)
            .disappearTo(0f)
            .animator(ANIMATOR));
  }

  private static Drawable buildRoundedRect(ComponentContext c, int color, int cornerRadiusDp) {
    final float cornerRadiusPx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp, c.getResources().getDisplayMetrics());
    final float[] radii = new float[8];
    Arrays.fill(radii, cornerRadiusPx);
    final RoundRectShape roundedRectShape = new RoundRectShape(radii, null, radii);
    final ShapeDrawable drawable = new ShapeDrawable(roundedRectShape);
    drawable.getPaint().setColor(color);
    return drawable;
  }
}
