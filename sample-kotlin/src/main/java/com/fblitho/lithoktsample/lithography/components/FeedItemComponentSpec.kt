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

import android.graphics.Typeface
import android.widget.LinearLayout.HORIZONTAL
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLifecycle
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.ListRecyclerConfiguration.SNAP_TO_CENTER
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import com.fblitho.lithoktsample.lithography.data.Artist
import com.fblitho.lithoktsample.lithography.sections.ImagesSection

@LayoutSpec
object FeedItemComponentSpec {
  private val recyclerConfiguration: ListRecyclerConfiguration<SectionBinderTarget> =
      ListRecyclerConfiguration(HORIZONTAL, false, SNAP_TO_CENTER)

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop artist: Artist) =
      Column.create(c)
          .child(
              Column.create(c)
                  .child(imageBlock(artist, c))
                  .child(
                      Text.create(c)
                          .text(artist.name)
                          .textStyle(Typeface.BOLD)
                          .textSizeDip(24f)
                          .backgroundColor(0xDDFFFFFF.toInt())
                          .positionType(YogaPositionType.ABSOLUTE)
                          .positionDip(YogaEdge.BOTTOM, 4f)
                          .positionDip(YogaEdge.LEFT, 4f)
                          .paddingDip(YogaEdge.HORIZONTAL, 6f))
                  .child(ActionsComponent.create(c)))
          .child(FooterComponent.create(c).text(artist.biography))
          .build()

  private fun imageBlock(artist: Artist, c: ComponentContext): Component<out ComponentLifecycle> =
      when (artist.images.size) {
        1 -> singleimage(c, artist)
        else -> recycler(c, artist)
      }

  private fun recycler(c: ComponentContext, artist: Artist): Component<RecyclerCollectionComponent> =
      RecyclerCollectionComponent.create(c)
          .recyclerConfiguration(recyclerConfiguration)
          .section(ImagesSection.create(SectionContext(c))
              .images(artist.images)
              .build())
          .aspectRatio(2f)
          .build()

  private fun singleimage(c: ComponentContext, artist: Artist): Component<SingleImageComponent> =
      SingleImageComponent.create(c)
          .image(artist.images[0])
          .imageAspectRatio(2f)
          .build()
}
