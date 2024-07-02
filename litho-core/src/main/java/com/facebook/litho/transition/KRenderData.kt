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

package com.facebook.litho.transition

import com.facebook.litho.Component.RenderData
import com.facebook.litho.Diff
import com.facebook.litho.KComponent

/**
 * A lightweight data structure for [KComponent]s to keep track of data from the last execution of a
 * [TransitionCreator].
 *
 * The exact params that are tracked are essentially the ones needed to properly support the [Diff]
 * API needed by [useTransition].
 *
 * @param dependencies a list of dependencies for the corresponding [TransitionCreator].
 * @param diffInputs a list of values that will serve as the previous [Diff] values in the next
 *   iteration of [TransitionCreator.createTransition]
 */
internal class KRenderData(val dependencies: Array<*>, val diffInputs: List<Any?>?) : RenderData
