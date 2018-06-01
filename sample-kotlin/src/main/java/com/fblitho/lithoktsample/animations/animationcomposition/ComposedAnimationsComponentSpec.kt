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

package com.fblitho.lithoktsample.animations.animationcomposition

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

@LayoutSpec
object ComposedAnimationsComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(
              DataDiffSection.create<Any>(SectionContext(c))
                  .data(generateData(20))
                  .renderEventHandler(ComposedAnimationsComponent.onRender(c))
                  .onCheckIsSameItemEventHandler(ComposedAnimationsComponent.isSameItem(c))
                  .build())
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent index: Int): RenderInfo {
    val numDemos = 5
    // Keep alternating between demos
    val component: Component = when (index % numDemos) {
      0 -> StoryFooterComponent.create(c).key("footer").build()
      1 -> UpDownBlocksComponent.create(c).build()
      2 -> LeftRightBlocksComponent.create(c).build()
      3 -> OneByOneLeftRightBlocksComponent.create(c).build()
      4 -> LeftRightBlocksSequenceComponent.create(c).build()
      else -> throw RuntimeException("Bad index: $index")
    }
    return ComponentRenderInfo.create().component(component).build()
  }

  @OnEvent(OnCheckIsSameItemEvent::class)
  fun isSameItem(
      c: ComponentContext,
      @FromEvent previousItem: Data,
      @FromEvent nextItem: Data
  ): Boolean = previousItem.number == nextItem.number

  private fun generateData(number: Int): List<Any> {
    val dummyData = mutableListOf<Any>()

    for (i in 0 until number) {
      dummyData.add(Data(i))
    }

    return dummyData
  }
}
