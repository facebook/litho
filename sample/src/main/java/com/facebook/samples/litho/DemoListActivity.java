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
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.bordereffects.BorderEffectsActivity;
import com.facebook.samples.litho.lithography.LithographyActivity;
import com.facebook.samples.litho.playground.PlaygroundActivity;
import com.facebook.samples.litho.transitionsdemo.TransitionsDemoActivity;
import java.util.Arrays;
import java.util.List;

public class DemoListActivity extends AppCompatActivity {

  final class DemoListDataModel {
    public final String name;
    public final Intent intent;

    DemoListDataModel(String name, Intent intent) {
      this.name = name;
      this.intent = intent;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);

    List<DemoListDataModel> dataModels =
        Arrays.asList(
            new DemoListDataModel("Lithography", new Intent(this, LithographyActivity.class)),
            new DemoListDataModel("Playground", new Intent(this, PlaygroundActivity.class)),
            new DemoListDataModel(
                "Transitions Demo", new Intent(this, TransitionsDemoActivity.class)),
            new DemoListDataModel("Border effects", new Intent(this, BorderEffectsActivity.class)));

    setContentView(
        LithoView.create(
            this, DemoListComponent.create(componentContext).dataModels(dataModels).build()));
  }
}
