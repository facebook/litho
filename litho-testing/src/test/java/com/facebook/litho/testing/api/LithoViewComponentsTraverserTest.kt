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

package com.facebook.litho.testing.api

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Row
import com.facebook.litho.SpecGeneratedComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.testing.api.helpers.LayoutWithSizeSpecs
import com.facebook.litho.testing.api.helpers.SimpleLayoutWithSizeSpecs
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.collection.LazyList
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoViewComponentsTraverserTest : RunWithDebugInfoTest() {

  private val traverser = LithoViewComponentsTraverser()

  @Test
  fun `traverse should be able to traverser a simple hierarchy`() {
    val lithoView = renderLithoViewWithComponent(RootComponent())

    val result = StringBuilder()

    traverser.traverse(lithoView) { component, parent ->
      result.appendComponentData("component", component)
      parent?.let {
        result.append("  ")
        result.appendComponentData("parent", parent)
      }
      result.append('\n')
    }

    val actual = result.toString()
    val expected =
        """
              component[type=RootComponent]
              component[type=Column]  parent[type=RootComponent]
              component[type=Text, testKey=hello]  parent[type=Column]
              component[type=Header]  parent[type=Column]
              component[type=Row]  parent[type=Header]
              component[type=Text, testKey=first-name]  parent[type=Row]
              component[type=Text, testKey=last-name]  parent[type=Row]

              """
            .trimIndent()

    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `traverse should be able to traverser a complex hierarchy`() {
    val lithoView = renderLithoViewWithComponent(CollectionRootComponent())

    val result = StringBuilder()

    traverser.traverse(lithoView) { component, parent ->
      result.appendComponentData("component", component)
      parent?.let {
        result.append("  ")
        result.appendComponentData("parent", parent)
      }
      result.append('\n')
    }

    val actual = result.toString()
    val expected =
        """
          component[type=CollectionRootComponent]
          component[type=Column]  parent[type=CollectionRootComponent]
          component[type=Text, testKey=hello]  parent[type=Column]
          component[type=Header]  parent[type=Column]
          component[type=Row]  parent[type=Header]
          component[type=Text, testKey=first-name]  parent[type=Row]
          component[type=Text, testKey=last-name]  parent[type=Row]
          component[type=CollectionComponent]  parent[type=Column]
          component[type=LazyCollection]  parent[type=CollectionComponent]
          component[type=CollectionRecycler]  parent[type=LazyCollection]
          component[type=Recycler]  parent[type=CollectionRecycler]
          component[type=Text, testKey=item-#0]  parent[type=Recycler]
          component[type=Text, testKey=item-#1]  parent[type=Recycler]
          component[type=Text, testKey=item-#2]  parent[type=Recycler]
          component[type=Text, testKey=item-#3]  parent[type=Recycler]
          component[type=Text, testKey=item-#4]  parent[type=Recycler]
          component[type=Text, testKey=item-#5]  parent[type=Recycler]
          component[type=Text, testKey=item-#6]  parent[type=Recycler]
          component[type=Text, testKey=item-#7]  parent[type=Recycler]
          component[type=Text, testKey=item-#8]  parent[type=Recycler]
          component[type=Text, testKey=item-#9]  parent[type=Recycler]
          component[type=Text, testKey=item-#10]  parent[type=Recycler]

          """
            .trimIndent()

    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `traverse should be able to traverser a complex hierarchy which uses a complex nested tree result`() {
    val lithoView = renderLithoViewWithComponent(ComponentWithLayoutWithSizeSpecs())

    val result = StringBuilder()

    traverser.traverse(lithoView) { component, parent ->
      result.appendComponentData("component", component)
      parent?.let {
        result.append("  ")
        result.appendComponentData("parent", parent)
      }
      result.append('\n')
    }

    val actual = result.toString()
    val expected =
        """
          component[type=ComponentWithLayoutWithSizeSpecs]
          component[type=Column]  parent[type=ComponentWithLayoutWithSizeSpecs]
          component[type=Text, testKey=header]  parent[type=Column]
          component[type=Row]  parent[type=Column]
          component[type=LayoutWithSizeSpecs]  parent[type=Row]
          component[type=Row]  parent[type=LayoutWithSizeSpecs]
          component[type=Text, testKey=text-with-size-specs]  parent[type=Row]
          component[type=Text, testKey=row-text]  parent[type=Row]
          component[type=Text, testKey=footer]  parent[type=Column]
          
          """
            .trimIndent()

    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `traverse should be able to traverser a complex hierarchy which uses a simple nested tree result`() {
    val lithoView = renderLithoViewWithComponent(ComponentWithSimpleLayoutWithSizeSpecs())

    val result = StringBuilder()

    traverser.traverse(lithoView) { component, parent ->
      result.appendComponentData("component", component)
      parent?.let {
        result.append("  ")
        result.appendComponentData("parent", parent)
      }
      result.append('\n')
    }

    val actual = result.toString()
    val expected =
        """
          component[type=ComponentWithSimpleLayoutWithSizeSpecs]
          component[type=Column]  parent[type=ComponentWithSimpleLayoutWithSizeSpecs]
          component[type=Text, testKey=header]  parent[type=Column]
          component[type=Row]  parent[type=Column]
          component[type=SimpleLayoutWithSizeSpecs]  parent[type=Row]
          component[type=Text, testKey=text-with-size-specs]  parent[type=SimpleLayoutWithSizeSpecs]
          component[type=Text, testKey=row-text]  parent[type=Row]
          component[type=Text, testKey=footer]  parent[type=Column]
          
          """
            .trimIndent()

    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `traverse should be able to traverser a complex hierarchy with a vertical scroll wrapper`() {
    val lithoView = renderLithoViewWithComponent(ComponentWithVerticalScroll())

    val result = StringBuilder()

    traverser.traverse(lithoView) { component, parent ->
      result.appendComponentData("component", component)
      parent?.let {
        result.append("  ")
        result.appendComponentData("parent", parent)
      }
      result.append('\n')
    }

    val actual = result.toString()
    val expected =
        """
        component[type=ComponentWithVerticalScroll]
        component[type=VerticalScroll]  parent[type=ComponentWithVerticalScroll]
        component[type=Column]  parent[type=VerticalScroll]
        component[type=Header]  parent[type=Column]
        component[type=Row]  parent[type=Header]
        component[type=Text, testKey=first-name]  parent[type=Row]
        component[type=Text, testKey=last-name]  parent[type=Row]
        component[type=RootComponent]  parent[type=Column]
        component[type=Column]  parent[type=RootComponent]
        component[type=Text, testKey=hello]  parent[type=Column]
        component[type=Header]  parent[type=Column]
        component[type=Row]  parent[type=Header]
        component[type=Text, testKey=first-name]  parent[type=Row]
        component[type=Text, testKey=last-name]  parent[type=Row]
    
        """
            .trimIndent()

    Assertions.assertThat(actual).isEqualTo(expected)
  }

  private fun renderLithoViewWithComponent(component: Component): LithoView {
    val componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val lithoView = LithoView(componentContext)
    val componentTree = ComponentTree.create(componentContext).build()

    componentTree.setRoot(component)

    lithoView.componentTree = componentTree

    lithoView.onAttachedToWindowForTest()
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  private fun StringBuilder.appendComponentData(label: String, component: Component) {
    append(label)
    append("[type=${component.javaClass.simpleName}")

    val testKey = if (component is SpecGeneratedComponent) component.commonProps?.testKey else null
    if (!testKey.isNullOrBlank() && testKey != "null") {
      append(", testKey=$testKey")
    }

    append("]")
  }

  private class RootComponent : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Column {
        child(Text("Hello", Style.testKey("hello")))

        child(Header())
      }
    }
  }

  private class Header : KComponent() {
    override fun ComponentScope.render(): Component? {
      return Row {
        child(Text("First Name", Style.testKey("first-name")))

        child(Text("Last name", Style.testKey("last-name")))
      }
    }
  }

  private class CollectionRootComponent : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Column {
        child(Text("Hello", Style.testKey("hello")))
        child(Header())
        child(CollectionComponent())
      }
    }
  }

  private class CollectionComponent : KComponent() {
    override fun ComponentScope.render(): Component? {
      return LazyList {
        children(items = (0..10), id = { it }) { Text("Item #$it", Style.testKey("item-#$it")) }
      }
    }
  }

  /**
   * This component uses a [LayoutWithSizeSpecs] which uses the [@OnCreateLayoutWithSizeSpecs].
   * Internally this creates a [NestedTreeHolderResult], which requires a special handling when
   * traversing down the Component Tree to build the Testing Tree.
   *
   * Therefore, we need to test this special scenario.
   */
  private class ComponentWithLayoutWithSizeSpecs : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Column {
        child(Text("Header", Style.testKey("header")))
        child(
            Row {
              child(LayoutWithSizeSpecs.create(context).build())
              child(Text("Row-text", Style.testKey("row-text")))
            })

        child(Text("Footer", Style.testKey("footer")))
      }
    }
  }

  private class ComponentWithSimpleLayoutWithSizeSpecs : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Column {
        child(Text("Header", Style.testKey("header")))
        child(
            Row {
              child(SimpleLayoutWithSizeSpecs.create(context).build())
              child(Text("Row-text", Style.testKey("row-text")))
            })

        child(Text("Footer", Style.testKey("footer")))
      }
    }
  }

  private class ComponentWithVerticalScroll : KComponent() {
    override fun ComponentScope.render(): Component {
      return VerticalScroll {
        Column {
          child(Header())
          child(RootComponent())
        }
      }
    }
  }
}
