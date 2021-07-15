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

package com.facebook.samples.litho.java.lifecycle;

import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_BIND;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_BOUNDS_DEFINED;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_MEASURE;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_MOUNT;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_PREPARE;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_UNBIND;
import static com.facebook.samples.litho.java.lifecycle.DelegateListener.ON_UNMOUNT;
import static com.facebook.samples.litho.java.lifecycle.LifecycleDelegateLog.onDelegateMethodCalled;

import android.content.Context;
import android.widget.TextView;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;

@MountSpec
public class LifecycleDelegateMountComponentSpec {

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener,
      Output<String> contentString) {
    contentString.set("Hello, Litho-World!");
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_PREPARE, id);
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener) {
    size.height = 100;
    size.width = 600;
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_MEASURE, id);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener) {
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_BOUNDS_DEFINED, id);
  }

  @OnCreateMountContent
  static TextView onCreateMountContent(Context c) {
    return new TextView(c);
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      TextView textView,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener,
      @FromPrepare String contentString) {
    textView.setText(contentString);
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_MOUNT, id);
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      TextView textView,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener) {
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_BIND, id);
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext c,
      TextView textView,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener) {
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_UNBIND, id);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      TextView textView,
      @Prop DelegateListener delegateListener,
      @Prop String id,
      @Prop(optional = true) DelegateListener consoleDelegateListener) {
    onDelegateMethodCalled(delegateListener, consoleDelegateListener, ON_UNMOUNT, id);
  }
}
