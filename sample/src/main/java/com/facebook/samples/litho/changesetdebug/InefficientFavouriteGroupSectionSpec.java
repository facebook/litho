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

import com.facebook.litho.Row;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.List;

@GroupSectionSpec
public class InefficientFavouriteGroupSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List<DataModel> dataModels) {
    return Children.create()
        .child(
            DataDiffSection.<DataModel>create(new SectionContext(c))
                .data(dataModels)
                .renderEventHandler(InefficientFavouriteGroupSection.onRender(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(SectionContext c, @FromEvent DataModel model) {
    return ComponentRenderInfo.create()
        .component(
            Row.create(c)
                .child(Text.create(c).text(model.getData()).textSizeDip(30))
                .child(RowItem.create(c))
                .build())
        .build();
  }
}
