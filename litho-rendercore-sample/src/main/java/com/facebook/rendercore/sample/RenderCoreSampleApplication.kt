// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.sample

import android.app.Application
import com.facebook.soloader.SoLoader

class RenderCoreSampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
  }
}
