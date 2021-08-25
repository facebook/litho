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

import androidx.annotation.UiThread

/**
 * [useCallback] allows a parent to pass a child component a callback which: 1) maintains
 * referential equality across multiple layout passes via MemoizedCallback 2) is updated to have the
 * latest parent props and state, even if the child doesn't re-render.
 *
 * ## Example
 *
 * This sounds complicated, so it helps to step back a bit to provide context via example:
 *
 * Say we have a parent component which renders a list of children via a Collection. Let's say we
 * also want this parent to implement multi-select behavior, meaning on click, a row can be marked
 * as selected/de-selected. We will store the state of which components are currently selected in
 * the parent, and the parent will pass a lambda to the child to allow that state to be updated when
 * a row is clicked.
 *
 * When the first row is clicked, it will update the state in the parent via an onClick lambda. At
 * this point, we ideally want to re-render only that row since that's the only row whose UI will be
 * updated, i.e. to a selected state. However, that means the rest of the children will still have
 * that original onClick lambda set on them: if that lambda captured props/state, then invoking it
 * will operate on stale props/state! In this case, that means selecting a second row will de-select
 * the first one which is incorrect.
 *
 * An alternative is to always re-render all children whenever the list of selected items changes.
 * This is effective in that it will make sure all children have a lambda capturing the latest
 * props/state, however it's inefficient since it re-renders all children even though their UI
 * doesn't appear different.
 *
 * [useCallback] tries to give the best of both worlds: it doesn't cause children to re-render, and
 * also gives a mechanism for them to invoke a lambda that has captured the latest props and state.
 *
 * ## Notes
 *
 * The lambda reference in the returned [MemoizedCallback] is updated when a new layout has been
 * committed **on the main thread**. This means that [useCallback] should only be used with events
 * that will also be invoked from the main thread, e.g. onClick.
 */
fun <CallbackT : Function<*>> ComponentScope.useCallback(
    callback: CallbackT
): MemoizedCallback<CallbackT> {
  val callbackRef = useState { MemoizedCallback(callback) }
  useEffect {
    callbackRef.value.callback = callback
    null
  }
  return callbackRef.value
}

/**
 * A simple box for a lambda/function which can receive the latest version of the callback on the
 * main thread. Primarily used to just access and invoke the contained lambda (`current`).
 */
class MemoizedCallback<CallbackT : Function<*>>(@UiThread internal var callback: CallbackT) {

  /**
   * The current version of the lambda stored in this MemoizedCallback. Should only be accessed on
   * the main thread (see note in docs on [useCallback])
   */
  val current
    @UiThread
    get(): CallbackT {
      ThreadUtils.assertMainThread()
      return callback
    }
}
