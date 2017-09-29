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

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;

public class Artist implements Datum {

  public final String name;
  public final String biography;
  public final String[] images;
  public final int year;

  public Artist(String name, String biography, int year, String... images) {
    this.name = name;
    this.biography = biography;
    this.year = year;
    this.images = images;
  }

  @Override
  public Component createComponent(ComponentContext c) {
    final RecyclerBinder imageRecyclerBinder = new RecyclerBinder.Builder()
        .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.HORIZONTAL, false))
        .build(c);

    for (String image : images) {
      ComponentRenderInfo.Builder imageRenderInfoBuilder = ComponentRenderInfo.create();
      imageRenderInfoBuilder.component(
          SingleImageComponent.create(c).image(image).imageAspectRatio(2f).build());
      imageRecyclerBinder.insertItemAt(
          imageRecyclerBinder.getItemCount(),
          imageRenderInfoBuilder.build());
    }
    return FeedItemCard.create(c)
        .artist(this)
        .binder(imageRecyclerBinder)
        .build();
  }
}
