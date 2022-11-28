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
 * This abstraction allows you to represent any given predicate on a [TestNode]. This is the base to
 * the mechanism that allows filtering test nodes to perform assertions or the matchers used in the
 * assertions.
 */
class TestNodeMatcher(val description: String, private val predicate: (TestNode) -> Boolean) {

  fun matches(node: TestNode): Boolean = predicate(node)

  infix fun or(nodeMatcher: TestNodeMatcher): TestNodeMatcher =
      TestNodeMatcher("$description or ${nodeMatcher.description}") {
        matches(it) || nodeMatcher.matches(it)
      }

  infix fun and(nodeMatcher: TestNodeMatcher): TestNodeMatcher =
      TestNodeMatcher("$description and ${nodeMatcher.description}") {
        matches(it) && nodeMatcher.matches(it)
      }

  operator fun not(): TestNodeMatcher = TestNodeMatcher("not $description") { !matches(it) }
}
