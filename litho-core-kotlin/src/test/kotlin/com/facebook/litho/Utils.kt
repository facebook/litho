/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

/**
 * There are a couple tests that can't be expressed as typical unit tests that check the effects of
 * public APIs -- this isn't a fault of that API but because of limitations in API versioning,
 * Robolectric, and native code loading. For them, we are resorting to testing against the created
 * layout to make sure the properties are applied. This definition allows access to this
 * implementation detail to tests that don't live under `com.facebook.litho`.
 */
@Deprecated("Whenever possible, don't write tests depending on implementation details")
internal fun resolveComponentToNodeForTest(parent: ComponentContext, component: Component) =
    Layout.create(parent, component)
