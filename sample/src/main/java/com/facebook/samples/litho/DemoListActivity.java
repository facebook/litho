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

package com.facebook.samples.litho;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.samples.litho.lithography.LithographyActivity;
import com.facebook.samples.litho.playground.PlaygroundActivity;
import com.facebook.samples.litho.transitionsdemo.TransitionsDemoActivity;

public class DemoListActivity extends AppCompatActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
        .layoutInfo(new LinearLayoutInfo(componentContext, OrientationHelper.VERTICAL, false))
        .build(componentContext);

    addDemo(
        componentContext,
        recyclerBinder,
        "Lithography",
        new Intent(this, LithographyActivity.class));
    addDemo(
        componentContext,
        recyclerBinder,
        "Playground",
        new Intent(this, PlaygroundActivity.class));
    addDemo(
        componentContext,
        recyclerBinder,
        "Transitions Demo",
        new Intent(this, TransitionsDemoActivity.class));

    setContentView(
        LithoView.create(
            this,
            DemoListComponent.create(componentContext)
                .recyclerBinder(recyclerBinder)
                .build()));
  }

  private void addDemo(
      ComponentContext c,
      RecyclerBinder recyclerBinder,
      String name,
      Intent intentToLaunch) {
    recyclerBinder.insertItemAt(
        recyclerBinder.getItemCount(),
        DemoListItemComponent.create(c)
            .name(name)
            .intent(intentToLaunch)
            .build());
  }
}
