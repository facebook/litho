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

package com.facebook.rendercore;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/** Defines a class that can provide mountable content to be pooled. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ContentAllocator {

  /** Creates a mount-content that can be pooled. This is typically a View or Drawable subclass */
  Object createPoolableContent(Context context);

  /**
   * Returns an object defining the type of the mount-content. Typically the mount-content's Class.
   */
  Object getPoolableContentType();

  /** Return true if pooling should be disabled for this mount content. */
  boolean isRecyclingDisabled();

  /**
   * Creates an ItemPool for this mountable content. Returning null will generate a default pool.
   */
  @Nullable
  MountItemsPool.ItemPool createRecyclingPool();
}
