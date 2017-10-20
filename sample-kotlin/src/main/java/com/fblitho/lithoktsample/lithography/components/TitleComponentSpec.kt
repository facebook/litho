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

import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

import android.graphics.Typeface.BOLD
import com.facebook.litho.annotations.ResType.STRING

@LayoutSpec
object TitleComponentSpec {
    @OnCreateLayout
    fun onCreateLayout(
            c: ComponentContext,
            @Prop(resType = STRING) title: String): ComponentLayout {
        return Text.create(c)
                .text(title)
                .textStyle(BOLD)
                .textSizeDip(24f)
                .withLayout()
                .backgroundColor(0xDDFFFFFF.toInt())
                .positionType(YogaPositionType.ABSOLUTE)
                .positionDip(YogaEdge.BOTTOM, 4f)
                .positionDip(YogaEdge.LEFT, 4f)
                .paddingDip(YogaEdge.HORIZONTAL, 6f)
                .build()
    }
}

