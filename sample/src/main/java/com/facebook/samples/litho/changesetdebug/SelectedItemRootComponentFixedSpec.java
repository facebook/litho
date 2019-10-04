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

package com.facebook.samples.litho.changesetdebug;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent;
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.List;

@LayoutSpec
public class SelectedItemRootComponentFixedSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop List<DataModel> dataModels) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            DataDiffSection.<DataModel>create(new SectionContext(c))
                .data(dataModels)
                .renderEventHandler(SelectedItemRootComponentFixed.onRender(c))
                .onCheckIsSameContentEventHandler(SelectedItemRootComponentFixed.isSameContent(c))
                .onCheckIsSameItemEventHandler(SelectedItemRootComponentFixed.isSameItem(c))
                .build())
        .flexGrow(1)
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent DataModel model) {
    return ComponentRenderInfo.create()
        .component(
            Row.create(c)
                .child(Text.create(c).text(model.getData()).textSizeDip(30))
                .child(FixedRowItem.create(c).favourited(model.isSelected()))
                .build())
        .build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  static boolean isSameItem(
      ComponentContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
    return previousItem.getId() == nextItem.getId();
  }

  @OnEvent(OnCheckIsSameContentEvent.class)
  static boolean isSameContent(
      ComponentContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
    return previousItem.getData().equals(nextItem.getData())
        && previousItem.isSelected() == nextItem.isSelected();
  }
}
