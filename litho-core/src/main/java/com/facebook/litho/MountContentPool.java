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

import com.facebook.rendercore.MountItemsPool;

/**
 * A pool dedicated to recycling mount content.
 *
 * <p>Note! This class MUST be implemented in a thread safe manner! See info in the javadocs below.
 */
public interface MountContentPool<T> extends PoolWithDebugInfo, MountItemsPool.ItemPool<T> {
  // All relevant methods extended from MountItemsPool.ItemPool<T> and PoolWithDebugInfo
}
