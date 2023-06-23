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

package com.facebook.rendercore.extensions

import com.facebook.rendercore.primitives.MountConfigurationScope

/**
 * If true, the nested tree hierarchy (if present) will be notified about parent's bounds changes.
 * It will ensure that visibility events and incremental mount works correctly for the nested tree
 * hierarchy.
 *
 * Default is false.
 */
var <ContentType : Any> MountConfigurationScope<ContentType>.doesMountRenderTreeHosts: Boolean
  get() {
    throw IllegalStateException(
        "Getting the current value of doesMountRenderTreeHosts is not supported.")
  }
  set(value) {
    addExtra(R.id.does_mount_render_tree_hosts, value)
  }
