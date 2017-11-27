/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor.integration.resources;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;

@MountSpec
class SimpleMountSpec {

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop double ratio) {
  }

  @OnCreateMountContent
  static LithoView onCreateMountContent(ComponentContext c) {
    return new LithoView(c);
  }

  @OnMount
  static void onMount(ComponentContext c, LithoView lithoView, @Prop Component content) {
    lithoView.setComponentTree(
        ComponentTree.create(c, content).incrementalMount(false).layoutDiffing(false).build());
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      LithoView mountedView) {
    mountedView.setComponentTree(null);
  }
}

