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

/**
 * This is the entry point through which nodes are discovered for testing.
 *
 * It will typically be implemented by a test rule
 */
interface TestNodeSelectionProvider {

  /**
   * Finds a test node that matches the given condition.
   *
   * The result is a lazy representation of the matching node which can then be evaluated on-demand.
   * For usage patterns and concepts, see [TestNodeSelection]
   */
  fun selectNode(matcher: TestNodeMatcher): TestNodeSelection

  /**
   * Finds a collection of tests nodes that match the given conditions.
   *
   * The result is a lazy representation of the matching nodes which can then be evaluated
   * on-demand. For usage patterns and concepts, see [TestNodeCollectionSelection]
   */
  fun selectNodes(matcher: TestNodeMatcher): TestNodeCollectionSelection
}
