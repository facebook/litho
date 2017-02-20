// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.stetho.inspector.elements.DescriptorProvider;
import com.facebook.stetho.inspector.elements.DescriptorRegistrar;

public final class ComponentsDescriptorProvider implements DescriptorProvider {

  @Override
  public void registerDescriptor(DescriptorRegistrar registrar) {
    registrar.registerDescriptor(ComponentView.class, new ComponentViewDescriptor());
  }
}
