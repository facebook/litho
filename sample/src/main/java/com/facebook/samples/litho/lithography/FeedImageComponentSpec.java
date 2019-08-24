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

import static androidx.recyclerview.widget.LinearSmoothScroller.SNAP_TO_START;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.fresco.FrescoImage;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.Arrays;

@LayoutSpec
public class FeedImageComponentSpec {

  private static final RecyclerConfiguration LIST_CONFIGURATION =
      new ListRecyclerConfiguration(
          LinearLayoutManager.HORIZONTAL, /*reverseLayout*/ false, SNAP_TO_START);

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop final String[] images) {
    return images.length == 1
        ? createImageComponent(c, images[0]).build()
        : RecyclerCollectionComponent.create(c)
            .disablePTR(true)
            .recyclerConfiguration(LIST_CONFIGURATION)
            .section(
                DataDiffSection.<String>create(new SectionContext(c))
                    .data(Arrays.asList(images))
                    .renderEventHandler(FeedImageComponent.onRender(c))
                    .build())
            .canMeasureRecycler(true)
            .aspectRatio(2)
            .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent String model) {
    return ComponentRenderInfo.create().component(createImageComponent(c, model).build()).build();
  }

  private static Component.Builder createImageComponent(ComponentContext c, String image) {
    final DraweeController controller = Fresco.newDraweeControllerBuilder().setUri(image).build();

    return FrescoImage.create(c).controller(controller).imageAspectRatio(2f);
  }
}
