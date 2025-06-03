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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test

class BinderObserverTest {

  @Test
  fun `test bind and unbind`() {
    val size = 10
    val map = mutableSetOf<BinderId>()
    val observer = TestBinderObserver(map)
    repeat(size) { index -> observer.observeBind(binderId(index)) {} }
    assertThat(map.size).isEqualTo(size)

    repeat(size) { index -> observer.observeUnbind(binderId(index)) {} }
    assertThat(map).isEmpty()
  }

  @Test
  fun `test bind func must execute exactly once`() {
    val binderId = binderId(0)
    var exception = catchThrowable {
      TestBinderObserver(executionCount = 1).observeBind(binderId) {}
    }
    assertThat(exception).isNull()

    exception = catchThrowable { TestBinderObserver(executionCount = 0).observeBind(binderId) {} }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)

    exception = catchThrowable { TestBinderObserver(executionCount = 2).observeBind(binderId) {} }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `test unbind func must execute exactly once`() {
    val binderId = binderId(0)
    var exception = catchThrowable {
      TestBinderObserver(executionCount = 1).observeUnbind(binderId) {}
    }
    assertThat(exception).isNull()

    exception = catchThrowable { TestBinderObserver(executionCount = 0).observeUnbind(binderId) {} }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)

    exception = catchThrowable { TestBinderObserver(executionCount = 2).observeUnbind(binderId) {} }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)
  }

  companion object {

    private fun binderId(key: Any) = BinderId(1, BinderType.MOUNT, TestBinderKey(key))

    private data class TestBinderKey(private val key: Any) : BinderKey

    private class TestBinderObserver(
        private val set: MutableSet<BinderId> = mutableSetOf(),
        private val executionCount: Int = 1,
    ) : BinderObserver() {
      override fun onBind(binderId: BinderId, func: () -> Unit) {
        repeat(executionCount) { func() }
        set.add(binderId)
      }

      override fun onUnbind(binderId: BinderId, func: () -> Unit) {
        repeat(executionCount) { func() }
        set.remove(binderId)
      }
    }
  }
}
