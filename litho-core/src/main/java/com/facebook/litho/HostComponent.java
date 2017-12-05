/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.config.ComponentsConfiguration;

class HostComponent extends Component {

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    return new ComponentHost(c);
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  static Component create() {
    return new HostComponent();
  }

  @Override
  public String getSimpleName() {
    return "HostComponent";
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }

  @Override
  protected int poolSize() {
    // Pooling is done via mScrapHosts in ComponentHost.
    return ComponentsConfiguration.scrapHostRecyclingForComponentHosts
        ? 0
        : ComponentsConfiguration.componentHostPoolSize;
  }

  @Override
  protected boolean shouldUpdate(Component previous, Component next) {
    return true;
  }
}
