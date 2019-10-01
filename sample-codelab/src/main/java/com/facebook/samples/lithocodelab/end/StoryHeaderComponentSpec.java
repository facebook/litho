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

package com.facebook.samples.lithocodelab.end;

import static android.widget.Toast.LENGTH_SHORT;
import static com.facebook.samples.lithocodelab.end.StoryCardComponentSpec.CARD_INSET;
import static com.facebook.samples.lithocodelab.end.StoryCardComponentSpec.CARD_INTERNAL_PADDING;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;

import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.fresco.FrescoImage;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import com.facebook.samples.lithocodelab.R;

/**
 * Renders a "story header" with a grey box representing an image for the author, a title, subtitle,
 * and a menu button which just Toasts to indicate that the menu button was pressed.
 */
@LayoutSpec
class StoryHeaderComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String title, @Prop String subtitle) {
    return Row.create(c)
        .paddingDip(HORIZONTAL, CARD_INSET)
        .paddingDip(TOP, CARD_INSET)
        .child(
            FrescoImage.create(c)
                .controller(
                    Fresco.newDraweeControllerBuilder()
                        .setUri("http://placekitten.com/g/200/200")
                        .build())
                .widthDip(40)
                .heightDip(40)
                .marginDip(END, CARD_INTERNAL_PADDING)
                .marginDip(BOTTOM, CARD_INTERNAL_PADDING))
        .child(
            Column.create(c)
                .flexGrow(1f)
                .child(
                    Text.create(c, 0, R.style.header_title)
                        .text(title)
                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING))
                .child(
                    Text.create(c, 0, R.style.header_subtitle)
                        .text(subtitle)
                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING)))
        .child(
            Image.create(c)
                .drawableRes(R.drawable.menu)
                .clickHandler(StoryHeaderComponent.onClickMenuButton(c))
                .widthDip(15)
                .heightDip(15)
                .marginDip(START, CARD_INTERNAL_PADDING)
                .marginDip(BOTTOM, CARD_INTERNAL_PADDING))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickMenuButton(ComponentContext c) {
    Toast.makeText(c.getApplicationContext(), "Menu button clicked.", LENGTH_SHORT).show();
  }
}
