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

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.withSave
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil
import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.LayoutTextContentItem
import com.facebook.litho.TextContent
import com.facebook.litho.Touchable
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.text.BidiIsolationMarkerSpan
import com.facebook.rendercore.text.ClickableSpanListener
import com.facebook.rendercore.text.LongClickableSpan
import com.facebook.rendercore.text.TouchableSpanListener
import kotlin.math.floor

/**
 * A [Drawable] for mounting text content from a [com.facebook.litho.Component].
 *
 * @see [com.facebook.litho.Component]
 * @see [TextComponentSpec]
 */
class TextDrawable : Drawable(), Touchable, TextContent, Drawable.Callback {
  var layout: Layout? = null
    private set

  private var layoutTranslationX = 0f
  private var layoutTranslationY = 0f
  private var clipToBounds = false
  private var shouldHandleTouch = false
  var text: CharSequence? = null
    private set

  /**
   * The complete text the component was given, before any truncation. Differs from [text] only for
   * a custom ellipsis, where [text] is the pre-truncated string actually drawn. Null until set by
   * the full [mount] overload (other overloads leave it null and fall back to [text]).
   */
  var fullText: CharSequence? = null
    private set

  /**
   * Whether the component pre-truncated the text for a custom ellipsis. A standard ellipsize leaves
   * the full text in place and is detected from the layout's ellipsis metadata instead.
   */
  var isExplicitlyTruncated: Boolean = false
    private set

  private var colorStateList: ColorStateList? = null
  private var userColor = 0
  private var highlightColor = 0
  var outlineWidth: Float = 0f
    private set

  var outlineColor: Int = 0
    private set

  var clickableSpans: Array<ClickableSpan>? = null
    private set

  private var imageSpans: Array<ImageSpan>? = null

  private var selectionStart = 0
  private var selectionEnd = 0
  private var _selectionPath: Path? = null
  private var touchAreaPath: Path? = null
  private var selectionPathNeedsUpdate = false
  private var highlightPaint: Paint? = null
  private var textOffsetOnTouchListener: TextOffsetOnTouchListener? = null
  private var clickableSpanExpandedOffset = 0f
  private var longClickActivated = false
  private var longClickHandler: Handler? = null
  private var longClickRunnable: LongClickRunnable? = null
  private var longClickSpanAreaRegion: Region? = null
  private var spanListener: ClickableSpanListener? = null
  private var touchableSpanListener: TouchableSpanListener? = null
  private var contextLogTag: String? = null
  private var currentlyTouchedSpan: ClickableSpan? = null

  /**
   * This should be lazy loaded so that it is only created whenever it is needed. In most cases we
   * won't need this data, so we can avoid this extra instance creation.
   */
  private var textContentItem: TextContent.Item? = null

  override fun draw(canvas: Canvas) {
    val layoutToUse = layout ?: return

    canvas.withSave {
      val bounds = bounds
      if (clipToBounds) {
        clipRect(bounds)
      }
      translate(bounds.left + layoutTranslationX, bounds.top + layoutTranslationY)
      try {
        maybeDrawOutline(canvas)
        layoutToUse.draw(canvas, selectionPath, highlightPaint, 0)
      } catch (e: IndexOutOfBoundsException) {
        val withDebugInfo = RuntimeException("Debug info for IOOB: " + debugInfo, e)
        withDebugInfo.stackTrace = arrayOf()
        throw withDebugInfo
      }
    }
  }

  /**
   * All texts drawn on top of images and videos need contrast outlines and shadows to be more
   * visible against busy backgrounds. Standard Android shadows do not produce the separation of
   * intensity needed, so the Litho library Text Component provides a special outline attribute that
   * draws contrast outlines usually combined with shadows. These outlines are drawn outside the
   * contours to avoid reducing the visible surface of character glyphs. However, since Android has
   * no mode for drawing outside strokes, they need to be drawn twice: the first pass draws strokes,
   * and the second pass draws inner filled shapes. This method performs the first outlining pass if
   * needed.
   *
   * @param canvas - A canvas to draw on.
   */
  private fun maybeDrawOutline(canvas: Canvas) {
    val layoutToUse = layout ?: return
    trace({ "TextDrawable.maybeDrawOutline" }) {
      if (outlineWidth > 0f && outlineColor != 0) {
        val p: Paint = layoutToUse.paint
        val savedColor = p.color
        val savedStyle = p.style
        val savedStrokeWidth = p.strokeWidth
        val savedJoin = p.strokeJoin
        p.strokeJoin = Paint.Join.ROUND
        p.color = outlineColor
        p.style = Paint.Style.STROKE
        p.strokeWidth = outlineWidth
        layoutToUse.draw(canvas)
        p.strokeWidth = savedStrokeWidth
        p.style = savedStyle
        p.color = savedColor
        p.strokeJoin = savedJoin
      }
    }
  }

  private val debugInfo: String
    get() {
      return buildString {
        append(" [")
        append(contextLogTag)
        append("] ")
        val textToUse = text
        if (textToUse is SpannableStringBuilder) {
          val spans = textToUse.getSpans(0, textToUse.length, Any::class.java)
          append("spans: ")
          for (span in spans) {
            append(span.javaClass.simpleName)
            append(", ")
          }
        }
        append("ellipsizedWidth: ")
        append(layout?.ellipsizedWidth)
        append(", lineCount: ")
        append(layout?.lineCount)
      }
    }

  override fun isStateful(): Boolean {
    return colorStateList != null
  }

  override fun onStateChange(states: IntArray): Boolean {
    val colorStateListToUse = colorStateList
    val layoutToUse = layout
    if (colorStateListToUse != null && layoutToUse != null) {
      val previousColor = layoutToUse.paint.color
      val currentColor = colorStateListToUse.getColorForState(states, userColor)

      if (currentColor != previousColor) {
        layoutToUse.paint.color = currentColor
        invalidateSelf()
      }
    }

    return super.onStateChange(states)
  }

  override fun onTouchEvent(event: MotionEvent, host: View): Boolean {
    if (
        (shouldHandleTouchForClickableSpan(event) ||
            shouldHandleTouchForLongClickableSpan(event)) && handleOnTouchForSpans(event, host)
    ) {
      return true
    }

    if (shouldHandleTextOffsetOnTouch(event)) {
      handleTextOffsetChange(event)
      // We will not consume touch events at this point because TextOffsetOnTouch event has
      // lower priority than click/longclick events.
    }

    return false
  }

  private fun handleOnTouchForSpans(event: MotionEvent, view: View): Boolean {
    if (!ComponentsConfiguration.enableNewHandleTouchForSpansMethod) {
      return handleTouchForSpans(event, view)
    }
    val action = event.actionMasked
    if (action == MotionEvent.ACTION_MOVE && !longClickActivated && longClickRunnable != null) {
      trackLongClickBoundaryOnMove(event)
    }
    val clickActivationAllowed = !longClickActivated
    if (action == MotionEvent.ACTION_UP) {
      resetLongClick()
    }
    val bounds = bounds
    if (!isWithinBounds(bounds, event)) {
      if (action == MotionEvent.ACTION_CANCEL) {
        handleNoSpanOrOutOfBoundsCancelEvent(currentlyTouchedSpan, event, view)
      }
      currentlyTouchedSpan = null
      return false
    }
    var currentSpan = currentlyTouchedSpan
    if (action == MotionEvent.ACTION_UP) {
      clearSelection()
      val spanListenerToUse = spanListener
      if (
          clickActivationAllowed &&
              currentSpan != null &&
              (spanListenerToUse == null || !spanListenerToUse.onClick(currentSpan, view))
      ) {
        currentSpan.onClick(view)
      }
      currentlyTouchedSpan = null
    } else if (action == MotionEvent.ACTION_DOWN) {
      val x = event.x.toInt() - bounds.left
      val y = event.y.toInt() - bounds.top
      currentSpan = findClickableSpanAtCoordinates(x, y)
      if (currentSpan is LongClickableSpan) {
        registerForLongClick(currentSpan, view)
      }
      setSelection(currentSpan)
      currentlyTouchedSpan = currentSpan
    } else if (action == MotionEvent.ACTION_CANCEL) {
      clearSelection()
      resetLongClick()
      currentlyTouchedSpan = null
    }
    val touchableSpanListenerToUse = touchableSpanListener
    touchableSpanListenerToUse?.onTouch(currentSpan, event, view)
    if (currentSpan == null) {
      return false
    }
    return true
  }

  private fun handleTouchForSpans(event: MotionEvent, view: View): Boolean {
    val action = event.actionMasked
    if (action == MotionEvent.ACTION_CANCEL) {
      clearSelection()
      resetLongClick()
      val touchableSpanListenerToUse = touchableSpanListener
      touchableSpanListenerToUse?.onTouch(null, event, view)
      return false
    }
    val cachedLongClickableSpanForMove =
        if (action == MotionEvent.ACTION_MOVE && longClickSpanAreaRegion != null) {
          longClickRunnable?.longClickableSpan
        } else {
          null
        }

    if (action == MotionEvent.ACTION_MOVE && !longClickActivated && longClickRunnable != null) {
      trackLongClickBoundaryOnMove(event)
    }

    val clickActivationAllowed = !longClickActivated
    if (action == MotionEvent.ACTION_UP) {
      resetLongClick()
    }

    val bounds = bounds
    if (!isWithinBounds(bounds, event)) {
      return false
    }

    if (
        ComponentsConfiguration.enableTextDrawableTouchAnrFix && action == MotionEvent.ACTION_MOVE
    ) {
      return false
    }
    if (cachedLongClickableSpanForMove != null) {
      if (longClickRunnable?.longClickableSpan !== cachedLongClickableSpanForMove) {
        clearSelection()
        return false
      }
      touchableSpanListener?.onTouch(cachedLongClickableSpanForMove, event, view)
      return true
    }
    val x = event.x.toInt() - bounds.left
    val y = event.y.toInt() - bounds.top

    var clickedSpan = getClickableSpanInCoords(x, y)

    if (clickedSpan == null && clickableSpanExpandedOffset > 0) {
      clickedSpan =
          getClickableSpanInProximityToClick(x.toFloat(), y.toFloat(), clickableSpanExpandedOffset)
    }

    if (clickedSpan == null) {
      clearSelection()
      return false
    }

    if (action == MotionEvent.ACTION_UP) {
      clearSelection()
      val spanListenerToUse = spanListener
      if (
          clickActivationAllowed &&
              (spanListenerToUse == null || !spanListenerToUse.onClick(clickedSpan, view))
      ) {
        clickedSpan.onClick(view)
      }
    } else if (action == MotionEvent.ACTION_DOWN) {
      if (clickedSpan is LongClickableSpan) {
        registerForLongClick(clickedSpan, view)
      }
      setSelection(clickedSpan)
    }
    val touchableSpanListenerToUse = touchableSpanListener
    if (touchableSpanListenerToUse != null) {
      touchableSpanListenerToUse.onTouch(clickedSpan, event, view)
    }
    return true
  }

  private fun findClickableSpanAtCoordinates(x: Int, y: Int): ClickableSpan? {
    var clickedSpan = getClickableSpanInCoords(x, y)

    if (clickedSpan == null && clickableSpanExpandedOffset > 0) {
      clickedSpan =
          getClickableSpanInProximityToClick(x.toFloat(), y.toFloat(), clickableSpanExpandedOffset)
    }

    return clickedSpan
  }

  private fun handleNoSpanOrOutOfBoundsCancelEvent(
      currentlyTouchedSpan: ClickableSpan?,
      event: MotionEvent,
      view: View,
  ) {
    clearSelection()
    resetLongClick()
    touchableSpanListener?.onTouch(currentlyTouchedSpan, event, view)
  }

  private fun resetLongClick() {
    longClickRunnable?.let { runnable -> longClickHandler?.removeCallbacks(runnable) }
    longClickRunnable = null
    longClickSpanAreaRegion = null
    longClickActivated = false
  }

  private fun registerForLongClick(longClickableSpan: LongClickableSpan, view: View) {
    longClickSpanAreaRegion =
        if (
            !ComponentsConfiguration.enableTextDrawableTouchAnrFix && hasBidiIsolationMarker(text)
        ) {
          createLongClickSpanAreaRegion(longClickableSpan)
        } else {
          null
        }
    val runnable = LongClickRunnable(longClickableSpan, view)
    longClickRunnable = runnable
    longClickHandler?.postDelayed(runnable, ViewConfiguration.getLongPressTimeout().toLong())
  }

  private fun handleTextOffsetChange(event: MotionEvent) {
    val bounds = bounds
    val x = event.x.toInt() - bounds.left
    val y = event.y.toInt() - bounds.top

    val offset = getTextOffsetAt(x, y)
    val textToUse = text
    if (textToUse != null && offset >= 0 && offset <= textToUse.length) {
      textOffsetOnTouchListener?.textOffsetOnTouch(offset)
    }
  }

  override fun shouldHandleTouchEvent(event: MotionEvent): Boolean {
    return (shouldHandleTouchForClickableSpan(event) ||
        shouldHandleTouchForLongClickableSpan(event) ||
        shouldHandleTextOffsetOnTouch(event))
  }

  private fun shouldHandleTouchForClickableSpan(event: MotionEvent): Boolean {
    val action = event.actionMasked
    val isUpOrDown = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN
    return ((shouldHandleTouch && isWithinBounds(bounds, event) && isUpOrDown) ||
        action == MotionEvent.ACTION_CANCEL)
  }

  private fun shouldHandleTouchForLongClickableSpan(event: MotionEvent): Boolean {
    return shouldHandleTouch && longClickHandler != null && event.action != MotionEvent.ACTION_DOWN
  }

  private fun trackLongClickBoundaryOnMove(event: MotionEvent) {
    val bounds = bounds
    if (!isWithinBounds(bounds, event)) {
      resetLongClick()
      return
    }

    val spanAreaRegion = longClickSpanAreaRegion
    if (spanAreaRegion != null) {
      val x = event.x.toInt() - bounds.left
      val y = event.y.toInt() - bounds.top
      if (!spanAreaRegion.contains(x, y)) {
        resetLongClick()
      }
      return
    }

    val clickableSpan = getClickableSpanInCoords(
        event.x.toInt() - bounds.left,
        event.y.toInt() - bounds.top,
    )
    if (longClickRunnable?.longClickableSpan !== clickableSpan) {
      resetLongClick()
    }
  }

  private fun createLongClickSpanAreaRegion(longClickableSpan: LongClickableSpan): Region? {
    val layoutToUse = layout ?: return null
    val textToUse = text as? Spanned ?: return null
    val spanStart = textToUse.getSpanStart(longClickableSpan)
    val spanEnd = textToUse.getSpanEnd(longClickableSpan)
    if (spanStart < 0 || spanEnd <= spanStart) {
      return null
    }

    val clipBoundsRegion = Region()
    val startLine = layoutToUse.getLineForOffset(spanStart)
    val endLine = layoutToUse.getLineForOffset(spanEnd - 1)
    for (line in startLine..endLine) {
      clipBoundsRegion.op(getTextLineBounds(layoutToUse, line), Region.Op.UNION)
    }

    val spanAreaPath = Path()
    layoutToUse.getSelectionPath(spanStart, spanEnd, spanAreaPath)
    val spanAreaRegion = Region().apply { setPath(spanAreaPath, clipBoundsRegion) }
    return if (spanAreaRegion.isEmpty) null else spanAreaRegion
  }

  private fun getTextLineBounds(layoutToUse: Layout, line: Int): Rect {
    val left: Float
    val right: Float
    if (layoutToUse.alignment == Layout.Alignment.ALIGN_CENTER) {
      left = layoutToUse.getLineLeft(line)
      right = layoutToUse.getLineRight(line)
    } else {
      val direction = layoutToUse.getParagraphDirection(line)
      val alignment = layoutToUse.getParagraphAlignment(line)
      val rightAligned =
          ((direction == Layout.DIR_RIGHT_TO_LEFT && alignment == Layout.Alignment.ALIGN_NORMAL) ||
              (direction == Layout.DIR_LEFT_TO_RIGHT &&
                  alignment == Layout.Alignment.ALIGN_OPPOSITE))
      left =
          if (rightAligned) layoutToUse.width - layoutToUse.getLineMax(line)
          else layoutToUse.getParagraphLeft(line).toFloat()
      right =
          if (rightAligned) layoutToUse.getParagraphRight(line).toFloat()
          else layoutToUse.getLineMax(line)
    }

    return Rect(
        floor(left.toDouble()).toInt().coerceAtLeast(0),
        layoutToUse.getLineTop(line),
        (floor(right.toDouble()).toInt() + 1).coerceAtMost(layoutToUse.width),
        layoutToUse.getLineBottom(line),
    )
  }

  private fun shouldHandleTextOffsetOnTouch(event: MotionEvent): Boolean {
    return textOffsetOnTouchListener != null &&
        event.actionMasked == MotionEvent.ACTION_DOWN &&
        bounds.contains(event.x.toInt(), event.y.toInt())
  }

  fun mount(
      text: CharSequence?,
      layout: Layout?,
      userColor: Int,
      clickableSpans: Array<ClickableSpan>?,
  ) {
    mount(
        text,
        layout,
        0f,
        0f,
        false,
        null,
        userColor,
        0,
        0f,
        0,
        clickableSpans,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null,
    )
  }

  fun mount(text: CharSequence?, layout: Layout?, userColor: Int, highlightColor: Int) {
    mount(
        text,
        layout,
        0f,
        0f,
        false,
        null,
        userColor,
        highlightColor,
        0f,
        0,
        null,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null,
    )
  }

  fun mount(
      text: CharSequence?,
      layout: Layout?,
      layoutTranslationY: Float,
      colorStateList: ColorStateList?,
      userColor: Int,
      highlightColor: Int,
      clickableSpans: Array<ClickableSpan>?,
  ) {
    mount(
        text,
        layout,
        0f,
        0f,
        false,
        null,
        userColor,
        highlightColor,
        0f,
        0,
        clickableSpans,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null,
    )
  }

  fun mount(
      text: CharSequence?,
      layout: Layout?,
      layoutTranslationX: Float,
      layoutTranslationY: Float,
      clipToBounds: Boolean,
      colorStateList: ColorStateList?,
      userColor: Int,
      highlightColor: Int,
      outlineWidth: Float,
      outlineColor: Int,
      clickableSpans: Array<ClickableSpan>?,
      imageSpans: Array<ImageSpan>?,
      spanListener: ClickableSpanListener?,
      touchableSpanListener: TouchableSpanListener?,
      textOffsetOnTouchListener: TextOffsetOnTouchListener?,
      highlightStartOffset: Int,
      highlightEndOffset: Int,
      clickableSpanExpandedOffset: Float,
      contextLogTag: String?,
      fullText: CharSequence? = null,
      isExplicitlyTruncated: Boolean = false,
  ) {
    this.layout = layout
    longClickSpanAreaRegion = null
    this.layoutTranslationX = layoutTranslationX
    this.layoutTranslationY = layoutTranslationY
    this.clipToBounds = clipToBounds
    this.text = text
    this.fullText = fullText
    this.isExplicitlyTruncated = isExplicitlyTruncated
    this.clickableSpans = clickableSpans
    this.touchableSpanListener = touchableSpanListener
    if (longClickHandler == null && containsLongClickableSpan(clickableSpans)) {
      longClickHandler = Handler()
    }
    this.spanListener = spanListener
    this.textOffsetOnTouchListener = textOffsetOnTouchListener
    shouldHandleTouch = !clickableSpans.isNullOrEmpty()
    this.highlightColor = highlightColor
    setOutline(outlineWidth, outlineColor)
    this.clickableSpanExpandedOffset = clickableSpanExpandedOffset
    if (userColor != 0) {
      this.colorStateList = null
      this.userColor = userColor
    } else {
      val colorStateListToUse = colorStateList ?: TextComponentSpec.textColorStateList
      this.colorStateList = colorStateListToUse
      this.userColor = colorStateListToUse.defaultColor
      val layoutToUse = this.layout
      if (layoutToUse != null) {
        layoutToUse.paint.color = colorStateListToUse.getColorForState(state, this.userColor)
      }
    }

    if (highlightOffsetsValid(text, highlightStartOffset, highlightEndOffset)) {
      setSelection(highlightStartOffset, highlightEndOffset)
    } else {
      clearSelection()
    }

    if (imageSpans != null) {
      for (i in 0 until imageSpans.size) {
        val drawable = imageSpans[i].drawable
        drawable.callback = this
        drawable.setVisible(true, false)
      }
    }
    this.imageSpans = imageSpans
    this.contextLogTag = contextLogTag

    invalidateSelf()
  }

  fun setTextColor(@ColorInt textColor: Int) {
    userColor = textColor
    layout?.paint?.color = textColor
    invalidateSelf()
  }

  fun setOutline(outlineWidth: Float, @ColorInt outlineColor: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      this.outlineWidth = outlineWidth
      this.outlineColor = outlineColor
      invalidateSelf()
    }
  }

  private fun highlightOffsetsValid(
      text: CharSequence?,
      highlightStart: Int,
      highlightEnd: Int,
  ): Boolean {
    return text != null &&
        highlightStart >= 0 &&
        highlightEnd <= text.length &&
        highlightStart < highlightEnd
  }

  fun unmount() {
    if (hasBidiIsolationMarker(text)) {
      resetLongClick()
    }
    textContentItem = null
    layout = null
    layoutTranslationY = 0f
    text = null
    fullText = null
    isExplicitlyTruncated = false
    clickableSpans = null
    shouldHandleTouch = false
    highlightColor = 0
    spanListener = null
    textOffsetOnTouchListener = null
    currentlyTouchedSpan = null
    colorStateList = null
    userColor = 0
    imageSpans?.let { spans ->
      for (i in 0 until spans.size) {
        val drawable = spans[i].drawable
        drawable.callback = null
        drawable.setVisible(false, false)
      }
      imageSpans = null
    }
  }

  override fun setAlpha(alpha: Int) {}

  override fun setColorFilter(cf: ColorFilter?) {}

  override fun getOpacity(): Int {
    return PixelFormat.UNKNOWN
  }

  val color: Int
    get() = requireNotNull(layout).paint.color

  val textSize: Float
    get() = requireNotNull(layout).paint.textSize

  /**
   * Get the clickable span that is at the exact coordinates
   *
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's located where the click occurred, or: `null` if no clickable
   *   span was located there
   */
  private fun getClickableSpanInCoords(x: Int, y: Int): ClickableSpan? {
    val offset = getTextOffsetAt(x, y)
    if (offset < 0) {
      return null
    }
    if (text is Spanned) {
      val clickableSpans = (text as Spanned).getSpans(offset, offset, ClickableSpan::class.java)

      if (clickableSpans != null && clickableSpans.size > 0) {
        return clickableSpans[0]
      }
    }

    return null
  }

  private fun getTextOffsetAt(x: Int, y: Int): Int {
    val layoutToUse = requireNotNull(layout)
    val line = layoutToUse.getLineForVertical(y)

    val left: Float
    val right: Float

    if (layoutToUse.alignment == Layout.Alignment.ALIGN_CENTER) {
      /**
       * [Layout.getLineLeft] and [Layout.getLineRight] properly account for paragraph margins on
       * centered text.
       */
      left = layoutToUse.getLineLeft(line)
      right = layoutToUse.getLineRight(line)
    } else {
      /**
       * [Layout.getLineLeft] and [Layout.getLineRight] do NOT properly account for paragraph
       * margins on non-centered text, so we need an alternative.
       *
       * To determine the actual bounds of the line, we need the line's direction and alignment,
       * leading margin, and extent, but only the first is available directly. The margin is given
       * by either [Layout.getParagraphLeft] or [Layout.getParagraphRight] depending on line
       * direction, and [Layout.getLineMax] gives the extent *plus* the leading margin, so we can
       * figure out the rest from there.
       */
      val direction = layoutToUse.getParagraphDirection(line)
      val alignment = layoutToUse.getParagraphAlignment(line)
      val rightAligned =
          ((direction == Layout.DIR_RIGHT_TO_LEFT && alignment == Layout.Alignment.ALIGN_NORMAL) ||
              (direction == Layout.DIR_LEFT_TO_RIGHT &&
                  alignment == Layout.Alignment.ALIGN_OPPOSITE))
      left =
          if (rightAligned) layoutToUse.width - layoutToUse.getLineMax(line)
          else layoutToUse.getParagraphLeft(line).toFloat()
      right =
          if (rightAligned) layoutToUse.getParagraphRight(line).toFloat()
          else layoutToUse.getLineMax(line)
    }

    if (x < left || x > right) {
      return -1
    }

    return try {
      layoutToUse.getOffsetForHorizontal(line, x.toFloat())
    } catch (e: ArrayIndexOutOfBoundsException) {
      // This happens for bidi text on Android 7-8.
      // See
      // https://android.googlesource.com/platform/frameworks/base/+/821e9bd5cc2be4b3210cb0226e40ba0f42b51aed
      -1
    }
  }

  @get:VisibleForTesting
  val layoutAlignment: Layout.Alignment?
    get() = layout?.alignment

  /**
   * Get the clickable span that's close to where the view was clicked.
   *
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's close the click position, or: `null` if no clickable span was
   *   close to the click, or if a link was directly clicked or if more than one clickable span was
   *   in proximity to the click.
   */
  private fun getClickableSpanInProximityToClick(
      x: Float,
      y: Float,
      tapRadius: Float,
  ): ClickableSpan? {
    val touchAreaRegion = Region()
    val clipBoundsRegion = Region()

    val touchAreaPathToUse = Path()
    if (touchAreaPath == null) {
      touchAreaPath = touchAreaPathToUse
    }

    clipBoundsRegion[0, 0, LayoutMeasureUtil.getWidth(layout)] = LayoutMeasureUtil.getHeight(layout)
    touchAreaPathToUse.reset()
    touchAreaPathToUse.addCircle(x, y, tapRadius, Path.Direction.CW)
    touchAreaRegion.setPath(touchAreaPathToUse, clipBoundsRegion)

    var result: ClickableSpan? = null
    val clickableSpansToUse = clickableSpans
    if (clickableSpansToUse != null) {
      for (span in clickableSpansToUse) {
        if (span is ExactBoundsClickableSpan) {
          continue
        }
        if (
            !isClickCloseToSpan(span, text as Spanned?, layout, touchAreaRegion, clipBoundsRegion)
        ) {
          continue
        }

        if (result != null) {
          // This is the second span that's close to the tap, so we don't have a definitive answer
          return null
        }

        result = span
      }
    }

    return result
  }

  private val selectionPath: Path?
    get() {
      if (selectionStart == selectionEnd) {
        return null
      }

      if (Color.alpha(highlightColor) == 0) {
        return null
      }

      if (selectionPathNeedsUpdate) {
        if (_selectionPath == null) {
          _selectionPath = Path()
        }

        layout?.getSelectionPath(selectionStart, selectionEnd, _selectionPath)
        selectionPathNeedsUpdate = false
      }

      return _selectionPath
    }

  private fun setSelection(span: ClickableSpan?) {
    val text = text as? Spanned ?: return
    setSelection(text.getSpanStart(span), text.getSpanEnd(span))
  }

  /**
   * Updates selection to [selectionStart, selectionEnd] range.
   *
   * @param selectionStart
   * @param selectionEnd
   */
  private fun setSelection(selectionStart: Int, selectionEnd: Int) {
    if (
        Color.alpha(highlightColor) == 0 ||
            (this.selectionStart == selectionStart && this.selectionEnd == selectionEnd)
    ) {
      return
    }

    this.selectionStart = selectionStart
    this.selectionEnd = selectionEnd

    val highlightPaintToUse = highlightPaint ?: Paint()
    if (highlightPaint == null) {
      highlightPaint = highlightPaintToUse
    }
    highlightPaintToUse.color = highlightColor

    selectionPathNeedsUpdate = true
    invalidateSelf()
  }

  private fun clearSelection() {
    setSelection(0, 0)
  }

  private fun isClickCloseToSpan(
      span: ClickableSpan?,
      buffer: Spanned?,
      layout: Layout?,
      touchAreaRegion: Region,
      clipBoundsRegion: Region,
  ): Boolean {
    if (layout == null || buffer == null) {
      return false
    }

    val clickableSpanAreaRegion = Region()
    val clickableSpanAreaPath = Path()

    layout.getSelectionPath(
        buffer.getSpanStart(span),
        buffer.getSpanEnd(span),
        clickableSpanAreaPath,
    )
    clickableSpanAreaRegion.setPath(clickableSpanAreaPath, clipBoundsRegion)

    return clickableSpanAreaRegion.op(touchAreaRegion, Region.Op.INTERSECT)
  }

  override fun invalidateDrawable(drawable: Drawable) {
    invalidateSelf()
  }

  override fun scheduleDrawable(drawable: Drawable, runnable: Runnable, l: Long) {
    scheduleSelf(runnable, l)
  }

  override fun unscheduleDrawable(drawable: Drawable, runnable: Runnable) {
    unscheduleSelf(runnable)
  }

  override val items: List<TextContent.Item>
    get() {
      val item = orCreateTextItem
      return if (item == null) {
        emptyList()
      } else {
        listOf(item)
      }
    }

  override val textList: List<CharSequence>
    get() {
      val item = orCreateTextItem
      return if (item == null) {
        emptyList()
      } else {
        listOf(item.text)
      }
    }

  private val orCreateTextItem: TextContent.Item?
    get() {
      val layout = layout ?: return null
      val existing = textContentItem
      if (existing != null) {
        return existing
      }
      val text = requireNotNull(text)
      return LayoutTextContentItem(
          layout,
          text,
          fullText ?: text,
          isExplicitlyTruncated,
          isMounted = { this.layout === layout },
      )
          .also { textContentItem = it }
    }

  fun interface TextOffsetOnTouchListener {
    fun textOffsetOnTouch(textOffset: Int)
  }

  private inner class LongClickRunnable(
      val longClickableSpan: LongClickableSpan,
      private val longClickableSpanView: View,
  ) : Runnable {
    override fun run() {
      val spanListenerToUse = spanListener
      longClickActivated =
          ((spanListenerToUse != null &&
              spanListenerToUse.onLongClick(longClickableSpan, longClickableSpanView)) ||
              longClickableSpan.onLongClick(longClickableSpanView))
    }
  }

  companion object {
    private fun isWithinBounds(bounds: Rect, event: MotionEvent): Boolean {
      return bounds.contains(event.x.toInt(), event.y.toInt())
    }

    private fun hasBidiIsolationMarker(text: CharSequence?): Boolean {
      val spannedText = text as? Spanned ?: return false
      return spannedText
          .getSpans(0, spannedText.length, BidiIsolationMarkerSpan::class.java)
          .isNotEmpty()
    }

    private fun containsLongClickableSpan(clickableSpans: Array<ClickableSpan>?): Boolean {
      if (clickableSpans == null) {
        return false
      }

      for (span in clickableSpans) {
        if (span is LongClickableSpan) {
          return true
        }
      }

      return false
    }
  }
}
