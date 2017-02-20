// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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
