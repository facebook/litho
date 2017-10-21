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

package com.fblitho.lithoktsample.lithography.sections

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.fblitho.lithoktsample.lithography.components.DecadeSeparator
import com.fblitho.lithoktsample.lithography.components.FeedItemCard
import com.fblitho.lithoktsample.lithography.data.Artist
import com.fblitho.lithoktsample.lithography.data.Decade

@GroupSectionSpec
object DecadeSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(c: SectionContext, @Prop decade: Decade): Children =
      Children.create()
          .child(
              SingleComponentSection.create(c)
                  .component(DecadeSeparator.create(c).decade(decade))
                  .sticky(true))
          .child(
              DataDiffSection.create<Artist>(c)
                  .data(decade.artists)
                  .renderEventHandler(
                      DecadeSection.render(c))
                  .onCheckIsSameItemEventHandler(
                      DecadeSection.isSameItem(c)))
          .build()


  @JvmStatic
  @OnEvent(RenderEvent::class)
  fun render(
      c: SectionContext,
      @FromEvent model: Artist): RenderInfo =
      ComponentRenderInfo.create()
          .component(FeedItemCard.create(c).artist(model).build())
          .build()

  @JvmStatic
  @OnEvent(OnCheckIsSameItemEvent::class)
  fun isSameItem(
      c: SectionContext,
      @FromEvent previousItem: Artist,
      @FromEvent nextItem: Artist): Boolean = previousItem.name == nextItem.name
}
