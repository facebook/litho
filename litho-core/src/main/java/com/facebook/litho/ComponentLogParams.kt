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

/** Data class for params used to log error in LithoView. */
class ComponentLogParams
@JvmOverloads
constructor(
    @JvmField val logProductId: String,
    @JvmField val logType: String,
    @JvmField val samplingFrequency: Int = 0,
    @JvmField val failHarder: Boolean = false
)
