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

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.yoga.YogaEdge

@LayoutSpec
object ErrorRootComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop dataModels: List<ListRow>): Component =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(
              DataDiffSection.create<ListRow>(SectionContext(c))
                  .data(dataModels)
                  .renderEventHandler(ErrorRootComponent.onRender(c))
                  .build())
          .paddingDip(YogaEdge.TOP, 8f)
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: ListRow): RenderInfo =
      ComponentRenderInfo.create().component(model.createComponent(c)).build()
}
