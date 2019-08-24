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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class DecadeSeparatorSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop final Decade decade) {
    return Row.create(c)
        .alignItems(YogaAlign.CENTER)
        .paddingDip(YogaEdge.ALL, 16)
        .child(Row.create(c).heightPx(1).backgroundColor(0xFFAAAAAA).flex(1))
        .child(
            Text.create(c)
                .text(String.valueOf(decade.year))
                .textSizeDip(14)
                .textColor(0xFFAAAAAA)
                .marginDip(YogaEdge.HORIZONTAL, 10)
                .flex(0))
        .child(Row.create(c).heightPx(1).backgroundColor(0xFFAAAAAA).flex(1))
        .backgroundColor(0xFFFAFAFA)
        .build();
  }
}
