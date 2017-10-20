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

package com.fblitho.lithoktsample.lithography.components

import android.graphics.Color
import com.facebook.litho.*
import com.facebook.litho.annotations.*
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaAlign.STRETCH
import com.facebook.yoga.YogaEdge.*
import com.facebook.yoga.YogaJustify.CENTER

/**
 * Renders a "story card" with a header and message. This also has a togglable "saved" state.
 *
 *
 * This and the header do most of the interesting stuff for the approximate end state for the lab
 * activity.
 */
@LayoutSpec
object StoryCardComponentSpec {

  internal val CARD_INSET = 12
  internal val CARD_INTERNAL_PADDING = 7

  @JvmStatic
  @OnCreateLayout
  internal fun onCreateLayout(
      c: ComponentContext, @Prop header: Component<*>, @Prop content: String, @State saved: Boolean): ComponentLayout {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(header)
        .child(
            Text.create(c)
                .text(content)
                .withLayout()
                .paddingDip(HORIZONTAL, CARD_INSET.toFloat())
                .paddingDip(BOTTOM, CARD_INTERNAL_PADDING.toFloat()))
        .child(
            Row.create(c)
                .backgroundColor(if (saved) Color.BLUE else Color.TRANSPARENT)
                .alignSelf(STRETCH)
                .paddingDip(HORIZONTAL, CARD_INSET.toFloat())
                .paddingDip(BOTTOM, CARD_INTERNAL_PADDING.toFloat())
                .paddingDip(TOP, CARD_INTERNAL_PADDING.toFloat())
                .justifyContent(CENTER)
                .child(
                    Image.create(c)
                        .drawableRes(android.R.drawable.ic_menu_save)
                        .withLayout()
                        .alignSelf(YogaAlign.CENTER)
                        .widthDip(20f)
                        .heightDip(20f)
                        .marginDip(END, CARD_INTERNAL_PADDING.toFloat()))
                .child(Text.create(c).text("Save"))
                .clickHandler(StoryCardComponent.onClickSave(c))
                .border(Border.create(c).color(ALL, Color.BLACK).widthDip(TOP, 1).build()))
        .border(Border.create(c).color(ALL, Color.BLACK).widthDip(ALL, 1).build())
        .build()
  }

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      saved: StateValue<Boolean>) {
    saved.set(false)
  }

  @JvmStatic
  @OnUpdateState
  fun onToggleSavedState(saved: StateValue<Boolean>) {
    saved.set(!saved.get())
  }

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onClickSave(c: ComponentContext) {
    StoryCardComponent.onToggleSavedState(c)
  }
}
