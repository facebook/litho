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

// The no-deps overloads under test are deprecated with DeprecationLevel.ERROR on
// purpose: they fail fast with a guidance message. Suppressing DEPRECATION_ERROR
// for this file is what allows verifying that fail-fast contract.
@file:Suppress("DEPRECATION_ERROR")

package com.facebook.litho.binders

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.RenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BinderStylesTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  @Test
  fun onBind_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBind { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBindWithDescription_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBindWithDescription("binder") { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBindView_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBindView { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBindViewWithDescription_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBindViewWithDescription("binder") { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBindHostView_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBindHostView { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBindHostViewWithDescription_withoutDeps_throwsGuidanceError() {
    assertThatThrownBy { Style.onBindHostViewWithDescription("binder") { onUnbind {} } }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage(ON_BIND_NO_DEPS_ERROR)
  }

  @Test
  fun onBind_appendsMountBinder() {
    val style = Style.onBind(Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("MOUNT_BINDER")
    val delegate = items[0].value as RenderUnit.DelegateBinder<*, *, *>
    assertThat(delegate.binder.description).startsWith("binder:")
  }

  @Test
  fun onBind_withNullDep_appendsBinder() {
    val style = Style.onBind(null) { onUnbind {} }

    assertThat(styleItemsOf(style)).hasSize(1)
  }

  @Test
  fun onBindWithDescription_usesCustomDescription() {
    val style = Style.onBindWithDescription({ "test-binder" }, Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("MOUNT_BINDER")
    val delegate = items[0].value as RenderUnit.DelegateBinder<*, *, *>
    assertThat(delegate.binder.description).isEqualTo("binder:test-binder")
  }

  @Test
  fun onBindView_appendsViewMountBinder() {
    val style = Style.onBindView(Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("VIEW_MOUNT_BINDER")
    val delegate = items[0].value as RenderUnit.DelegateBinder<*, *, *>
    assertThat(delegate.binder.description).startsWith("binder:")
  }

  @Test
  fun onBindHostView_appendsDeferredHostViewMountBinder() {
    val style = Style.onBindHostView(Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("HOST_VIEW_MOUNT_BINDER")
    // Host-view binders defer binder creation until mount, so unlike the mount and
    // view-mount variants the stored value is not an immediate DelegateBinder.
    assertThat(items[0].value).isNotNull()
    assertThat(items[0].value).isNotInstanceOf(RenderUnit.DelegateBinder::class.java)
  }

  @Test
  fun onBindHostViewWithDescription_appendsDeferredHostViewMountBinder() {
    val style = Style.onBindHostViewWithDescription({ "test-binder" }, Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("HOST_VIEW_MOUNT_BINDER")
    assertThat(items[0].value).isNotNull()
  }

  @Test
  fun viewBinder_withModel_wrapsBinderForViewMount() {
    val fake = FakeViewBinder<String>()
    val style = Style.viewBinder(fake, "model")

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("VIEW_MOUNT_BINDER")
    val delegate = items[0].value as RenderUnit.DelegateBinder<*, *, *>
    assertThat(delegate.binder).isSameAs(fake)
  }

  @Test
  fun viewBinder_withoutModel_wrapsBinderForViewMount() {
    val fake = FakeViewBinder<Unit>()
    val style = Style.viewBinder(fake)

    val items = styleItemsOf(style)
    assertThat(items).hasSize(1)
    assertThat(items[0].field.toString()).isEqualTo("VIEW_MOUNT_BINDER")
    val delegate = items[0].value as RenderUnit.DelegateBinder<*, *, *>
    assertThat(delegate.binder).isSameAs(fake)
  }

  @Test
  fun chainedBinders_preserveOrder() {
    val style = Style.onBind(Unit) { onUnbind {} }.onBindView(Unit) { onUnbind {} }

    val items = styleItemsOf(style)
    assertThat(items).hasSize(2)
    assertThat(items[0].field.toString()).isEqualTo("MOUNT_BINDER")
    assertThat(items[1].field.toString()).isEqualTo("VIEW_MOUNT_BINDER")
  }

  @Test
  fun onBindHostView_invokesBindAndUnbindOnContent() {
    var boundContent: Any? = null
    var unbindCalls = 0
    val handle = lithoTestRule.render {
      HostBindableComponent(
          style =
              Style.onBindHostView(Unit) { content ->
                boundContent = content
                onUnbind { unbindCalls++ }
              },
      )
    }

    assertThat(boundContent).isNotNull()
    assertThat(boundContent).isInstanceOf(View::class.java)
    assertThat(unbindCalls).isEqualTo(0)

    handle.lithoView.unmountAllItems()

    assertThat(unbindCalls).isEqualTo(1)
  }

  @Test
  fun onBindHostView_rebindsWhenDepsChange() {
    var bindCalls = 0
    var unbindCalls = 0

    class DepsComponent(val dep: () -> Any?) : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { 0 }
        return Column {
          child(
              Text(
                  "Count: ${state.value}",
                  style =
                      Style.onBindHostView(dep()) {
                        bindCalls++
                        onUnbind { unbindCalls++ }
                      },
              ),
          )
          child(Text("Tap", Style.onClick { state.update { it + 1 } }))
        }
      }
    }

    var dep: Any? = Unit
    val handle = lithoTestRule.render { DepsComponent { dep } }

    assertThat(bindCalls).isEqualTo(1)

    lithoTestRule.act(handle) { clickOnText("Tap") }
    assertThat(bindCalls).isEqualTo(1)
    assertThat(unbindCalls).isEqualTo(0)

    dep = Any()
    lithoTestRule.act(handle) { clickOnText("Tap") }
    assertThat(bindCalls).isEqualTo(2)
    assertThat(unbindCalls).isEqualTo(1)
  }

  private fun styleItemsOf(style: Style): List<StyleItem<*>> {
    val items = mutableListOf<StyleItem<*>>()
    style.forEach { items.add(it) }
    return items
  }

  private class FakeViewBinder<ModelT> : RenderUnit.Binder<ModelT, View, Any> {
    override fun shouldUpdate(
        currentModel: ModelT,
        newModel: ModelT,
        currentLayoutData: Any?,
        nextLayoutData: Any?,
    ): Boolean = false
  }

  private class HostBindableComponent(private val style: Style) : KComponent() {
    override fun ComponentScope.render(): Component {
      return Column { child(Text(text = "Hello", style = style)) }
    }
  }
}
