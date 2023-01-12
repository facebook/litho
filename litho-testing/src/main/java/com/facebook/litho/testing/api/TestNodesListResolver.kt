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

import com.facebook.litho.Component
import com.facebook.litho.LithoView

class TestNodesListResolver {

  private val lithoViewComponentsTraverser = LithoViewComponentsTraverser()

  fun getCurrentTestNodes(lithoView: LithoView): List<TestNode> {
    val componentToTestTreeNode: MutableMap<Component, TestNode> = mutableMapOf()

    lithoViewComponentsTraverser.traverse(lithoView) { component, parentComponent ->
      // 1. create the test data
      val componentTestNode = TestNode(component)

      // 2. store the association between component and test node
      componentToTestTreeNode[component] = componentTestNode

      // 3. attempt to store parent data if exists
      val parentTestNode = parentComponent?.let { componentToTestTreeNode[it] }
      componentTestNode.parent = parentTestNode

      // 4. attempt to update children's of the parent
      if (parentTestNode != null) {
        val siblings = parentTestNode.children + componentTestNode
        parentTestNode.children = siblings
      }
    }

    return componentToTestTreeNode.values.toList()
  }
}
