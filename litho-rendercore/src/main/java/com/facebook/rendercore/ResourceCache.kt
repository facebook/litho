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

package com.facebook.rendercore

import android.content.res.Configuration
import androidx.annotation.VisibleForTesting

abstract class ResourceCache protected constructor(private val configuration: Configuration) {

  abstract operator fun <T> get(key: Int): T?

  abstract operator fun set(key: Int, value: Any)

  companion object {
    private var latest: ResourceCache? = null

    @JvmStatic
    @Synchronized
    fun getLatest(configuration: Configuration): ResourceCache {
      var localLatestCache = latest
      if (
          localLatestCache == null ||
              !areConfigurationsEqualSafely(localLatestCache.configuration, configuration)
      ) {
        localLatestCache = LruResourceCache(Configuration(configuration))
        latest = localLatestCache
      }

      return localLatestCache
    }

    /**
     * Compares two [Configuration]s for equality, treating any [NullPointerException] thrown by the
     * framework's equals/compareTo implementation as "not equal". This works around an OEM-modified
     * Android framework bug where [Configuration.compareTo] dereferences a null [java.util.Locale]
     * inside `mLocale.getLanguage()`, observed on at least Samsung (Android 9) and Huawei
     * (Android 10) builds. Falling back to "not equal" causes the resource cache to be rebuilt —
     * behaviorally identical to a real configuration change. Follows the same vendor/OS
     * boundary-containment precedent as `MatrixDrawable.setDrawableVisibilitySafe` and the
     * Choreographer.getInstance MIUI workaround in `ComponentTree`.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun areConfigurationsEqualSafely(a: Configuration, b: Configuration): Boolean =
        try {
          a == b
        } catch (e: NullPointerException) {
          false
        }
  }
}
