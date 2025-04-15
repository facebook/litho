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

package com.facebook.litho.widget

import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import android.text.Layout
import androidx.annotation.VisibleForTesting
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil
import java.lang.ref.WeakReference

/**
 * A class that schedules a background draw of a [Layout] or [Drawable]. Drawing a [Layout] in the
 * background ensures that the glyph caches are warmed up and ready for drawing the same [Layout] on
 * a real [Canvas]. This will substantially reduce drawing times for big chunks of text. On the
 * other hand over-using text warming might rotate the glyphs cache too quickly and diminish the
 * optimization. Similarly, for [Drawable] starting on art it will be put in a texture cache of
 * RenderNode, which will speed up drawing.
 */
class TextureWarmer private constructor() {
  private val handler: WarmerHandler

  class WarmDrawable(val drawable: Drawable, val width: Int, val height: Int)

  init {
    val handlerThread = HandlerThread(TAG, WARMER_THREAD_PRIORITY)
    handlerThread.start()

    handler = WarmerHandler(handlerThread.looper)
  }

  @get:VisibleForTesting
  val warmerLooper: Looper
    get() = handler.looper

  /**
   * Schedules a [Layout] to be drawn in the background. This warms up the Glyph cache for that
   * [Layout].
   */
  fun warmLayout(layout: Layout) {
    handler.obtainMessage(WarmerHandler.WARM_LAYOUT, WeakReference(layout)).sendToTarget()
  }

  /**
   * Schedules a [Drawable] to be drawn in the background. This warms up the texture cache for that
   * [Drawable].
   */
  fun warmDrawable(drawable: WarmDrawable) {
    handler.obtainMessage(WarmerHandler.WARM_DRAWABLE, WeakReference(drawable)).sendToTarget()
  }

  private class WarmerHandler(looper: Looper) : Handler(looper) {
    private val picture: Picture?

    init {
      this.picture =
          try {
            Picture()
          } catch (e: RuntimeException) {
            null
          }
    }

    override fun handleMessage(msg: Message) {
      if (picture == null) {
        return
      }

      try {
        when (msg.what) {
          WARM_LAYOUT -> {
            (msg.obj as WeakReference<Layout?>).get()?.let { layout ->
              val canvas = picture.beginRecording(layout.width, LayoutMeasureUtil.getHeight(layout))
              layout.draw(canvas)
              picture.endRecording()
            }
          }
          WARM_DRAWABLE -> {
            (msg.obj as WeakReference<WarmDrawable?>).get()?.let { warmDrawable ->
              val canvas = picture.beginRecording(warmDrawable.width, warmDrawable.height)
              warmDrawable.drawable.draw(canvas)
              picture.endRecording()
            }
          }
        }
      } catch (e: Exception) {
        // Nothing to do here. This is a best effort. No real problem if it fails.
      }
    }

    companion object {
      const val WARM_LAYOUT: Int = 0
      const val WARM_DRAWABLE: Int = 1
    }
  }

  companion object {
    private val TAG: String = TextureWarmer::class.java.name

    private const val WARMER_THREAD_PRIORITY =
        (Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_LOWEST) / 2

    private var Instance: TextureWarmer? = null

    @get:Synchronized
    @get:JvmStatic
    val instance: TextureWarmer
      /** @return the global [TextureWarmer] instance. */
      get() {
        if (Instance == null) {
          Instance = TextureWarmer()
        }

        return requireNotNull(Instance)
      }
  }
}
