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

import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Tests for [ComponentTree.Builder] */
@RunWith(LithoTestRunner::class)
class ComponentTreeBuilderTest {

  private lateinit var looper: Looper
  private lateinit var componentsLogger: ComponentsLogger
  private lateinit var context: ComponentContext
  private lateinit var root: Component
  private lateinit var componentTreeBuilder: ComponentTree.Builder

  @Before
  fun setup() {
    looper = mock()
    componentsLogger = mock()
    context =
        ComponentContext(ApplicationProvider.getApplicationContext(), LOG_TAG, componentsLogger)
    root = TestLayoutComponent.create(context).build()
    componentTreeBuilder = ComponentTree.create(context, root)
  }

  @Test
  fun testDefaultCreation() {
    val componentTree = componentTreeBuilder.build()
    assertSameAsInternalState(componentTree, root, "mRoot")
    assertDefaults(componentTree)
  }

  @Test
  fun testCreationWithInputs() {
    val componentTree = componentTreeBuilder.layoutThreadLooper(looper).build()
    assertSameAsInternalState(componentTree, root, "mRoot")
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled")
    assertThat(componentTree.isIncrementalMountEnabled).isTrue
    assertThat(context.logger).isEqualTo(componentsLogger)
    assertThat(context.logTag).isEqualTo(LOG_TAG)
    val handler = Whitebox.getInternalState<Handler>(componentTree, "mLayoutThreadHandler")
    assertThat(looper).isSameAs(handler.looper)
  }

  companion object {
    private const val LOG_TAG = "logTag"

    @JvmStatic
    private fun assertSameAsInternalState(
        componentTree: ComponentTree,
        obj: Any?,
        internalName: String
    ) {
      assertThat(obj).isSameAs(Whitebox.getInternalState(componentTree, internalName))
    }

    @JvmStatic
    private fun assertEqualToInternalState(
        componentTree: ComponentTree,
        obj: Any,
        internalName: String
    ) {
      assertThat(Whitebox.getInternalState(componentTree, internalName) as Any).isEqualTo(obj)
    }

    @JvmStatic
    private fun assertDefaults(componentTree: ComponentTree) {
      assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled")
      assertThat(componentTree.isIncrementalMountEnabled).isTrue
    }
  }
}
