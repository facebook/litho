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

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;

/**
 * A simple Component for learning some common {@literal @}Props on Text Components. Also a good way
 * to learn the basics of {@literal @}Props and the builders they generate.
 */
@LayoutSpec
public class LearningTextWidgetComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Text.create(c)
        .text("Hello, World!")
        .textColor(Color.RED)
        .textSizePx(70)
        .typeface(Typeface.DEFAULT_BOLD)
        .build();
  }
}
