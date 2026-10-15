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

import android.annotation.SuppressLint
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.facebook.litho.ThreadUtils.assertMainThread
import com.facebook.litho.annotations.ExperimentalLithoApi

/** Retains the thread-safe Litho store for the lifetime of an Android ViewModel owner. */
internal class LithoViewModelStoreHolder : ViewModel() {
  val store = LithoViewModelStore()

  override fun onCleared() {
    store.close()
  }
}

/**
 * Returns the thread-safe Litho ViewModel store retained by this AndroidX owner.
 *
 * Repeated calls for the same owner return the same store. The owner controls its lifetime and
 * closes the store when its AndroidX [androidx.lifecycle.ViewModelStore] is cleared. Acquire the
 * store on the main thread before passing it to a [ComponentTree] that may be built or resolved on
 * another thread.
 *
 * @throws IllegalStateException if this owner is a destroyed [LifecycleOwner].
 */
@ExperimentalLithoApi
@UiThread
@SuppressLint("ViewModelProviderInNonFragmentOrActivityClass")
fun ViewModelStoreOwner.getLithoViewModelStore(): LithoViewModelStore {
  assertMainThread()
  check(this !is LifecycleOwner || lifecycle.currentState != Lifecycle.State.DESTROYED) {
    "Cannot use a destroyed ViewModelStoreOwner"
  }
  return ViewModelProvider.create(viewModelStore, LithoViewModelStoreHolderFactory)[
          LITHO_VIEW_MODEL_STORE_HOLDER_KEY,
          LithoViewModelStoreHolder::class,
      ]
      .store
}

private object LithoViewModelStoreHolderFactory : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    check(modelClass == LithoViewModelStoreHolder::class.java)
    @Suppress("UNCHECKED_CAST")
    return LithoViewModelStoreHolder() as T
  }
}

private const val LITHO_VIEW_MODEL_STORE_HOLDER_KEY = "com.facebook.litho.LithoViewModelStoreHolder"
