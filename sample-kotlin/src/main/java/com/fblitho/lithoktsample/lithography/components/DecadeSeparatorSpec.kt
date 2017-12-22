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

package com.fblitho.lithoktsample.lithography.components

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign.CENTER
import com.facebook.yoga.YogaEdge.ALL
import com.facebook.yoga.YogaEdge.HORIZONTAL
import com.fblitho.lithoktsample.lithography.data.Decade

@LayoutSpec
object DecadeSeparatorSpec {
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop decade: Decade): Component =
      Row.create(c)
          .alignItems(CENTER)
          .paddingDip(ALL, 16f)
          .child(
              Row.create(c)
                  .heightPx(1)
                  .backgroundColor(0xFFAAAAAA.toInt())
                  .flex(1f))
          .child(
              Text.create(c)
                  .text(decade.year.toString())
                  .textSizeDip(14f)
                  .textColor(0xFFAAAAAA.toInt())
                  .marginDip(HORIZONTAL, 10f)
                  .flex(0f))
          .child(
              Row.create(c)
                  .heightPx(1)
                  .backgroundColor(0xFFAAAAAA.toInt())
                  .flex(1f))
          .backgroundColor(0xFFFAFAFA.toInt())
          .build()
}
