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

    val view = ComponentHost(context, null)
    ViewAttributesViewBinder.bind(context, view, model, null)
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

    val view = ComponentHost(context, null)
    val bindData = ViewAttributesViewBinder.bind(context, view, model, null)
    Assertions.assertThat(view.contentDescription).isEqualTo("my-content-description")

    ViewAttributesViewBinder.unbind(context, view, model, null, bindData)
    Assertions.assertThat(view.contentDescription).isNull()
  }

  @Test
  fun `should update if view attributes are different`() {
    val firstModel =
        ViewAttributesViewBinder.Model(
            renderUnit = DummyRenderUnit(id = 1L),
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
            cloneStateListAnimators = false,
            isEventHandlerRedesignEnabled = true)

    Assertions.assertThat(ViewAttributesViewBinder.shouldUpdate(firstModel, firstModel, null, null))
        .isFalse()

    val secondModel =
        ViewAttributesViewBinder.Model(
            renderUnit = DummyRenderUnit(id = 1L),
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-different-description" },
            isRootHost = false,
            cloneStateListAnimators = false,
            isEventHandlerRedesignEnabled = true)

    Assertions.assertThat(
            ViewAttributesViewBinder.shouldUpdate(firstModel, secondModel, null, null))
        .isTrue()
  }

  private class DummyRenderUnit(override val id: Long) :
      RenderUnit<ComponentHost>(RenderType.VIEW) {

    override val contentAllocator: ContentAllocator<ComponentHost>
      get() = ViewAllocator { ComponentHost(it, null) }
  }
}
