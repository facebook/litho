/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.DescriptorRegistrar;

public final class ComponentsDescriptorProvider implements DescriptorProvider {

  @Override
  public void registerDescriptor(DescriptorRegistrar registrar) {
    registrar.registerDescriptor(ComponentView.class, new ComponentViewDescriptor());
    registrar.registerDescriptor(StethoInternalNode.class, new StethoInternalNodeDescriptor());
  }
}
