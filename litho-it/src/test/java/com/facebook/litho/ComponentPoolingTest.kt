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
import android.view.View
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MountItemsPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentPoolingTest {

  private val context: Context = RuntimeEnvironment.application

  @Before
  fun setup() {
    MountItemsPool.clear()
  }

  @After
  fun cleanup() {
    MountItemsPool.clear()
  }

  @Test
  fun testMaybePreallocateContent() {
    val component = PooledComponent("PooledComponent")

    // Preallocate content more times than the defined pool size
    for (i in 0 until POOL_SIZE * 2) {
      MountItemsPool.maybePreallocateContent(context, component)
    }

    // Ensure onCreateMountContent was called POOL_SIZE times.
    assertThat(component.onCreateMountContentCount).isEqualTo(POOL_SIZE)

    // Acquire POOL_SIZE contents
    val objects = arrayOfNulls<Any>(POOL_SIZE + 1)
    for (i in 0 until POOL_SIZE) {
      objects[i] = MountItemsPool.acquireMountContent(context, component)
    }

    // Ensure onCreateMountContent wasn't called an additional time.
    assertThat(component.onCreateMountContentCount).isEqualTo(POOL_SIZE)

    // Acquire one more content
    objects[POOL_SIZE] = MountItemsPool.acquireMountContent(context, component)

    // Ensure onCreateMountContent was called an additional time.
    assertThat(component.onCreateMountContentCount).isEqualTo(POOL_SIZE + 1)

    // Release all acquired content
    for (`object` in objects) {
      MountItemsPool.release(context, component, `object`)
    }

    // Reacquire POOL_SIZE contents
    for (i in 0 until POOL_SIZE) {
      MountItemsPool.acquireMountContent(context, component)
    }

    // Ensure onCreateMountContent wasn't called an additional time.
    assertThat(component.onCreateMountContentCount).isEqualTo(POOL_SIZE + 1)
  }

  private class PooledComponent(simpleName: String) : SpecGeneratedComponent(simpleName) {
    var onCreateMountContentCount: Int = 0
      private set

    override fun poolSize(): Int = POOL_SIZE

    public override fun onCreateMountContent(context: Context): Any {
      onCreateMountContentCount++
      return View(context)
    }
  }

  companion object {
    private const val POOL_SIZE = 2
  }
}
