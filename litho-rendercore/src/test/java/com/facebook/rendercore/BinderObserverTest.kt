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
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1
import com.facebook.rendercore.testing.TestHostView
import com.facebook.rendercore.testing.TestNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
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

  @Test
  fun `test observeShouldUpdate delegates to func and returns its result`() {
    val observer = TestBinderObserver()
    var invocations = 0

    val shouldUpdate =
        observer.observeShouldUpdate(binderId(0)) {
          invocations++
          true
        }
    assertThat(shouldUpdate).isTrue
    assertThat(invocations).isEqualTo(1)

    assertThat(observer.observeShouldUpdate(binderId(1)) { false }).isFalse
  }

  @Test
  fun `test shouldUpdate func must execute exactly once`() {
    val binderId = binderId(0)
    var exception = catchThrowable {
      ShouldUpdateBinderObserver(invocationCount = 1).observeShouldUpdate(binderId) { true }
    }
    assertThat(exception).isNull()

    exception = catchThrowable {
      ShouldUpdateBinderObserver(invocationCount = 0).observeShouldUpdate(binderId) { true }
    }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)

    exception = catchThrowable {
      ShouldUpdateBinderObserver(invocationCount = 2).observeShouldUpdate(binderId) { true }
    }
    assertThat(exception).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `bind and unbind observation are triggered when render unit binder updates`() {
    val c: Context = RuntimeEnvironment.application

    class BinderRecord(var bindCount: Int, var unbindCount: Int)
    val binderRecord = hashMapOf<BinderId, BinderRecord>()

    val observer =
        object : BinderObserver() {
          override fun onBind(binderId: BinderId, func: () -> Unit) {
            val record = binderRecord.getOrPut(binderId) { BinderRecord(0, 0) }
            record.bindCount++
            func()
          }

          override fun onUnbind(binderId: BinderId, func: () -> Unit) {
            val record = binderRecord.getOrPut(binderId) { BinderRecord(0, 0) }
            record.unbindCount++
            func()
          }
        }

    val mountState = MountState(TestHostView(c), binderObserver = observer)

    val binder = TestBinderWithBindData1()
    val renderUnit =
        TestRenderUnit().apply {
          addOptionalMountBinder(DelegateBinder.createDelegateBinder(this, binder))
        }

    val binderId = BinderId(renderUnit.id, BinderType.MOUNT, binder.key)

    val root =
        TestNode().apply {
          setRenderUnit(renderUnit)
          setLayoutData(0)
        }

    val renderTree = createRenderTree(c, root)
    mountState.mount(renderTree)

    val record = binderRecord[binderId]
    checkNotNull(record)
    assertThat(record.bindCount).isEqualTo(1)
    assertThat(record.unbindCount).isEqualTo(0)

    // update the layout data so that the render unit binder rebinds
    root.setLayoutData(1)
    val newRenderTree = createRenderTree(c, root)
    mountState.mount(newRenderTree)

    assertThat(record.bindCount).isEqualTo(2)
    assertThat(record.unbindCount).isEqualTo(1)
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

    private class ShouldUpdateBinderObserver(
        private val invocationCount: Int = 1,
    ) : BinderObserver() {
      override fun onBind(binderId: BinderId, func: () -> Unit) = func()

      override fun onUnbind(binderId: BinderId, func: () -> Unit) = func()

      override fun shouldUpdate(binderId: BinderId, func: BooleanFunc): Boolean {
        var result = false
        repeat(invocationCount) { result = func.invoke() }
        return result
      }
    }

    private fun createRenderTree(c: Context, root: Node<Any?>): RenderTree {
      val sizeConstraints = SizeConstraints.exact(200, 200)
      val layoutContext = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
      val result = root.calculateLayout(layoutContext, sizeConstraints)
      return getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)
    }
  }
}
