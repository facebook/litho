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

import android.os.Looper
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.AttachDetachTester
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class EffectHandlerAttachDetachTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun component_setRootWithLayout_onAttachedIsCalled() {
    val steps: MutableList<String> = ArrayList()
    val root = AttachDetachTester(steps = steps, name = "root")
    val testLithoView = lithoTestRule.render { root }
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTester.ON_ATTACHED}")
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    val currentAttached = attachDetachHandler?.effects ?: error("Should not be null")

    assertThat(currentAttached.size).isEqualTo(1)
  }

  @Test
  fun component_setEmptyRootAfterAttach_onDetachedIsCalled() {
    val steps: MutableList<String> = ArrayList()
    val root = AttachDetachTester(steps = steps, name = "root")
    val testLithoView = lithoTestRule.render { root }
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTester.ON_ATTACHED}")
    steps.clear()
    testLithoView.setRoot(Column.create(lithoTestRule.context).build())
    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:${AttachDetachTester.ON_DETACHED}")
  }

  @Test
  fun component_releaseLithoView_onDetachedIsCalled() {
    val steps: MutableList<String> = ArrayList()
    val root = AttachDetachTester(steps = steps, name = "root")
    val testLithoView = lithoTestRule.render { root }
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    steps.clear()
    testLithoView.release()
    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:${AttachDetachTester.ON_DETACHED}")
    assertThat(attachDetachHandler?.effects).isNullOrEmpty()
  }

  @Test
  fun component_replaceRootWithSameComponent_onDetachedIsNotCalled() {
    val steps: MutableList<String> = ArrayList()
    val c3 = AttachDetachTester(steps = steps, name = "c3")
    val c4 = AttachDetachTester(steps = steps, name = "c4")
    val c1 = AttachDetachTester(steps = steps, name = "c1", children = listOf(c3, c4))
    val c2 = AttachDetachTester(steps = steps, name = "c2")
    val r1 = AttachDetachTester(steps = steps, name = "r1", children = listOf(c1, c2))
    val testLithoView = lithoTestRule.render { r1 }
    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:${AttachDetachTester.ON_ATTACHED}",
            "c4:${AttachDetachTester.ON_ATTACHED}",
            "c1:${AttachDetachTester.ON_ATTACHED}",
            "c2:${AttachDetachTester.ON_ATTACHED}",
            "r1:${AttachDetachTester.ON_ATTACHED}",
        )
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    var currentAttached = attachDetachHandler?.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(5)
    steps.clear()

    /*
             r1                r2
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    val c5 = AttachDetachTester(steps = steps, name = "c5")
    val c7 = AttachDetachTester(steps = steps, name = "c7")
    val c6 = AttachDetachTester(steps = steps, name = "c6", children = listOf(c7))
    val r2 = AttachDetachTester(steps = steps, name = "r2", children = listOf(c5, c6))
    testLithoView.setRoot(r2)
    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:${AttachDetachTester.ON_DETACHED}",
            "c4:${AttachDetachTester.ON_DETACHED}",
            "c1:${AttachDetachTester.ON_DETACHED}",
            "c2:${AttachDetachTester.ON_DETACHED}",
            "r1:${AttachDetachTester.ON_DETACHED}",
            "c5:${AttachDetachTester.ON_ATTACHED}",
            "c7:${AttachDetachTester.ON_ATTACHED}",
            "c6:${AttachDetachTester.ON_ATTACHED}",
            "r2:${AttachDetachTester.ON_ATTACHED}",
        )

    currentAttached = attachDetachHandler.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(4)
    steps.clear()

    /*
            r2                  r3
          /    \              /
        c5      c6     =>   c8
                  \
                   c7
    */
    val c8 = AttachDetachTester(steps = steps, name = "c8")
    val r3 = AttachDetachTester(steps = steps, name = "r3", children = listOf(c8))
    testLithoView.setRoot(r3)
    assertThat(steps)
        .describedAs("Should call @OnDetached methods in expect order")
        .containsExactly(
            "c7:${AttachDetachTester.ON_DETACHED}",
            "c6:${AttachDetachTester.ON_DETACHED}",
            "c5:${AttachDetachTester.ON_DETACHED}",
            "r2:${AttachDetachTester.ON_DETACHED}",
            "c8:${AttachDetachTester.ON_ATTACHED}",
            "r3:${AttachDetachTester.ON_ATTACHED}",
        )

    currentAttached = attachDetachHandler.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(2)
  }

  @Test
  fun component_replaceRootWithDifferentComponent_onDetachedIsNotCalled() {
    val steps: MutableList<String> = ArrayList()
    val c3 = AttachDetachTester(steps = steps, name = "c3")
    val c4 = AttachDetachTester(steps = steps, name = "c4")
    val c1 = AttachDetachTester(steps = steps, name = "c1", children = listOf(c3, c4))
    val c2 = AttachDetachTester(steps = steps, name = "c2")
    val r1 = AttachDetachTester(steps = steps, name = "r1", children = listOf(c1, c2))
    val testLithoView = lithoTestRule.render { r1 }
    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:${AttachDetachTester.ON_ATTACHED}",
            "c4:${AttachDetachTester.ON_ATTACHED}",
            "c1:${AttachDetachTester.ON_ATTACHED}",
            "c2:${AttachDetachTester.ON_ATTACHED}",
            "r1:${AttachDetachTester.ON_ATTACHED}",
        )
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    var currentAttached = attachDetachHandler?.effects ?: error("Should not be null")

    assertThat(currentAttached.size).isEqualTo(5)
    steps.clear()

    /*
             r1                r2 (TestWrappedComponentProp)
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    val c5 = AttachDetachTester(steps = steps, name = "c5")
    val c7 = AttachDetachTester(steps = steps, name = "c7")
    val c6 = AttachDetachTester(steps = steps, name = "c6", children = listOf(c7))
    val r2 =
        key("newKey") { AttachDetachTester(steps = steps, name = "r2", children = listOf(c5, c6)) }
    testLithoView.setRoot(r2)
    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:${AttachDetachTester.ON_DETACHED}",
            "c4:${AttachDetachTester.ON_DETACHED}",
            "c1:${AttachDetachTester.ON_DETACHED}",
            "c2:${AttachDetachTester.ON_DETACHED}",
            "r1:${AttachDetachTester.ON_DETACHED}",
            "c5:${AttachDetachTester.ON_ATTACHED}",
            "c7:${AttachDetachTester.ON_ATTACHED}",
            "c6:${AttachDetachTester.ON_ATTACHED}",
            "r2:${AttachDetachTester.ON_ATTACHED}",
        )

    currentAttached = attachDetachHandler.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(4)
  }

  @Test
  fun component_setRootAndSizeSpecTwice_onAttachAndOnDetachedAreCalledOnlyOnce() {
    val steps: MutableList<String> = ArrayList()
    val c1 = AttachDetachTester(steps = steps, name = "c1")
    val component = AttachDetachTester(steps = steps, name = "root", children = listOf(c1))
    val testLithoView = lithoTestRule.render(widthPx = 100, heightPx = 100) { component }
    testLithoView.setRootAndSizeSpecSync(
        component,
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
    )
    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTester.ON_ATTACHED}",
            "root:${AttachDetachTester.ON_ATTACHED}",
        )
    steps.clear()
    testLithoView.release()
    assertThat(steps)
        .describedAs("Should call @OnDetached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTester.ON_DETACHED}",
            "root:${AttachDetachTester.ON_DETACHED}",
        )
  }

  @Test
  fun component_setRootAndSizeSpecConcurrently_onAttachAndOnDetachedAreCalledOnlyOnce() {
    val steps: MutableList<String> = ArrayList()
    val c1 = AttachDetachTester(steps = steps, name = "c1")
    val component = AttachDetachTester(steps = steps, name = "root", children = listOf(c1))
    val testLithoView = lithoTestRule.createTestLithoView()
    val componentTree = testLithoView.componentTree
    val latch1 = CountDownLatch(1)
    val thread1 = Thread {
      for (i in 0..9) {
        componentTree.setRootAndSizeSpecSync(
            component,
            SizeSpec.makeSizeSpec(100 + 10 * i, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100 + 10 * i, SizeSpec.EXACTLY),
        )
      }
      latch1.countDown()
    }
    val latch2 = CountDownLatch(1)
    val thread2 = Thread {
      for (i in 0..9) {
        componentTree.setRootAndSizeSpecSync(
            component,
            SizeSpec.makeSizeSpec(200 + 10 * i, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200 + 10 * i, SizeSpec.EXACTLY),
        )
      }
      latch2.countDown()
    }
    thread1.start()
    thread2.start()
    assertThat(latch1.await(5_000, TimeUnit.MILLISECONDS)).isTrue
    assertThat(latch2.await(5_000, TimeUnit.MILLISECONDS)).isTrue
    (Shadow.extract(Looper.getMainLooper()) as ShadowLooper).idle()
    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTester.ON_ATTACHED}",
            "root:${AttachDetachTester.ON_ATTACHED}",
        )
  }

  @Test
  fun component_setRoot_onAttachedIsCalledOnUIThread() {
    val steps: MutableList<String> = ArrayList()
    val extraThreadInfo = ConcurrentHashMap<String, Any?>()
    val root = AttachDetachTester(steps = steps, name = "root", extraThreadInfo = extraThreadInfo)
    val testLithoView = lithoTestRule.render { root }
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTester.ON_ATTACHED}")
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    val currentAttached = attachDetachHandler?.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(1)
    val isMainThreadLayout = extraThreadInfo[AttachDetachTester.IS_MAIN_THREAD_LAYOUT] as Boolean?
    assertThat(isMainThreadLayout).isTrue
    val isMainThreadAttached =
        extraThreadInfo[AttachDetachTester.IS_MAIN_THREAD_ON_ATTACHED] as Boolean?
    assertThat(isMainThreadAttached).isTrue
  }

  @Test
  fun component_setRootAsync_onAttachedIsCalledOnUIThread() {
    val steps: MutableList<String> = ArrayList()
    val extraThreadInfo = ConcurrentHashMap<String, Any?>()
    val root = AttachDetachTester(steps = steps, name = "root", extraThreadInfo = extraThreadInfo)
    val testLithoView = lithoTestRule.createTestLithoView().measure().layout()
    testLithoView.setRootAsync(root)
    lithoTestRule.idle()
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTester.ON_ATTACHED}")
    val attachDetachHandler = testLithoView.componentTree.effectHandler
    val currentAttached = attachDetachHandler?.effects ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(1)
    val isMainThreadLayout = extraThreadInfo[AttachDetachTester.IS_MAIN_THREAD_LAYOUT] as Boolean?
    assertThat(isMainThreadLayout).isFalse
    val isMainThreadAttached =
        extraThreadInfo[AttachDetachTester.IS_MAIN_THREAD_ON_ATTACHED] as Boolean?
    assertThat(isMainThreadAttached).isTrue
  }
}
