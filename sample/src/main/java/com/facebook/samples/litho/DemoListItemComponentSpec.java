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

package com.facebook.samples.litho;

import static com.facebook.yoga.YogaEdge.ALL;

import android.content.Intent;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class DemoListItemComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop final DemoListActivity.DemoListDataModel model) {
    return Column.create(c)
        .paddingDip(ALL, 16)
        .child(Text.create(c).text(model.name).textSizeSp(18).build())
        .clickHandler(DemoListItemComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @FromEvent View view,
      @Prop final DemoListActivity.DemoListDataModel model,
      @Prop final int[] currentIndices) {
    final Intent intent =
        new Intent(
            c.getAndroidContext(), model.datamodels == null ? model.klass : DemoListActivity.class);
    intent.putExtra(DemoListActivity.INDICES, currentIndices);
    c.getAndroidContext().startActivity(intent);
  }
}
