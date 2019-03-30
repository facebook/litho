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

package com.fblitho.lithoktsample.bordereffects

import com.facebook.litho.Border
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge

@LayoutSpec
object RtlColorWidthBorderSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Row.create(c)
          .layoutDirection(YogaDirection.RTL)
          .child(Text.create(c).textSizeSp(20f).text("This component is RTL"))
          .border(
              Border.create(c)
                  .color(YogaEdge.START, NiceColor.RED)
                  .color(YogaEdge.TOP, NiceColor.YELLOW)
                  .color(YogaEdge.END, NiceColor.GREEN)
                  .color(YogaEdge.BOTTOM, NiceColor.BLUE)
                  .widthDip(YogaEdge.START, 2f)
                  .widthDip(YogaEdge.TOP, 4f)
                  .widthDip(YogaEdge.END, 8f)
                  .widthDip(YogaEdge.BOTTOM, 16f)
                  .build())
          .build()

}
