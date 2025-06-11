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

import android.view.View
import androidx.core.util.Pair
import com.facebook.rendercore.RenderUnit.DelegateBinder.Companion.createDelegateBinder
import com.facebook.rendercore.TestBinder.TestBinder1
import com.facebook.rendercore.TestBinder.TestBinder2
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData4
import java.util.Arrays
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WrapperRenderUnitTest {

  private val bindOrder: MutableList<Any?> = ArrayList()
  private val unbindOrder: MutableList<Any?> = ArrayList()
  private val context: MountContext =
      MountContext(RuntimeEnvironment.getApplication(), RenderCoreSystrace.getInstance())
  private val content = View(context.androidContext)
  private val bindData = BindData()

  @Before
  fun setup() {
    bindOrder.clear()
    unbindOrder.clear()
  }

  @Test
  fun mountUnmountBinders_originalBindersMountFirstAndUnmountLast() {
    val originalMountBinder = TestBinder1(bindOrder, unbindOrder)
    val originalRenderUnit =
        TestRenderUnit(listOf(createDelegateBinder(TestRenderUnit(), originalMountBinder)))
    val wrapperMountBinder = TestBinder2(bindOrder, unbindOrder)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addOptionalMountBinder(
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder))
    wrapperRenderUnit.mountBinders(context, content, null, bindData)
    assertThat(bindOrder).containsExactly(originalMountBinder, wrapperMountBinder)
    wrapperRenderUnit.unmountBinders(context, content, null, bindData)
    assertThat(unbindOrder).containsExactly(wrapperMountBinder, originalMountBinder)
  }

  @Test
  fun attachDetachBinders_originalBindersAttachFirstAndDetachLast() {
    val originalAttachBinder = TestBinder1(bindOrder, unbindOrder)
    val originalRenderUnit =
        TestRenderUnit(
            emptyList(),
            emptyList(),
            listOf(createDelegateBinder(TestRenderUnit(), originalAttachBinder)))
    val wrapperAttachBinder = TestBinder2(bindOrder, unbindOrder)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addAttachBinder(createDelegateBinder(TestRenderUnit(), wrapperAttachBinder))
    wrapperRenderUnit.attachBinders(context, content, null, bindData)
    assertThat(bindOrder).containsExactly(originalAttachBinder, wrapperAttachBinder)
    wrapperRenderUnit.detachBinders(context, content, null, bindData)
    assertThat(unbindOrder).containsExactly(wrapperAttachBinder, originalAttachBinder)
  }

  @Test
  fun addSameTypeOptionalMountBinderToOriginalAndWrapperRenderUnit_throwsAnException() {
    val mountBinder = TestBinder1(bindOrder, unbindOrder)
    val originalRenderUnit = TestRenderUnit()
    originalRenderUnit.addOptionalMountBinder(createDelegateBinder(TestRenderUnit(), mountBinder))
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    assertThatThrownBy {
          wrapperRenderUnit.addOptionalMountBinder(
              createDelegateBinder(TestRenderUnit(), mountBinder))
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Binder TestBinder1 already exists in the wrapped TestRenderUnit")
  }

  @Test
  fun addSameTypeAttachBinderToOriginalAndWrapperRenderUnit_throwsAnException() {
    val attachBinder = TestBinder1(bindOrder, unbindOrder)
    val originalRenderUnit = TestRenderUnit()
    originalRenderUnit.addAttachBinder(createDelegateBinder(TestRenderUnit(), attachBinder))
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    assertThatThrownBy {
          wrapperRenderUnit.addAttachBinder(createDelegateBinder(TestRenderUnit(), attachBinder))
        }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Binder TestBinder1 already exists in the wrapped TestRenderUnit")
  }

  @Test
  fun mountBinders_withBindData_createsBindDataContainingOriginalAndWrapperBinders() {
    val originalFixedBinder1 = TestBinderWithBindData1(1)
    val originalFixedBinder2 = TestBinderWithBindData2(2)
    val originalMountBinder1 = TestBinderWithBindData1(3)
    val originalMountBinder2 = TestBinderWithBindData2(4)
    val originalRenderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), originalFixedBinder1),
                createDelegateBinder(TestRenderUnit(), originalFixedBinder2)))
    originalRenderUnit.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), originalMountBinder1),
        createDelegateBinder(TestRenderUnit(), originalMountBinder2))
    val wrapperMountBinder1 = TestBinderWithBindData3(5)
    val wrapperMountBinder2 = TestBinderWithBindData4(6)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder2))
    val wrapperBindData = BindData()
    wrapperRenderUnit.mountBinders(context, content, null, wrapperBindData)

    // assert fixed binders bind data is correct
    assertThat(wrapperBindData.fixedBindersBindData).hasSize(2)
    assertThat(wrapperBindData.fixedBindersBindData).containsExactly(1, 2)

    // assert optional mount binders bind data is correct
    assertThat(wrapperBindData.optionalMountBindersBindData).hasSize(4)
    assertThat(wrapperBindData.optionalMountBindersBindData?.get(originalMountBinder1.key))
        .isEqualTo(3)
    assertThat(wrapperBindData.optionalMountBindersBindData?.get(originalMountBinder2.key))
        .isEqualTo(4)
    assertThat(wrapperBindData.optionalMountBindersBindData?.get(wrapperMountBinder1.key))
        .isEqualTo(5)
    assertThat(wrapperBindData.optionalMountBindersBindData?.get(wrapperMountBinder2.key))
        .isEqualTo(6)
  }

  @Test
  fun attachBinders_withBindData_createsBindDataContainingOriginalAndWrapperBinders() {
    val originalAttachBinder1 = TestBinderWithBindData1(1)
    val originalAttachBinder2 = TestBinderWithBindData2(2)
    val originalRenderUnit = TestRenderUnit()
    originalRenderUnit.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), originalAttachBinder1),
        createDelegateBinder(TestRenderUnit(), originalAttachBinder2))
    val wrapperAttachBinder1 = TestBinderWithBindData3(3)
    val wrapperAttachBinder2 = TestBinderWithBindData4(4)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder2))
    val wrapperBindData = BindData()
    wrapperRenderUnit.attachBinders(context, content, null, wrapperBindData)

    // assert no fixed mount binders bind data is present when calling attachBinders
    assertThat(wrapperBindData.fixedBindersBindData).isNull()

    // assert no optional mount binders bind data is present when calling attachBinders
    assertThat(wrapperBindData.optionalMountBindersBindData).isNull()

    // assert attach binders bind data is correct
    assertThat(wrapperBindData.attachBindersBindData).hasSize(4)
    assertThat(wrapperBindData.attachBindersBindData?.get(originalAttachBinder1.key)).isEqualTo(1)
    assertThat(wrapperBindData.attachBindersBindData?.get(originalAttachBinder2.key)).isEqualTo(2)
    assertThat(wrapperBindData.attachBindersBindData?.get(wrapperAttachBinder1.key)).isEqualTo(3)
    assertThat(wrapperBindData.attachBindersBindData?.get(wrapperAttachBinder2.key)).isEqualTo(4)
  }

  @Test
  fun unmountBinders_withBindData_passesBindDataToOriginalAndWrapperBinders() {
    val originalUnbindOrder = mutableListOf<Any?>()
    val originalFixedBinder1 = TestBinderWithBindData1(bindOrder, originalUnbindOrder)
    val originalFixedBinder2 = TestBinderWithBindData2(bindOrder, originalUnbindOrder)
    val originalMountBinder1 = TestBinderWithBindData1(bindOrder, originalUnbindOrder)
    val originalMountBinder2 = TestBinderWithBindData2(bindOrder, originalUnbindOrder)
    val originalRenderUnit =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), originalFixedBinder1),
                createDelegateBinder(TestRenderUnit(), originalFixedBinder2)))
    originalRenderUnit.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), originalMountBinder1),
        createDelegateBinder(TestRenderUnit(), originalMountBinder2))
    val wrapperUnbindOrder = mutableListOf<Any?>()
    val wrapperMountBinder1 = TestBinderWithBindData3(bindOrder, wrapperUnbindOrder)
    val wrapperMountBinder2 = TestBinderWithBindData4(bindOrder, wrapperUnbindOrder)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        RenderUnitTest.createBindData(
            mutableListOf<Any>(1, 2),
            Arrays.asList(
                Pair(originalMountBinder1.key, 3),
                Pair(originalMountBinder2.key, 4),
                Pair(wrapperMountBinder1.key, 7),
                Pair(wrapperMountBinder2.key, 8)),
            null)
    wrapperRenderUnit.unmountBinders(context, content, null, bindData)

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(4)
    assertThat((originalUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(4)
    assertThat((originalUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(3)
    assertThat((originalUnbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(2)
    assertThat((originalUnbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(2)
    assertThat((wrapperUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(8)
    assertThat((wrapperUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(7)
  }

  @Test
  fun detachBinders_withBindData_passesBindDataToOriginalAndWrapperBinders() {
    val originalUnbindOrder = mutableListOf<Any?>()
    TestBinderWithBindData2(bindOrder, originalUnbindOrder)
    val originalAttachBinder1 = TestBinderWithBindData1(bindOrder, originalUnbindOrder)
    val originalAttachBinder2 = TestBinderWithBindData2(bindOrder, originalUnbindOrder)
    val originalRenderUnit = TestRenderUnit()
    originalRenderUnit.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), originalAttachBinder1),
        createDelegateBinder(TestRenderUnit(), originalAttachBinder2))
    val wrapperUnbindOrder = mutableListOf<Any?>()
    val wrapperAttachBinder1 = TestBinderWithBindData3(bindOrder, wrapperUnbindOrder)
    val wrapperAttachBinder2 = TestBinderWithBindData4(bindOrder, wrapperUnbindOrder)
    val wrapperRenderUnit = WrapperRenderUnit(originalRenderUnit)
    wrapperRenderUnit.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        RenderUnitTest.createBindData(
            null,
            null,
            listOf(
                Pair(originalAttachBinder1.key, 1),
                Pair(originalAttachBinder2.key, 2),
                Pair(wrapperAttachBinder1.key, 3),
                Pair(wrapperAttachBinder2.key, 4)))
    wrapperRenderUnit.detachBinders(context, content, null, bindData)

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(2)
    assertThat((originalUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(2)
    assertThat((originalUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(2)
    assertThat((wrapperUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(4)
    assertThat((wrapperUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(3)
  }

  @Test
  fun updateBinders_withBindData_passesBindDataToOriginalAndWrapperBindersAndUpdatesIt() {
    val originalUnbindOrder = mutableListOf<Any?>()
    val originalAttachBinder1 = TestBinderWithBindData1(bindOrder, originalUnbindOrder, 100)
    val originalAttachBinder2 = TestBinderWithBindData2(bindOrder, originalUnbindOrder, 200)
    val originalMountBinder1 = TestBinderWithBindData2(bindOrder, originalUnbindOrder, 300)
    val originalMountBinder2 = TestBinderWithBindData3(bindOrder, originalUnbindOrder, 400)
    val originalFixedMountBinder1 = TestBinderWithBindData3(bindOrder, originalUnbindOrder, 500)
    val originalFixedMountBinder2 = TestBinderWithBindData3(bindOrder, originalUnbindOrder, 600)
    val currentOriginalRU =
        TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(TestRenderUnit(), originalFixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), originalFixedMountBinder2)))
    currentOriginalRU.addOptionalMountBinder(
        createDelegateBinder(currentOriginalRU, originalMountBinder1))
    currentOriginalRU.addOptionalMountBinder(
        createDelegateBinder(currentOriginalRU, originalMountBinder2))
    currentOriginalRU.addAttachBinder(
        createDelegateBinder(currentOriginalRU, originalAttachBinder1))
    currentOriginalRU.addAttachBinder(
        createDelegateBinder(currentOriginalRU, originalAttachBinder2))
    val wrapperUnbindOrder = mutableListOf<Any?>()
    val wrapperAttachBinder1 = TestBinderWithBindData3(bindOrder, wrapperUnbindOrder, 700)
    val wrapperAttachBinder2 = TestBinderWithBindData4(bindOrder, wrapperUnbindOrder, 800)
    val wrapperMountBinder1 = TestBinderWithBindData1(bindOrder, wrapperUnbindOrder, 900)
    val wrapperMountBinder2 = TestBinderWithBindData4(bindOrder, wrapperUnbindOrder, 1000)
    val currentWrapperRU = WrapperRenderUnit(currentOriginalRU)
    currentWrapperRU.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder2))
    currentWrapperRU.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder2))
    val nextRU =
        TestRenderUnit(
            listOf(
                createDelegateBinder(TestRenderUnit(), originalFixedMountBinder1),
                createDelegateBinder(TestRenderUnit(), originalFixedMountBinder2)))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, originalMountBinder1))
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, originalMountBinder2))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, originalAttachBinder1))
    nextRU.addAttachBinder(createDelegateBinder(nextRU, originalAttachBinder2))
    val nextWrapperRU = WrapperRenderUnit(nextRU)
    nextWrapperRU.addOptionalMountBinders(
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperMountBinder2))
    nextWrapperRU.addAttachBinders(
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(TestRenderUnit(), wrapperAttachBinder2))

    // Create BindData that will be passed to updateBinders
    val bindData =
        RenderUnitTest.createBindData(
            mutableListOf<Any>(1, 2),
            Arrays.asList(
                Pair(originalMountBinder1.key, 5),
                Pair(originalMountBinder2.key, 6),
                Pair(wrapperMountBinder1.key, 7),
                Pair(wrapperMountBinder2.key, 8)),
            Arrays.asList(
                Pair(originalAttachBinder1.key, 9),
                Pair(originalAttachBinder2.key, 10),
                Pair(wrapperAttachBinder1.key, 11),
                Pair(wrapperAttachBinder2.key, 12)))
    nextWrapperRU.updateBinders(
        context, content, currentWrapperRU, null, Any(), null, bindData, true)

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(6)
    assertThat((originalUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(10)
    assertThat((originalUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(9)
    assertThat((originalUnbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(6)
    assertThat((originalUnbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(5)
    assertThat((originalUnbindOrder[4] as Pair<Any?, Any>).second).isEqualTo(2)
    assertThat((originalUnbindOrder[5] as Pair<Any?, Any>).second).isEqualTo(1)

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(4)
    assertThat((wrapperUnbindOrder[0] as Pair<Any?, Any>).second).isEqualTo(12)
    assertThat((wrapperUnbindOrder[1] as Pair<Any?, Any>).second).isEqualTo(11)
    assertThat((wrapperUnbindOrder[2] as Pair<Any?, Any>).second).isEqualTo(8)
    assertThat((wrapperUnbindOrder[3] as Pair<Any?, Any>).second).isEqualTo(7)

    // assert fixed binders bind data is correct
    assertThat(bindData.fixedBindersBindData).containsExactly(500, 600)

    // assert optional mount binders bind data is correct
    assertThat(bindData.optionalMountBindersBindData).hasSize(4)
    assertThat(bindData.optionalMountBindersBindData?.get(originalMountBinder1.key)).isEqualTo(300)
    assertThat(bindData.optionalMountBindersBindData?.get(originalMountBinder2.key)).isEqualTo(400)
    assertThat(bindData.optionalMountBindersBindData?.get(wrapperMountBinder1.key)).isEqualTo(900)
    assertThat(bindData.optionalMountBindersBindData?.get(wrapperMountBinder2.key)).isEqualTo(1000)

    // assert attach binders bind data is correct
    assertThat(bindData.attachBindersBindData).hasSize(4)
    assertThat(bindData.attachBindersBindData?.get(originalAttachBinder1.key)).isEqualTo(100)
    assertThat(bindData.attachBindersBindData?.get(originalAttachBinder2.key)).isEqualTo(200)
    assertThat(bindData.attachBindersBindData?.get(wrapperAttachBinder1.key)).isEqualTo(700)
    assertThat(bindData.attachBindersBindData?.get(wrapperAttachBinder2.key)).isEqualTo(800)
  }
}
