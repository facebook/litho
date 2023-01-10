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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LayoutOutput.getLayoutOutput
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
import com.facebook.litho.widget.TreePropTestContainerComponentSpec
import com.facebook.rendercore.LogLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentGlobalKeyTest {

  private lateinit var context: ComponentContext
  private lateinit var componentsReporter: TestComponentsReporter

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    componentsReporter = TestComponentsReporter()
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    ComponentsReporter.provide(componentsReporter)
  }

  @Test
  fun testComponentKey() {
    val component: Component = SimpleMountSpecTester.create(context).build()
    assertEquals(component.key, "${component.typeId.toString()}")
  }

  @Test
  fun testComponentManualKey() {
    val component: Component = SimpleMountSpecTester.create(context).key("someKey").build()
    assertEquals(component.key, "someKey")
  }

  @Test
  fun testRootComponentGlobalKey() {
    val component: Component =
        SimpleMountSpecTester.create(context).widthDip(10f).heightDip(10f).build()
    val lithoView = getLithoView(component)
    assertEquals(
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey, component.key)
  }

  @Test
  fun testRootComponentGlobalKeyManualKey() {
    val component =
        SimpleMountSpecTester.create(context).widthDip(10f).heightDip(10f).key("someKey").build()
    val lithoView = getLithoView(component)
    assertEquals(
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey, "\$someKey")
  }

  @Test
  fun testMultipleChildrenComponentKey() {
    val component = multipleChildrenComponent
    val layoutSpecId = component.typeId
    val nestedLayoutSpecId = layoutSpecId - 1
    val column = Column.create(context).build()
    val columnSpecId = column.typeId
    val lithoView = getLithoView(component)

    // Text
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$[Text2]"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    // TestViewComponent in child layout
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[TestViewComponent1]"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(1))!!.globalKey)

    // CardClip in child
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip1]"),
        LithoRenderUnit.getComponentContext(
                (lithoView.getMountItemAt(2).content as ComponentHost).getMountItemAt(0))!!
            .globalKey)

    // Text in child
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[Text1]"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(3))!!.globalKey)

    // CardClip
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, columnSpecId, "$[CardClip2]"),
        LithoRenderUnit.getComponentContext(
                (lithoView.getMountItemAt(4).content as ComponentHost).getMountItemAt(0))!!
            .globalKey)
    // TestViewComponent
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, "$[TestViewComponent2]"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(5))!!.globalKey)
  }

  @Test
  fun testSiblingsUniqueKeyRequirement() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Text.create(c).text("").key("sameKey"))
                  .child(Text.create(c).text("").key("sameKey"))
                  .build()
        }
    getLithoView(component)
    val expectedError =
        ("The manual key sameKey you are setting on this Text is a duplicate and will be changed into a unique one. This will result in unexpected behavior if you don't change it.")
    assertThat(componentsReporter.loggedMessages)
        .contains(Pair.create(LogLevel.WARNING, expectedError))
  }

  @Test
  fun testSiblingsManualKeyDeduplication() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                  .child(EditText.create(c).text("").widthDip(10f).heightDip(10f))
                  .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                  .child(EditText.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                  .build()
        }
    val lithoView = getLithoView(component)
    val column = Column.create(context).build()
    val columnSpecId = column.typeId
    val layoutSpecId = component.typeId
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(2))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(3))!!.globalKey)
  }

  @Test
  fun testSiblingsOfDifferentTypesManualKeyDeduplication() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Text.create(c).text("").key("sameKey").widthDip(10f).heightDip(10f))
                  .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                  .child(TestViewComponent.create(c).widthDip(10f).heightDip(10f).key("sameKey"))
                  .child(
                      TextInput.create(c)
                          .initialText("")
                          .key("sameKey")
                          .widthDip(10f)
                          .heightDip(10f))
                  .build()
        }
    val lithoView = getLithoView(component)
    val column = Column.create(context).build()
    val columnSpecId = column.typeId
    val layoutSpecId = component.typeId
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!1"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(2))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "\$sameKey!2"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(3))!!.globalKey)
  }

  @Test
  fun testColumnSiblingsUniqueKeyRequirement() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).key("sameKey"))
                  .child(Column.create(c).key("sameKey"))
                  .build()
        }
    getLithoView(component)
    val expectedError =
        ("The manual key sameKey you are setting on this Column is a duplicate and will be changed into a unique one. This will result in unexpected behavior if you don't change it.")
    assertThat(componentsReporter.loggedMessages)
        .contains(Pair.create(LogLevel.WARNING, expectedError))
  }

  @Test
  fun testAutogenSiblingsUniqueKeys() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text(""))
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text(""))
                  .build()
        }
    val layoutSpecId = component.typeId
    val text = Text.create(context).text("").build()
    val textSpecId = text.typeId
    val column = Column.create(context).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(component)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(1))!!.globalKey)
  }

  @Test
  fun testAutogenSiblingsUniqueKeysSkipsManualKeys() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                  .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                  .child(Text.create(c).text("").widthDip(10f).heightDip(10f).key("manual_key"))
                  .child(Text.create(c).text("").widthDip(10f).heightDip(10f))
                  .build()
        }
    val lithoView = getLithoView(component)
    val firstKey = LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey
    val secondKey = LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(1))!!.globalKey
    val fourthKey = LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(3))!!.globalKey
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
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(context)
                          .child(Text.create(c).widthDip(10f).heightDip(10f).text("")))
                  .child(
                      Column.create(context)
                          .child(Text.create(c).widthDip(10f).heightDip(10f).text("")))
                  .build()
        }
    val layoutSpecId = component.typeId
    val text = Text.create(context).text("").build()
    val textSpecId = text.typeId
    val column = Column.create(context).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(component)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, columnTypeId, textSpecId),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, "$columnTypeId!1", textSpecId),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(1))!!.globalKey)
  }

  @Test
  fun testAutogenSiblingsUniqueKeysNested() {
    val component: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text(""))
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text(""))
                  .build()
        }
    val root: Component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(component)
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text("test"))
                  .child(Text.create(context).widthDip(10f).heightDip(10f).text("test"))
                  .build()
        }
    val layoutSpecId = root.typeId
    val nestedLayoutSpecId = component.typeId
    val text = Text.create(context).text("").build()
    val textSpecId = text.typeId
    val column = Column.create(context).build()
    val columnTypeId = column.typeId
    val lithoView = getLithoView(root)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(0))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, "$textSpecId!1"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(1))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(2))!!.globalKey)
    assertEquals(
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, "$textSpecId!1"),
        LithoRenderUnit.getComponentContext(lithoView.getMountItemAt(3))!!.globalKey)
  }

  @Test
  fun testOwnerGlobalKey() {
    val root = multipleChildrenComponent
    val layoutSpecId = root.typeId
    val nestedLayoutSpecId = layoutSpecId - 1
    val columnSpecId = Column.create(context).build().typeId
    val lithoView = getLithoView(root)
    val rootGlobalKey = ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId)
    val nestedLayoutGlobalKey =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, nestedLayoutSpecId)

    // Text
    assertEquals(rootGlobalKey, getComponentAt(lithoView, 0).ownerGlobalKey)

    // TestViewComponent in child layout
    assertEquals(nestedLayoutGlobalKey, getComponentAt(lithoView, 1).ownerGlobalKey)

    // CardClip in child
    assertEquals(nestedLayoutGlobalKey, getNestedComponentAt(lithoView, 2, 0).ownerGlobalKey)

    // Text in child
    assertEquals(nestedLayoutGlobalKey, getComponentAt(lithoView, 3).ownerGlobalKey)

    // CardClip
    assertEquals(rootGlobalKey, getNestedComponentAt(lithoView, 4, 0).ownerGlobalKey)

    // TestViewComponent
    assertEquals(rootGlobalKey, getComponentAt(lithoView, 5).ownerGlobalKey)
  }

  @Test
  fun nestedTreeRemeasureKeyStabilityTest() {
    val componentWithoutRemeasure = TreePropTestContainerComponentSpec.create(context)
    val lithoView = getLithoView(componentWithoutRemeasure)
    assertEquals(
        TreePropTestContainerComponentSpec.EXPECTED_GLOBAL_KEY,
        getComponentAt(lithoView, 2).ownerGlobalKey)
  }

  private fun getLithoView(component: Component): LithoView {
    val lithoView = LithoView(context)
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
    private fun getComponentAt(lithoView: LithoView, index: Int): Component =
        LayoutOutput.getLayoutOutput(lithoView.getMountItemAt(index)).component

    private fun getNestedComponentAt(
        lithoView: LithoView,
        index: Int,
        nestedIndex: Int
    ): Component =
        LayoutOutput.getLayoutOutput(
                (lithoView.getMountItemAt(index).content as ComponentHost).getMountItemAt(
                    nestedIndex))
            .component

    private val multipleChildrenComponent: Component
      get() {
        val color = -0x10000
        val testGlobalKeyChildComponent: Component =
            object : InlineLayoutSpec() {
              @OnCreateLayout
              override fun onCreateLayout(c: ComponentContext): Component? =
                  Column.create(c)
                      .child(
                          TestViewComponent.create(c)
                              .widthDip(10f)
                              .heightDip(10f)
                              .key("[TestViewComponent1]"))
                      .child(
                          Column.create(c)
                              .backgroundColor(color)
                              .child(
                                  CardClip.create(c)
                                      .widthDip(10f)
                                      .heightDip(10f)
                                      .key("[CardClip1]")))
                      .child(
                          Text.create(c).text("Test").widthDip(10f).heightDip(10f).key("[Text1]"))
                      .build()
            }
        return object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
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
  }
}
