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


package com.facebook.samples.litho.transitionsdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

public class TransitionsDemoActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    final RecyclerBinder binder = createRecyclerBinder(componentContext);

    setContentView(
        LithoView.create(
            this,
            Recycler.create(componentContext)
                .binder(binder)
                .build()));
  }

  private RecyclerBinder createRecyclerBinder(ComponentContext c) {
    final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
        .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false))
        .build(c);

    final int numDemos = 5;
    for (int i = 0; i < numDemos * 4; i++) {
      Component component;

      // Keep alternating between demos
      switch (i % numDemos) {
        case 0:
          component = StoryFooterComponent.create(c).build();
          break;
        case 1:
          component = UpDownBlocksComponent.create(c).build();
          break;
        case 2:
          component = LeftRightBlocksComponent.create(c).build();
          break;
        case 3:
          component = OneByOneLeftRightBlocksComponent.create(c).build();
          break;
        case 4:
          component = LeftRightBlocksSequenceComponent.create(c).build();
          break;
        default:
          throw new RuntimeException("Bad index: " + i);
      }

      recyclerBinder.insertItemAt(i, component);
    }

    return recyclerBinder;
  }
}
