/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.rendercore.testing;

import android.content.Context;
import com.facebook.rendercore.HostView;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.List;

public class TestHost extends HostView {

  private final List bindOrder;
  private final List unbindOrder;
  private int mMoveCount;

  public TestHost(Context context) {
    super(context, null);
    this.bindOrder = new ArrayList<>();
    this.unbindOrder = new ArrayList<>();
  }

  public TestHost(Context context, List bindOrder, List unbindOrder) {
    super(context, null);
    this.bindOrder = bindOrder;
    this.unbindOrder = unbindOrder;
  }

  @Override
  public void mount(int index, MountItem mountItem) {
    super.mount(index, mountItem);
    bindOrder.add(TestHost.this);
  }

  @Override
  public void unmount(MountItem mountItem) {
    super.unmount(mountItem);
    unbindOrder.add(TestHost.this);
  }

  @Override
  public void unmount(int index, MountItem mountItem) {
    super.unmount(index, mountItem);
    unbindOrder.add(TestHost.this);
  }

  @Override
  public void moveItem(MountItem item, int oldIndex, int newIndex) {
    super.moveItem(item, oldIndex, newIndex);
    mMoveCount++;
  }

  public int getMoveCount() {
    return mMoveCount;
  }
}
