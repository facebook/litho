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

import android.content.pm.PackageManager

/**
 * A state object that can be hoisted to control and observe [permission] status changes.
 *
 * In most cases, this will be created via [usePermission].
 *
 * It's recommended that apps exercise the permissions workflow as described in the
 * [documentation](https://developer.android.com/training/permissions/requesting#workflow_for_requesting_permissions).
 */
interface PermissionState {

  /** The permission to request and observe. */
  val permission: String

  /** The status of [permission]. */
  val status: PermissionStatus

  /**
   * Request the [permission] to the user.
   *
   * This should always be triggered from non-composable scope, for example, from a side-effect or a
   * non-composable callback. Otherwise, this will result in an IllegalStateException.
   *
   * This triggers a system dialog that asks the user to grant or revoke the permission. Note that
   * this dialog might not appear on the screen if the user doesn't want to be asked again or has
   * denied the permission multiple times. This behavior varies depending on the Android level API.
   */
  fun requestPermission()
}

/** Model for the status of a permission. It can be granted or denied or never ask again. */
@JvmInline
value class PermissionStatus private constructor(val value: Int) {

  val isGranted: Boolean
    get() = this == Granted

  val isDenied: Boolean
    get() = this == NotGranted

  val shouldShowRationale: Boolean
    get() = this == DeniedNeverAskAgain

  companion object {
    /**
     * The permission has been granted. Same as [ContextCompat.checkSelfPermission] retuing
     * [PackageManager.PERMISSION_GRANTED].
     */
    val Granted: PermissionStatus = PermissionStatus(PackageManager.PERMISSION_GRANTED)

    /**
     * The permission has been denied. Same as [ContextCompat.checkSelfPermission] retuing
     * [PackageManager.PERMISSION_DENIED]
     */
    val NotGranted: PermissionStatus = PermissionStatus(PackageManager.PERMISSION_DENIED)

    /**
     * The permission has been denied and the user has selected "Never ask again". Same as
     * [ActivityCompat.shouldShowRequestPermissionRationale] returning `false`.
     */
    val DeniedNeverAskAgain: PermissionStatus = PermissionStatus(-2)
  }
}
