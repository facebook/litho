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

package com.facebook.rendercore

/**
 * BinderObserver is a utility that plugs into the execution of binders.
 *
 * It makes it possible to run specialized code as the binder gets bound and unbound. For example,
 * one could collect the list of binders as they get bound and unbound. Better yet, state reads
 * during the execution of such binder may be recorded and used later to optimize future execution
 * of said binders.
 */
abstract class BinderObserver {

  /**
   * This function ensures that implementations of [onBind] actually execute [func] and that it is
   * executed exactly once.
   *
   * @throws [IllegalStateException] if [func] is not executed exactly once
   */
  internal inline fun observeBind(binderId: BinderId, crossinline func: () -> Unit) {
    if (BuildConfig.DEBUG) {
      var executionCount = 0
      onBind(binderId) {
        func()
        executionCount++
      }
      check(executionCount == 1) { "BinderObserver: bind func must be executed exactly once" }
    } else {
      onBind(binderId) { func() }
    }
  }

  /**
   * Called when a fixed binder is to be bound.
   *
   * It is expected that [func] is executed exactly once as it ensures that the binder's
   * [RenderUnit.Binder.bind] function will be called.
   *
   * @param binderId the identifier for the binder that's being bound
   * @param func logic that executes the [RenderUnit.Binder.bind] of the associated binder
   */
  protected abstract fun onBind(binderId: BinderId, func: () -> Unit)

  /**
   * This function ensures that implementations of [onUnbind] actually execute [func] and that it is
   * executed exactly once.
   *
   * @throws [IllegalStateException] if [func] is not executed exactly once
   */
  internal inline fun observeUnbind(binderId: BinderId, crossinline func: () -> Unit) {
    if (BuildConfig.DEBUG) {
      var executionCount = 0
      onUnbind(binderId) {
        func()
        executionCount++
      }
      check(executionCount == 1) { "BinderObserver: unbind func must be executed exactly once" }
    } else {
      onUnbind(binderId) { func() }
    }
  }

  /**
   * Called when a fixed binder is to be unbound.
   *
   * This may be used for example, to perform cleanups as it means that the associated binder has
   * been disposed. It is expected that [func] is executed exactly once as it ensures that the
   * binder's [RenderUnit.Binder.unbind] function will be called.
   *
   * @param binderId the identifier of the binder that's being disposed
   * @param func logic that executes the [RenderUnit.Binder.unbind] of the associated binder
   */
  protected abstract fun onUnbind(binderId: BinderId, func: () -> Unit)
}
