/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.core.height
import com.facebook.litho.core.orDefault
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.kotlin.widget.Text as KText
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.CardClip
import com.facebook.litho.widget.Text
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class ComponentTreePropWithReconciliationTest {

  private lateinit var c: ComponentContext

  @Before
  fun setup() {
    val componentsReporter = TestComponentsReporter()
    c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    ComponentsReporter.provide(componentsReporter)
  }

  @Test
  fun test() {
    val component = TreePropTestContainerComponent()
    getLithoView(component)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(c)
    val componentTree = ComponentTree.create(c, component).enableChildClipping(true).build()
    lithoView.componentTree = componentTree
    lithoView.measure(unspecified(640), unspecified(480))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  private class ChildComponentWithStateUpdate : KComponent() {
    override fun ComponentScope.render(): Component {
      val countState = useState<Int> { 0 }
      if (countState.value == 0) {
        countState.updateSync { it + 1 }
      }
      return Column {
        child(
            Text.create(context)
                .text("click to dispatch")
                .textSizeSp(18f)
                .paddingDip(YogaEdge.ALL, 16f)
                .build(),
        )
        child(Text.create(context).textSizeSp(18f).text(countState.value.toString()).build())
      }
    }
  }

  private class NestedTreeChildComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val a = getTreeProp<RootComponentWithTreeProps.A>()
      val b = getTreeProp<NestedTreeParentComponent.B>()
      val c = getTreeProp<NestedTreeComponent.C>()
      checkNotNull(a) { "A is null" }
      checkNotNull(b) { "B is null" }
      checkNotNull(c) { "C is null" }
      return key("Column") {
        Column { child(key("CardClip") { CardClip(style = Style.width(10.dp).height(10.dp)) }) }
      }
    }
  }

  private class NestedTreeComponent(private val style: Style = Style) : KComponent() {
    override fun ComponentScope.render(): Component {
      return RenderWithConstraints(style) { constraints ->
        val props = getTreeProp<ExtraProps>()
        props?.steps?.add(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
        val style = props?.direction?.let { Style.layoutDirection(it) }.orDefault()
        val row =
            key("Row") {
              Row(style = style) {
                child(key("NestedTreeChildComponent") { NestedTreeChildComponent() })
              }
            }
        TreePropProvider(legacyTreePropOf<C>() to C()) { row }
      }
    }

    class C

    class ExtraProps {
      @JvmField var steps: MutableList<LifecycleStep>? = null
      @JvmField var direction: YogaDirection? = null
    }
  }

  private class NestedTreeParentComponent(private val style: Style = Style) : KComponent() {
    companion object {
      const val ROOT_KEY = "NestedTreeParentComponent"
    }

    override fun ComponentScope.render(): Component {
      return TreePropProvider(legacyTreePropOf<B>() to B()) {
        key("NestedTreeComponent") {
          NestedTreeComponent(style = Style.padding(all = 5.px) + style)
        }
      }
    }

    class B
  }

  private class RootComponentWithTreeProps(
      private val shouldNotUpdateState: Boolean = false,
      private val style: Style = Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      return TreePropProvider(legacyTreePropOf<A>() to A()) {
        key("Row") {
          Row(style = style) {
            child(
                if (shouldNotUpdateState) KText("hello world") else ChildComponentWithStateUpdate(),
            )
            child(
                key("NestedTreeParentComponent") {
                  NestedTreeParentComponent(style = Style.flex(grow = 1f))
                },
            )
          }
        }
      }
    }

    class A
  }

  private class TreePropTestContainerComponent : KComponent() {
    companion object {
      const val KEY = "TreePropTestContainerComponent"
      const val EXPECTED_GLOBAL_KEY =
          ("$${KEY},\$RootComponentWithTreeProps,\$Row,\$NestedTreeParentComponent,\$NestedTreeComponent,\$Row,\$NestedTreeChildComponent")
    }

    override fun ComponentScope.render(): Component {
      return key("RootComponentWithTreeProps") { RootComponentWithTreeProps() }
    }
  }
}
