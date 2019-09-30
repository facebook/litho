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

package com.facebook.samples.litho.errors;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Card;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class ListRowComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop ListRow row) {
    return Column.create(c)
        .paddingDip(YogaEdge.VERTICAL, 8)
        .paddingDip(YogaEdge.HORIZONTAL, 32)
        .child(
            Card.create(c)
                .content(
                    Column.create(c)
                        .marginDip(YogaEdge.ALL, 32)
                        .child(TitleComponent.create(c).title(row.title))
                        .child(PossiblyCrashingSubTitleComponent.create(c).subtitle(row.subtitle))
                        .build())
                .build())
        .build();
  }
}
