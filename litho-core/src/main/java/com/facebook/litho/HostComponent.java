/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

class HostComponent extends Component {

  protected HostComponent() {
    super("HostComponent");
  }

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
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }

  @Override
  protected int poolSize() {
    return 45;
  }

  @Override
  protected boolean shouldUpdate(Component previous, Component next) {
    return true;
  }
}
