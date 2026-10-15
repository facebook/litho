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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.LithoKeyTestingUtil.getScopedComponentInfos
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CardClip
import com.facebook.litho.widget.Text
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class GlobalKeyWithoutRootHostTest {
  private var context: ComponentContext? = null

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
    context = ComponentContext(getApplicationContext<Context>())
  }

  @Test
  fun testMultipleChildrenComponentKey() {
    val component = multipleChildrenComponent
    val layoutSpecId = component.typeId
    val nestedLayoutSpecId = layoutSpecId - 1
    val column: Component = Column.create(context).build()
    val columnSpecId = column.typeId
    val lithoView = getLithoView(component)
    val globalKeysInfo = getScopedComponentInfos(lithoView)

    // Text
    val expectedGlobalKeyText =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$[Text2]")
    val scopedComponentInfoText = globalKeysInfo["Text"]!![0]
    assertThat(scopedComponentInfoText.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyText)
    // TestViewComponent in child layout
    val expectedGlobalKeyTestViewComponent =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            "$[TestViewComponent1]",
        )
    val scopedComponentInfoTestViewComponent = globalKeysInfo["TestViewComponent"]!![0]
    assertThat(scopedComponentInfoTestViewComponent.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyTestViewComponent)
    // CardClip in child
    val expectedGlobalKeyCardClip =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip1]",
        )
    val scopedComponentInfoCardClip = globalKeysInfo["CardClip"]!![0]
    assertThat(scopedComponentInfoCardClip.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyCardClip)

    // Text in child
    val expectedGlobalKeyTextChild =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            "$[Text1]",
        )
    val scopedComponentInfoTextChild = globalKeysInfo["Text"]!![1]
    assertThat(scopedComponentInfoTextChild.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyTextChild)

    // CardClip
    val expectedGlobalKeyCardClip2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip2]",
        )
    val scopedComponentInfoCardClip2 = globalKeysInfo["CardClip"]!![1]
    assertThat(scopedComponentInfoCardClip2.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyCardClip2)

    // TestViewComponent
    val expectedGlobalKeyTestViewComponent2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            "$[TestViewComponent2]",
        )
    val scopedComponentInfoTestViewComponent2 = globalKeysInfo["TestViewComponent"]!![1]
    assertThat(scopedComponentInfoTestViewComponent2.context.globalKey.toString())
        .isEqualTo(expectedGlobalKeyTestViewComponent2)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(requireNotNull(context))
    lithoView.setComponent(component)
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.UNSPECIFIED),
    )
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  companion object {
    private val multipleChildrenComponent: Component
      get() {
        val color = -0x10000
        val testGlobalKeyChildComponent: Component =
            object : InlineLayoutSpec() {
              @OnCreateLayout
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(
                        TestViewComponent.create(c)
                            .widthDip(10f)
                            .heightDip(10f)
                            .key("[TestViewComponent1]"),
                    )
                    .child(
                        Column.create(c)
                            .backgroundColor(color)
                            .child(
                                key("[CardClip1]") {
                                  CardClip(style = Style.width(10.dp).height(10.dp))
                                },
                            ),
                    )
                    .child(Text.create(c).text("Test").widthDip(10f).heightDip(10f).key("[Text1]"))
                    .build()
              }
            }
        val testGlobalKeyChild: Component =
            object : InlineLayoutSpec() {
              @OnCreateLayout
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(Text.create(c).text("test").widthDip(10f).heightDip(10f).key("[Text2]"))
                    .child(testGlobalKeyChildComponent)
                    .child(
                        Column.create(c)
                            .backgroundColor(color)
                            .child(
                                key("[CardClip2]") {
                                  CardClip(style = Style.width(10.dp).height(10.dp))
                                },
                            ),
                    )
                    .child(
                        TestViewComponent.create(c)
                            .widthDip(10f)
                            .heightDip(10f)
                            .key("[TestViewComponent2]"),
                    )
                    .build()
              }
            }
        return testGlobalKeyChild
      }
  }
}
