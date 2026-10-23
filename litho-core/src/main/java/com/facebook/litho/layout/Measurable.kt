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

package com.facebook.litho.layout

import com.facebook.litho.LithoLayoutContext
import com.facebook.litho.LithoNode
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.rendercore.LayoutContext

/**
 * A child that has been resolved but not yet laid out.
 *
 * Created by the framework during layout, where the context needed to lay a child out exists. An
 * instance is only meaningful inside the [LayoutFunction] call that received it. It has no public
 * members: the scope that call runs in measures it ([MeasureScope]).
 *
 * Final, and only Litho creates one: its constructor is internal, so a member added later is not a
 * breaking change. A layout function is tested by rendering a tree that holds its container.
 */
@ExperimentalLithoApi
class Measurable
internal constructor(
    /** The child's declaration index in its container. */
    internal val index: Int,
    /** The child's node. */
    internal val node: LithoNode,
    /** The layout context the child is measured in. */
    internal val context: LayoutContext<LithoLayoutContext>,
    /**
     * The container whose layout function was handed this child: only its placement may place it.
     * Untyped while two kinds of container make children.
     */
    internal val owner: Any,
)
