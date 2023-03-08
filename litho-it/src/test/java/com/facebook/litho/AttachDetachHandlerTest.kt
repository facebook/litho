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
import android.os.Looper.getMainLooper
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.AttachDetachTester
import com.facebook.litho.widget.AttachDetachTesterSpec
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class AttachDetachHandlerTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun component_setRootWithLayout_onAttachedIsCalled() {
    val steps: List<String> = ArrayList()
    val root =
        AttachDetachTester.create(legacyLithoViewRule.context).name("root").steps(steps).build()
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_ATTACHED}")
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    val currentAttached = attachDetachHandler?.attached ?: error("Should not be null")

    assertThat(currentAttached.size).isEqualTo(1)
  }

  @Test
  fun component_setEmptyRootAfterAttach_onDetachedIsCalled() {
    val steps: MutableList<String> = ArrayList()
    val root =
        AttachDetachTester.create(legacyLithoViewRule.context).name("root").steps(steps).build()
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_ATTACHED}")
    steps.clear()
    legacyLithoViewRule.setRoot(Column.create(legacyLithoViewRule.context).build())
    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_DETACHED}")
  }

  @Test
  fun component_releaseLithoView_onDetachedIsCalled() {
    val steps: MutableList<String> = ArrayList()
    val root =
        AttachDetachTester.create(legacyLithoViewRule.context).name("root").steps(steps).build()
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    steps.clear()
    legacyLithoViewRule.release()
    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_DETACHED}")
    assertThat(attachDetachHandler?.attached).isNullOrEmpty()
  }

  @Test
  fun component_replaceRootWithSameComponent_onDetachedIsNotCalled() {
    val c = legacyLithoViewRule.context
    val c3 = AttachDetachTester.create(c).name("c3")
    val c4 = AttachDetachTester.create(c).name("c4")
    val c1 = AttachDetachTester.create(c).name("c1").children(listOf(c3, c4))
    val c2 = AttachDetachTester.create(c).name("c2")
    val steps: MutableList<String> = ArrayList()
    val r1 = AttachDetachTester.create(c).children(listOf(c1, c2)).name("r1").steps(steps).build()
    legacyLithoViewRule.setRoot(r1)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c4:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c1:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c2:${AttachDetachTesterSpec.ON_ATTACHED}",
            "r1:${AttachDetachTesterSpec.ON_ATTACHED}")
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    var currentAttached = attachDetachHandler?.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(5)
    steps.clear()

    /*
             r1                r2
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    val c5 = AttachDetachTester.create(c).name("c5")
    val c7 = AttachDetachTester.create(c).name("c7")
    val c6 = AttachDetachTester.create(c).name("c6").children(listOf(c7))
    val r2 = AttachDetachTester.create(c).children(listOf(c5, c6)).name("r2").steps(steps).build()
    legacyLithoViewRule.setRoot(r2)
    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:${AttachDetachTesterSpec.ON_DETACHED}",
            "c4:${AttachDetachTesterSpec.ON_DETACHED}",
            "c7:${AttachDetachTesterSpec.ON_ATTACHED}")

    currentAttached = attachDetachHandler.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(4)
    steps.clear()

    /*
            r2                  r3
          /    \              /
        c5      c6     =>   c8
                  \
                   c7
    */
    val c8 = AttachDetachTester.create(c).name("c8")
    val r3 = AttachDetachTester.create(c).children(listOf(c8)).name("r3").steps(steps).build()
    legacyLithoViewRule.setRoot(r3)
    assertThat(steps)
        .describedAs("Should call @OnDetached methods in expect order")
        .containsExactly(
            "c7:${AttachDetachTesterSpec.ON_DETACHED}", "c6:${AttachDetachTesterSpec.ON_DETACHED}")

    currentAttached = attachDetachHandler.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(2)
  }

  @Test
  fun component_replaceRootWithDifferentComponent_onDetachedIsNotCalled() {
    val c = legacyLithoViewRule.context
    val c3 = AttachDetachTester.create(c).name("c3")
    val c4 = AttachDetachTester.create(c).name("c4")
    val c1 = AttachDetachTester.create(c).name("c1").children(listOf(c3, c4))
    val c2 = AttachDetachTester.create(c).name("c2")
    val steps: MutableList<String> = ArrayList()
    val r1 = AttachDetachTester.create(c).children(listOf(c1, c2)).name("r1").steps(steps).build()
    legacyLithoViewRule.setRoot(r1)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c4:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c1:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c2:${AttachDetachTesterSpec.ON_ATTACHED}",
            "r1:${AttachDetachTesterSpec.ON_ATTACHED}")
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    var currentAttached = attachDetachHandler?.attached ?: error("Should not be null")

    assertThat(currentAttached.size).isEqualTo(5)
    steps.clear()

    /*
             r1                r2 (TestWrappedComponentProp)
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    val c5 = AttachDetachTester.create(c).name("c5")
    val c7 = AttachDetachTester.create(c).name("c7")
    val c6 = AttachDetachTester.create(c).name("c6").children(listOf(c7))
    val r2 =
        AttachDetachTester.create(c)
            .children(listOf(c5, c6))
            .name("r2")
            .key("newKey")
            .steps(steps)
            .build()
    legacyLithoViewRule.setRoot(r2)
    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:${AttachDetachTesterSpec.ON_DETACHED}",
            "c4:${AttachDetachTesterSpec.ON_DETACHED}",
            "c1:${AttachDetachTesterSpec.ON_DETACHED}",
            "c2:${AttachDetachTesterSpec.ON_DETACHED}",
            "r1:${AttachDetachTesterSpec.ON_DETACHED}",
            "c5:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c7:${AttachDetachTesterSpec.ON_ATTACHED}",
            "c6:${AttachDetachTesterSpec.ON_ATTACHED}",
            "r2:${AttachDetachTesterSpec.ON_ATTACHED}")

    currentAttached = attachDetachHandler?.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(4)
  }

  @Test
  fun component_setRootAndSizeSpecTwice_onAttachAndOnDetachedAreCalledOnlyOnce() {
    val c = legacyLithoViewRule.context
    val steps: MutableList<String> = ArrayList()
    val c1 = AttachDetachTester.create(c).name("c1")
    val component =
        AttachDetachTester.create(c).name("root").steps(steps).children(listOf(c1)).build()
    legacyLithoViewRule.setRootAndSizeSpecSync(
        component,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
    legacyLithoViewRule.attachToWindow().measure().layout()
    legacyLithoViewRule.setRootAndSizeSpecSync(
        component,
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY))
    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTesterSpec.ON_ATTACHED}",
            "root:${AttachDetachTesterSpec.ON_ATTACHED}")
    steps.clear()
    legacyLithoViewRule.release()
    assertThat(steps)
        .describedAs("Should call @OnDetached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTesterSpec.ON_DETACHED}",
            "root:${AttachDetachTesterSpec.ON_DETACHED}")
  }

  @Test
  fun component_setRootAndSizeSpecConcurrently_onAttachAndOnDetachedAreCalledOnlyOnce() {
    val c = legacyLithoViewRule.context
    val steps: List<String> = ArrayList()
    val c1 = AttachDetachTester.create(c).name("c1")
    val component =
        AttachDetachTester.create(c).name("root").steps(steps).children(listOf(c1)).build()
    val componentTree = legacyLithoViewRule.componentTree
    val latch1 = CountDownLatch(1)
    val thread1 = Thread {
      for (i in 0..9) {
        componentTree.setRootAndSizeSpecSync(
            component,
            SizeSpec.makeSizeSpec(100 + 10 * i, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100 + 10 * i, SizeSpec.EXACTLY))
      }
      latch1.countDown()
    }
    val latch2 = CountDownLatch(1)
    val thread2 = Thread {
      for (i in 0..9) {
        componentTree.setRootAndSizeSpecSync(
            component,
            SizeSpec.makeSizeSpec(200 + 10 * i, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200 + 10 * i, SizeSpec.EXACTLY))
      }
      latch2.countDown()
    }
    thread1.start()
    thread2.start()
    assertThat(latch1.await(5_000, TimeUnit.MILLISECONDS)).isTrue
    assertThat(latch2.await(5_000, TimeUnit.MILLISECONDS)).isTrue
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:${AttachDetachTesterSpec.ON_ATTACHED}",
            "root:${AttachDetachTesterSpec.ON_ATTACHED}")
  }

  @Test
  fun component_setRoot_onAttachedIsCalledOnUIThread() {
    val steps: List<String> = ArrayList()
    val extraThreadInfo = ConcurrentHashMap<String, Any>()
    val root =
        AttachDetachTester.create(legacyLithoViewRule.context)
            .name("root")
            .steps(steps)
            .extraThreadInfo(extraThreadInfo)
            .build()
    legacyLithoViewRule.attachToWindow().measure().layout()
    legacyLithoViewRule.setRoot(root)
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_ATTACHED}")
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    val currentAttached = attachDetachHandler?.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(1)
    val isMainThreadLayout =
        extraThreadInfo[AttachDetachTesterSpec.IS_MAIN_THREAD_LAYOUT] as Boolean?
    assertThat(isMainThreadLayout).isTrue
    val isMainThreadAttached =
        extraThreadInfo[AttachDetachTesterSpec.IS_MAIN_THREAD_ON_ATTACHED] as Boolean?
    assertThat(isMainThreadAttached).isTrue
  }

  @Test
  fun component_setRootAsync_onAttachedIsCalledOnUIThread() {
    val steps: List<String> = ArrayList()
    val extraThreadInfo = ConcurrentHashMap<String, Any>()
    val root =
        AttachDetachTester.create(legacyLithoViewRule.context)
            .name("root")
            .steps(steps)
            .extraThreadInfo(extraThreadInfo)
            .build()
    legacyLithoViewRule.attachToWindow().measure().layout()
    legacyLithoViewRule.setRootAsync(root)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:${AttachDetachTesterSpec.ON_ATTACHED}")
    val attachDetachHandler = legacyLithoViewRule.componentTree.attachDetachHandler
    val currentAttached = attachDetachHandler?.attached ?: error("Should not be null")
    assertThat(currentAttached.size).isEqualTo(1)
    val isMainThreadLayout =
        extraThreadInfo[AttachDetachTesterSpec.IS_MAIN_THREAD_LAYOUT] as Boolean?
    assertThat(isMainThreadLayout).isFalse
    val isMainThreadAttached =
        extraThreadInfo[AttachDetachTesterSpec.IS_MAIN_THREAD_ON_ATTACHED] as Boolean?
    assertThat(isMainThreadAttached).isTrue
  }
}
