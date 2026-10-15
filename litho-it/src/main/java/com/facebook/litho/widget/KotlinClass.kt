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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.layout.flexbox.Column

class KotlinClass(
    private val getPropDefaultAssertion: ((String) -> Unit)? = null,
    private val constPropDefaultAssertion: ((Int) -> Unit)? = null,
    private val jvmFieldPropDefaultAssertion: ((List<String>) -> Unit)? = null,
    private val justPropDefaultAssertion: ((Long) -> Unit)? = null,
    private val getPropDefault: String = GetPropDefault,
    private val constPropDefault: Int = ConstPropDefault,
    private val jvmFieldPropDefault: List<String> = JvmFieldPropDefault,
    private val justPropDefault: Long = JustPropDefault,
) : KComponent() {

  override fun ComponentScope.render(): Component {

    getPropDefaultAssertion?.invoke(getPropDefault)
    constPropDefaultAssertion?.invoke(constPropDefault)
    jvmFieldPropDefaultAssertion?.invoke(jvmFieldPropDefault)
    justPropDefaultAssertion?.invoke(justPropDefault)

    return Column {}
  }

  companion object {
    internal const val GetPropDefault: String = "Default"
    internal const val ConstPropDefault: Int = 10_000
    internal @JvmField val JvmFieldPropDefault: List<String> = listOf("Prop", "Default")
    internal const val JustPropDefault: Long = 54321L
  }
}
