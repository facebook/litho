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

package com.facebook.samples.litho.changesetdebug;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.ArrayList;
import java.util.List;

public class StateResettingActivity extends NavigatableDemoActivity {

  private LithoView mLithoView;
  private List<DataModel> mData;
  private ComponentContext mComponentContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mData = getData();
    mComponentContext = new ComponentContext(this);

    mLithoView =
        LithoView.create(
            this,
            StateResettingRootComponent.create(mComponentContext)
                .dataModels(mData)
                .showHeader(false)
                .build());

    setContentView(mLithoView);

    fetchHeader();
  }

  private void fetchHeader() {
    final HandlerThread thread = new HandlerThread("bg");
    thread.start();

    final Handler handler = new Handler(thread.getLooper());
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            mLithoView.setComponent(
                StateResettingRootComponent.create(mComponentContext)
                    .dataModels(mData)
                    .showHeader(true)
                    .build());
          }
        },
        4000);
  }

  private List<DataModel> getData() {
    final List<DataModel> dataModels = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      dataModels.add(new DataModel("Item " + i, i));
    }

    return dataModels;
  }
}
