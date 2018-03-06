/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.error;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;

@MountSpec
class TestCrasherOnMountSpec {

  @OnCreateMountContent
  static LithoView onCreateMountContent(ComponentContext c) {
    return new LithoView(c);
  }

  @OnMount
  static void onMount(ComponentContext c, LithoView lithoView) {
    throw new RuntimeException("onMount crash");
  }

  @OnUnmount
  static void onUnmount(ComponentContext c, LithoView mountedView) {
    mountedView.setComponentTree(null);
  }
}
