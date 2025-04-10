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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.TouchExpansionDelegateTest.Companion.emulateClickEvent
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout
import com.facebook.litho.testing.error.TestHasDelegateThatCrashesOnCreateLayout
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.OnClickCallbackComponent
import com.facebook.litho.widget.OnErrorNotPresentChild
import com.facebook.litho.widget.OnErrorPassUpChildTester
import com.facebook.litho.widget.OnErrorPassUpParentTester
import com.facebook.litho.widget.TriggerCallbackComponent
import java.lang.RuntimeException
import java.util.ArrayList
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.core.Is
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests to make sure we wrap exceptions from common apis with the right metadata. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoMetadataExceptionWrapperTest {

  @JvmField @Rule var lithoTestRule = LithoTestRule()

  @JvmField @Rule var expectedException = ExpectedException.none()

  @Test
  fun onCreateLayout_deepComponentStack_exceptionShowsComponentStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage(
        object : BaseMatcher<String?>() {
          override fun matches(item: Any): Boolean {
            if (item is String) {
              return item.contains("Wrapper(Row)") &&
                  item.contains("Row") &&
                  item.contains("Column") &&
                  item.contains("TestHasDelegateThatCrashesOnCreateLayout") &&
                  item.contains("TestCrasherOnCreateLayout")
            }
            return false
          }

          override fun describeTo(description: Description) = Unit
        })
    val c = lithoTestRule.context
    lithoTestRule.render {
      Wrapper.create(c)
          .delegate(
              Row.create(c)
                  .child(Column.create(c).child(TestHasDelegateThatCrashesOnCreateLayout.create(c)))
                  .build())
          .build()
    }
  }

  @Test
  fun onCreateLayout_onlyRootComponent_exceptionShowsComponentStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("layout_stack: TestCrasherOnCreateLayout")
    val c = lithoTestRule.context
    lithoTestRule.render { TestCrasherOnCreateLayout.create(c).build() }
  }

  @Test
  fun onCreateLayout_withReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    Assume.assumeThat(
        "Error boundary is enabled in debug builds.",
        LithoDebugConfigurations.isDebugModeEnabled,
        Is.`is`(true))
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage(
        object : BaseMatcher<String?>() {
          override fun matches(item: Any): Boolean {
            if (item is String) {
              return item.contains("OnErrorPassUpParentTester") &&
                  item.contains("OnErrorPassUpChildTester") &&
                  item.contains("ThrowExceptionGrandChildTester")
            }
            return false
          }

          override fun describeTo(description: Description) = Unit
        })
    val info: List<String> = ArrayList()
    val context = lithoTestRule.context
    val component: Component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorPassUpChildTester.create(context).info(info).build())
            .info(info)
            .build()
    lithoTestRule.render { component }
  }

  @Test
  fun onCreateLayout_withIndirectReRaisedErrorFromErrorBoundary_showsRightComponentStack() {
    Assume.assumeThat(
        "Error boundary is enabled in debug builds.",
        LithoDebugConfigurations.isDebugModeEnabled,
        Is.`is`(true))
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage(
        object : BaseMatcher<String?>() {
          override fun matches(item: Any): Boolean {
            if (item is String) {
              return item.contains("OnErrorPassUpParentTester") &&
                  item.contains("OnErrorNotPresentChild") &&
                  item.contains("ThrowExceptionGrandChildTester")
            }
            return false
          }

          override fun describeTo(description: Description) = Unit
        })
    val info: List<String> = ArrayList()
    val context = lithoTestRule.context
    val component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorNotPresentChild.create(context).build())
            .info(info)
            .build()
    lithoTestRule.render { component }
  }

  @Test
  fun onCreateLayout_withLogTag_showsLogTagInStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("log_tag: myLogTag")
    val c = getComponentContextForTest()
    lithoTestRule.render(componentTree = ComponentTree.create(c).build()) {
      TestCrasherOnCreateLayout.create(c).build()
    }
  }

  @Test
  fun onMount_withLogTag_showsLogTagInStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("log_tag: myLogTag")
    val c = getComponentContextForTest()
    lithoTestRule.render(
        componentTree = ComponentTree.create(c).build(), widthPx = 100, heightPx = 100) {
          TestCrasherOnCreateLayout.create(c).build()
        }
  }

  @Test
  fun onClickEvent_withLogTag_showsLogTagInStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("log_tag: myLogTag")
    expectedException.expectMessage("<cls>com.facebook.litho.widget.OnClickCallbackComponent</cls>")
    val c = getComponentContextForTest()
    val component: Component =
        Column.create(c)
            .child(
                OnClickCallbackComponent.create(c).widthPx(10).heightPx(10).callback {
                  throw RuntimeException("Expected test exception")
                })
            .build()
    val testLithoView =
        lithoTestRule.render(componentTree = ComponentTree.create(c).build()) { component }
    testLithoView.lithoView.emulateClickEvent(7, 7)
  }

  @Test
  fun onTrigger_withLogTag_showsLogTagInStack() {
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("log_tag: myLogTag")
    expectedException.expectMessage("<cls>com.facebook.litho.widget.TriggerCallbackComponent</cls>")
    val c = getComponentContextForTest()
    val handle = Handle()
    val component: Component =
        Column.create(c)
            .child(
                TriggerCallbackComponent.create(c)
                    .widthPx(10)
                    .heightPx(10)
                    .handle(handle)
                    .callback { throw RuntimeException("Expected test exception") })
            .build()
    lithoTestRule.render(componentTree = ComponentTree.create(c).build()) { component }
    TriggerCallbackComponent.doTrigger(lithoTestRule.context, handle)
  }

  private fun getComponentContextForTest(): ComponentContext {
    val androidContext = ApplicationProvider.getApplicationContext<Context>()
    return ComponentContext(
        androidContext,
        ComponentContextUtils.buildDefaultLithoConfiguration(
            context = androidContext,
            componentsConfig = ComponentsConfiguration.defaultInstance.copy(logTag = "myLogTag")),
        null)
  }
}
