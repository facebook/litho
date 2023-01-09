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

package com.facebook.litho.widget

import android.content.Context
import android.view.View
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Size
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State

/**
 * This MountSpec throws an exception if @OnUnmount is invoked on the component before @OnMount was
 * invoked for it. This allows tests to assert that @OnUnmount is always invoked on a component for
 * which @OnMount was invoked.
 */
@MountSpec
object MountSpecWithMountUnmountAssertionSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      @Prop container: Container,
      holder: StateValue<Container>
  ) {
    holder.set(container)
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size
  ) {
    size.width = 100
    size.height = 100
  }

  @JvmStatic @UiThread @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic
  @UiThread
  @OnMount
  fun onMount(
      context: ComponentContext,
      view: View,
      @State holder: Container,
      @Prop(optional = true) hasTagSet: Boolean
  ) {
    holder.value = "mounted"
    check(view.parent == null) { "The view must be attached after mount is called" }
    check(!(hasTagSet && view.tag != null)) { "The tag must be set after mount is called." }
  }

  @JvmStatic
  @UiThread
  @OnUnmount
  fun onUnmount(
      context: ComponentContext,
      view: View,
      @State holder: Container,
      @Prop(optional = true) hasTagSet: Boolean
  ) {
    checkNotNull(holder.value) {
      ("The value was never set in @onMount. Which means that @OnMount was not invoked for this instance of the component or @OnUnmount was called without @OnMount.")
    }
    check(view.parent == null) { "The view must be detached before unmount is called" }
    check(!(hasTagSet && view.tag != null)) { "The tag must be unset before unmount is called." }
  }

  @JvmStatic
  @OnBind
  fun onBind(c: ComponentContext, content: View, @Prop(optional = true) hasTagSet: Boolean) {
    checkNotNull(content.parent) { "The view must be attached when bind is called" }
    check(!(hasTagSet && content.tag == null)) { "The tag must be set before bind is called." }
  }

  @JvmStatic
  @UiThread
  @OnUnbind
  fun onUnbind(c: ComponentContext, content: View, @Prop(optional = true) hasTagSet: Boolean) {
    check(content.parent == null) { "The view must be detached when unbind is called" }
    check(!(hasTagSet && content.tag == null)) { "The tag must be set when unbind is called." }
  }

  class Container {
    var value: String? = null
  }
}
