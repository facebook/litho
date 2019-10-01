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

package com.fblitho.lithoktsample.errors

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Card
import com.facebook.yoga.YogaEdge

@LayoutSpec
object ListRowComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop row: ListRow): Component =
      Column.create(c)
          .paddingDip(YogaEdge.VERTICAL, 8f)
          .paddingDip(YogaEdge.HORIZONTAL, 32f)
          .child(
              Card.create(c)
                  .content(
                      Column.create(c)
                          .marginDip(YogaEdge.ALL, 32f)
                          .child(TitleComponent.create(c).title(row.title))
                          .child(PossiblyCrashingSubTitleComponent.create(c).subtitle(row.subtitle))
                          .build())
                  .build())
          .build()
}
