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

import android.view.View
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.dp
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith

/** Tests for [ComponentContext.findViewWithTag]. */
@RunWith(LithoTestRunner::class)
class FindViewWithTagTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val expectedException = ExpectedException.none()

  @Test
  fun `findViewWithTag returns correct view when called from onVisible, if tag exists`() {
    class MyComponent(val viewRef: AtomicReference<View>) : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column(
            style = Style.wrapInView().onVisible { viewRef.set(findViewWithTag("Find Me!")) }) {
              child(Column(style = Style.viewTag("not_this_one").width(100.dp).height(100.dp)))
              child(Column(style = Style.viewTag("Find Me!").width(100.dp).height(100.dp)))
            }
      }
    }

    val viewRef = AtomicReference<View>()
    lithoViewRule.render { MyComponent(viewRef = viewRef) }

    assertThat(viewRef.get()).isNotNull()
    assertThat(viewRef.get().tag).isEqualTo("Find Me!")
  }

  @Test
  fun `findViewWithTag returns null when tag doesn't exists`() {
    class MyComponent(val viewRef: AtomicReference<View>) : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column(
            style =
                Style.wrapInView().onVisible { viewRef.set(findViewWithTag("I don't exist")) }) {
              child(Column(style = Style.viewTag("not_this_one").width(100.dp).height(100.dp)))
              child(Column(style = Style.viewTag("Find Me!").width(100.dp).height(100.dp)))
            }
      }
    }

    val viewRef = AtomicReference<View>()
    lithoViewRule.render { MyComponent(viewRef = viewRef) }

    assertThat(viewRef.get()).isNull()
  }

  @Test
  fun `findViewWithTag returns correct view for complex object tag, if tag exists`() {
    val handleTag1 = Handle()
    val handleTag2 = Handle()

    class MyComponent(val viewRef: AtomicReference<View>) : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column(
            style = Style.wrapInView().onVisible { viewRef.set(findViewWithTag(handleTag2)) }) {
              child(Column(style = Style.viewTag(handleTag1).width(100.dp).height(100.dp)))
              child(Column(style = Style.viewTag(handleTag2).width(100.dp).height(100.dp)))
            }
      }
    }

    val viewRef = AtomicReference<View>()
    lithoViewRule.render { MyComponent(viewRef = viewRef) }

    assertThat(viewRef.get()).isNotNull()
    assertThat(viewRef.get().tag).isEqualTo(handleTag2)
  }

  @Test
  fun `findViewWithTag throws when called with incorrect ComponentContext`() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage("render")

    ComponentContext(lithoViewRule.context).findViewWithTag<View>("Some Tag")
  }

  @Test
  fun `findViewWithTag throws when called with incorrect ComponentScope`() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage("render")

    ComponentScope(ComponentContext(lithoViewRule.context)).findViewWithTag<View>("Some Tag")
  }
}
