// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceCacheTest {

  @Test
  fun testSameConfigurationDoesNotUpdateResourceCache() {
    val configuration = ApplicationProvider.getApplicationContext<Context>().resources.configuration
    val cache = ResourceCache.getLatest(configuration)
    assertThat(cache).isEqualTo(ResourceCache.getLatest(configuration))
  }

  @Test
  fun testSameConfigurationNewInstanceDoesNotUpdateResourceCache() {
    val configuration = ApplicationProvider.getApplicationContext<Context>().resources.configuration
    val cache = ResourceCache.getLatest(configuration)
    assertThat(cache).isEqualTo(ResourceCache.getLatest(Configuration(configuration)))
  }

  @Suppress("AppBundleLocaleChanges")
  @Test
  fun testDifferentLocaleUpdatesResourceCache() {
    val configuration =
        Configuration(ApplicationProvider.getApplicationContext<Context>().resources.configuration)
    configuration.setLocale(Locale("en"))
    val cache = ResourceCache.getLatest(configuration)
    configuration.setLocale(Locale("it"))
    assertThat(cache).isNotEqualTo(ResourceCache.getLatest(configuration))
  }
}
