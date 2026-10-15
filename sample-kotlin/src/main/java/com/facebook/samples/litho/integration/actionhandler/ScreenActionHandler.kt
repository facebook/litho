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

package com.facebook.samples.litho.integration.actionhandler

/**
 * Action handler interface for component-to-host communication.
 *
 * Components never navigate directly. Instead, they invoke methods on an action handler interface.
 * The host (Activity, Fragment, or ViewModel) implements the interface and handles navigation,
 * analytics, and other side effects.
 *
 * This pattern is used extensively in IG's CommentListViewActionHandler which has ~50 action
 * methods that components invoke for user interactions.
 *
 * Benefits:
 * - Components remain pure UI with no navigation/platform dependencies
 * - Easy to test: just verify the handler method was called
 * - In apps with Compose, add @Stable to tell the framework the identity is stable across
 *   re-renders
 */
// start_example
interface ScreenActionHandler {
  fun onItemClick(itemId: Int)

  fun onItemLongPress(itemId: Int)

  fun onRetryClick()

  fun onProfileClick(userId: String)

  fun onShareClick(itemId: Int)
}
// end_example
