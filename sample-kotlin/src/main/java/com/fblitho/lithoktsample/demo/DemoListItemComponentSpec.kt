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

package com.fblitho.lithoktsample.demo

import android.content.Intent
import android.view.View
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text

import com.facebook.yoga.YogaEdge.ALL

@LayoutSpec
object DemoListItemComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop model: DemoListDataModel): Component =
      Column.create(c)
          .paddingDip(ALL, 16f)
          .child(Text.create(c).text(model.name).textSizeSp(18f).build())
          .clickHandler(DemoListItemComponent.onClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(
      c: ComponentContext,
      @FromEvent view: View,
      @Prop model: DemoListDataModel,
      @Prop currentIndices: IntArray
  ) {
    val activityClass = if (model.datamodels == null) {
      model.klass
    } else {
      DemoListActivity::class.java
    }

    val intent = Intent(c.getAndroidContext(), activityClass)
    intent.putExtra(DemoListActivity.INDICES, currentIndices)
    c.getAndroidContext().startActivity(intent)
  }
}
