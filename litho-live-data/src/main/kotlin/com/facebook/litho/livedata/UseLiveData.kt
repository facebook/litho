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

package com.facebook.litho.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.facebook.litho.AOSPLithoLifecycleProvider
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.State
import com.facebook.litho.annotations.Hook
import com.facebook.litho.getTreeProp
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState

/**
 * Uses the current value of a given [LiveData] in a Litho Kotlin component.
 *
 * The live data will be observed following the [AOSPLithoLifecycleProvider] lifecycle definition,
 * and it's usage requires the [LithoView] or [ComponentTree] to be using the
 * [AOSPLithoLifecycleProvider]
 */
@Hook
fun <T> ComponentScope.useLiveData(liveData: LiveData<T>): T? {
  return useLiveData(liveData = liveData, initialValue = { liveData.value })
}

/**
 * Uses the current value of a given [LiveData] in a Litho Kotlin component.
 *
 * The live data will be observed following the [AOSPLithoLifecycleProvider] lifecycle definition,
 * and it's usage requires the [LithoView] or [ComponentTree] to be using the
 * [AOSPLithoLifecycleProvider].
 *
 * This observation will also be canceled whenever [deps] change.
 */
@Hook
fun <T> ComponentScope.useLiveData(
    liveData: LiveData<T>,
    vararg deps: Any?,
    initialValue: () -> T?
): T? {
  val lifecycleOwner: LifecycleOwner =
      getTreeProp<LifecycleOwner>()
          ?: error(
              "There is no lifecycle owner. Make you created your LithoView with an AOSPLithoLifecycleProvider.")

  val state: State<T?> = useState { initialValue() }

  useEffect(lifecycleOwner, deps) {
    val observer = Observer<T> { liveDataResult -> state.update { liveDataResult } }
    liveData.observe(lifecycleOwner, observer)
    onCleanup { liveData.removeObserver(observer) }
  }

  return state.value
}
