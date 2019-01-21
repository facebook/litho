/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.content.Context;
import android.os.Build;

class HostComponent extends Component {

  protected HostComponent() {
    super("HostComponent");
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return new ComponentHost(c);
  }

  @Override
  protected void onMount(ComponentContext c, Object convertContent) {
    final ComponentHost host = (ComponentHost) convertContent;

    if (Build.VERSION.SDK_INT >= 11) {
      // We need to do this in case an external user of this ComponentHost has manually set alpha
      // to 0, which will mean that it won't draw anything.
      host.setAlpha(1.0f);
    }
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    final ComponentHost host = (ComponentHost) mountedContent;

    // Some hosts might be duplicating parent state which could be 'pressed' and under certain
    // conditions that state might not be cleared from this host and carried to next reuse,
    // therefore applying wrong drawable state. Particular case where this might happen is when
    // host is unmounted as soon as click event is triggered, and host is unmounted before it has
    // chance to reset its internal pressed state.
    if (host.isPressed()) {
      host.setPressed(false);
    }
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
