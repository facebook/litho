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

package com.facebook.samples.litho.kotlin.treeprops

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.TreeProp
import com.facebook.litho.TreePropProvider
import com.facebook.litho.legacyTreePropOf
import com.facebook.litho.treePropOf

// declaring_tree_prop_recommended_start
val typefaceTreeProp: TreeProp<Typeface> = treePropOf { error("No default value supplied") }
val titleTreeProp: TreeProp<String?> = legacyTreePropOf()

// declaring_tree_prop_recommended_end

class TreePropsExampleComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    // declaring_tree_prop_start
    return TreePropProvider(
        typefaceTreeProp to Typeface.DEFAULT_BOLD,
        titleTreeProp to getTextTitle(),
        legacyTreePropOf<Int>() to Color.RED) {
          TreePropsChildComponent()
        }
    // declaring_tree_prop_end
  }
}

private fun getTextTitle(): String {
  return "Text Title"
}
