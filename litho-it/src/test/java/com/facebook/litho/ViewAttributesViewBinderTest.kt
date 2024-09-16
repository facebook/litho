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
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.primitives.ViewAllocator
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViewAttributesViewBinderTest {

  @Test
  fun `bind sets the view attributes`() {
    val context = RuntimeEnvironment.getApplication() as Context

    val model =
        ViewAttributesViewBinder.Model(
            renderUnit = DummyRenderUnit(id = 1L),
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
            cloneStateListAnimators = false,
            isEventHandlerRedesignEnabled = true)

    val binder = ViewAttributesViewBinder.create(model)

    val view = ComponentHost(context, null)
    binder.bind(context, view, null)
    Assertions.assertThat(view.contentDescription).isEqualTo("my-content-description")
  }

  @Test
  fun `unbind resets previously set view attributes`() {
    val context = RuntimeEnvironment.getApplication() as Context

    val model =
        ViewAttributesViewBinder.Model(
            renderUnit = DummyRenderUnit(id = 1L),
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
            cloneStateListAnimators = false,
            isEventHandlerRedesignEnabled = true)

    val binder = ViewAttributesViewBinder.create(model)

    val view = ComponentHost(context, null)
    val bindData = binder.bind(context, view, null)
    Assertions.assertThat(view.contentDescription).isEqualTo("my-content-description")

    binder.unbind(context, view, null, bindData)
    Assertions.assertThat(view.contentDescription).isNull()
  }

  @Test
  fun `should update if view attributes are different`() {
    val firstBinder =
        ViewAttributesViewBinder.create(
            ViewAttributesViewBinder.Model(
                renderUnit = DummyRenderUnit(id = 1L),
                viewAttributes =
                    ViewAttributes().apply { contentDescription = "my-content-description" },
                isRootHost = false,
                cloneStateListAnimators = false,
                isEventHandlerRedesignEnabled = true))

    Assertions.assertThat(firstBinder.shouldUpdate(firstBinder, null, null)).isFalse()

    val secondBinder =
        ViewAttributesViewBinder.create(
            ViewAttributesViewBinder.Model(
                renderUnit = DummyRenderUnit(id = 1L),
                viewAttributes =
                    ViewAttributes().apply { contentDescription = "my-different-description" },
                isRootHost = false,
                cloneStateListAnimators = false,
                isEventHandlerRedesignEnabled = true))

    Assertions.assertThat(secondBinder.shouldUpdate(previous = firstBinder, null, null)).isTrue()
  }

  private class DummyRenderUnit(override val id: Long) :
      RenderUnit<ComponentHost>(RenderType.VIEW) {

    override val contentAllocator: ContentAllocator<ComponentHost>
      get() = ViewAllocator { ComponentHost(it, null) }
  }
}
