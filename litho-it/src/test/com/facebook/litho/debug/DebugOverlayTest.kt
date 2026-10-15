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

package com.facebook.litho.debug

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.facebook.litho.ThreadUtils
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class DebugOverlayTest {

  @Test
  fun draw_mixedHistory_rendersOrderedColorsAndLatestThreadBackground() {
    val overlay = DebugOverlay(listOf(true, false))
    overlay.setBounds(0, 0, 100, 100)
    val canvas = RecordingCanvas()

    overlay.draw(canvas)

    assertThat(canvas.backgrounds)
        .containsExactly(BackgroundDraw(Rect(0, 0, 100, 100), greenSemitransparent()))
    assertThat(canvas.historyBoxes)
        .containsExactly(
            RectDraw(4f, 4f, 12f, 14f, redOpaque()),
            RectDraw(16f, 4f, 24f, 14f, greenOpaque()),
        )
    assertThat(canvas.texts).containsExactly(TextDraw("2x", 8f, 36f, Color.BLACK))
  }

  @Test
  fun draw_emptyResolveHistory_rendersTransparentBackgroundAndEmptyCount() {
    val overlay = DebugOverlay.getDebugOverlay(nextId.incrementAndGet())
    overlay.setBounds(0, 0, 100, 100)
    val canvas = RecordingCanvas()

    overlay.draw(canvas)

    assertThat(canvas.backgrounds)
        .containsExactly(BackgroundDraw(Rect(0, 0, 100, 100), Color.TRANSPARENT))
    assertThat(canvas.historyBoxes).isEmpty()
    assertThat(canvas.texts).containsExactly(TextDraw("", 8f, 36f, Color.BLACK))
  }

  @Test
  fun draw_boxEndingAtRightBound_excludesBoxAndClampsBottom() {
    val overlay = DebugOverlay(listOf(false, true))
    overlay.setBounds(0, 0, 24, 10)
    val canvas = RecordingCanvas()

    overlay.draw(canvas)

    assertThat(canvas.backgrounds)
        .containsExactly(BackgroundDraw(Rect(0, 0, 24, 10), redSemitransparent()))
    assertThat(canvas.historyBoxes).containsExactly(RectDraw(4f, 4f, 12f, 10f, greenOpaque()))
    assertThat(canvas.texts).containsExactly(TextDraw("2x", 8f, 36f, Color.BLACK))
  }

  @Test
  fun updateResolveHistory_recordsMainAndBackgroundThreadClassification() {
    val id = nextId.incrementAndGet()
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE)
    DebugOverlay.updateResolveHistory(id)
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE)
    DebugOverlay.updateResolveHistory(id)
    val overlay = DebugOverlay.getDebugOverlay(id)
    overlay.setBounds(0, 0, 100, 100)
    val canvas = RecordingCanvas()

    overlay.draw(canvas)

    assertThat(canvas.backgrounds)
        .containsExactly(BackgroundDraw(Rect(0, 0, 100, 100), redSemitransparent()))
    assertThat(canvas.historyBoxes)
        .containsExactly(
            RectDraw(4f, 4f, 12f, 14f, greenOpaque()),
            RectDraw(16f, 4f, 24f, 14f, redOpaque()),
        )
    assertThat(canvas.texts).containsExactly(TextDraw("2x", 8f, 36f, Color.BLACK))
  }

  @After
  fun tearDown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED)
  }

  private data class BackgroundDraw(val bounds: Rect, val color: Int)

  private data class RectDraw(
      val left: Float,
      val top: Float,
      val right: Float,
      val bottom: Float,
      val color: Int,
  )

  private data class TextDraw(
      val text: String,
      val x: Float,
      val y: Float,
      val color: Int,
  )

  private class RecordingCanvas : Canvas() {
    val backgrounds = mutableListOf<BackgroundDraw>()
    val historyBoxes = mutableListOf<RectDraw>()
    val texts = mutableListOf<TextDraw>()

    override fun drawRect(rect: Rect, paint: Paint) {
      backgrounds.add(BackgroundDraw(Rect(rect), paint.color))
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
    ) {
      historyBoxes.add(RectDraw(left, top, right, bottom, paint.color))
    }

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
      texts.add(TextDraw(text, x, y, paint.color))
    }
  }

  private companion object {
    private val nextId = AtomicInteger()

    private fun redSemitransparent() = Color.argb(0x22, 0xFF, 0x00, 0x00)

    private fun greenSemitransparent() = Color.argb(0x22, 0x00, 0xFF, 0x00)

    private fun redOpaque() = Color.argb(0xCC, 0xFF, 0x00, 0x00)

    private fun greenOpaque() = Color.argb(0xCC, 0x00, 0xFF, 0x00)
  }
}
