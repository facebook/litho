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

package com.facebook.samples.litho.changesetdebug;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.ArrayList;
import java.util.List;

public class ItemsRerenderingActivity extends NavigatableDemoActivity {

  private LithoView mLithoView;
  private ComponentContext mComponentContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mComponentContext = new ComponentContext(this);

    mLithoView =
        LithoView.create(
            this,
            ItemsRerenderingRootComponent.create(mComponentContext)
                .dataModels(getData(15))
                .build());

    setContentView(mLithoView);

    fetchData();
  }

  private void fetchData() {
    final HandlerThread thread = new HandlerThread("bg");
    thread.start();

    final Handler handler = new Handler(thread.getLooper());
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            mLithoView.setComponent(
                ItemsRerenderingRootComponent.create(mComponentContext)
                    .dataModels(getData(16))
                    .build());
          }
        },
        4000);
  }

  private List<DataModel> getData(int size) {
    final List<DataModel> dataModels = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      dataModels.add(new DataModel("Item " + i, i));
    }

    return dataModels;
  }
}
