/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.DslScope
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.testing.match.ViewMatchNode

/** Shorthand for creating a [View.MeasureSpec.EXACTLY] measure spec. */
fun exactly(px: Int) = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY)

/** Shorthand for creating a [View.MeasureSpec.AT_MOST] measure spec. */
fun atMost(px: Int) = View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.AT_MOST)

/** Shorthand for creating a [View.MeasureSpec.UNSPECIFIED] measure spec. */
fun unspecified() = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

/** Shorthand helper to create a root component that needs a DslScope. */
fun LithoViewRule.setRoot(componentFunction: DslScope.() -> Component) =
    setRoot(with(DslScope(context)) { componentFunction() })

/**
 * Measures/lays out/attaches to window if not already done, than matches the hierarchy against the
 * provided [ViewMatchNode].
 */
fun LithoViewRule.assertMatches(matchNode: ViewMatchNode) {
  measure().layout().attachToWindow()
  ViewAssertions.assertThat(lithoView).matches(matchNode)
}

/** Kotlin DSL for creating and configuring a root ViewMatchNode. */
inline fun <reified T : View> match(init: (ViewMatchNode.() -> Unit) = {}) =
    ViewMatchNode.forType(T::class.java).apply(init)

/** Kotlin DSL for creating and configuring a child ViewMatchNode. */
inline fun <reified T : View> ViewMatchNode.child(init: (ViewMatchNode.() -> Unit) = {}) {
  this.child(match<T>(init))
}
