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

import android.R.drawable.star_off
import android.R.drawable.star_on
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State

@LayoutSpec
object FavouriteButtonSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State favourited: Boolean): Component =
      Row.create(c)
          .backgroundRes(if (favourited) star_on else star_off)
          .widthDip(32f)
          .heightDip(32f)
          .clickHandler(FavouriteButton.onClick(c))
          .build()

  @OnUpdateState
  fun toggleFavourited(favourited: StateValue<Boolean>) = favourited.set(!(favourited.get()!!))

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext): Unit = FavouriteButton.toggleFavourited(c)
}
