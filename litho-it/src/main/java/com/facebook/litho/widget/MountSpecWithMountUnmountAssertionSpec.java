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

package com.facebook.litho.widget;

import android.content.Context;
import android.view.View;
import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;

/**
 * This MountSpec throws an exception if @OnUnmount is invoked on the component before @OnMount was
 * invoked for it. This allows tests to assert that @OnUnmount is always invoked on a component for
 * which @OnMount was invoked.
 */
@MountSpec
public class MountSpecWithMountUnmountAssertionSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      final ComponentContext c,
      final @Prop Container container,
      final StateValue<Container> holder) {
    holder.set(container);
  }

  @OnMeasure
  static void onMeasure(
      final ComponentContext context,
      final ComponentLayout layout,
      final int widthSpec,
      int heightSpec,
      final Size size) {
    size.width = 100;
    size.height = 100;
  }

  @UiThread
  @OnCreateMountContent
  static View onCreateMountContent(Context c) {
    return new View(c);
  }

  @UiThread
  @OnMount
  static void onMount(
      final ComponentContext context,
      final View view,
      final @State Container holder,
      final @Prop(optional = true) boolean hasTagSet) {
    holder.value = "mounted";
    if (view.getParent() != null) {
      throw new IllegalStateException("The view must be attached after mount is called");
    }
    if (hasTagSet && view.getTag() != null) {
      throw new IllegalStateException("The tag must be set after mount is called.");
    }
  }

  @UiThread
  @OnUnmount
  static void onUnmount(
      final ComponentContext context,
      final View view,
      final @State Container holder,
      final @Prop(optional = true) boolean hasTagSet) {
    if (holder.value == null) {
      throw new IllegalStateException(
          "The value was never set in @onMount. Which means that @OnMount was not invoked "
              + "for this instance of the component or @OnUnmount was called without @OnMount.");
    }
    if (view.getParent() != null) {
      throw new IllegalStateException("The view must be detached before unmount is called");
    }
    if (hasTagSet && view.getTag() != null) {
      throw new IllegalStateException("The tag must be unset before unmount is called.");
    }
  }

  @OnBind
  static void onBind(
      final ComponentContext c,
      final View content,
      final @Prop(optional = true) boolean hasTagSet) {
    if (content.getParent() == null) {
      throw new IllegalStateException("The view must be attached when bind is called");
    }
    if (hasTagSet && content.getTag() == null) {
      throw new IllegalStateException("The tag must be set before bind is called.");
    }
  }

  @UiThread
  @OnUnbind
  static void onUnbind(
      final ComponentContext c,
      final View content,
      final @Prop(optional = true) boolean hasTagSet) {
    if (content.getParent() != null) {
      throw new IllegalStateException("The view must be detached when unbind is called");
    }
    if (hasTagSet && content.getTag() == null) {
      throw new IllegalStateException("The tag must be set when unbind is called.");
    }
  }

  public static class Container {
    public String value;
  }
}
