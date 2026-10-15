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

/**
 * A callback triggered when a [LazyCollection] is scrolled "near" to a specific position, as
 * defined by being within [offset] items away.
 *
 * Intended for use with [LazyCollection]'s `onNearEnd` parameter, or child's `onNearViewport`
 * parameter.
 */
class OnNearCallback(val offset: Int = 0, val callback: () -> Unit)
