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
import android.view.View
import android.widget.TextView
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.RenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountableComponentsTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

  @Test
  fun `should render mountable component`() {
    TempComponentsConfigurations.setDelegateToRenderCoreMount(true)

    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val root =
        Column.create(c)
            .child(
                Wrapper.create(c)
                    .widthPx(100)
                    .heightPx(100)
                    .delegate(TestMountableComponent(TextView(c.androidContext), steps)))
            .build()

    lithoViewRule.render { root }
    lithoViewRule.lithoView.unmountAllItems()

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_UNMOUNT)

    TempComponentsConfigurations.restoreDelegateToRenderCoreMount()
  }
}

class TestMountableComponent(
    val view: View,
    val steps: MutableList<LifecycleStep.StepInfo>,
) : MountableComponent() {

  override fun ComponentScope.render(): ViewMountable {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.RENDER))
    return ViewMountable(view, steps)
  }
}

class ViewMountable(
    val view: View,
    val steps: MutableList<LifecycleStep.StepInfo>,
) : SimpleMountable<View>() {

  override fun createContent(context: Context): View {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_MOUNT_CONTENT))
    return view
  }

  override fun measure(
      context: Context,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      previousLayoutData: Any?,
  ): ViewLayoutData {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_MEASURE))
    val width =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.EXACTLY) {
          SizeSpec.getSize(widthSpec)
        } else {
          100
        }

    val height =
        if (SizeSpec.getMode(heightSpec) == SizeSpec.EXACTLY) {
          SizeSpec.getSize(heightSpec)
        } else {
          100
        }

    size.width = width
    size.height = height

    return ViewLayoutData(width, height)
  }

  override fun mount(c: Context?, content: View, layoutData: Any?) {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_MOUNT))
    layoutData as ViewLayoutData
  }

  override fun unmount(c: Context?, content: View, layoutData: Any?) {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_UNMOUNT))
    layoutData as ViewLayoutData
  }

  override fun shouldUpdate(
      currentMountable: SimpleMountable<View>,
      newMountable: SimpleMountable<View>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    steps.add(LifecycleStep.StepInfo(LifecycleStep.SHOULD_UPDATE))
    currentMountable as ViewMountable
    newMountable as ViewMountable
    return true
  }

  override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.VIEW
}

class ViewLayoutData(val width: Int, val height: Int)
