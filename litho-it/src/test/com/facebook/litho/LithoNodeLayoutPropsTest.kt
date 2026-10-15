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
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutWithSizeSpecWithCachedValue
import com.facebook.litho.widget.LayoutWithSizeSpecWithCachedValueSpec
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.px
import com.facebook.rendercore.visibility.VisibleEvent
import com.facebook.yoga.YogaEdge
import java.lang.reflect.Proxy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** A node's layout props, replayed into a sink that is not a Yoga node. */
@RunWith(LithoTestRunner::class)
class LithoNodeLayoutPropsTest {

  private val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())

  @Test
  fun `a node's layout props reach any sink, in the order a Yoga writer receives them`() {
    val node = resolve(
        Column(
            style =
                Style.width(80.px)
                    .height(40.px)
                    .margin(start = 4.px)
                    .padding(all = 6.px)
                    .flex(grow = 1f)
                    .border(width = 2.px, color = Color.BLACK),
        ),
    )
    val calls = ArrayList<String>()

    node.copyLayoutPropsInto(layoutContext(), recording(calls))

    // The style's layout props, then the border, which is applied after every component's props.
    assertThat(calls)
        .containsExactly(
            "widthPx(80)",
            "heightPx(40)",
            "flexGrow(1.0)",
            "paddingPx(ALL, 6)",
            "marginPx(START, 4)",
            "setBorderWidth(LEFT, 2.0)",
            "setBorderWidth(TOP, 2.0)",
            "setBorderWidth(RIGHT, 2.0)",
            "setBorderWidth(BOTTOM, 2.0)",
        )
  }

  @Test
  fun `a node with no layout props of its own replays none`() {
    val node = resolve(Column())
    val calls = ArrayList<String>()

    node.copyLayoutPropsInto(layoutContext(), recording(calls))

    assertThat(calls).isEmpty()
  }

  @Test
  fun `a Yoga node gets a node's props through the replay`() {
    val node = resolve(Column(style = Style.width(80.px).padding(all = 6.px)))
    val yoga = NodeConfig.createYogaNode()
    val writer = YogaLayoutProps(yoga)

    node.copyLayoutPropsInto(layoutContext(), writer)

    assertThat(yoga.width.value).isEqualTo(80f)
    assertThat(yoga.getPadding(YogaEdge.ALL).value).isEqualTo(6f)
    assertThat(writer.isPaddingSet).isTrue()
  }

  @Test
  fun `replaying a node's props into sinks does not change the node`() {
    val holder = DeferredLithoNode(null)
    val handler = EventHandlerTestUtil.create<VisibleEvent>(1, null)
    holder.addVisibleEventListener(handler)
    val sizeSpecComponent =
        LayoutWithSizeSpecWithCachedValue.create(context)
            .counter(LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter())
            .build()
    check(Component.isLayoutSpecWithSizeSpec(sizeSpecComponent))
    val node = LithoNode()
    node.appendComponent(ScopedComponentInfo(sizeSpecComponent, context, null))
    holder.copyInto(node)

    node.copyLayoutPropsInto(layoutContext(), recording(ArrayList()))
    node.copyLayoutPropsInto(layoutContext(), recording(ArrayList()))

    // The holder hands its props over where the content node is created
    // (LayoutUtils.measureDeferredNode), so however often the props are replayed, the node keeps
    // what it had.
    assertThat(node.onVisible).describedAs("after two replays").isNull()
    assertThat(holder.onVisible).describedAs("the holder's own").isSameAs(handler)
  }

  private fun resolve(component: Component): LithoNode {
    val resolveContext = context.setRenderStateContextForTests()
    return requireNotNull(Resolver.resolveTree(resolveContext, context, component))
  }

  private fun layoutContext(): LayoutContext<LithoLayoutContext> {
    val resolveContext = context.setRenderStateContextForTests()
    val lithoLayoutContext = LithoLayoutContext(
        resolveContext.treeId,
        resolveContext.cache,
        context,
        resolveContext.treeState,
        resolveContext.layoutVersion,
        resolveContext.rootComponentId,
        resolveContext.isAccessibilityEnabled,
        LayoutCache(),
        null,
        null,
    )
    return LayoutContext(context.androidContext, lithoLayoutContext, 0, LayoutCache(), null)
  }

  /** A [LayoutProps] that notes each call it receives, with its arguments. */
  private fun recording(calls: MutableList<String>): LayoutProps =
      Proxy.newProxyInstance(
          LayoutProps::class.java.classLoader,
          arrayOf(LayoutProps::class.java),
      ) { _, method, args ->
        calls.add("${method.name}(${args?.joinToString() ?: ""})")
        null
      } as LayoutProps
}
