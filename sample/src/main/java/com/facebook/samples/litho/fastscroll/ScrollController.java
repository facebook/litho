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

import static com.facebook.samples.litho.fastscroll.FastScrollHandleComponentSpec.HANDLE_SIZE_DP;
import static com.facebook.samples.litho.fastscroll.FastScrollHandleComponentSpec.HANDLE_VERTICAL_MARGIN;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;

class ScrollController extends RecyclerView.OnScrollListener implements View.OnTouchListener {
  private final DynamicValue<Float> handleOffsetDV;
  private final RecyclerCollectionEventsController scrollController;

  private int scrollOffsetRange = -1;
  private int handleOffsetRange = -1;

  private int scrollOffset = 0;
  private int handleOffset = 0;

  private boolean userControlling = false;

  private float prevHandleTouchEventY;

  ScrollController(
      RecyclerCollectionEventsController scrollController, DynamicValue<Float> handleOffsetDV) {
    this.scrollController = scrollController;
    this.handleOffsetDV = handleOffsetDV;
  }

  @Override
  public void onScrolled(RecyclerView rv, int dx, int dy) {
    final int scrollRange = rv.computeVerticalScrollRange();
    final int scrollExtent = rv.computeVerticalScrollExtent();

    scrollOffsetRange = scrollRange - scrollExtent;
    handleOffsetRange =
        scrollExtent - dpToPixel(rv.getContext(), 2 * HANDLE_VERTICAL_MARGIN + HANDLE_SIZE_DP);

    scrollOffset = rv.computeVerticalScrollOffset();

    if (userControlling) {
      return;
    }

    final float scale = ((float) scrollOffset) / scrollOffsetRange;
    handleOffset = (int) (handleOffsetRange * scale);
    handleOffsetDV.set((float) handleOffset);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return onHandleTouchEvent(event);
  }

  boolean onHandleTouchEvent(MotionEvent event) {
    final float currentY = event.getRawY();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        userControlling = true;
        break;

      case MotionEvent.ACTION_MOVE:
        final float dy = prevHandleTouchEventY - currentY;

        if (handleOffsetRange > 0) {
          // Adjust offset
          handleOffset -= dy;
          // Check if it is in the correct range
          handleOffset = Math.max(0, handleOffset);
          handleOffset = Math.min(handleOffsetRange, handleOffset);
          // Pass it on to the DynamicValue
          handleOffsetDV.set((float) handleOffset);

          final float scale = ((float) handleOffset) / handleOffsetRange;
          final int newScrollOffset = (int) (scale * scrollOffsetRange);

          scrollController.requestScrollBy(0, newScrollOffset - scrollOffset);

          scrollOffset = newScrollOffset;
        }
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        userControlling = false;
        break;
    }

    prevHandleTouchEventY = currentY;

    return true;
  }

  private static int dpToPixel(Context context, int dp) {
    return (int)
        (dp
            * ((float) context.getResources().getDisplayMetrics().densityDpi
                / DisplayMetrics.DENSITY_DEFAULT));
  }
}
