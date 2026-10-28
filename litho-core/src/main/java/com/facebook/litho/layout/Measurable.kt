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

import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.internal.DefaultLayoutBuilder
import kotlin.reflect.KClass

/**
 * A child that has been resolved but not yet laid out.
 *
 * Made by its container's builder for one pass: the child's index under that builder, which
 * measures it. An instance is only meaningful inside the [LayoutFunction] call that received it. It
 * has no public members: the scope that call runs in measures it ([MeasureScope]), and extensions
 * read the [LayoutProps] it carries.
 *
 * Final, and only Litho creates one: its constructor is internal, so a member added later is not a
 * breaking change. A layout function is tested by rendering a tree that holds its container.
 */
@ExperimentalLithoApi
class Measurable
internal constructor(
    /** The child's declaration index in its container. */
    internal val index: Int,
    /**
     * The container whose layout function was handed this child: only its placement may place it.
     */
    internal val owner: DefaultLayoutBuilder,
)

/**
 * The value of [type] this child carries, set in its own `Style` with `layoutProps`, or null if it
 * set none. A type this container does not read is never asked for, which is how one child can
 * carry values for more than one kind of container. Readable anywhere in the layout function: a
 * child's values don't change during a pass.
 */
@ExperimentalLithoApi
fun <T : LayoutProps> Measurable.layoutProps(type: Class<T>): T? = owner.layoutPropsOf(index, type)

/**
 * The value of [T] this child carries, or [default] if it set none. [T] is the key: for a sealed
 * type, name it, as in `layoutProps<Side>(Side.Start)`.
 */
@ExperimentalLithoApi
inline fun <reified T : LayoutProps> Measurable.layoutProps(default: T): T =
    layoutProps(T::class.java) ?: default

/**
 * The value of [type] this child carries, or null if it set none: `child[RadialPosition::class]`.
 *
 * Inline, so `Foo::class` compiles to a class constant and a read allocates nothing.
 */
@ExperimentalLithoApi
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T : LayoutProps> Measurable.get(type: KClass<T>): T? = layoutProps(type.java)
