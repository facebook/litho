/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.java.changesetdebug;

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
