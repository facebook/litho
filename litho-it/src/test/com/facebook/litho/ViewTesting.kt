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

import android.view.View
import com.facebook.litho.testing.TestLithoView
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.testing.match.ViewMatchNode

/** Provide the compatible testing API of rendercore for TestLithoView. */
fun TestLithoView.assertMatches(matchNode: ViewMatchNode) {
  ViewAssertions.assertThat(lithoView).matches(matchNode)
}

/** Kotlin DSL for creating and configuring a root ViewMatchNode. */
inline fun <reified T : View> match(init: (ViewMatchNode.() -> Unit) = {}) =
    ViewMatchNode.forType(T::class.java).apply(init)

/** Kotlin DSL for creating and configuring a child ViewMatchNode. */
inline fun <reified T : View> ViewMatchNode.child(init: (ViewMatchNode.() -> Unit) = {}) {
  this.child(match<T>(init))
}
