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
import android.util.Pair
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.LithoKeyTestingUtil.getScopedComponentInfos
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.CardClip
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextInput
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class GlobalKeyTest {
  private var mContext: ComponentContext? = null
  private var mComponentsReporter: TestComponentsReporter? = null

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    mComponentsReporter = TestComponentsReporter()
    mContext = ComponentContext(getApplicationContext<Context>())
    ComponentsReporter.provide(mComponentsReporter!!)
  }

  @Test
  fun testComponentKey() {
    val component: Component = SimpleMountSpecTester()
    Assert.assertEquals(component.key, component.typeId.toString() + "")
  }

  @Test
  fun testComponentManualKey() {
    val component: Component = key("someKey") { SimpleMountSpecTester() }
    Assert.assertEquals(component.key, "someKey")
  }

  @Test
  fun testRootComponentGlobalKey() {
    val component: Component = SimpleMountSpecTester(style = Style.width(10f.dp).height(10f.dp))
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val rootScopedComponentInfo = scopedComponentInfos["SimpleMountSpecTester"]!![0]
    val scopedContext = rootScopedComponentInfo.context
    assertThat(scopedContext).isNotNull
    assertThat(scopedContext.globalKey.toString()).isEqualTo(component.key)
  }

  @Test
  fun testRootComponentGlobalKeyManualKey() {
    val component: Component =
        key("someKey") { SimpleMountSpecTester(style = Style.width(10f.dp).height(10f.dp)) }
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val rootScopedComponentInfo = scopedComponentInfos["SimpleMountSpecTester"]!![0]
    val scopedContext = rootScopedComponentInfo.context
    assertThat(scopedContext).isNotNull
    assertThat(scopedContext.globalKey.toString()).isEqualTo("\$someKey")
  }

  @Test
  fun testMultipleChildrenComponentKey() {
    val component = multipleChildrenComponent
    val layoutSpecId = component.typeId
    val nestedLayoutSpecId = layoutSpecId - 1
    val column: Component = Column.create(mContext).build()
    val columnSpecId = column.typeId
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)

    // Text
    val expectedGlobalKeyText =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$[Text2]")
    val scopedComponentInfoText = scopedComponentInfos["Text"]!![0]
    Assert.assertEquals(scopedComponentInfoText.context.globalKey.toString(), expectedGlobalKeyText)
    // TestViewComponent in child layout
    val expectedGlobalKeyTestViewComponent =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            "$[TestViewComponent1]",
        )
    val scopedComponentInfoTestViewComponent = scopedComponentInfos["TestViewComponent"]!![0]
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent.context.globalKey.toString(),
        expectedGlobalKeyTestViewComponent,
    )
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
    val scopedComponentInfoCardClip = scopedComponentInfos["CardClip"]!![0]
    Assert.assertEquals(
        scopedComponentInfoCardClip.context.globalKey.toString(),
        expectedGlobalKeyCardClip,
    )

    // Text in child
    val expectedGlobalKeyTextChild =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            "$[Text1]",
        )
    val scopedComponentInfoTextChild = scopedComponentInfos["Text"]!![1]
    Assert.assertEquals(
        scopedComponentInfoTextChild.context.globalKey.toString(),
        expectedGlobalKeyTextChild,
    )

    // CardClip
    val expectedGlobalKeyCardClip2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip2]",
        )
    val scopedComponentInfoCardClip2 = scopedComponentInfos["CardClip"]!![1]
    Assert.assertEquals(
        scopedComponentInfoCardClip2.context.globalKey.toString(),
        expectedGlobalKeyCardClip2,
    )

    // TestViewComponent
    val expectedGlobalKeyTestViewComponent2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            "$[TestViewComponent2]",
        )
    val scopedComponentInfoTestViewComponent2 = scopedComponentInfos["TestViewComponent"]!![1]
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent2.context.globalKey.toString(),
        expectedGlobalKeyTestViewComponent2,
    )
  }

  @Test
  fun testSiblingsUniqueKeyRequirement() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(Text.create(c).text("").key("sameKey"))
                .child(Text.create(c).text("").key("sameKey"))
                .build()
          }
        }
    getLithoView(component)
    val expectedError =
        ("The manual key " +
            "sameKey you are setting on " +
            "this Text is a duplicate and will be changed into a unique one. This will " +
            "result in unexpected behavior if you don't change it.")
    assertThat(mComponentsReporter!!.loggedMessages)
        .contains(Pair.create(LogLevel.WARNING, expectedError))
  }

  @Test
  fun testSiblingsManualKeyDeduplication() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10f).heightDip(10f),
                )
                .child(TextInput.create(c).initialText("").widthDip(10f).heightDip(10f))
                .child(
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10f).heightDip(10f),
                )
                .child(
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10f).heightDip(10f),
                )
                .build()
          }
        }
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val column: Component = Column.create(mContext).build()
    val columnSpecId = column.typeId
    val layoutSpecId = component.typeId
    val expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey")
    val firstEditTextSCI = scopedComponentInfos["TextInput"]!![0]
    assertThat(firstEditTextSCI.context.globalKey.toString()).isEqualTo(expectedKey0)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1")
    val secondEditTextSCI = scopedComponentInfos["TextInput"]!![2]
    assertThat(secondEditTextSCI.context.globalKey.toString()).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2")
    val thirdEditTextSCI = scopedComponentInfos["TextInput"]!![3]
    assertThat(thirdEditTextSCI.context.globalKey.toString()).isEqualTo(expectedKey3)
  }

  @Test
  fun testSiblingsOfDifferentTypesManualKeyDeduplication() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(Text.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                .child(TestViewComponent.create(c).widthDip(10f).heightDip(10f).key("sameKey"))
                .child(
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10f).heightDip(10f),
                )
                .build()
          }
        }
    val lithoView = getLithoView(component)
    val column: Component = Column.create(mContext).build()
    val columnSpecId = column.typeId
    val layoutSpecId = component.typeId
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey")
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey.toString()).isEqualTo(expectedKey0)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1")
    val firstTextViewComponentSCI = scopedComponentInfos["TestViewComponent"]!![0]
    assertThat(firstTextViewComponentSCI.context.globalKey.toString()).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2")
    val firstTextInputSCI = scopedComponentInfos["TextInput"]!![0]
    assertThat(firstTextInputSCI.context.globalKey.toString()).isEqualTo(expectedKey3)
  }

  @Test
  fun testColumnSiblingsUniqueKeyRequirement() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(Column.create(c).key("sameKey"))
                .child(Column.create(c).key("sameKey"))
                .build()
          }
        }
    getLithoView(component)
    val expectedError =
        ("The manual key " +
            "sameKey you are setting on " +
            "this Column is a duplicate and will be changed into a unique one. This will " +
            "result in unexpected behavior if you don't change it.")
    assertThat(mComponentsReporter!!.loggedMessages)
        .contains(Pair.create(LogLevel.WARNING, expectedError))
  }

  @Test
  fun testAutogenSiblingsUniqueKeys() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text(""))
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text(""))
                .build()
          }
        }
    val layoutSpecId = component.typeId
    val text: Component = Text.create(mContext).text("").build()
    val textSpecId = text.typeId
    val column: Component = Column.create(mContext).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId)
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey.toString()).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1")
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey.toString()).isEqualTo(expectedKey1)
  }

  @Test
  fun testAutogenSiblingsUniqueKeysSkipsManualKeys() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                .child(Text.create(c).text("").widthDip(10f).heightDip(10f).key("manual_key"))
                .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                .build()
          }
        }
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val firstSCI = scopedComponentInfos["Text"]!![0]
    val secondSCI = scopedComponentInfos["Text"]!![1]
    val fourthSCI = scopedComponentInfos["Text"]!![3]
    val firstKey = firstSCI.context.globalKey.toString()
    val secondKey = secondSCI.context.globalKey.toString()
    val fourthKey = fourthSCI.context.globalKey.toString()
    assertThat(firstKey).isNotBlank
    assertThat(secondKey).isEqualTo("$firstKey!1")
    // The third key is a manual key, so will have no impact on the unique suffix
    assertThat(fourthKey).isEqualTo("$firstKey!2")
  }

  @Test
  fun testAutogenColumnSiblingsUniqueKeys() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(
                    Column.create(mContext)
                        .child(Text.create(c).widthDip(10f).heightDip(10f).text("")),
                )
                .child(
                    Column.create(mContext)
                        .child(Text.create(c).widthDip(10f).heightDip(10f).text("")),
                )
                .build()
          }
        }
    val layoutSpecId = component.typeId
    val text: Component = Text.create(mContext).text("").build()
    val textSpecId = text.typeId
    val column: Component = Column.create(mContext).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnTypeId,
            columnTypeId,
            textSpecId,
        )
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey.toString()).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnTypeId,
            "$columnTypeId!1",
            textSpecId,
        )
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey.toString()).isEqualTo(expectedKey1)
  }

  @Test
  fun testAutogenSiblingsUniqueKeysNested() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text(""))
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text(""))
                .build()
          }
        }
    val root: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(component)
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text("test"))
                .child(Text.create(mContext).widthDip(10f).heightDip(10f).text("test"))
                .build()
          }
        }
    val layoutSpecId = root.typeId
    val nestedLayoutSpecId = component.typeId
    val text: Component = Text.create(mContext).text("").build()
    val textSpecId = text.typeId
    val column: Component = Column.create(mContext).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(root)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnTypeId,
            nestedLayoutSpecId,
            columnTypeId,
            textSpecId,
        )
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey.toString()).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnTypeId,
            nestedLayoutSpecId,
            columnTypeId,
            "$textSpecId!1",
        )
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey.toString()).isEqualTo(expectedKey1)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId)
    val thirdTextSCI = scopedComponentInfos["Text"]!![2]
    assertThat(thirdTextSCI.context.globalKey.toString()).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1")
    val fourthTextSCI = scopedComponentInfos["Text"]!![3]
    assertThat(fourthTextSCI.context.globalKey.toString()).isEqualTo(expectedKey3)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(requireNotNull(mContext))
    lithoView.setComponent(component)
    lithoView.measure(unspecified(640), unspecified(480))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun structuralGlobalKey_toString_isHashCodeBased_inNonDebugBuilds() {
    // Debug builds keep the full structural key string (used for inspection); non-debug builds skip
    // the O(depth^2) rebuild and return a cheap identity string, since toString() is no longer used
    // as a functional key off the debug path.
    val parent = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val default = DefaultGlobalKey(key = 2, position = 3, parent = parent)
    val manual = ManualGlobalKey(key = "\$k", position = 0, parent = parent)

    val wasDebugModeEnabled = LithoDebugConfigurations.isDebugModeEnabled
    try {
      LithoDebugConfigurations.isDebugModeEnabled = true
      assertThat(default.toString()).isEqualTo("1,2!3")
      assertThat(manual.toString()).isEqualTo("1,\$k")

      LithoDebugConfigurations.isDebugModeEnabled = false
      assertThat(default.toString()).isEqualTo("GlobalKey@" + default.hashCode())
      assertThat(manual.toString()).isEqualTo("GlobalKey@" + manual.hashCode())
    } finally {
      LithoDebugConfigurations.isDebugModeEnabled = wasDebugModeEnabled
    }
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
