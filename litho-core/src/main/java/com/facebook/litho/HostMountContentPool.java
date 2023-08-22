/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho;

import android.content.Context;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.MountItemsPool;
import javax.annotation.Nullable;

/**
 * A specific MountContentPool for HostComponent - needed to do correct recycling with things like
 * duplicateParentState.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class HostMountContentPool implements MountItemsPool.ItemPool {

  @Nullable private final MountItemsPool.DefaultItemPool mPool;

  public HostMountContentPool(int maxSize, boolean isEnabled) {
    if (isEnabled) {
      mPool = new MountItemsPool.DefaultItemPool(ComponentHost.class, maxSize, true);
    } else {
      mPool = null;
    }
  }

  @Override
  @Nullable
  public Object acquire(ContentAllocator contentAllocator) {
    if (mPool == null) {
      return null;
    } else {
      return mPool.acquire(contentAllocator);
    }
  }

  @Override
  public boolean release(Object item) {
    if (mPool == null) {
      return false;
    }

    // See ComponentHost#hadChildWithDuplicateParentState() for reason for this check
    if (((ComponentHost) item).hadChildWithDuplicateParentState()) {
      return false;
    }

    return mPool.release(item);
  }

  @Override
  public boolean maybePreallocateContent(Context c, ContentAllocator contentAllocator) {
    if (mPool == null) {
      return false;
    }

    if (contentAllocator.canPreallocate()) {
      return mPool.maybePreallocateContent(c, contentAllocator);
    } else {
      return false;
    }
  }
}
