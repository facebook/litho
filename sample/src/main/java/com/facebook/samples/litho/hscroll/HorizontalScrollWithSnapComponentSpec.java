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

package com.facebook.samples.litho.hscroll;

import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER_CHILD;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_START;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.ItemSelectedEvent;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Spinner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;

@LayoutSpec
public class HorizontalScrollWithSnapComponentSpec {

  static String[] SNAP_MODE_STRING =
      new String[] {"SNAP_NONE", "SNAP_TO_START", "SNAP_TO_CENTER", "SNAP_TO_CENTER_CHILD"};

  static int[] SNAP_MODE_INT =
      new int[] {SNAP_NONE, SNAP_TO_START, SNAP_TO_CENTER, SNAP_TO_CENTER_CHILD};

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<RecyclerCollectionEventsController> eventsController,
      StateValue<Integer> snapMode) {
    eventsController.set(new RecyclerCollectionEventsController());
    snapMode.set(SNAP_NONE);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Integer[] colors,
      @State RecyclerCollectionEventsController eventsController,
      @State int snapMode) {

    final RecyclerConfiguration recyclerConfiguration =
        new ListRecyclerConfiguration(
            LinearLayoutManager.HORIZONTAL, /*reverseLayout*/ false, snapMode);
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(
            RecyclerCollectionComponent.create(c)
                .key("snapMode" + snapMode)
                .disablePTR(true)
                .recyclerConfiguration(recyclerConfiguration)
                .section(
                    DataDiffSection.<Integer>create(new SectionContext(c))
                        .data(Arrays.asList(colors))
                        .renderEventHandler(HorizontalScrollWithSnapComponent.onRender(c))
                        .build())
                .canMeasureRecycler(true)
                .itemDecoration(
                    new RecyclerView.ItemDecoration() {
                      @Override
                      public void getItemOffsets(
                          Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                        super.getItemOffsets(outRect, view, parent, state);
                        int spacingPx = 40;
                        int exteriorSpacingPx = 0;

                        int startPx = spacingPx;
                        int endPx = 0;
                        int position = parent.getChildLayoutPosition(view);
                        if (position == 0) {
                          startPx = exteriorSpacingPx;
                        }
                        if (position == state.getItemCount() - 1) {
                          endPx = exteriorSpacingPx;
                        }

                        outRect.left = startPx;
                        outRect.right = endPx;
                      }
                    })
                .eventsController(eventsController)
                .build())
        .child(
            Text.create(c)
                .paddingDip(YogaEdge.TOP, 5)
                .text("SCROLL TO NEXT")
                .textSizeSp(30)
                .clickHandler(HorizontalScrollWithSnapComponent.onClick(c, true)))
        .child(
            Text.create(c)
                .text("SCROLL TO PREVIOUS")
                .textSizeSp(30)
                .clickHandler(HorizontalScrollWithSnapComponent.onClick(c, false)))
        .child(
            Spinner.create(c)
                .options(Arrays.asList(SNAP_MODE_STRING))
                .selectedOption(getSnapModeString(snapMode))
                .itemSelectedEventHandler(HorizontalScrollWithSnapComponent.onItemSelected(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent Object model) {
    return ComponentRenderInfo.create()
        .component(Row.create(c).widthDip(120).heightDip(120).backgroundColor((int) model))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @State RecyclerCollectionEventsController eventsController,
      @Param boolean forward) {
    if (forward) {
      eventsController.requestScrollToNextPosition(true);
    } else {
      eventsController.requestScrollToPreviousPosition(true);
    }
  }

  @OnEvent(ItemSelectedEvent.class)
  static void onItemSelected(ComponentContext c, @FromEvent String newSelection) {
    HorizontalScrollWithSnapComponent.updateSnapMode(c, getSnapModeInt(newSelection));
  }

  @OnUpdateState
  static void updateSnapMode(StateValue<Integer> snapMode, @Param int newSnapMode) {
    snapMode.set(newSnapMode);
  }

  private static String getSnapModeString(int snapMode) {
    for (int i = 0; i < SNAP_MODE_INT.length; i++) {
      if (snapMode == SNAP_MODE_INT[i]) {
        return SNAP_MODE_STRING[i];
      }
    }
    return SNAP_MODE_STRING[0];
  }

  private static int getSnapModeInt(String snapMode) {
    for (int i = 0; i < SNAP_MODE_STRING.length; i++) {
      if (snapMode.equals(SNAP_MODE_STRING[i])) {
        return SNAP_MODE_INT[i];
      }
    }
    return SNAP_NONE;
  }
}
