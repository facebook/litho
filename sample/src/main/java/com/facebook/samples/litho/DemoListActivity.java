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

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.animations.animatedbadge.AnimatedBadgeActivity;
import com.facebook.samples.litho.animations.animationcomposition.ComposedAnimationsActivity;
import com.facebook.samples.litho.animations.bounds.BoundsAnimationActivity;
import com.facebook.samples.litho.animations.expandableelement.ExpandableElementActivity;
import com.facebook.samples.litho.animations.pageindicators.PageIndicatorsActivity;
import com.facebook.samples.litho.animations.renderthread.RenderThreadAnimationActivity;
import com.facebook.samples.litho.bordereffects.BorderEffectsActivity;
import com.facebook.samples.litho.dynamicprops.DynamicPropsActivity;
import com.facebook.samples.litho.errors.ErrorHandlingActivity;
import com.facebook.samples.litho.fastscroll.FastScrollHandleActivity;
import com.facebook.samples.litho.hscroll.HorizontalScrollWithSnapActivity;
import com.facebook.samples.litho.lithography.LithographyActivity;
import com.facebook.samples.litho.playground.PlaygroundActivity;
import com.facebook.samples.litho.staticscroll.horizontalscroll.HorizontalScrollActivity;
import java.util.Arrays;
import java.util.List;

public class DemoListActivity extends NavigatableDemoActivity {

  static final String INDICES = "INDICES";
  static final List<DemoListDataModel> DATA_MODELS =
      Arrays.asList(
          new DemoListDataModel("Lithography", LithographyActivity.class),
          new DemoListDataModel("Playground", PlaygroundActivity.class),
          new DemoListDataModel("Border effects", BorderEffectsActivity.class),
          new DemoListDataModel("Error boundaries", ErrorHandlingActivity.class),
          new DemoListDataModel("HScroll with Snapping", HorizontalScrollWithSnapActivity.class),
          new DemoListDataModel(
              "Non-recycling scroll",
              Arrays.asList(
                  new DemoListDataModel("HorizontalScroll", HorizontalScrollActivity.class))),
          new DemoListDataModel(
              "Animations",
              Arrays.asList(
                  new DemoListDataModel("Animations Composition", ComposedAnimationsActivity.class),
                  new DemoListDataModel("Expandable Element", ExpandableElementActivity.class),
                  new DemoListDataModel("Animated Badge", AnimatedBadgeActivity.class),
                  new DemoListDataModel("Bounds Animation", BoundsAnimationActivity.class),
                  new DemoListDataModel("Page Indicators", PageIndicatorsActivity.class),
                  new DemoListDataModel("Render Thread", RenderThreadAnimationActivity.class))),
          new DemoListDataModel("Dynamic Props", DynamicPropsActivity.class),
          new DemoListDataModel("Fast Scroll Handle", FastScrollHandleActivity.class));

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final int[] indices = getIntent().getIntArrayExtra(INDICES);
    final List<DemoListDataModel> dataModels = getDataModels(indices);

    final ComponentContext componentContext = new ComponentContext(this);
    setContentView(
        LithoView.create(
            this,
            DemoListComponent.create(componentContext)
                .dataModels(dataModels)
                .parentIndices(indices)
                .build()));
  }

  private List<DemoListDataModel> getDataModels(@Nullable int[] indices) {
    List<DemoListDataModel> dataModels = DATA_MODELS;
    if (indices == null) {
      return dataModels;
    }

    for (int i = 0; i < indices.length; i++) {
      dataModels = dataModels.get(indices[i]).datamodels;
    }
    return dataModels;
  }

  static final class DemoListDataModel {
    final String name;
    @Nullable final Class klass;
    @Nullable final List<DemoListDataModel> datamodels;

    DemoListDataModel(String name, Class klass) {
      this.name = name;
      this.klass = klass;
      this.datamodels = null;
    }

    DemoListDataModel(String name, List<DemoListDataModel> datamodels) {
      this.name = name;
      this.datamodels = datamodels;
      this.klass = null;
    }
  }
}
