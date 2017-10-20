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
import android.view.View
import com.facebook.litho.*
import com.facebook.litho.annotations.*

@LayoutSpec
object FavouriteButtonSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State favourited: Boolean): ComponentLayout =
      Row.create(c)
          .backgroundRes(if (favourited) star_on else star_off)
          .widthDip(32f)
          .heightDip(32f)
          .clickHandler(FavouriteButton.onClick(c))
          .build()

  @JvmStatic
  @OnUpdateState
  fun toggleFavourited(favourited: StateValue<Boolean>) = favourited.set(!favourited.get())

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @FromEvent view: View) = FavouriteButton.toggleFavourited(c)
}
