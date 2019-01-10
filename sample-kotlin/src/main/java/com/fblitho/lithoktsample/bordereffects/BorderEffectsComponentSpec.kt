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

package com.fblitho.lithoktsample.bordereffects

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.Text
import java.lang.reflect.InvocationTargetException

@LayoutSpec
object BorderEffectsComponentSpec {

  private val componentsToBuild = listOf(
      AlternateColorBorder::class.java,
      AlternateWidthBorder::class.java,
      AlternateColorWidthBorder::class.java,
      RtlColorWidthBorder::class.java,
      DashPathEffectBorder::class.java,
      VerticalDashPathEffectBorder::class.java,
      AlternateColorPathEffectBorder::class.java,
      AlternateColorCornerPathEffectBorder::class.java,
      CompositePathEffectBorder::class.java,
      VaryingRadiiBorder::class.java)

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(
              DataDiffSection.create<Class<out Component>>(SectionContext(c))
                  .data(componentsToBuild)
                  .renderEventHandler(BorderEffectsComponent.onRender(c))
                  .build())
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Class<out Component>): RenderInfo {
    val component = try {
      val createMethod = model.getMethod("create", ComponentContext::class.java)
      val componentBuilder = createMethod.invoke(null, c) as Component.Builder<*>
      componentBuilder.build()
    } catch (ex: Exception) {
      val textComponent = Text.create(c).textSizeDip(32f).text(ex.localizedMessage).build()

      when (ex) {
        is NoSuchMethodException,
        is IllegalAccessException,
        is IllegalArgumentException,
        is InvocationTargetException -> textComponent
        else -> textComponent
      }
    }

    return ComponentRenderInfo.create()
        .component(component)
        .build()
  }
}
