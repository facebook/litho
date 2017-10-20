/*
 *
 *  This file provided by Facebook is for non-commercial testing and evaluation
 *  purposes only.  Facebook reserves all rights not expressly granted.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 *  FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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

import android.widget.LinearLayout
import com.facebook.litho.*

import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.fblitho.lithoktsample.lithography.data.Artist
import com.fblitho.lithoktsample.lithography.sections.ImagesSection

@LayoutSpec
object FeedItemComponentSpec {
  private val recyclerConfiguration: ListRecyclerConfiguration<SectionBinderTarget> =
      ListRecyclerConfiguration(
          LinearLayout.HORIZONTAL,
          false,
          ListRecyclerConfiguration.SNAP_TO_CENTER);

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop artist: Artist): ComponentLayout =
      Column.create(c)
          .child(
              Column.create(c)
                  .child(imageBlock(artist, c))
                  .child(TitleComponent.create(c).title(artist.name))
                  .child(ActionsComponent.create(c)))
          .child(FooterComponent.create(c).text(artist.biography))
          .build()


  private inline fun imageBlock(artist: Artist, c: ComponentContext): Component<out ComponentLifecycle> =
      if (artist.images.size == 1) {
        singleimage(c, artist)
      } else {
        recycler(c, artist);
      }


  private inline fun recycler(c: ComponentContext, artist: Artist): Component<RecyclerCollectionComponent> =
      RecyclerCollectionComponent.create(c)
          .recyclerConfiguration(recyclerConfiguration)
          .section(ImagesSection.create(SectionContext(c))
              .images(artist.images)
              .build())
          .aspectRatio(2f)
          .build()


  private inline fun singleimage(c: ComponentContext, artist: Artist): Component<SingleImageComponent> =
      SingleImageComponent.create(c)
          .image(artist.images[0])
          .imageAspectRatio(2f)
          .build()

}