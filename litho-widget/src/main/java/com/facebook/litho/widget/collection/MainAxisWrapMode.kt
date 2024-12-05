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

package com.facebook.litho.widget.collection

/** Specifies how a [Collection] will wrap its contents across the main axis. */
enum class MainAxisWrapMode {

  /** No wrapping specified. The size should be specified on the [Collection]'s style parameter. */
  NoWrap,

  /**
   * The main axis dimension will match the actual size of [Collection] if it is smaller than the
   * maximum size. If the actual size is larger than the maximum size, the [Collection] will be:
   * - Clipped: The excess content will be hidden.
   * - Scrollable on the main axis: The user can scroll to access the clipped content.
   *
   * This behavior ensures that the layout remains within the specified bounds while still providing
   * access to all content.
   */
  Wrap
}
