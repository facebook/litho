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

import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class TreePropsTest {
  @JvmField @Rule val lithoTestRule: LithoViewRule = LithoViewRule()

  val TestAnyTreeProp: TreeProp<Any> = treePropOf { "string" }
  val TestIntTreeProp: TreeProp<Int> = treePropOf { 0 }

  private val parentTreeProps: TreePropContainer = TreePropContainer()
  private val parentContext: ComponentContext by lazy {
    parentTreeProps.put(TestIntTreeProp, TestIntTreeProp.defaultValue)
    parentTreeProps.put(TestAnyTreeProp, TestAnyTreeProp.defaultValue)
    ComponentContext(lithoTestRule.context, parentTreeProps)
  }

  @Test
  fun `when ComponentTree is created with TreeProps then component should receive tree props`() {
    val tree = ComponentTree.create(parentContext).build()
    val assertion = AtomicReference<ComponentScope.() -> Unit>()
    val component = TestComponent { assertion.get().invoke(this) }

    var didRun = false
    assertion.set {
      didRun = true
      assertThat(TestIntTreeProp.value).isEqualTo(TestIntTreeProp.defaultValue)
      assertThat(TestAnyTreeProp.value).isEqualTo(TestAnyTreeProp.defaultValue)
    }
    lithoTestRule.render(componentTree = tree) { component }
    assertThat(didRun).describedAs("assertions did not run").isTrue
  }

  @Test
  fun `when TreeProps change then component should resolve again and receive new tree props`() {

    // Override properties with local variables for the test
    val parentTreeProps = TreePropContainer()
    parentTreeProps.put(TestIntTreeProp, 1)
    parentTreeProps.put(TestAnyTreeProp, "not-string")
    val parentContext = ComponentContext(lithoTestRule.context, parentTreeProps)

    val tree = ComponentTree.create(parentContext).build()
    val assertion = AtomicReference<ComponentScope.() -> Unit>()
    val component = TestComponent { assertion.get().invoke(this) }

    var didRun = false
    assertion.set {
      didRun = true
      assertThat(TestIntTreeProp.value).isEqualTo(1)
      assertThat(TestAnyTreeProp.value).isEqualTo("not-string")
    }
    lithoTestRule.render(componentTree = tree) { component }
    assertThat(didRun).describedAs("assertions did not run").isTrue

    didRun = false
    assertion.set {
      didRun = true
      assertThat(TestIntTreeProp.value).isEqualTo(2)
      /* TODO(T192860025): This will fail because the tree prop input will override component tree tree props */
      // assertThat(TestAnyTreeProp.value).isEqualTo("not-string")
    }

    // just to set tree props
    val treeprops = TreePropContainer()
    treeprops.put(TestIntTreeProp, 2)
    tree.setRootAndSizeSpecSync(
        component,
        /* TODO(T192860025): This is required because LayoutState::isCompatibleComponentAndSpec doesn't compare tree props */
        exactly(100),
        exactly(100),
        Size(),
        treeprops)

    assertThat(didRun).describedAs("resolves the component again").isTrue
  }

  @Test
  fun `when TreeProps do not change then component should not resolve again`() {
    val tree = ComponentTree.create(parentContext).build()
    val assertion = AtomicReference<ComponentScope.() -> Unit>()
    val component = TestComponent { assertion.get().invoke(this) }

    var didRun = false
    assertion.set {
      didRun = true
      assertThat(TestIntTreeProp.value).isEqualTo(TestIntTreeProp.defaultValue)
      assertThat(TestAnyTreeProp.value).isEqualTo(TestAnyTreeProp.defaultValue)
    }
    lithoTestRule.render(componentTree = tree) { component }
    assertThat(didRun).describedAs("assertions did not run").isTrue

    didRun = false
    assertion.set { didRun = true }

    // for same tree prop
    lithoTestRule.render(componentTree = tree, widthPx = 100, heightPx = 100) { component }

    assertThat(didRun).describedAs("does not resolve the component again").isFalse()

    // for equivalent tree props
    val treeprops = TreePropContainer()
    treeprops.put(TestIntTreeProp, TestIntTreeProp.defaultValue)
    treeprops.put(TestAnyTreeProp, TestAnyTreeProp.defaultValue)
    tree.setRootAndSizeSpecSync(
        component,
        /* TODO(T192860025): This is required because LayoutState::isCompatibleComponentAndSpec doesn't compare tree props */
        exactly(200),
        exactly(200),
        Size(),
        treeprops)

    assertThat(didRun).describedAs("does not resolve the component again").isFalse()
  }

  private class TestComponent(val onRender: ComponentScope.() -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component {
      onRender()
      return Text(text = "Hello World")
    }
  }
}
