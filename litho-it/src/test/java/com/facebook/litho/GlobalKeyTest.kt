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
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.CardClip
import com.facebook.litho.widget.EditText
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextInput
import com.facebook.rendercore.LogLevel
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
    val component: Component = SimpleMountSpecTester.create(mContext).build()
    Assert.assertEquals(component.key, component.typeId.toString() + "")
  }

  @Test
  fun testComponentManualKey() {
    val component: Component = SimpleMountSpecTester.create(mContext).key("someKey").build()
    Assert.assertEquals(component.key, "someKey")
  }

  @Test
  fun testRootComponentGlobalKey() {
    val component: Component =
        SimpleMountSpecTester.create(mContext).widthDip(10f).heightDip(10f).build()
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val rootScopedComponentInfo = scopedComponentInfos["SimpleMountSpecTester"]!![0]
    val scopedContext = rootScopedComponentInfo.context
    assertThat(scopedContext).isNotNull
    assertThat(scopedContext.globalKey).isEqualTo(component.key)
  }

  @Test
  fun testRootComponentGlobalKeyManualKey() {
    val component: Component =
        SimpleMountSpecTester.create(mContext).widthDip(10f).heightDip(10f).key("someKey").build()
    val lithoView = getLithoView(component)
    val scopedComponentInfos = getScopedComponentInfos(lithoView)
    val rootScopedComponentInfo = scopedComponentInfos["SimpleMountSpecTester"]!![0]
    val scopedContext = rootScopedComponentInfo.context
    assertThat(scopedContext).isNotNull
    assertThat(scopedContext.globalKey).isEqualTo("\$someKey")
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
    Assert.assertEquals(scopedComponentInfoText.context.globalKey, expectedGlobalKeyText)
    // TestViewComponent in child layout
    val expectedGlobalKeyTestViewComponent =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[TestViewComponent1]")
    val scopedComponentInfoTestViewComponent = scopedComponentInfos["TestViewComponent"]!![0]
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent.context.globalKey, expectedGlobalKeyTestViewComponent)
    // CardClip in child
    val expectedGlobalKeyCardClip =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip1]")
    val scopedComponentInfoCardClip = scopedComponentInfos["CardClip"]!![0]
    Assert.assertEquals(scopedComponentInfoCardClip.context.globalKey, expectedGlobalKeyCardClip)

    // Text in child
    val expectedGlobalKeyTextChild =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[Text1]")
    val scopedComponentInfoTextChild = scopedComponentInfos["Text"]!![1]
    Assert.assertEquals(scopedComponentInfoTextChild.context.globalKey, expectedGlobalKeyTextChild)

    // CardClip
    val expectedGlobalKeyCardClip2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, columnSpecId, "$[CardClip2]")
    val scopedComponentInfoCardClip2 = scopedComponentInfos["CardClip"]!![1]
    Assert.assertEquals(scopedComponentInfoCardClip2.context.globalKey, expectedGlobalKeyCardClip2)

    // TestViewComponent
    val expectedGlobalKeyTestViewComponent2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, "$[TestViewComponent2]")
    val scopedComponentInfoTestViewComponent2 = scopedComponentInfos["TestViewComponent"]!![1]
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent2.context.globalKey,
        expectedGlobalKeyTestViewComponent2)
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
                .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                .child(EditText.create(c).text("").widthDip(10f).heightDip(10f))
                .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
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
    val firstEditTextSCI = scopedComponentInfos["EditText"]!![0]
    assertThat(firstEditTextSCI.context.globalKey).isEqualTo(expectedKey0)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1")
    val secondEditTextSCI = scopedComponentInfos["EditText"]!![2]
    assertThat(secondEditTextSCI.context.globalKey).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2")
    val thirdEditTextSCI = scopedComponentInfos["EditText"]!![3]
    assertThat(thirdEditTextSCI.context.globalKey).isEqualTo(expectedKey3)
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
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10f).heightDip(10f))
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
    assertThat(firstTextSCI.context.globalKey).isEqualTo(expectedKey0)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1")
    val firstTextViewComponentSCI = scopedComponentInfos["TestViewComponent"]!![0]
    assertThat(firstTextViewComponentSCI.context.globalKey).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2")
    val firstTextInputSCI = scopedComponentInfos["TextInput"]!![0]
    assertThat(firstTextInputSCI.context.globalKey).isEqualTo(expectedKey3)
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
    assertThat(firstTextSCI.context.globalKey).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1")
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey).isEqualTo(expectedKey1)
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
    val firstKey = firstSCI.context.globalKey
    val secondKey = secondSCI.context.globalKey
    val fourthKey = fourthSCI.context.globalKey
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
                        .child(Text.create(c).widthDip(10f).heightDip(10f).text("")))
                .child(
                    Column.create(mContext)
                        .child(Text.create(c).widthDip(10f).heightDip(10f).text("")))
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
            layoutSpecId, columnTypeId, columnTypeId, textSpecId)
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, "$columnTypeId!1", textSpecId)
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey).isEqualTo(expectedKey1)
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
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId)
    val firstTextSCI = scopedComponentInfos["Text"]!![0]
    assertThat(firstTextSCI.context.globalKey).isEqualTo(expectedKey0)
    val expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, "$textSpecId!1")
    val secondTextSCI = scopedComponentInfos["Text"]!![1]
    assertThat(secondTextSCI.context.globalKey).isEqualTo(expectedKey1)
    val expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId)
    val thirdTextSCI = scopedComponentInfos["Text"]!![2]
    assertThat(thirdTextSCI.context.globalKey).isEqualTo(expectedKey2)
    val expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1")
    val fourthTextSCI = scopedComponentInfos["Text"]!![3]
    assertThat(fourthTextSCI.context.globalKey).isEqualTo(expectedKey3)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(mContext)
    lithoView.setComponent(component)
    lithoView.measure(unspecified(640), unspecified(480))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  companion object {
    private val multipleChildrenComponent: Component
      private get() {
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
                            .key("[TestViewComponent1]"))
                    .child(
                        Column.create(c)
                            .backgroundColor(color)
                            .child(
                                CardClip.create(c).widthDip(10f).heightDip(10f).key("[CardClip1]")))
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
                                CardClip.create(c).widthDip(10f).heightDip(10f).key("[CardClip2]")))
                    .child(
                        TestViewComponent.create(c)
                            .widthDip(10f)
                            .heightDip(10f)
                            .key("[TestViewComponent2]"))
                    .build()
              }
            }
        return testGlobalKeyChild
      }
  }
}
