/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

class HostComponent extends ComponentLifecycle {

  private static final HostComponent sInstance = new HostComponent();
  private static final int HOST_POOL_SIZE = 30;

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    return new ComponentHost(c);
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  static Component create() {
    return new State();
  }

  private static class State extends Component<HostComponent> implements Cloneable {

    State() {
      super(sInstance);
    }

    @Override
    public String getSimpleName() {
      return "HostComponent";
    }
  }

  @Override
  protected int poolSize() {
    return HOST_POOL_SIZE;
  }
}
