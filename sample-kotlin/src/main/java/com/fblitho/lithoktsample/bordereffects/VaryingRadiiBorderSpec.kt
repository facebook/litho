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

import android.graphics.Color
import com.facebook.litho.Border
import com.facebook.litho.Border.Corner
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge

@LayoutSpec
object VaryingRadiiBorderSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Row.create(c)
          .child(Text.create(c).textSizeSp(20f).text("This component has varying corner radii"))
          .border(
              Border.create(c)
                  .widthDip(YogaEdge.ALL, 3f)
                  .color(YogaEdge.LEFT, Color.BLACK)
                  .color(YogaEdge.TOP, NiceColor.GREEN)
                  .color(YogaEdge.BOTTOM, NiceColor.BLUE)
                  .color(YogaEdge.RIGHT, NiceColor.RED)
                  .radiusDip(Corner.TOP_LEFT, 10f)
                  .radiusDip(Corner.TOP_RIGHT, 5f)
                  .radiusDip(Corner.BOTTOM_RIGHT, 20f)
                  .radiusDip(Corner.BOTTOM_LEFT, 30f)
                  .build())
          .build()

}
