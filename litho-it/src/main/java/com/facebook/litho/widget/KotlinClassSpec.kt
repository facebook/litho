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

package com.facebook.litho.widget

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault

@LayoutSpec
class KotlinClassSpec {

  companion object {

    @get:PropDefault val getPropDefault: String = "Default"
    @PropDefault const val constPropDefault: Int = 10_000
    @PropDefault @JvmField val jvmFieldPropDefault: List<String> = listOf("Prop", "Default")
    @PropDefault val justPropDefault: Long = 54321L

    @JvmStatic
    @OnCreateLayout
    fun onCreateLayout(
        c: ComponentContext,
        @Prop(optional = true) getPropDefaultAssertion: ((String) -> Unit)?,
        @Prop(optional = true) constPropDefaultAssertion: ((Int) -> Unit)?,
        @Prop(optional = true) jvmFieldPropDefaultAssertion: ((List<String>) -> Unit)?,
        @Prop(optional = true) justPropDefaultAssertion: ((Long) -> Unit)?,
        @Prop(optional = true) getPropDefault: String,
        @Prop(optional = true) constPropDefault: Int,
        @Prop(optional = true) jvmFieldPropDefault: List<String>,
        @Prop(optional = true) justPropDefault: Long,
    ): Component {
      getPropDefaultAssertion?.invoke(getPropDefault)
      constPropDefaultAssertion?.invoke(constPropDefault)
      jvmFieldPropDefaultAssertion?.invoke(jvmFieldPropDefault)
      justPropDefaultAssertion?.invoke(justPropDefault)

      return Column.create(c).build()
    }
  }
}
