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

@file:Suppress("MatchingDeclarationName")

package com.facebook.litho

import androidx.annotation.UiThread
import com.facebook.litho.annotations.Hook

/** A simple mutable holder of a value. */
class Ref<T>(@UiThread var value: T)

/**
 * Declares a mutable, main-thread confined reference that will be persisted across renders with an
 * initial value provided by the initializer. It's similar to [useState], except that the returned
 * reference is mutable and updating it will *not* cause a re-render.
 *
 * IMPORTANT: It's only safe to read/write the [Ref]'s value on the main thread!
 *
 * An example of a safe way to use `useRef` is to store an Animator that is started/stopped from
 * [useEffect] or a click event, both of which run on the UI thread.
 */
@Hook
fun <T> ComponentScope.useRef(initializer: () -> T): Ref<T> = useState { Ref(initializer()) }.value
