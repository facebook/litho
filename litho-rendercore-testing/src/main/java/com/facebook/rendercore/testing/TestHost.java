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

/*
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
import android.util.SparseArray;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.List;

public class TestHost extends Host {

  private final SparseArray<MountItem> mMountItems = new SparseArray<>();

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
    bindOrder.add(TestHost.this);
    mMountItems.put(index, mountItem);
  }

  @Override
  public void unmount(MountItem mountItem) {
    unbindOrder.add(TestHost.this);
    int idx = -1;
    for (int i = 0; i < mMountItems.size(); i++) {
      if (mMountItems.valueAt(i) == mountItem) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      mMountItems.removeAt(idx);
    }
  }

  @Override
  public void unmount(int index, MountItem mountItem) {
    unbindOrder.add(TestHost.this);
    mMountItems.remove(index);
  }

  @Override
  public int getMountItemCount() {
    return mMountItems.size();
  }

  @Override
  public MountItem getMountItemAt(int index) {
    return mMountItems.get(index);
  }

  @Override
  public void moveItem(MountItem item, int oldIndex, int newIndex) {
    mMoveCount++;
  }

  @Override
  protected void onLayout(boolean b, int i, int i1, int i2, int i3) {}

  public int getMoveCount() {
    return mMoveCount;
  }
}
