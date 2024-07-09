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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.facebook.litho.ComponentScope
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.findComponentActivity
import com.facebook.litho.lifecycle.LifecycleOwnerTreeProp
import com.facebook.litho.onCleanup
import com.facebook.litho.useCached
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import java.util.UUID

/**
 * Litho Hook to request and observe the status of a permission. The API returns a [PermissionState]
 * which can be used to request the permission if it is not granted. The [PermissionState.status]
 * will be the [PermissionStatus] of the [permission] when the `render` function is called; this is
 * to ensure race conditions and tearing.
 *
 * Check [rules for hooks](https://fblitho.com/docs/mainconcepts/hooks-intro/#rules-for-hooks)
 */
@ExperimentalLithoApi
@Hook
fun ComponentScope.usePermission(
    permission: String,
    onReceivePermissionResult: ((Boolean) -> Unit)? = null
): PermissionState {

  val context = androidContext
  val activity =
      useCached(context) {
        context.findComponentActivity()
            ?: throw IllegalStateException(
                "Permissions should be called in the context of an Activity")
      }

  // region Update the permission status state change when the permission updates
  val manager =
      useCached(permission) {
        PermissionStatusManager(
            permission,
            activity,
        )
      }
  val permissionStatus = useState { manager.status }
  useEffect(manager) {
    manager.onUpdate = { permissionStatus.update(it) }
    onCleanup { manager.onUpdate = null }
  }
  // endregion

  // region Check and update the permission if it changed when the lifecycle resumes
  val lifecycle = (LifecycleOwnerTreeProp.value ?: activity).lifecycle
  val permissionCheckerObserver =
      useCached(manager) {
        LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            // If the permission is revoked, check again.
            // don't check if the permission was denied as
            // that triggers a process restart.
            if (manager.status != PermissionStatus.Granted) {
              manager.updatePermissionStatus()
            }
          }
        }
      }
  useEffect(lifecycle, permissionCheckerObserver) {
    lifecycle.addObserver(permissionCheckerObserver)
    onCleanup { lifecycle.removeObserver(permissionCheckerObserver) }
  }
  // endregion

  // region Register with Activity Result Register to get Launcher to request permission
  val key = useCached { UUID.randomUUID().toString() }
  useEffect(key, manager) {
    manager.launcher =
        activity.activityResultRegistry.register(
            key, ActivityResultContracts.RequestPermission()) { granted ->
              manager.updatePermissionStatus()
              onReceivePermissionResult?.invoke(granted)
            }
    onCleanup { manager.launcher = null }
  }
  // endregion

  return LithoPermissionState(status = permissionStatus.value, manager = manager)
}

internal fun Activity.getPermissionStatus(permission: String): PermissionStatus {
  val hasPermission = this.checkPermission(permission)
  return if (hasPermission) {
    PermissionStatus.Granted
  } else if (this.shouldShowRationale(permission)) {
    PermissionStatus.NotGranted
  } else {
    PermissionStatus.DeniedNeverAskAgain
  }
}

internal fun Context.checkPermission(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

internal fun Activity.shouldShowRationale(permission: String): Boolean {
  return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}

internal class LithoPermissionState(
    override val status: PermissionStatus,
    private val manager: PermissionStatusManager,
) : PermissionState {

  override val permission: String = manager.permission

  override fun requestPermission() {
    manager.requestPermission()
  }
}
