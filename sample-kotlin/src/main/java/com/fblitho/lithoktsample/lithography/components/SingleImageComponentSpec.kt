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

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.fresco.FrescoImage

@LayoutSpec
object SingleImageComponentSpec {
  @PropDefault
  @JvmField
  val imageAspectRatio = 1f

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop image: String,
      @Prop(optional = true) imageAspectRatio: Float): Component =
      Fresco.newDraweeControllerBuilder()
          .setUri(image)
          .build().let {
        FrescoImage.create(c)
            .controller(it)
            .imageAspectRatio(imageAspectRatio)
            .build()
      }
}
