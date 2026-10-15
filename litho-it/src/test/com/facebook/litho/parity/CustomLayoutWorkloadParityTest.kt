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

package com.facebook.litho.parity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The trees the device benchmarks time, laid out on the JVM: each custom layout lands its leaves
 * where the flexbox container it stands in for does. The device correctness test checks the same.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutWorkloadParityTest {

  private val w = CustomLayoutWorkload(ApplicationProvider.getApplicationContext<Context>())

  @Test
  fun `a flat row of children lands where a Row puts them`() {
    for (count in listOf(1, 10, 50)) {
      assertThat(CustomLayoutWorkload.bounds(w.layOut(w.flatRowCustom(count)), leavesOnly = true))
          .describedAs("%s children", count)
          .isEqualTo(
              CustomLayoutWorkload.bounds(w.layOut(w.flatRowFlexbox(count)), leavesOnly = true),
          )
    }
  }

  @Test
  fun `nested containers place leaves where nested Columns do`() {
    for (depth in listOf(1, 4, 8)) {
      assertThat(CustomLayoutWorkload.bounds(w.layOut(w.nestedCustom(depth)), leavesOnly = true))
          .describedAs("depth %s", depth)
          .isEqualTo(
              CustomLayoutWorkload.bounds(w.layOut(w.nestedFlexbox(depth)), leavesOnly = true),
          )
    }
  }

  @Test
  fun `a child's own box props come out as in a Column`() {
    val mismatches =
        CustomLayoutWorkload.BOX_CASES.filterNot { it.name.take(2) in DIFFERS_BY_DESIGN }
            .mapNotNull { case ->
              val custom = CustomLayoutWorkload.bounds(w.layOut(w.boxCaseCustom(case)), false)
              val column = CustomLayoutWorkload.bounds(w.layOut(w.boxCaseFlexbox(case)), false)
              if (custom == column) null else "${case.name}: custom $custom, Column $column"
            }
    assertThat(mismatches).describedAs("situations that differ").isEmpty()
  }

  @Test
  fun `a re-layout at a new width lands leaves where a Row does`() {
    for (make in listOf({ w.flatRowCustom(10) } to { w.flatRowFlexbox(10) })) {
      val custom = w.treeFor(make.first())
      val flexbox = w.treeFor(make.second())
      for (width in listOf(CustomLayoutWorkload.NARROW, CustomLayoutWorkload.WIDTH, 300)) {
        w.relayout(custom, width)
        w.relayout(flexbox, width)
        assertThat(CustomLayoutWorkload.bounds(rootOf(custom), leavesOnly = true))
            .describedAs("at %s", width)
            .isEqualTo(CustomLayoutWorkload.bounds(rootOf(flexbox), leavesOnly = true))
      }
      custom.release()
      flexbox.release()
    }
  }

  private fun rootOf(tree: com.facebook.litho.ComponentTree): com.facebook.litho.LithoLayoutResult =
      checkNotNull(tree.mainThreadLayoutState?.rootLayoutResult)
          as com.facebook.litho.LithoLayoutResult

  private companion object {
    /**
     * Cases where a custom layout's child differs from a Column's by design, the same on either
     * engine: a size that does not fit its room is kept within it and centred, as in Compose, where
     * a Column lets it overflow or start-aligns it (B2, B3, C1, C2, C4); and a child's margin is
     * part of the room it is measured in, so a width that fills the room leaves its margin inside
     * it (D1).
     */
    val DIFFERS_BY_DESIGN = setOf("B2", "B3", "C1", "C2", "C4", "D1")
  }
}
