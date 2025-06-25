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

package com.facebook.litho.effects

import android.util.SparseArray
import android.view.View
import com.facebook.litho.Column
import com.facebook.litho.EmptyComponent
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.onCleanup
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTags
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class TestEffectsObserver {

  val runs = mutableListOf<String>()
  val cleanups = mutableListOf<String>()

  fun clear() {
    runs.clear()
    cleanups.clear()
  }
}

private enum class RenderScope {
  RENDER,
  LAYOUT,
}

private class TestPrimitive(
    val observer: TestEffectsObserver,
    val deps: (RenderScope) -> Any = { Unit },
    val update: (Any) -> Any = {},
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val state = useState { Any() }

    useEffect(deps(RenderScope.RENDER)) {
      observer.runs.add("useEffect")
      onCleanup { observer.cleanups.add("useEffect") }
    }

    useEffect(Unit) {
      observer.runs.add("useEffect")
      onCleanup { observer.cleanups.add("useEffect") }
    }

    val tags = SparseArray<Any>().apply { put(com.facebook.litho.it.R.id.test_tag, state.value) }

    return LithoPrimitive(
        layoutBehavior = TestLayoutBehavior(observer = observer, deps = deps(RenderScope.LAYOUT)),
        mountBehavior = TestMountBehavior(state.value),
        style = Style.viewTags(tags).onClick { state.update { update(it) } })
  }
}

private class TestLayoutBehavior(
    val observer: TestEffectsObserver,
    val deps: Any,
) : LayoutBehavior {
  override fun LayoutScope.layout(constraints: SizeConstraints): PrimitiveLayoutResult {
    useEffect(deps) {
      observer.runs.add("useLayoutEffect")
      onCleanup { observer.cleanups.add("useLayoutEffect") }
    }
    return PrimitiveLayoutResult(width = 100, height = 100, layoutData = null)
  }
}

fun PrimitiveComponentScope.TestMountBehavior(state: Any): MountBehavior<View> {
  return MountBehavior<View>(
      ViewAllocator { context -> View(context).apply { tag = "test-view" } },
  ) {}
}

@RunWith(LithoTestRunner::class)
class LayoutEffectsTest {

  @Rule @JvmField val lithoViewRule = LithoTestRule()

  @Test
  fun `when layout effects is declared then effects should run`() {
    val observer = TestEffectsObserver()
    val component = TestPrimitive(observer = observer)

    val handle = lithoViewRule.render { component }

    handle.findViewWithTag("test-view")

    assertThat(observer.runs).containsExactly("useLayoutEffect", "useEffect")
    assertThat(observer.cleanups).isEmpty()
    observer.clear()

    lithoViewRule.render(lithoView = handle.lithoView) { EmptyComponent() }

    assertThat(observer.runs).isEmpty()
    assertThat(observer.cleanups).containsExactly("useLayoutEffect", "useEffect")
  }

  @Test
  fun `when layout effects deps don't change then effects should not run`() {
    val observer = TestEffectsObserver()
    val update: (Any) -> Any = { "state-updated" }
    val component = TestPrimitive(observer = observer, update = update)

    val handle = lithoViewRule.render { component }

    val view = handle.findViewWithTag("test-view")

    assertThat(observer.runs).containsExactly("useLayoutEffect", "useEffect")
    assertThat(observer.cleanups).isEmpty()
    observer.clear()

    lithoViewRule.act(handle) { clickOnTag("test-view") }

    assertThat(view.getTag(com.facebook.litho.it.R.id.test_tag)).isEqualTo("state-updated")
    assertThat(observer.runs).isEmpty()
    assertThat(observer.cleanups).isEmpty()
  }

  @Test
  fun `when layout effects deps change then effects should run`() {
    val observer = TestEffectsObserver()
    val update: (Any) -> Any = { "state-updated" }
    var dep = Any()
    val deps: (RenderScope) -> Any = {
      when (it) {
        RenderScope.RENDER -> Unit
        RenderScope.LAYOUT -> dep
      }
    }
    val component = TestPrimitive(observer = observer, deps = deps, update = update)

    val handle = lithoViewRule.render { component }

    val view = handle.findViewWithTag("test-view")

    assertThat(observer.runs).containsExactly("useLayoutEffect", "useEffect")
    assertThat(observer.cleanups).isEmpty()
    observer.clear()

    dep = Any() // change the dependency for the next render

    lithoViewRule.act(handle) { clickOnTag("test-view") }

    assertThat(view.getTag(com.facebook.litho.it.R.id.test_tag)).isEqualTo("state-updated")
    assertThat(observer.runs).containsExactly("useLayoutEffect")
    assertThat(observer.cleanups).containsExactly("useLayoutEffect")
  }

  @Test
  fun `when the primitive is re-rendered then effects should not run`() {
    val observer = TestEffectsObserver()

    val handle = lithoViewRule.render { Column { child(TestPrimitive(observer = observer)) } }

    assertThat(observer.runs).containsExactly("useLayoutEffect", "useEffect")
    assertThat(observer.cleanups).isEmpty()
    observer.clear()

    lithoViewRule.render(handle.lithoView) { Column { child(TestPrimitive(observer = observer)) } }

    assertThat(observer.runs).isEmpty()
    assertThat(observer.cleanups).isEmpty()
  }
}
