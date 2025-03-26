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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.SparseArray
import android.view.View
import androidx.core.util.Pair
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.RenderUnit.DelegateBinder.Companion.createDelegateBinder
import com.facebook.rendercore.TestBinder.TestBinder1
import com.facebook.rendercore.TestBinder.TestBinder2
import com.facebook.rendercore.TestBinder.TestBinder3
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3
import java.util.Arrays
import org.assertj.core.api.Java6Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RenderUnitTest {

  private val bindOrder: MutableList<Any?> = ArrayList()
  private val unbindOrder: MutableList<Any?> = ArrayList()
  private val context: Context = RuntimeEnvironment.getApplication()
  private val content = View(context)
  private val tracer = RenderCoreSystrace.getInstance()
  private val bindData = BindData()

  @Before
  fun setup() {
    bindOrder.clear()
    unbindOrder.clear()
  }

  @Test
  fun testAddMountBinders_WithSameBinderType_WontAddDuplicates() {
    val renderUnit = TestRenderUnit()
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder3sameTypeAs1 = TestBinder1(bindOrder, unbindOrder)
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2),
        createDelegateBinder(renderUnit, mountBinder3sameTypeAs1))
    renderUnit.mountBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactly(mountBinder2, mountBinder3sameTypeAs1)
    Java6Assertions.assertThat(unbindOrder).isEmpty()
  }

  @Test
  fun testMountExtensions() {
    val renderUnit = TestRenderUnit()
    val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))
    renderUnit.mountBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactly(mountBinder1, mountBinder2)
    Java6Assertions.assertThat(unbindOrder).isEmpty()
  }

  @Test
  fun testUnmountExtensions() {
    val renderUnit = TestRenderUnit()
    val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))
    renderUnit.unmountBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder).isEmpty()
    Java6Assertions.assertThat(unbindOrder).containsExactly(mountBinder2, mountBinder1)
  }

  @Test
  fun testAttachExtensions() {
    val renderUnit = TestRenderUnit()
    val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))
    renderUnit.attachBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactly(attachBinder1, attachBinder2)
    Java6Assertions.assertThat(unbindOrder).isEmpty()
  }

  @Test
  fun testDetachExtensions() {
    val renderUnit = TestRenderUnit()
    val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))
    renderUnit.detachBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder).isEmpty()
    Java6Assertions.assertThat(unbindOrder).containsExactly(attachBinder2, attachBinder1)
  }

  @Test
  fun testAddFixedMountBinders_WithSameBinderType_WillAddDuplicates() {
    val fixedMountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val fixedMountBinder2 = TestBinder2(bindOrder, unbindOrder)
    val fixedMountBinder3sameTypeAs1 = TestBinder1(bindOrder, unbindOrder)
    val renderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder3sameTypeAs1)))
    renderUnit.mountBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder)
        .containsExactly(fixedMountBinder1, fixedMountBinder2, fixedMountBinder3sameTypeAs1)
    Java6Assertions.assertThat(unbindOrder).isEmpty()
  }

  @Test
  fun testMountUnmountBinders_WillBeDoneInCorrectOrder() {
    val fixedMountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val fixedMountBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
    val renderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder1))
    renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder2))
    renderUnit.mountBinders(context, content, null, bindData, tracer)
    renderUnit.unmountBinders(context, content, null, bindData, tracer)
    Java6Assertions.assertThat(bindOrder)
        .containsExactly(fixedMountBinder1, fixedMountBinder2, mountBinder1, mountBinder2)
    Java6Assertions.assertThat(unbindOrder)
        .containsExactly(mountBinder2, mountBinder1, fixedMountBinder2, fixedMountBinder1)
  }

  @Test
  fun testUpdateExtensionsWithFixedBinders_WillBeDoneInCorrectOrder() {
    val binders: MutableList<TestBinder<*>?> = ArrayList()
    val fixedMountBinders: MutableList<DelegateBinder<*, in View, *>> = ArrayList()
    for (i in 0..63) {
      val binder = TestBinder1(bindOrder, unbindOrder)
      binders.add(binder)
      fixedMountBinders.add(createDelegateBinder(TestRenderUnit(), binder))
    }
    val currentRU = TestRenderUnit(fixedMountBinders)
    val nextRU = TestRenderUnit(fixedMountBinders)
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactlyElementsOf(binders)
    Java6Assertions.assertThat(unbindOrder).containsExactlyElementsOf(binders.reversed())
  }

  @Test
  fun testUpdateExtensionsWithDifferentTypesOfBinders_WillBeDoneInCorrectOrder() {
    val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinder2(bindOrder, unbindOrder)
    val mountBinder2 = TestBinder3(bindOrder, unbindOrder)
    val fixedMountBinder1 = TestBinder3(bindOrder, unbindOrder)
    val fixedMountBinder2 = TestBinder3(bindOrder, unbindOrder)
    val currentRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    val nextRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder1))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder2))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder2))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder1))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder2))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(unbindOrder)
        .containsExactly(
            attachBinder2,
            attachBinder1,
            mountBinder2,
            mountBinder1,
            fixedMountBinder2,
            fixedMountBinder1)
    Java6Assertions.assertThat(bindOrder)
        .containsExactly(
            fixedMountBinder1,
            fixedMountBinder2,
            mountBinder1,
            mountBinder2,
            attachBinder1,
            attachBinder2)
  }

  @Test
  fun testUpdateExtensions_withDifferentFixedBindersCount_shouldCrash() {
    val fixedMountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val fixedMountBinder2 = TestBinder1(bindOrder, unbindOrder)
    val currentRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    val nextRU = TestRenderUnit(listOf(createDelegateBinder(TestRenderUnit(), fixedMountBinder1)))

    val throwable =
        Java6Assertions.catchThrowable {
          nextRU.updateBinders(
              context, content, currentRU, null, Any(), null, bindData, true, tracer)
        }
    Java6Assertions.assertThat(throwable.message)
        .isEqualTo("[TestRenderUnit] Exception resolving fixed mount binders to update")
  }

  @Test
  fun testUpdateExtensions_withRemovedFixedBinder_shouldCrash() {
    val fixedMountBinder = TestBinder1(bindOrder, unbindOrder)
    val currentRU = TestRenderUnit(listOf(createDelegateBinder(TestRenderUnit(), fixedMountBinder)))
    val nextRU = TestRenderUnit()
    val throwable =
        Java6Assertions.catchThrowable {
          nextRU.updateBinders(
              context, content, currentRU, null, Any(), null, bindData, true, tracer)
        }
    Java6Assertions.assertThat(throwable.message)
        .isEqualTo("[TestRenderUnit] Exception resolving fixed mount binders to update")
  }

  @Test
  fun testUpdateExtensions_withFixedBinderAndDifferentModelTypes_shouldNotCrash() {
    val fixedDrawableMountBinder = TestBinder<Drawable>(bindOrder, unbindOrder)
    val drawable1 = ColorDrawable(Color.CYAN)
    val drawable2 = GradientDrawable()
    val currentRU =
        TestRenderUnit(listOf(createDelegateBinder(drawable1, fixedDrawableMountBinder)))
    val nextRU = TestRenderUnit(listOf(createDelegateBinder(drawable2, fixedDrawableMountBinder)))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactly(fixedDrawableMountBinder)
    Java6Assertions.assertThat(unbindOrder).containsExactly(fixedDrawableMountBinder)
  }

  @Test
  fun testAdd64FixedMountBinders_shouldNotCrash() {
    val fixedMountBinders: MutableList<DelegateBinder<*, in View, *>> = ArrayList()
    for (i in 0..63) {
      fixedMountBinders.add(createDelegateBinder(TestRenderUnit(), TestBinder1()))
    }
    TestRenderUnit(fixedMountBinders)
  }

  @Test(expected = IllegalStateException::class)
  fun testAdd65FixedMountBinders_shouldCrash() {
    val fixedMountBinders: MutableList<DelegateBinder<*, in View, *>> = ArrayList()
    for (i in 0..64) {
      fixedMountBinders.add(createDelegateBinder(TestRenderUnit(), TestBinder1()))
    }
    TestRenderUnit(fixedMountBinders)
  }

  @Test
  fun testUpdateExtensions_WithDifferentExtensionLists() {
    val currentRU = TestRenderUnit()
    val nextRU = TestRenderUnit()
    val attachBinder = TestBinder1(bindOrder, unbindOrder)
    val mountBinder = TestBinder1(bindOrder, unbindOrder)
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(unbindOrder).containsExactly(mountBinder)
    Java6Assertions.assertThat(bindOrder).containsExactly(attachBinder)
  }

  @Test
  fun testUpdateExtensions_withAlreadyDetachedItem_shouldNotUnbind() {
    val currentRU = TestRenderUnit()
    val nextRU = TestRenderUnit()
    val attachBinder = TestBinder1(bindOrder, unbindOrder)
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder))

    // Pass false to isAttached, indicating currentRU is already unbound
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, false, tracer)

    // unbind should not happen, so unbind-order should be empty.
    Java6Assertions.assertThat(unbindOrder).isEmpty()

    // bind should still happen normally
    Java6Assertions.assertThat(bindOrder).containsExactly(attachBinder)
  }

  @Test
  fun testUpdateExtensions_WithDifferentNumberOfExtensions2_shouldUpdateTrue() {
    val currentRU = TestRenderUnit()
    val nextRU = TestRenderUnit()
    val currentAttachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val currentAttachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val currentMountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val nextAttachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val nextMountBinder2 = TestBinder2(bindOrder, unbindOrder)
    currentRU.addAttachBinders(
        createDelegateBinder(currentRU, currentAttachBinder1),
        createDelegateBinder(currentRU, currentAttachBinder2))
    currentRU.addOptionalMountBinders(createDelegateBinder(currentRU, currentMountBinder1))
    nextRU.addAttachBinders(createDelegateBinder(nextRU, nextAttachBinder2))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, nextMountBinder2))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(unbindOrder)
        .containsExactly(currentAttachBinder2, currentAttachBinder1, currentMountBinder1)
    Java6Assertions.assertThat(bindOrder).containsExactly(nextMountBinder2, nextAttachBinder2)
  }

  @Test
  fun testUpdateExtensions_WithDifferentNumberOfExtensions_shouldUpdateFalse() {
    val currentRU = TestRenderUnit()
    val nextRU = TestRenderUnit()
    val currentAttachBinder1 = TestBinder1(bindOrder, unbindOrder)
    val currentAttachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val currentMountBinder1 = TestBinder1(bindOrder, unbindOrder)
    val nextAttachBinder2 = TestBinder2(bindOrder, unbindOrder)
    val nextMountBinder2 = TestBinder2(bindOrder, unbindOrder)
    currentRU.addAttachBinders(
        createDelegateBinder(currentRU, currentAttachBinder1),
        createDelegateBinder(currentRU, currentAttachBinder2))
    currentRU.addOptionalMountBinders(createDelegateBinder(currentRU, currentMountBinder1))
    nextRU.addAttachBinders(createDelegateBinder(nextRU, nextAttachBinder2))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, nextMountBinder2))
    nextRU.updateBinders(context, content, currentRU, null, null, null, bindData, true, tracer)
    Java6Assertions.assertThat(unbindOrder)
        .containsExactly(currentAttachBinder1, currentMountBinder1)
    Java6Assertions.assertThat(bindOrder).containsExactly(nextMountBinder2)
  }

  @Test
  fun testUpdateExtensions_withDifferentModelTypes_shouldNotCrash() {
    val currentRU = TestRenderUnit()
    val nextRU = TestRenderUnit()
    val drawableMountBinder = TestBinder<Drawable>(bindOrder, unbindOrder)
    val drawable1 = ColorDrawable(Color.CYAN)
    val drawable2 = GradientDrawable()
    currentRU.addOptionalMountBinders(createDelegateBinder(drawable1, drawableMountBinder))
    nextRU.addOptionalMountBinders(createDelegateBinder(drawable2, drawableMountBinder))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)
    Java6Assertions.assertThat(bindOrder).containsExactly(drawableMountBinder)
    Java6Assertions.assertThat(unbindOrder).containsExactly(drawableMountBinder)
  }

  @Test
  fun mountBinders_withBindData_createsBindData() {
    val fixedBinder1 = TestBinderWithBindData1(1)
    val fixedBinder2 = TestBinderWithBindData2(2)
    val mountBinder1 = TestBinderWithBindData1(3)
    val mountBinder2 = TestBinderWithBindData2(4)
    val renderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedBinder1),
                createDelegateBinder(TestRenderUnit(), fixedBinder2)))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))
    val bindData = createBindData(null, null, null)
    renderUnit.mountBinders(context, content, null, bindData, tracer)

    // assert fixed binders bind data is correct
    Java6Assertions.assertThat(bindData.fixedBindersBindData).containsExactly(1, 2)

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(3)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder2.type))
        .isEqualTo(4)

    // assert no attach binders bind data is present when calling mountBinders
    Java6Assertions.assertThat(bindData.attachBindersBindData).isNull()
  }

  @Test
  fun attachBinders_withBindData_createsBindData() {
    val attachBinder1 = TestBinderWithBindData1(1)
    val attachBinder2 = TestBinderWithBindData2(2)
    val renderUnit = TestRenderUnit()
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))
    val bindData = createBindData(null, null, null)
    renderUnit.attachBinders(context, content, null, bindData, tracer)

    // assert no fixed binders bind data is present when calling attachBinders
    Java6Assertions.assertThat(bindData.fixedBindersBindData).isNull()

    // assert no optional mount binders bind data is present when calling attachBinders
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).isNull()

    // assert attach binders bind data is correct
    Java6Assertions.assertThat(bindData.attachBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.attachBindersBindData?.get(attachBinder1.type)).isEqualTo(1)
    Java6Assertions.assertThat(bindData.attachBindersBindData?.get(attachBinder2.type)).isEqualTo(2)
  }

  @Test
  fun unmountBinders_withBindData_passesBindDataToUnbind() {
    val unbindOrder = mutableListOf<Any?>()
    val fixedBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder)
    val fixedBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder)
    val mountBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder)
    val mountBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder)
    val renderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedBinder1),
                createDelegateBinder(TestRenderUnit(), fixedBinder2)))
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        createBindData(
            mutableListOf<Any>(1, 2),
            listOf(Pair(mountBinder1.type, 3), Pair(mountBinder2.type, 4)),
            null)
    renderUnit.unmountBinders(context, content, null, bindData, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(4)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(4)
    Java6Assertions.assertThat((unbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(3)
    Java6Assertions.assertThat((unbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(2)
    Java6Assertions.assertThat((unbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(1)
  }

  @Test
  fun detachBinders_withBindData_passesBindDataToUnbind() {
    val unbindOrder = mutableListOf<Any?>()
    val attachBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder)
    val attachBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder)
    val renderUnit = TestRenderUnit()
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        createBindData(
            null, null, Arrays.asList(Pair(attachBinder1.type, 1), Pair(attachBinder2.type, 2)))
    renderUnit.detachBinders(context, content, null, bindData, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(2)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(2)
    Java6Assertions.assertThat((unbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(1)
  }

  @Test
  fun updateBinders_withBindData_passesBindDataToUnbindAndUpdatesIt() {
    val unbindOrder = mutableListOf<Any?>()
    val attachBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder, 100)
    val attachBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder, 200)
    val mountBinder1 = TestBinderWithBindData2(bindOrder, unbindOrder, 300)
    val mountBinder2 = TestBinderWithBindData3(bindOrder, unbindOrder, 400)
    val fixedMountBinder1 = TestBinderWithBindData3(bindOrder, unbindOrder, 500)
    val fixedMountBinder2 = TestBinderWithBindData3(bindOrder, unbindOrder, 600)
    val currentRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    val nextRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder1))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder2))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder2))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder1))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        createBindData(
            mutableListOf<Any>(1, 2),
            Arrays.asList(Pair(mountBinder1.type, 3), Pair(mountBinder2.type, 4)),
            Arrays.asList(Pair(attachBinder1.type, 5), Pair(attachBinder2.type, 6)))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(6)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(6)
    Java6Assertions.assertThat((unbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(5)
    Java6Assertions.assertThat((unbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(4)
    Java6Assertions.assertThat((unbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(3)
    Java6Assertions.assertThat((unbindOrder[4] as Pair<Any?, Any>).second).isEqualTo(2)
    Java6Assertions.assertThat((unbindOrder[5] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert fixed binders bind data is correct
    Java6Assertions.assertThat(bindData.fixedBindersBindData).containsExactly(500, 600)

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(300)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder2.type))
        .isEqualTo(400)

    // assert attach binders bind data is correct
    Java6Assertions.assertThat(bindData.attachBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.attachBindersBindData?.get(attachBinder1.type))
        .isEqualTo(100)
    Java6Assertions.assertThat(bindData.attachBindersBindData?.get(attachBinder2.type))
        .isEqualTo(200)
  }

  @Test
  fun updateBinders_withBindDataAndDifferentNumberOfBinders_passesBindDataToUnbindAndUpdatedsIt() {
    val unbindOrder = mutableListOf<Any?>()
    val attachBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder, 100)
    val attachBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder, 200)
    val mountBinder1 = TestBinderWithBindData2(bindOrder, unbindOrder, 300)
    val mountBinder2 = TestBinderWithBindData3(bindOrder, unbindOrder, 400)
    val fixedMountBinder1 = TestBinderWithBindData3(bindOrder, unbindOrder, 500)
    val fixedMountBinder2 = TestBinderWithBindData3(bindOrder, unbindOrder, 600)
    val currentRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    val nextRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), fixedMountBinder2)))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder1))
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder2))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder1))

    // Create BindData that will be passed to updateBinders
    val bindData =
        createBindData(
            mutableListOf<Any>(1, 2),
            Arrays.asList(Pair(mountBinder1.type, 3), Pair(mountBinder2.type, 4)),
            Arrays.asList(Pair(attachBinder1.type, 5), Pair(attachBinder2.type, 6)))
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(6)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(6)
    Java6Assertions.assertThat((unbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(5)
    Java6Assertions.assertThat((unbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(4)
    Java6Assertions.assertThat((unbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(3)
    Java6Assertions.assertThat((unbindOrder[4] as Pair<Any?, Any>).second).isEqualTo(2)
    Java6Assertions.assertThat((unbindOrder[5] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert fixed binders bind data is correct
    Java6Assertions.assertThat(bindData.fixedBindersBindData).containsExactly(500, 600)

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(1)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(300)

    // assert attach binders bind data is correct
    Java6Assertions.assertThat(bindData.attachBindersBindData).hasSize(1)
    Java6Assertions.assertThat(bindData.attachBindersBindData?.get(attachBinder1.type))
        .isEqualTo(100)
  }

  @Test
  fun updateBindersMultipleTimes_withBindData_passesBindDataToUnbind() {
    val binderShouldUpdate = booleanArrayOf(true, false)
    val unbindOrder: MutableList<Any> = ArrayList()
    val mountBinder1: RenderUnit.Binder<TestRenderUnit, View, Any> =
        object : RenderUnit.Binder<TestRenderUnit, View, Any> {
          override fun shouldUpdate(
              currentModel: TestRenderUnit,
              newModel: TestRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean {
            return binderShouldUpdate[0]
          }

          override fun bind(
              context: Context,
              content: View,
              model: TestRenderUnit,
              layoutData: Any?
          ): Any? {
            return 100
          }

          override fun unbind(
              context: Context,
              content: View,
              model: TestRenderUnit,
              layoutData: Any?,
              bindData: Any?
          ) {
            unbindOrder.add(Pair<Any?, Any?>(this, bindData))
          }
        }
    val mountBinder2: RenderUnit.Binder<TestRenderUnit, View, Any> =
        object : RenderUnit.Binder<TestRenderUnit, View, Any> {
          override fun shouldUpdate(
              currentModel: TestRenderUnit,
              newModel: TestRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean {
            return binderShouldUpdate[1]
          }

          override fun bind(
              context: Context,
              content: View,
              model: TestRenderUnit,
              layoutData: Any?
          ): Any {
            return 200
          }

          override fun unbind(
              context: Context,
              content: View,
              model: TestRenderUnit,
              layoutData: Any?,
              bindData: Any?
          ) {
            unbindOrder.add(Pair<Any?, Any?>(this, bindData))
          }
        }
    val currentRU = TestRenderUnit()
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1))
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2))
    val nextRU = TestRenderUnit()
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        createBindData(null, listOf(Pair(mountBinder1.type, 1), Pair(mountBinder2.type, 2)), null)

    // Call update -  only first binder should update
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(1)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert no fixed binders bind data is present
    Java6Assertions.assertThat(bindData.fixedBindersBindData).isNull()

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(100)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder2.type))
        .isEqualTo(2)

    // assert no attach binders bind data is present
    Java6Assertions.assertThat(bindData.attachBindersBindData).isNull()

    // Call update -  only second binder should update
    unbindOrder.clear()
    binderShouldUpdate[0] = false
    binderShouldUpdate[1] = true
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(1)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(2)

    // assert no fixed binders bind data is present
    Java6Assertions.assertThat(bindData.fixedBindersBindData).isNull()

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(100)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder2.type))
        .isEqualTo(200)

    // assert no attach binders bind data is present
    Java6Assertions.assertThat(bindData.attachBindersBindData).isNull()

    // Call update -  both binders should update
    unbindOrder.clear()
    binderShouldUpdate[0] = true
    binderShouldUpdate[1] = true
    nextRU.updateBinders(context, content, currentRU, null, Any(), null, bindData, true, tracer)

    // assert that unbind was called in correct order and correct bind data was passed
    Java6Assertions.assertThat(unbindOrder).hasSize(2)
    Java6Assertions.assertThat((unbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(200)
    Java6Assertions.assertThat((unbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(100)

    // assert no fixed binders bind data is present
    Java6Assertions.assertThat(bindData.fixedBindersBindData).isNull()

    // assert optional mount binders bind data is correct
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData).hasSize(2)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder1.type))
        .isEqualTo(100)
    Java6Assertions.assertThat(bindData.optionalMountBindersBindData?.get(mountBinder2.type))
        .isEqualTo(200)

    // assert no attach binders bind data is present
    Java6Assertions.assertThat(bindData.attachBindersBindData).isNull()
  }

  @Test
  fun createWithExtra_getExtra_returnsCorrectValues() {
    val extras = SparseArray<Any?>()
    extras.put(1, "test")
    extras.put(999, 42)
    extras.put(-50000, true)
    val renderUnit = TestRenderUnit(extras)
    Java6Assertions.assertThat(renderUnit.getExtra<Any>(0)).isNull()
    Java6Assertions.assertThat(renderUnit.getExtra<String>(1)).isEqualTo("test")
    Java6Assertions.assertThat(renderUnit.getExtra<Int>(999)).isEqualTo(42)
    Java6Assertions.assertThat(renderUnit.getExtra<Boolean>(-50000)).isEqualTo(true)
  }

  @Test
  fun addsDescriptionAndBinderDescriptionIfExceptionHappensDuringMount() {
    val testRenderUnit =
        TestRenderUnit(
            fixedMountBinders =
                listOf(
                    createDelegateBinder(Unit, CrashingBinder),
                ))

    val throwable =
        Java6Assertions.catchThrowable {
          testRenderUnit.mountBinders(context, content, null, bindData, tracer)
        }
    Java6Assertions.assertThat(throwable.message)
        .isEqualTo("[TestRenderUnit] Exception binding fixed mount binder: my-crashing-binder")
  }

  private object CrashingBinder : RenderUnit.Binder<Any, View, Any> {

    override fun shouldUpdate(
        currentModel: Any,
        newModel: Any,
        currentLayoutData: Any?,
        nextLayoutData: Any?
    ): Boolean = true

    override fun bind(context: Context, content: View, model: Any, layoutData: Any?): Any? {
      throw RuntimeException("ups!")
    }

    override fun unbind(
        context: Context,
        content: View,
        model: Any,
        layoutData: Any?,
        bindData: Any?
    ) = Unit

    override val description: String
      get() = "my-crashing-binder"
  }

  companion object {

    fun createBindData(
        fixedBindersBindData: List<Any>?,
        optionalMountBindersBindData: List<Pair<BinderKey, Any>>?,
        attachBindersBindData: List<Pair<BinderKey, Any>>?
    ): BindData {
      val bindData = BindData()
      if (fixedBindersBindData != null) {
        for (i in fixedBindersBindData.indices) {
          bindData.setFixedBindersBindData(fixedBindersBindData[i], i, fixedBindersBindData.size)
        }
      }
      if (optionalMountBindersBindData != null) {
        for (i in optionalMountBindersBindData.indices) {
          bindData.setOptionalMountBindersBindData(
              optionalMountBindersBindData[i].second,
              optionalMountBindersBindData[i].first,
              optionalMountBindersBindData.size)
        }
      }
      if (attachBindersBindData != null) {
        for (i in attachBindersBindData.indices) {
          bindData.setAttachBindersBindData(
              attachBindersBindData[i].second,
              attachBindersBindData[i].first,
              attachBindersBindData.size)
        }
      }
      return bindData
    }
  }
}
