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

package com.facebook.litho.drawable

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(LithoTestRunner::class)
class ComparableDrawableWrapperTest {

  @Test
  fun drawablePassedToTheConstructor_hasTheWrapperAsCallback() {
    val wrapped = Color.RED.toDrawable()
    val wrapper = TestWrapper(wrapped)
    assertThat(wrapped.callback).isSameAs(wrapper)
  }

  @Test
  fun invalidatingTheWrappedDrawable_invalidatesTheWrapper() {
    val wrapper = TestWrapper(Color.RED.toDrawable())
    val callback = mock<Drawable.Callback>()
    wrapper.callback = callback

    wrapper.wrappedDrawable.invalidateSelf()

    verify(callback).invalidateDrawable(wrapper)
  }

  @Test
  fun replacingTheWrappedDrawable_movesTheCallback() {
    val first = Color.RED.toDrawable()
    val second = Color.BLUE.toDrawable()
    val wrapper = TestWrapper(first)

    wrapper.wrappedDrawable = second

    assertThat(first.callback).isNull()
    assertThat(second.callback).isSameAs(wrapper)
  }

  private class TestWrapper(drawable: Drawable) : ComparableDrawableWrapper(drawable) {
    override fun isEquivalentTo(other: ComparableDrawable): Boolean = other === this
  }
}
