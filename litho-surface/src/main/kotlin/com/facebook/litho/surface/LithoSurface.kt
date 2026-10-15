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

@file:JvmName("LithoSurface")

package com.facebook.litho.surface

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.TreePropContainer
import com.facebook.litho.config.ComponentsConfiguration

// ============================================================================
// Public API — two surfaces, pick whichever fits the call site.
//
// 1. SIMPLE EXTENSIONS — single-surface owners only. No LithoViewModel
//    reference needed; the default-keyed VM is created/reused under the hood.
//
//      Fragment.setLithoContent { ... }       // declare/swap content
//      Fragment.getLithoView()                 // host-built LithoView
//      Fragment.bindLithoViewTo(view)          // caller-built LithoView
//      Fragment.onSurfaceClose { ... }         // paired side-resource cleanup
//      Fragment.setSurfaceVisibilityHint(b)    // visibility hint
//
//    Same shape on ComponentActivity. The default key is
//    "<cls>${receiver::class.java.name}</cls>". Containers cannot use the
//    simple extensions (every container would resolve to the same default key
//    via the `viewLifecycleOwner` bridge); use lithoViewModel(...) instead.
//
//    setLithoContent is callable multiple times — subsequent calls swap the
//    root via setRootAsync. getLithoView and bindLithoViewTo are mutex per
//    view-cycle on the same surface.
//
// 2. LITHO VIEW MODEL HANDLE — multi-surface owners and containers. Each
//    surface is a named LithoViewModel; surface operations are methods on the
//    handle.
//
//      // Fragment / Activity — property delegate, lazy.
//      private val feed by lithoViewModel(key = "feed")
//      private val composer by lithoViewModel(key = "composer")
//
//      // Container — function call returning the VM directly.
//      val page = owner.lithoViewModel(key = "SearchPage$viewType")
//
//    The default-key tag is reserved for the simple extensions —
//    lithoViewModel(...) requires a non-default key.
// ============================================================================

// ---------------------- Simple extensions: Fragment ------------------------

/**
 * Set or swap the root component of the default surface for this fragment. Builds the
 * [com.facebook.litho.ComponentTree] eagerly on first call; subsequent calls within the same
 * instance lifecycle queue an off-main-thread root swap.
 *
 * For multi-surface owners or non-default configuration, declare a property via
 * [Fragment.lithoViewModel] instead.
 */
fun Fragment.setLithoContent(content: () -> Component) {
  ensureSimpleVm(this).setLithoContent(content)
}

/**
 * Returns the view-cycle-cached [LithoView] for this fragment's default surface — lazily created
 * with the host's tree already attached. Mutex with [bindLithoViewTo] per view-cycle. Intended use
 * is in `Fragment.onCreateView`.
 */
fun Fragment.getLithoView(): LithoView = ensureSimpleVm(this).getLithoView()

/**
 * Attach the default surface's tree to a caller-supplied [LithoView] — typically inflated from XML
 * or constructed by hand to set a custom `id` or `layoutParams`. Mutex with [getLithoView] per
 * view-cycle. Intended use is in `Fragment.onCreateView`.
 */
fun Fragment.bindLithoViewTo(view: LithoView) {
  ensureSimpleVm(this).bindLithoViewTo(view)
}

/**
 * Eagerly release the per-view-cycle binding for this fragment's default surface. See
 * [LithoViewModel.unbind].
 */
fun Fragment.unbindLithoView() {
  ensureSimpleVm(this).unbind()
}

/**
 * Register an [action] to run when this fragment's default-surface [LithoViewModel] is cleared on
 * real teardown (NOT on rotation).
 */
fun Fragment.onSurfaceClose(action: () -> Unit) {
  ensureSimpleVm(this).onSurfaceClose(action)
}

/**
 * Drive the default surface's visibility hint independently of lifecycle events. Useful for
 * ViewPager-style hosting where a page fragment is attached and resumed but currently off-screen.
 */
fun Fragment.setSurfaceVisibilityHint(isVisible: Boolean) {
  ensureSimpleVm(this).setSurfaceVisibilityHint(isVisible)
}

/**
 * Declare a named [LithoViewModel] for a non-default surface on this fragment. The VM is
 * constructed lazily on first property access. Re-fires on rotation because the new fragment
 * instance reads its property fresh.
 *
 * Use for multi-surface owners (each surface gets its own delegate property) or when non-default
 * config ([componentsConfiguration], [treeProps]) is required.
 *
 * The [key] must not be the default-key tag (`<cls>...</cls>`) — that key is reserved for the
 * simple extensions.
 */
fun Fragment.lithoViewModel(
    key: String,
    componentsConfiguration: ComponentsConfiguration? = null,
    treeProps: (() -> TreePropContainer?)? = null,
): Lazy<LithoViewModel> =
    lazy(LazyThreadSafetyMode.NONE) {
      val fragment = this
      requireNonDefaultKey(key)
      val vm = createOrFetchVm(
          store = fragment.viewModelStore,
          key = key,
          componentsConfiguration = componentsConfiguration,
          treePropsFactory = treeProps,
          isExplicitlyConfigured = true,
      )
      vm.bind(
          contextProvider = { fragment.requireContext() },
          instanceOwnerProvider = { fragment },
          viewOwnerProvider = { fragment.viewLifecycleOwner },
      )
      vm
    }

// -------------------- Simple extensions: ComponentActivity -----------------

/** See [Fragment.setLithoContent]. */
fun ComponentActivity.setLithoContent(content: () -> Component) {
  ensureSimpleVm(this).setLithoContent(content)
}

/** See [Fragment.getLithoView]. */
fun ComponentActivity.getLithoView(): LithoView = ensureSimpleVm(this).getLithoView()

/** See [Fragment.bindLithoViewTo]. */
fun ComponentActivity.bindLithoViewTo(view: LithoView) {
  ensureSimpleVm(this).bindLithoViewTo(view)
}

/** See [Fragment.unbindLithoView]. */
fun ComponentActivity.unbindLithoView() {
  ensureSimpleVm(this).unbind()
}

/** See [Fragment.onSurfaceClose]. */
fun ComponentActivity.onSurfaceClose(action: () -> Unit) {
  ensureSimpleVm(this).onSurfaceClose(action)
}

/** See [Fragment.setSurfaceVisibilityHint]. */
fun ComponentActivity.setSurfaceVisibilityHint(isVisible: Boolean) {
  ensureSimpleVm(this).setSurfaceVisibilityHint(isVisible)
}

/** See [Fragment.lithoViewModel]. */
fun ComponentActivity.lithoViewModel(
    key: String,
    componentsConfiguration: ComponentsConfiguration? = null,
    treeProps: (() -> TreePropContainer?)? = null,
): Lazy<LithoViewModel> =
    lazy(LazyThreadSafetyMode.NONE) {
      val activity = this
      requireNonDefaultKey(key)
      val vm = createOrFetchVm(
          store = activity.viewModelStore,
          key = key,
          componentsConfiguration = componentsConfiguration,
          treePropsFactory = treeProps,
          isExplicitlyConfigured = true,
      )
      vm.bind(
          contextProvider = { activity },
          instanceOwnerProvider = { activity },
          viewOwnerProvider = { activity },
      )
      vm
    }

// ----------------- Container factory: LifecycleOwner + ViewModelStoreOwner ----
// Containers (RecyclerView adapters, ViewHolders, paged surfaces) take a passed-in
// owner and resolve a LithoViewModel via the function-call form. Property
// delegates aren't usable on a passed-in receiver, so containers use the
// plain function form.
//
// Note: a viewLifecycleOwner's ViewModelStore is cleared on every onDestroyView
// (not on real teardown), so container-hosted surfaces lose the rotation-
// survival benefit. Use the parent fragment itself as the owner if rotation
// survival matters.

/**
 * Resolve the [LithoViewModel] for [key] under this owner's [ViewModelStore]. Returns the VM
 * directly (not [Lazy] — containers typically declare this in `onCreateViewHolder` or similar).
 *
 * Containers must always supply an explicit non-default [key]; the default-key tag is reserved for
 * the simple extensions on Fragment / ComponentActivity.
 */
fun <T> T.lithoViewModel(
    context: Context,
    key: String,
    componentsConfiguration: ComponentsConfiguration? = null,
    treeProps: (() -> TreePropContainer?)? = null,
): LithoViewModel where T : LifecycleOwner, T : ViewModelStoreOwner {
  val owner = this
  requireNonDefaultKey(key)
  val vm = createOrFetchVm(
      store = owner.viewModelStore,
      key = key,
      componentsConfiguration = componentsConfiguration,
      treePropsFactory = treeProps,
      isExplicitlyConfigured = true,
  )
  vm.bind(
      contextProvider = { context },
      instanceOwnerProvider = { owner },
      viewOwnerProvider = { owner },
  )
  return vm
}

// ============================================================================
// Internal helpers
// ============================================================================

internal fun defaultKey(receiver: Any): String = "<cls>${receiver::class.java.name}</cls>"

internal fun isDefaultKey(key: String): Boolean = key.startsWith("<cls>") && key.endsWith("</cls>")

private fun requireNonDefaultKey(key: String) {
  require(!isDefaultKey(key)) {
    "[$key] looks like the default-key tag reserved for the simple extension form. Pass a " +
        "different key to lithoViewModel(...) — the default-key tag is computed automatically " +
        "for setLithoContent / getLithoView / bindLithoViewTo extensions on Fragment / " +
        "ComponentActivity."
  }
}

/**
 * Get-or-create the default-keyed [LithoViewModel] for the receiver and bind it to the receiver's
 * context + lifecycle. Used by the simple extensions only.
 */
private fun ensureSimpleVm(fragment: Fragment): LithoViewModel {
  val key = defaultKey(fragment)
  val vm = createOrFetchVm(
      store = fragment.viewModelStore,
      key = key,
      componentsConfiguration = null,
      treePropsFactory = null,
      isExplicitlyConfigured = false,
  )
  vm.bind(
      contextProvider = { fragment.requireContext() },
      instanceOwnerProvider = { fragment },
      viewOwnerProvider = { fragment.viewLifecycleOwner },
  )
  return vm
}

private fun ensureSimpleVm(activity: ComponentActivity): LithoViewModel {
  val key = defaultKey(activity)
  val vm = createOrFetchVm(
      store = activity.viewModelStore,
      key = key,
      componentsConfiguration = null,
      treePropsFactory = null,
      isExplicitlyConfigured = false,
  )
  vm.bind(
      contextProvider = { activity },
      instanceOwnerProvider = { activity },
      viewOwnerProvider = { activity },
  )
  return vm
}

private fun createOrFetchVm(
    store: ViewModelStore,
    key: String,
    componentsConfiguration: ComponentsConfiguration?,
    treePropsFactory: (() -> TreePropContainer?)?,
    isExplicitlyConfigured: Boolean,
): LithoViewModel {
  val factory = LithoViewModelFactory(
      key = key,
      componentsConfiguration = componentsConfiguration,
      treePropsFactory = treePropsFactory,
      isExplicitlyConfigured = isExplicitlyConfigured,
  )
  val vm = ViewModelProvider(store, factory)[key, LithoViewModel::class.java]
  if (isExplicitlyConfigured) {
    check(vm.isExplicitlyConfigured) {
      "[$key] lithoViewModel(key=\"$key\") called after a default-built LithoViewModel was " +
          "already created for this key. Move the lithoViewModel(...) declaration before any " +
          "implicit creation, or use a different key."
    }
    check(vm.componentsConfiguration === componentsConfiguration) {
      "[$key] lithoViewModel(key=\"$key\") called with a different ComponentsConfiguration " +
          "instance than the existing LithoViewModel was built with. Pass the same instance " +
          "across rotations (e.g. hoist it into a top-level `val` or a `companion object`), or " +
          "use a distinct key per configuration."
    }
    // treePropsFactory is a lambda — referential equality is unstable across captures, so we
    // intentionally don't check it. Callers should treat their treeProps lambda as set-once;
    // see follow-up #3 in the design doc.
  }
  return vm
}
