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

import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.widthPercent
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.viewcompat.SimpleViewBinder
import com.facebook.litho.viewcompat.ViewCreator
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.ViewRenderInfo
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.mock

@Ignore("This test stopped working when run against API 16") // TODO: T138559546
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
          |└── index 0: Collection Item \(id: identifier, visibility: full\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,50\}
          |
        """
                .trimMargin())
  }

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
          |└── index 0: Collection Item \(id: identifier, visibility: partial\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,150\}
          |
        """
                .trimMargin())
  }

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
          |└── index 0: Collection Item \(id: \d+:1, visibility: none\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,0\}
          |
        """
                .trimMargin())
  }

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
          |└── index 0: Collection Item \(id: \d+:1, visibility: full\)
          |      litho.Column\{\w+ V.E..... .. 0,0-100,10\}
          |
          |└── index 1: Collection Item \(id: row, visibility: partial\)
          |      litho.Row\{\w+ V.E..... .. 0,0-100,100\}
          |
          |└── index 2: Collection Item \(id: \d+:1, visibility: none\)
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
          |└── index 0: Collection Item \(id: null, visibility: none\)
          |      litho.null\{\w+ V.E..... .. 0,0-100,0\}
          |
          |└── index 1: Collection Item \(id: null, visibility: none\)
          |      Component${ESCAPED_DOLLAR_SIGN}MockitoMock${ESCAPED_DOLLAR_SIGN}\w+\{\w+\}
          |
        """
                .trimMargin())
  }

  @Test
  fun `prints sections recycler with TextView child`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          RecyclerCollectionComponent(
              style = Style.widthPercent(100f).heightPercent(100f),
              section =
                  DataDiffSection.create<Int>(SectionContext(context))
                      .data(listOf(100))
                      .renderEventHandler(
                          eventHandlerWithReturn {
                            ViewRenderInfo.create()
                                .viewCreator(VIEW_CREATOR)
                                .viewBinder(
                                    object : SimpleViewBinder<LinearLayout>() {
                                      override fun bind(view: LinearLayout) {
                                        view.addView(
                                            TextView(view.context).apply {
                                              text = "An embedded view! ${it.model}"
                                              textSize = 16f
                                              layoutParams =
                                                  ViewGroup.LayoutParams(
                                                      ViewGroup.LayoutParams.WRAP_CONTENT,
                                                      ViewGroup.LayoutParams.WRAP_CONTENT)
                                            })
                                      }
                                    })
                                .build()
                          })
                      .build())
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(4)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: null, visibility: full\)
          |      LinearLayout@\w+
          |        TextView@\w+ \(Found text: "An embedded view! 100", view is visible\)
          |
        """
                .trimMargin())
  }

  @Test
  fun `prints sections recycler with ImageView child`() {
    val gradientDrawable = GradientDrawable()

    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          RecyclerCollectionComponent(
              style = Style.widthPercent(100f).heightPercent(100f),
              section =
                  DataDiffSection.create<Int>(SectionContext(context))
                      .data(listOf(100))
                      .renderEventHandler(
                          eventHandlerWithReturn {
                            ViewRenderInfo.create()
                                .viewCreator(VIEW_CREATOR)
                                .viewBinder(
                                    object : SimpleViewBinder<LinearLayout>() {
                                      override fun bind(view: LinearLayout) {
                                        view.addView(
                                            ImageView(view.context).apply {
                                              setImageDrawable(gradientDrawable)
                                              layoutParams =
                                                  ViewGroup.LayoutParams(
                                                      ViewGroup.LayoutParams.WRAP_CONTENT,
                                                      ViewGroup.LayoutParams.WRAP_CONTENT)
                                            })
                                      }
                                    })
                                .build()
                          })
                      .build())
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(4)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: null, visibility: full\)
          |      LinearLayout@\w+
          |        ImageView@\w+ \(Found drawable: "$gradientDrawable", view is visible\)
          |
        """
                .trimMargin())
  }

  @Test
  fun `prints sections recycler with views where some are not visible`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          RecyclerCollectionComponent(
              style = Style.widthPercent(100f).heightPercent(100f),
              section =
                  DataDiffSection.create<Int>(SectionContext(context))
                      .data(listOf(100, 200, 300))
                      .renderEventHandler(
                          eventHandlerWithReturn {
                            ViewRenderInfo.create()
                                .viewCreator(VIEW_CREATOR)
                                .viewBinder(
                                    object : SimpleViewBinder<LinearLayout>() {
                                      override fun bind(view: LinearLayout) {
                                        view.addView(
                                            TextView(view.context).apply {
                                              text = "An embedded view! ${it.model}"
                                              textSize = 16f
                                              layoutParams =
                                                  ViewGroup.LayoutParams(
                                                      ViewGroup.LayoutParams.WRAP_CONTENT, 55)
                                            })
                                      }
                                    })
                                .build()
                          })
                      .build())
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(11)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: null, visibility: full\)
          |      LinearLayout@\w+
          |        TextView@\w+ \(Found text: "An embedded view! 100", view is visible\)
          |
          |└── index 1: Collection Item \(id: null, visibility: partial\)
          |      LinearLayout@\w+
          |        TextView@\w+ \(Found text: "An embedded view! 200", view is visible\)
          |
          |└── index 2: Collection Item \(id: null, visibility: none\)
          |      Found null item view \(no additional information available\)
          |
        """
                .trimMargin())
  }

  @Test
  fun `prints sections recycler with mixed view and component children`() {
    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          RecyclerCollectionComponent(
              style = Style.widthPercent(100f).heightPercent(100f),
              section =
                  DataDiffSection.create<Int>(SectionContext(context))
                      .data(listOf(0, 1))
                      .renderEventHandler(
                          eventHandlerWithReturn {
                            if (it.index == 0) {
                              return@eventHandlerWithReturn ComponentRenderInfo.create()
                                  .component(Text(text = "A Litho Component: ${it.index}"))
                                  .build()
                            }

                            ViewRenderInfo.create()
                                .viewCreator(VIEW_CREATOR)
                                .viewBinder(
                                    object : SimpleViewBinder<LinearLayout>() {
                                      override fun bind(view: LinearLayout) {
                                        view.addView(
                                            TextView(view.context).apply {
                                              text = "An embedded view! ${it.model}"
                                              textSize = 16f
                                              layoutParams =
                                                  ViewGroup.LayoutParams(
                                                      ViewGroup.LayoutParams.WRAP_CONTENT,
                                                      ViewGroup.LayoutParams.WRAP_CONTENT)
                                            })
                                      }
                                    })
                                .build()
                          })
                      .build())
        }
    val testCollection = testLithoView.findCollectionComponent()!!

    assertThat(TestCollectionDescriptionHelper.collectionToString(testCollection))
        .startsWith(testCollection.recyclerView.toString())
        .hasLineCount(7)
        .matches(
            """
          |${testCollection.recyclerView}
          |└── index 0: Collection Item \(id: null, visibility: full\)
          |      litho.Text\{\w+ V.E..... .. 0,0-100,\d+ text="A Litho Component: 0"\}
          |
          |└── index 1: Collection Item \(id: null, visibility: full\)
          |      LinearLayout@\w+
          |        TextView@\w+ \(Found text: "An embedded view! 1", view is visible\)
          |
        """
                .trimMargin())
  }

  private companion object {
    const val ESCAPED_DOLLAR_SIGN = "\\$"

    private val VIEW_CREATOR: ViewCreator<LinearLayout> =
        ViewCreator<LinearLayout> { c, _ ->
          LinearLayout(c).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
          }
        }
  }
}
