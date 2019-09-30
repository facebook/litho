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

package com.facebook.samples.litho.dynamicprops;

import android.content.Context;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBindDynamicValue;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.utils.MeasureUtils;

@MountSpec
class ClockComponentSpec {

  @OnMeasure
  static void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    final ClockView clockView = new ClockView(c.getAndroidContext());
    clockView.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec));
    size.width = clockView.getMeasuredWidth();
    size.height = clockView.getMeasuredHeight();
  }

  @OnCreateMountContent
  static ClockView onCreateMountContent(Context c) {
    return new ClockView(c);
  }

  @OnBindDynamicValue
  static void onBindTime(ClockView clockView, @Prop(dynamic = true) long time) {
    clockView.setTime(time % ClockView.TWELVE_HOURS);
  }
}
