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

package com.facebook.samples.litho.animations.expandableelement;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Transition;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

class ExpandableElementUtil {
  static final String TRANSITION_MSG_PARENT = "transition_msg_parent";
  static final String TRANSITION_TEXT_MESSAGE_WITH_BOTTOM = "transition_text_msg_with_bottom";
  static final String TRANSITION_TOP_DETAIL = "transition_top_detail";
  static final String TRANSITION_BOTTOM_DETAIL = "transition_bottom_detail";

  static ShapeDrawable getMessageBackground(ComponentContext c, int color) {
    final RoundRectShape roundedRectShape =
        new RoundRectShape(
            new float[] {40, 40, 40, 40, 40, 40, 40, 40},
            null,
            new float[] {40, 40, 40, 40, 40, 40, 40, 40});
    final ShapeDrawable oval = new ShapeDrawable(roundedRectShape);
    oval.getPaint().setColor(color);
    return oval;
  }

  @Nullable
  static Component.Builder maybeCreateBottomDetailComponent(
      ComponentContext c, boolean expanded, boolean seen) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.FLEX_END)
        .paddingDip(YogaEdge.RIGHT, 10)
        .transitionKey(TRANSITION_BOTTOM_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(seen ? "Seen" : "Sent");
  }

  @Nullable
  static Component.Builder maybeCreateTopDetailComponent(
      ComponentContext c, boolean expanded, String timestamp) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.CENTER)
        .transitionKey(TRANSITION_TOP_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(timestamp);
  }
}
