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

package com.facebook.samples.litho.hscroll;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class HorizontalScrollWithSnapActivity extends NavigatableDemoActivity {
  static Integer[] colors =
      new Integer[] {
        Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.GRAY
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    final LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    final RecyclerCollectionEventsController eventsController =
        new RecyclerCollectionEventsController();
    container.addView(
        LithoView.create(
            this,
            HorizontalScrollWithSnapComponent.create(componentContext)
                .colors(colors)
                .eventsController(eventsController)
                .build()));
    container.addView(
        LithoView.create(
            this,
            HorizontalScrollScrollerComponent.create(componentContext)
                .colors(colors)
                .eventsController(eventsController)
                .build()));
    setContentView(container);
  }
}
