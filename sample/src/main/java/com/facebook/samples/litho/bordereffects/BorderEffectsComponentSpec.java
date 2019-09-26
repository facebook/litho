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

package com.facebook.samples.litho.bordereffects;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@LayoutSpec
class BorderEffectsComponentSpec {

  private static final List<Class<? extends Component>> componentsToBuild =
      Arrays.<Class<? extends Component>>asList(
          AlternateColorBorder.class,
          AlternateWidthBorder.class,
          AlternateColorWidthBorder.class,
          RtlColorWidthBorder.class,
          DashPathEffectBorder.class,
          VerticalDashPathEffectBorder.class,
          AlternateColorPathEffectBorder.class,
          AlternateColorCornerPathEffectBorder.class,
          CompositePathEffectBorder.class,
          VaryingRadiiBorder.class);

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            DataDiffSection.<Class<? extends Component>>create(new SectionContext(c))
                .data(componentsToBuild)
                .renderEventHandler(BorderEffectsComponent.onRender(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent Class<? extends Component> model) {

    try {
      Method createMethod = model.getMethod("create", ComponentContext.class);
      Component.Builder componentBuilder = (Component.Builder) createMethod.invoke(null, c);
      return ComponentRenderInfo.create().component(componentBuilder.build()).build();
    } catch (NoSuchMethodException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException ex) {
      return ComponentRenderInfo.create()
          .component(Text.create(c).textSizeDip(32).text(ex.getLocalizedMessage()).build())
          .build();
    }
  }
}
