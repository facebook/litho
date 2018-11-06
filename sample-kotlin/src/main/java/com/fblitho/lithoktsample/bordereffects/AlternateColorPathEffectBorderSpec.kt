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
import com.facebook.yoga.YogaEdge

@LayoutSpec
object AlternateColorPathEffectBorderSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Row.create(c)
          .child(
              Text.create(c)
                  .textSizeSp(20f)
                  .text("This component has a path effect with multiple colors"))
          .border(
              Border.create(c)
                  .color(YogaEdge.LEFT, NiceColor.RED)
                  .color(YogaEdge.TOP, NiceColor.ORANGE)
                  .color(YogaEdge.RIGHT, NiceColor.GREEN)
                  .color(YogaEdge.BOTTOM, NiceColor.BLUE)
                  .widthDip(YogaEdge.ALL, 5f)
                  .discreteEffect(5f, 10f)
                  .build())
          .build()
}
