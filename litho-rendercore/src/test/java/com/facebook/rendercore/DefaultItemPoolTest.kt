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

package com.facebook.rendercore

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultItemPoolTest {

  @Test
  fun `acquire - return null if pool is empty`() {
    val pool = MountItemsPool.DefaultItemPool(this::class.java, 5, false)
    val contentAllocator = FakeContentAllocator()

    val content = pool.acquire(contentAllocator)
    Assertions.assertThat(content).isNull()
  }

  @Test
  fun `acquire - should return content if pool is not empty`() {
    val pool = MountItemsPool.DefaultItemPool(this::class.java, 5, false)
    val contentAllocator = FakeContentAllocator()

    val firstPooledContent = pool.acquire(contentAllocator)
    Assertions.assertThat(firstPooledContent).isNull()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val view = TextView(context)
    pool.release(view)

    val secondPooledContent = pool.acquire(contentAllocator)
    Assertions.assertThat(secondPooledContent).isEqualTo(view)
  }

  @Test
  fun `release - should be able to release until pool is full`() {
    val pool = MountItemsPool.DefaultItemPool(this::class.java, 1, false)
    val contentAllocator = FakeContentAllocator()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val view = TextView(context)
    Assertions.assertThat(pool.release(view)).isTrue

    val secondView = TextView(context)
    Assertions.assertThat(pool.release(secondView)).isFalse

    val pooledContent = pool.acquire(contentAllocator)
    Assertions.assertThat(pooledContent).isEqualTo(view)
  }

  @Test
  fun `maybePreallocate - should only pre-allocate if has space`() {
    val pool = MountItemsPool.DefaultItemPool(this::class.java, 1, false)
    val contentAllocator = FakeContentAllocator()

    val context = ApplicationProvider.getApplicationContext<Context>()

    Assertions.assertThat(pool.maybePreallocateContent(context, contentAllocator)).isTrue
    Assertions.assertThat(contentAllocator.numberAllocations).isEqualTo(1)

    Assertions.assertThat(pool.maybePreallocateContent(context, contentAllocator)).isFalse
    Assertions.assertThat(contentAllocator.numberAllocations).isEqualTo(1)
  }

  private class FakeContentAllocator : ContentAllocator<TextView> {

    var numberAllocations: Int = 0

    override fun createContent(context: Context): TextView =
        TextView(context).also { numberAllocations++ }

    override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.VIEW
  }
}
