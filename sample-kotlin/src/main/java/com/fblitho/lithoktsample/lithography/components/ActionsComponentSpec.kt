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
import com.facebook.yoga.YogaEdge.RIGHT
import com.facebook.yoga.YogaEdge.TOP
import com.facebook.yoga.YogaEdge.ALL
import com.facebook.yoga.YogaPositionType.ABSOLUTE

@LayoutSpec
object ActionsComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Row.create(c)
          .backgroundColor(0xDDFFFFFF.toInt())
          .positionType(ABSOLUTE)
          .positionDip(RIGHT, 4f)
          .positionDip(TOP, 4f)
          .paddingDip(ALL, 2f)
          .child(FavouriteButton.create(c))
          .build()
}
