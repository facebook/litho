/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import com.facebook.litho.DebugComponent;
import com.facebook.litho.LithoView;
import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.DescriptorRegistrar;

public final class ComponentsDescriptorProvider implements DescriptorProvider {

  @Override
  public void registerDescriptor(DescriptorRegistrar registrar) {
    registrar.registerDescriptor(LithoView.class, new LithoViewDescriptor());
    registrar.registerDescriptor(DebugComponent.class, new DebugComponentDescriptor());
  }
}
