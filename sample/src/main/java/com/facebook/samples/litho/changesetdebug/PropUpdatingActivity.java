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
import android.widget.Toast;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.ArrayList;
import java.util.List;

public class PropUpdatingActivity extends NavigatableDemoActivity {

  private LithoView mLithoView;
  private ComponentContext mComponentContext;
  private List<DataModel> mDataModels;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mComponentContext = new ComponentContext(this);

    mDataModels = getData(5);

    mLithoView =
        LithoView.create(
            this,
            SelectedItemRootComponent.create(mComponentContext)
                .dataModels(mDataModels)
                .selectedItem(0)
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
                SelectedItemRootComponent.create(mComponentContext)
                    .dataModels(mDataModels)
                    .selectedItem(1)
                    .build());
            Toast.makeText(
                mComponentContext.getAndroidContext(),
                "Updated selected item prop",
                Toast.LENGTH_SHORT);
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

// Uncomment to try fixed version.
/*public class PropUpdatingActivity extends NavigatableDemoActivity {

  private LithoView mLithoView;
  private ComponentContext mComponentContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mComponentContext = new ComponentContext(this);

    mLithoView = LithoView.create(
        this,
        SelectedItemRootComponentFixed
            .create(mComponentContext)
            .dataModels(getData(5, 0))
            .build());

    setContentView(mLithoView);

    fetchData();
  }

  private void fetchData() {
    final HandlerThread thread = new HandlerThread("bg");
    thread.start();

    final Handler handler = new Handler(thread.getLooper());
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(4000);
          mLithoView.setComponent(
              SelectedItemRootComponentFixed
                  .create(mComponentContext)
                  .dataModels(getData(5, 1))
                  .build()
          );
          Toast.makeText(mComponentContext.getAndroidContext(), "Updated selected item prop", Toast.LENGTH_SHORT);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
  }


  private List<DataModel> getData(int size, int selected) {
    final List<DataModel> dataModels = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      DataModel dataModel = new DataModel("Item " + i, i);
      dataModel.setSelected(i == selected);
      dataModels.add(dataModel);
    }

    return dataModels;
  }

}

*/
