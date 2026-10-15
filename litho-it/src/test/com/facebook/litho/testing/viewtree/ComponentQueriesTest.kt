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

package com.facebook.litho.testing.viewtree

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.google.common.base.Predicates
import com.google.common.collect.ImmutableList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests [ComponentQueries] */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentQueriesTest {
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(getApplicationContext())
  }

  @Test
  fun testNoTextOnComponent() {
    val view: LithoView =
        ComponentTestHelper.mountComponent(context, Text.create(context).text("goodbye").build())

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isFalse()
  }

  @Test
  fun testTextOnComponent() {
    val view: LithoView =
        ComponentTestHelper.mountComponent(context, Text.create(context).text("hello").build())

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isTrue()
  }

  @Test
  fun testExtractTextFromTextComponent() {
    val view: LithoView =
        ComponentTestHelper.mountComponent(context, Text.create(context).text("hello").build())

    assertThat(view.textContentText).isEqualTo(ImmutableList.of<CharSequence>("hello"))
  }
}
