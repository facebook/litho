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

package com.facebook.litho.testing

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.widthPercent
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.px
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.collection.LazyList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.mock

@RunWith(LithoTestRunner::class)
class TestCollectionDescriptionHelperTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `prints just recycler string when no children`() {
    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { LazyList {} }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .isEqualTo(testCollection.recyclerView.toString())
  }
  @Ignore("t138559546")
  @Test
  fun `prints recycler with fully visible child item info with id`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          LazyList {
            child(id = "identifier", deps = emptyArray()) { Column(style = Style.height(50.px)) }
          }
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(3)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: identifier, isVisible: true\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,50\}
          |
        """
                .trimMargin())
  }
  @Ignore("t138559546")
  @Test
  fun `prints recycler with partially visible child item info with id`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          LazyList {
            child(id = "identifier", deps = emptyArray()) { Column(style = Style.height(150.px)) }
          }
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(3)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: identifier, isVisible: true\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,150\}
          |
        """
                .trimMargin())
  }
  @Ignore("t138559546")
  @Test
  fun `prints recycler with invisible child item info without id set`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          LazyList { child(Column(style = Style.height(0.px))) }
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(3)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: \d+:1, isVisible: false\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,0\}
          |
        """
                .trimMargin())
  }
  @Ignore("t138559546")
  @Test
  fun `prints recycler with multiple children including fallback`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          LazyList {
            child(Column(style = Style.height(10.px)))
            child(id = "row", component = Row(style = Style.height(100.px)))
            child(Text(text = "word", style = Style.height(10.px)))
          }
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(9)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: \d+:1, isVisible: true\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,10\}
          |
          |└── index 1: Collection Item \(id: row, isVisible: true\)
          |      litho.Row\{\w+ V.E..... .. 0,0-100,100\}
          |
          |└── index 2: Collection Item \(id: \d+:1, isVisible: false\)
          |      Text\{\w+\}
          |
        """
                .trimMargin())
  }

  @Test
  fun `prints sections recycler with mock components`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          RecyclerCollectionComponent(
              style = Style.widthPercent(100f).heightPercent(100f),
              section =
                  DataDiffSection.create<Int>(SectionContext(context))
                      .data(listOf(100, 200))
                      .renderEventHandler(
                          eventHandlerWithReturn {
                            if (it.index == 0)
                            // Delegates to DebugComponent still
                            ComponentRenderInfo.create().component(mock<Component>()).build()
                            else
                            // Skips calling debug component
                            mock<ComponentRenderInfo>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
                          })
                      .build())
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(6)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: null, isVisible: false\)
          |      litho.null\{\w+ V.E..... .. 0,0-100,0\}
          |
          |└── index 1: Collection Item \(id: null, isVisible: false\)
          |      Component${ESCAPED_DOLLAR_SIGN}MockitoMock${ESCAPED_DOLLAR_SIGN}\w+\{\w+\}
          |
        """
                .trimMargin())
  }

  private companion object {
    const val ESCAPED_DOLLAR_SIGN = "\\$"
  }
}
