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

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.Prop;
import java.util.List;

@LayoutSpec
public class AttachDetachTesterSpec {

  public static final String ON_ATTACHED = "ON_ATTACHED";
  public static final String ON_DETACHED = "ON_DETACHED";

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop List<String> steps,
      @Prop(optional = true) AttachDetachTester.Builder[] children) {
    final Row.Builder containerBuilder = Row.create(c);
    if (children != null) {
      for (AttachDetachTester.Builder child : children) {
        containerBuilder.child(child.steps(steps));
      }
    }
    return containerBuilder.build();
  }

  @OnAttached
  static void onAttached(ComponentContext c, @Prop String name, @Prop List<String> steps) {
    steps.add(name + ":" + ON_ATTACHED);
  }

  @OnDetached
  static void onDetached(ComponentContext c, @Prop String name, @Prop List<String> steps) {
    steps.add(name + ":" + ON_DETACHED);
  }
}
