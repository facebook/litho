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

package com.facebook.litho.testing.treeprop

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.getTreeProp
import com.facebook.litho.layout.flexbox.Column

/**
 * Used in [TreePropSectionTestLeafGroupSpec] to mutate the treeprops. Currently without any effect
 * as treeprops aren't propagated from Sections. See T29504095.
 */
class TreePropSectionTestLeafLayout(
    private val resultPropB: TreePropSectionTestLeafGroupSpec.Result?,
    private val resultPropA: TreePropSectionTestLeafGroupSpec.Result? = null,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val propA = getTreeProp<TreePropNumberType>()
    val propB = getTreeProp<TreePropStringType>()

    if (resultPropA != null && propA != null) {
      resultPropA.prop = TreePropNumberType(propA.value + 1)
    }
    if (resultPropB != null && propB != null) {
      resultPropB.prop = TreePropStringType("${propB.value}_changed_again")
    }
    return Column {}
  }
}
