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

package com.facebook.litho.permissions

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

/**
 * A mutable object that can be used to request and observe permission status changes.
 *
 * In most cases, this will be created via [usePermission].
 *
 * @param permission the permission to control and observe.
 * @param activity to check if the user should be presented with a rationale for [permission].
 */
internal class PermissionStatusManager(
    val permission: String,
    private val activity: ComponentActivity,
    internal var onUpdate: ((PermissionStatus) -> Unit)? = null,
) {

  var status: PermissionStatus = activity.getPermissionStatus(permission)
    get() = activity.getPermissionStatus(permission)
    private set(value) {
      val hasChanged = field != value
      field = value
      if (hasChanged) {
        onUpdate?.invoke(value)
      }
    }

  fun requestPermission() {
    if (!status.isGranted) {
      launcher?.launch(permission) ?: error("Launcher has not been initialized")
    }
  }

  internal var launcher: ActivityResultLauncher<String>? = null

  internal fun updatePermissionStatus() {
    status = activity.getPermissionStatus(permission)
  }
}
