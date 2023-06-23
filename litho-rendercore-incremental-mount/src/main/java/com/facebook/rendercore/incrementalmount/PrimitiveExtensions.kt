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

package com.facebook.rendercore.incrementalmount

import com.facebook.rendercore.primitives.MountConfigurationScope

/**
 * Indicates whether the component skips Incremental Mount. If this is true then the Component will
 * not be involved in Incremental Mount.
 */
var <ContentType : Any> MountConfigurationScope<ContentType>.shouldExcludeFromIncrementalMount:
    Boolean
  get() {
    throw IllegalStateException(
        "Getting the current value of excludeFromIncrementalMount is not supported.")
  }
  set(value) {
    addExtra(R.id.should_exclude_from_incremental_mount, true)
  }
