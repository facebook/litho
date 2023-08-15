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

/**
 * Defines a class that can provide mountable content and have it be pooled.
 *
 * <p>Instances must implement at least createContent and getRenderType methods to allocate the
 * RenderUnit content (View or Drawable) and to make it possible to query the type of the content.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ContentAllocator<Content> {

  /** Default size of the content pool. */
  public static final int DEFAULT_MAX_PREALLOCATION = 3;

  /** Allocates the mountable content (View or Drawable). */
  Content createContent(Context context);

  /** Returns the [RenderUnit.RenderType] of the mountable content. */
  RenderUnit.RenderType getRenderType();

  /** Creates a mount-content that can be pooled. This is typically a View or Drawable subclass. */
  default Content createPoolableContent(Context context) {
    return createContent(context);
  }

  /** Returns the class of the content of the mountable content. */
  default Class<?> getPoolableContentType() {
    return getClass();
  }

  /** Return true if pooling should be disabled for this mount content. */
  default boolean isRecyclingDisabled() {
    return false;
  }

  /**
   * Creates an ItemPool for this mountable content. Returning null will generate a default pool.
   */
  @Nullable
  default MountItemsPool.ItemPool createRecyclingPool() {
    return onCreateMountContentPool();
  }

  /**
   * This API informs the framework to fill the content pool for this Mountable ahead of time. The
   * default value is {@code false}, i.e. content is not pre-allocated. Pre-allocation of the
   * content can improve performance in some circumstances where creating the content is expensive.
   *
   * @return {@code true} to preallocate the content, otherwise {@code false}
   */
  default boolean canPreallocate() {
    return false;
  }

  /** This API informs the framework about the size of the content pool. The default is 3. */
  default int poolSize() {
    return DEFAULT_MAX_PREALLOCATION;
  }

  /** Creates the content pool the framework should use for this Mountable. */
  default MountItemsPool.ItemPool onCreateMountContentPool() {
    return new MountItemsPool.DefaultItemPool(this.getClass(), poolSize(), false);
  }
}
