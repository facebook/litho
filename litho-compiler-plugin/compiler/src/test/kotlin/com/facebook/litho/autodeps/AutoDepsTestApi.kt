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

package com.facebook.litho.autodeps

import com.facebook.litho.ComponentScope
import com.facebook.litho.annotations.Hook
import com.facebook.rendercore.annotations.AutoDeps
import com.facebook.rendercore.annotations.Deps
import com.facebook.rendercore.annotations.DepsFrom

/**
 * Synthetic `@AutoDeps` APIs covering every declaration shape the transform must handle, plus the
 * recorder the tests assert on.
 *
 * These live in the test source set rather than in the compiled test data on purpose: the plugin
 * then reads the annotations off a *deserialised* class, which is the cross-module path every real
 * call site takes and the reason the annotations need `BINARY` retention.
 */
object DepsRecorder {
  private val received = mutableListOf<List<Any?>>()

  fun record(deps: Array<out Any?>) {
    received += deps.toList()
  }

  fun reset() {
    received.clear()
  }

  fun all(): List<List<Any?>> = received.toList()

  /** The dependencies of the single participating call in the compiled subject. */
  fun only(): List<Any?> = received.single()
}

/** A `fun interface` target, the shape the Custom Layout API uses. */
fun interface LayoutFunction {
  fun measure(width: Int): Int
}

/** Class plus `fun interface`, reached through a trailing SAM conversion. */
@AutoDeps
class Layout(@Deps vararg val deps: Any?, @DepsFrom val func: LayoutFunction) {
  init {
    DepsRecorder.record(deps)
  }
}

/** Top-level function taking a plain function type. */
@AutoDeps
fun onThing(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit) {
  DepsRecorder.record(deps)
  func()
}

/** Hook-shaped API used to verify Auto Deps runs before hook-identity rewriting. */
@Hook
@AutoDeps
fun ComponentScope.useAutoDepsThing(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit) {
  DepsRecorder.record(deps)
  func()
}

/**
 * A non-vararg `@Deps`, for an API whose vararg slot is taken by something else -- the shape the
 * Custom Layout `Layout` uses, where the children are the vararg.
 */
@AutoDeps
fun onValues(
    vararg values: Int,
    @Deps deps: Array<Any?> = emptyArray(),
    @DepsFrom func: () -> Unit,
) {
  DepsRecorder.record(deps)
}

/** Two `@DepsFrom` parameters: the dependency set is the union of their captures. */
@AutoDeps
fun onTwoThings(
    @Deps vararg deps: Any?,
    @DepsFrom first: () -> Unit,
    @DepsFrom second: () -> Unit,
) {
  DepsRecorder.record(deps)
}

class LayoutScope(val width: Int)

/**
 * A `fun interface` whose method has a receiver, which the analysis must not treat as a capture.
 */
fun interface ScopedFunction {
  fun LayoutScope.build()
}

@AutoDeps
fun onScoped(@Deps vararg deps: Any?, @DepsFrom func: ScopedFunction) {
  DepsRecorder.record(deps)
}

/** The general shape of [Container]'s layout function: every child arrives in one list. */
fun interface ContainerFunction {
  fun LayoutScope.measure(children: List<Int>)
}

/** The one-child shape, which names its child instead of indexing for it. */
fun interface ContainerFunction1 : ContainerFunction {

  fun LayoutScope.measureChild(child: Int)

  override fun LayoutScope.measure(children: List<Int>) {
    measureChild(children[0])
  }
}

/**
 * A class whose arity overloads are secondary constructors -- the shape the Custom Layout `Layout`
 * uses, and the one no top-level function can show.
 *
 * The children are a `List` rather than a vararg, which is what lets each arity take its own
 * `@DepsFrom` type: overloads specialised over a vararg cannot be told apart by a lambda's
 * parameter count. That also leaves `@Deps` a defaulted `Array<Any?>` rather than the vararg slot.
 */
@AutoDeps
class Container(
    val children: List<Int>,
    @Deps val deps: Array<Any?> = emptyArray(),
    @DepsFrom val func: ContainerFunction,
) {
  init {
    DepsRecorder.record(deps)
  }

  constructor(
      child1: Int,
      @Deps deps: Array<Any?> = emptyArray(),
      @DepsFrom func: ContainerFunction1,
  ) : this(listOf(child1), deps, func)
}

/**
 * An `@AutoDeps` class a subject can extend, so a `super(...)` delegation is a call site the
 * transform has to see.
 */
@AutoDeps
open class OpenContainer(
    @Deps val deps: Array<Any?> = emptyArray(),
    @DepsFrom val func: ContainerFunction,
) {
  init {
    DepsRecorder.record(deps)
  }
}

/**
 * An overridable `@AutoDeps` function. Overrides do not inherit annotations, so a call through a
 * subclass resolves to a function that does not carry the contract.
 */
open class AutoDepsBase {
  @AutoDeps
  open fun onThing(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit) {
    DepsRecorder.record(deps)
    func()
  }
}

/*
 * Declarations the declaration checker would reject, declared here so the transform can be shown to
 * reject their call sites too. The checker only runs on declarations in the compilation it is part
 * of, so an API compiled without the plugin reaches the transform unvalidated.
 */

@AutoDeps
fun onWronglyTypedDeps(@Deps vararg deps: String, @DepsFrom func: () -> Unit) {
  DepsRecorder.record(deps)
}

@AutoDeps
fun onMissingDeps(@DepsFrom func: () -> Unit) {
  func()
}

@AutoDeps
fun onMissingDepsFrom(@Deps vararg deps: Any?) {
  DepsRecorder.record(deps)
}

@AutoDeps
fun onDefaultedDepsFrom(@Deps vararg deps: Any?, @DepsFrom func: () -> Unit = {}) {
  DepsRecorder.record(deps)
  func()
}

/** An ambient value, named in the tests' `ignore` list. */
class AmbientContext

/** A value with no meaningful equivalence, named in the tests' `expand` list. */
class Measured(val value: Int)
