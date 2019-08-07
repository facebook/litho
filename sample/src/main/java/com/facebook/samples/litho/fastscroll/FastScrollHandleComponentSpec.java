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

package com.facebook.samples.litho.fastscroll;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.Layout;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.Border;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.StateValue;
import com.facebook.litho.TouchEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.VerticalGravity;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.Arrays;

@LayoutSpec
public class FastScrollHandleComponentSpec {
  static final int HANDLE_SIZE_DP = 60;
  static final int HANDLE_VERTICAL_MARGIN = 12;
  static final int HANDLE_RIGHT_MARGIN = 24;

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<RecyclerCollectionEventsController> recyclerEventsController,
      StateValue<DynamicValue<Float>> handleTranslation,
      StateValue<ScrollController> scrollController) {
    final RecyclerCollectionEventsController recyclerEventsControllerValue =
        new RecyclerCollectionEventsController();
    final DynamicValue<Float> handleTranslationValue = new DynamicValue<>(0f);
    final ScrollController scrollControllerValue =
        new ScrollController(recyclerEventsControllerValue, handleTranslationValue);

    recyclerEventsController.set(recyclerEventsControllerValue);
    handleTranslation.set(handleTranslationValue);
    scrollController.set(scrollControllerValue);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State RecyclerCollectionEventsController recyclerEventsController,
      @State DynamicValue<Float> handleTranslation,
      @State ScrollController scrollController) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(
            RecyclerCollectionComponent.create(c)
                .positionType(YogaPositionType.ABSOLUTE)
                .positionPx(YogaEdge.ALL, 0)
                .section(CountriesListSection.create(new SectionContext(c)).build())
                .onScrollListener(scrollController)
                .eventsController(recyclerEventsController)
                .disablePTR(true))
        .child(
            buildDragHandle(c)
                .positionType(YogaPositionType.ABSOLUTE)
                .positionDip(YogaEdge.RIGHT, HANDLE_RIGHT_MARGIN)
                .positionDip(YogaEdge.TOP, HANDLE_VERTICAL_MARGIN)
                .translationY(handleTranslation)
                .touchHandler(FastScrollHandleComponent.onTouchEvent(c)))
        .build();
  }

  private static Component.Builder buildDragHandle(ComponentContext c) {
    final int radiusDip = HANDLE_SIZE_DP / 2;
    return Text.create(c)
        .widthDip(HANDLE_SIZE_DP)
        .heightDip(HANDLE_SIZE_DP)
        .background(buildCircleDrawable(c, 0xFFDDDDDD, radiusDip))
        .border(
            Border.create(c)
                .color(YogaEdge.ALL, Color.BLACK)
                .widthDip(YogaEdge.ALL, 1)
                .radiusDip(radiusDip)
                .build())
        .verticalGravity(VerticalGravity.CENTER)
        .textAlignment(Layout.Alignment.ALIGN_CENTER)
        .textSizeDip(16)
        .typeface(Typeface.DEFAULT_BOLD)
        .text("DRAG");
  }

  private static Drawable buildCircleDrawable(ComponentContext c, int color, int radiusDp) {
    final float radiusPx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, radiusDp, c.getResources().getDisplayMetrics());

    final float[] radii = new float[8];
    Arrays.fill(radii, radiusPx);

    final RoundRectShape roundedRectShape = new RoundRectShape(radii, null, radii);

    final ShapeDrawable drawable = new ShapeDrawable(roundedRectShape);
    drawable.getPaint().setColor(color);
    return drawable;
  }

  @OnEvent(TouchEvent.class)
  static boolean onTouchEvent(
      ComponentContext c,
      @FromEvent View view,
      @FromEvent MotionEvent motionEvent,
      @State ScrollController scrollController) {
    return scrollController.onTouch(view, motionEvent);
  }
}
