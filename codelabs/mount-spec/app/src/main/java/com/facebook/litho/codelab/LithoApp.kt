package com.facebook.litho.codelab

import android.app.Application
import com.facebook.soloader.SoLoader

class LithoApp : Application() {

  override fun onCreate() {
    super.onCreate()

    SoLoader.init(this, false)
  }
}
