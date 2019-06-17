package com.facebook.litho.codelab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val componentContext = ComponentContext(this)
    setContentView(
      LithoView.create(
        this,
        RootComponent.create(componentContext).build()
      )
    )
  }
}
