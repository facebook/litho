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

package com.facebook.litho

import kotlin.jvm.JvmField

/**
 * Holds onto the mutable data used to dispatch an event. This data is kept up-to-date with the
 * latest version of the owning component or section by [EventHandlersController].
 */
class EventDispatchInfo(
    // These should only be null for manually created EventHandlers (i.e. not using
    // Component.newEventHandler
    @JvmField var hasEventDispatcher: HasEventDispatcher?,
    @JvmField var componentContext: ComponentContext?
) {
  @JvmField var tag: String? = null
}
