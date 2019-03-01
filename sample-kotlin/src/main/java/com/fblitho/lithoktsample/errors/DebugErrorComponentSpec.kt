/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fblitho.lithoktsample.errors

import android.graphics.Typeface
import androidx.annotation.ColorInt
import android.util.Log
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.utils.StacktraceHelper
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge


/**
 * Renders a throwable as a text with a title and provides a touch callback that logs the throwable
 * with WTF level.
 */
@LayoutSpec
object DebugErrorComponentSpec {

  private const val TAG = "DebugErrorComponentSpec"

  @ColorInt
  private val DARK_RED_FRAME = 0xffcd4928.toInt()

  @ColorInt
  private val LIGHT_RED_BACKGROUND = 0xfffcece9.toInt()

  @ColorInt
  private val LIGHT_GRAY_TEXT = 0xff606770.toInt()

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop message: String,
      @Prop throwable: Throwable
  ): Component {
    Log.e(TAG, message, throwable)

    return Column.create(c)
        .backgroundColor(DARK_RED_FRAME)
        .paddingDip(YogaEdge.ALL, 1f)
        .child(
            Text.create(c)
                .backgroundColor(LIGHT_RED_BACKGROUND)
                .paddingDip(YogaEdge.ALL, 4f)
                .textSizeDip(16f)
                .text(message))
        .child(
            Text.create(c)
                .backgroundColor(LIGHT_RED_BACKGROUND)
                .paddingDip(YogaEdge.ALL, 4f)
                .textSizeDip(12f)
                .textColor(LIGHT_GRAY_TEXT)
                .typeface(Typeface.MONOSPACE)
                .text(StacktraceHelper.formatStacktrace(throwable)))
        .clickHandler(DebugErrorComponent.onClick(c))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @Prop message: String, @Prop throwable: Throwable) {
    Log.wtf(TAG, message, throwable)
  }
}
