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

import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;

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
  public RenderInfo createComponent(ComponentContext c) {
    return ComponentRenderInfo.create()
        .component(FeedItemCard.create(c).artist(this).build())
        .build();
  }
}
