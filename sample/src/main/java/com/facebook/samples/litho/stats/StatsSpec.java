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

package com.facebook.samples.litho.stats;

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class StatsSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .paddingDip(YogaEdge.ALL, 5f)
        .child(Text.create(c).textSizeSp(20).text("LITHO STATS").marginDip(YogaEdge.BOTTOM, 10f))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text(
                    "Total applied state updates:           "
                        + LithoStats.getAppliedStateUpdates()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text("Total triggered *sync* state updates:  " + LithoStats.getStateUpdatesSync()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text(
                    "Total triggered *async* state updates: " + LithoStats.getStateUpdatesAsync()))
        .child(
            Text.create(c)
                .textSizeSp(14)
                .textColor(Color.DKGRAY)
                .textStyle(Typeface.ITALIC)
                .typeface(Typeface.MONOSPACE)
                .text("Total triggered *lazy* state updates:  " + LithoStats.getStateUpdatesLazy()))
        .build();
  }
}
