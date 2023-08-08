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

/**
 * A specific MountContentPool for HostComponent - needed to do correct recycling with things like
 * duplicateParentState.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class HostMountContentPool extends RecyclePool implements MountContentPool {

  private final boolean mIsEnabled;

  public HostMountContentPool(int maxSize, boolean isEnabled) {
    super(maxSize, true);
    mIsEnabled = isEnabled;
  }

  @Override
  public Object acquire(Context c, ContentAllocator component) {
    if (!mIsEnabled) {
      return component.createPoolableContent(c);
    }

    final Object fromPool = super.acquire();
    return fromPool != null ? fromPool : component.createPoolableContent(c);
  }

  @Override
  public boolean release(Object item) {
    if (!mIsEnabled) {
      return false;
    }

    // See ComponentHost#hadChildWithDuplicateParentState() for reason for this check
    if (((ComponentHost) item).hadChildWithDuplicateParentState()) {
      return false;
    }

    return super.release(item);
  }

  @Override
  public void maybePreallocateContent(Context c, ContentAllocator component) {
    // Pre-allocation not yet supported
  }
}
