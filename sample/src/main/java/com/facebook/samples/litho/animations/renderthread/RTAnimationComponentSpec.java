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

package com.facebook.samples.litho.animations.renderthread;

import android.graphics.Color;
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
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;

@LayoutSpec
public class RTAnimationComponentSpec {
  private static final int FADE_IN_OUT_DURATION = 3000;
  private static final int FADE_IN_DELAY = 1000;
  private static final int FADE_IN_STAGGER_DELAY = 500;

  private static final String[] RED_KEYS = {
    "red00", "red01", "red02",
    "red10", "red11", "red12",
  };

  private static final String[] GRAY_KEYS = {
    "gray00", "gray01", "gray02",
    "gray10", "gray11", "gray12",
  };

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean state) {
    final int color = state ? Color.RED : Color.LTGRAY;
    final String key = state ? "red" : "gray";

    return Column.create(c)
        .child(buildRow(c, color, key + 0))
        .child(buildRow(c, color, key + 1))
        .paddingDip(YogaEdge.ALL, 8)
        .clickHandler(RTAnimationComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    RTAnimationComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> state) {
    state.set(!state.get());
  }

  private static Component buildRow(ComponentContext c, int color, String key) {
    return Row.create(c)
        .child(buildCell(c, color, key + 0))
        .child(buildCell(c, color, key + 1))
        .child(buildCell(c, color, key + 2))
        .build();
  }

  private static Component buildCell(ComponentContext c, int color, String key) {
    return Column.create(c)
        .flexGrow(1f)
        .aspectRatio(0.75f)
        .marginDip(YogaEdge.ALL, 8)
        .background(buildRoundedRect(c, color, 8))
        .transitionKey(key)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .build();
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c, @Prop boolean useRT) {
    return useRT ? buildRenderThreadTransition() : buildUiThreadTransition();
  }

  private static Transition buildRenderThreadTransition() {
    final Transition[] pieces = new Transition[1 + RED_KEYS.length];
    pieces[0] =
        Transition.create(Transition.TransitionKeyType.GLOBAL, GRAY_KEYS)
            .animate(AnimatedProperties.ALPHA)
            .animator(Transition.renderThread(FADE_IN_OUT_DURATION))
            .disappearTo(0);

    int delay = FADE_IN_DELAY;
    for (int i = 0; i < RED_KEYS.length; i++, delay += FADE_IN_STAGGER_DELAY) {
      pieces[i + 1] =
          Transition.create(Transition.TransitionKeyType.GLOBAL, RED_KEYS[i])
              .animate(AnimatedProperties.ALPHA)
              .animator(Transition.renderThread(delay, FADE_IN_OUT_DURATION))
              .appearFrom(0);
    }

    return Transition.parallel(pieces);
  }

  private static Transition buildUiThreadTransition() {
    final Transition.TransitionAnimator fadeInOutAnimator = Transition.timing(FADE_IN_OUT_DURATION);

    final Transition fadeOut =
        Transition.create(Transition.TransitionKeyType.GLOBAL, GRAY_KEYS)
            .animate(AnimatedProperties.ALPHA)
            .animator(fadeInOutAnimator)
            .disappearTo(0);

    final Transition[] fadeInPieces = new Transition[RED_KEYS.length];
    for (int i = 0; i < RED_KEYS.length; i++) {
      fadeInPieces[i] =
          Transition.create(Transition.TransitionKeyType.GLOBAL, RED_KEYS[i])
              .animate(AnimatedProperties.ALPHA)
              .animator(fadeInOutAnimator)
              .appearFrom(0);
    }
    final Transition fadeIn = Transition.stagger(FADE_IN_STAGGER_DELAY, fadeInPieces);

    return Transition.stagger(FADE_IN_DELAY, fadeOut, fadeIn);
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
