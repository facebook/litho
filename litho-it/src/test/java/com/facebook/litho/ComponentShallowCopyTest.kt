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
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentShallowCopyTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testShallowCopyCachedLayoutSameLayoutState() {
    context =
        ComponentContextUtils.withComponentTree(context, ComponentTree.create(context).build())
    val c = ComponentContext(context)
    val resolveContext = c.setRenderStateContextForTests()
    val resultCache = resolveContext.cache
    val component = SimpleMountSpecTester.create(context).build()
    component.measure(c, 100, 100, Size())
    assertThat(resultCache.getCachedResult(component)).isNotNull
    val copyComponent = component.makeShallowCopy()
    assertThat(resultCache.getCachedResult(copyComponent)).isNotNull
    assertThat(resultCache.getCachedResult(component))
        .isEqualTo(resultCache.getCachedResult(copyComponent))
  }

  @Test
  fun testShallowCopyCachedLayoutOtherLayoutStateCacheLayoutState() {
    context =
        ComponentContextUtils.withComponentTree(context, ComponentTree.create(context).build())
    val c1 = ComponentContextUtils.withComponentTree(context, ComponentTree.create(context).build())
    val c2 = ComponentContextUtils.withComponentTree(context, ComponentTree.create(context).build())
    val rsc1 = c1.setRenderStateContextForTests()
    val rsc2 = c2.setRenderStateContextForTests()
    val resultCache1 = rsc1.cache
    val resultCache2 = rsc2.cache
    val component = SimpleMountSpecTester.create(context).build()
    component.measure(c1, 100, 100, Size())
    assertThat(resultCache1.getCachedResult(component)).isNotNull
    val copyComponent = component.makeShallowCopy()
    assertThat(resultCache2.getCachedResult(copyComponent)).isNull()
  }

  @Test
  fun shallowCopy_withManualKey_preservesManualKeyInformation() {
    val component = Text.create(context).text("test").key("manual_key").build() as Component
    assertThat(component.key).isEqualTo("manual_key")
    assertThat(component.hasManualKey()).isTrue
    val shallowCopy = component.makeShallowCopy()
    assertThat(shallowCopy.key).isEqualTo("manual_key")
    assertThat(shallowCopy.hasManualKey()).isTrue
  }
}
