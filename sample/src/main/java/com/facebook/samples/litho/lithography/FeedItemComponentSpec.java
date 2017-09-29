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

package com.facebook.samples.litho.lithography;

import android.support.v7.widget.PagerSnapHelper;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

@LayoutSpec
public class FeedItemComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Artist artist,
      @Prop final RecyclerBinder binder) {
    return Column.create(c)
        .child(
            Column.create(c)
                .child(artist.images.length == 1 ?
                    SingleImageComponent.create(c)
                        .image(artist.images[0])
                        .imageAspectRatio(2)
                        .withLayout() :
                    Recycler.create(c)
                        .binder(binder)
                        .snapHelper(new PagerSnapHelper())
                        .withLayout()
                        .aspectRatio(2))
                .child(
                    TitleComponent.create(c)
                        .title(artist.name))
                .child(
                    ActionsComponent.create(c)))
        .child(
            FooterComponent.create(c)
                .text(artist.biography))
        .build();
  }
}
