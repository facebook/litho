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

import com.facebook.litho.ClickEvent

/**
 * Dispatches a click event for the node represented in the given selection.
 *
 * It will throw an exception if this selection is invalid.
 */
fun TestNodeSelection.performClick() {
  val node = fetchTestNode("Failed: performClick")

  val clickHandler =
      node.clickHandler
          ?: throwGeneralError(
              "Failed performClick: the selected node has no click handler", selector, node)

  clickHandler.dispatchEvent(ClickEvent())
}
