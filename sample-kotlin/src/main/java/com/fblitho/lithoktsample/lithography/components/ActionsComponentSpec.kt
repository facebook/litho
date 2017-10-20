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

import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType


@LayoutSpec
object ActionsComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext): ComponentLayout =
      Row.create(c)
          .backgroundColor(0xDDFFFFFF.toInt())
          .positionType(YogaPositionType.ABSOLUTE)
          .positionDip(YogaEdge.RIGHT, 4f)
          .positionDip(YogaEdge.TOP, 4f)
          .paddingDip(YogaEdge.ALL, 2f)
          .child(FavouriteButton.create(c))
          .build()
}
