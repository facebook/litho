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

package com.facebook.samples.lithocodelab.examples.modules;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

/**
 * Introduction to basic {@literal @}Props usage. Make your own Component with {@literal @}Props and
 * see what is generated. and how to interact with it.
 */
@LayoutSpec
public class LearningPropsComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String text1, @Prop String text2) {
    return Column.create(c)
        .child(Text.create(c).text(text1).textSizeDip(50))
        .child(
            Text.create(c).text(text2).textColorRes(android.R.color.holo_green_dark).textSizeSp(30))
        .build();
  }
}
