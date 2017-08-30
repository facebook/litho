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

package com.facebook.samples.litho.bordereffects;

import android.graphics.Color;
import com.facebook.litho.Border;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class RtlColorWidthBorderSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Row.create(c)
        .layoutDirection(YogaDirection.RTL)
        .child(Text.create(c).textSizeSp(20).text("This component is RTL"))
        .border(
            Border.create(c)
                .color(YogaEdge.START, Color.RED)
                .color(YogaEdge.TOP, Color.YELLOW)
                .color(YogaEdge.END, Color.GREEN)
                .color(YogaEdge.BOTTOM, Color.BLUE)
                .widthDip(YogaEdge.START, 2)
                .widthDip(YogaEdge.TOP, 4)
                .widthDip(YogaEdge.END, 8)
                .widthDip(YogaEdge.BOTTOM, 16)
                .build())
        .build();
  }
}
