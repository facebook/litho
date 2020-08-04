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

package com.facebook.samples.litho;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import java.util.Arrays;

public class NavigatableDemoActivity extends AppCompatActivity {

  private DemoListActivity.DemoListDataModel mDataModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int[] indices = getIntent().getIntArrayExtra(DemoListActivity.INDICES);
    if (indices != null) {
      mDataModel = getDataModel(indices);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      setTitle(mDataModel.name);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mDataModel = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public @Nullable Intent getParentActivityIntent() {
    int[] indices = getIntent().getIntArrayExtra(DemoListActivity.INDICES);
    if (indices == null) {
      return null;
    }

    final Intent parentIntent = new Intent(this, DemoListActivity.class);
    if (indices.length > 1) {
      parentIntent.putExtra(DemoListActivity.INDICES, Arrays.copyOf(indices, indices.length - 1));
    }

    return parentIntent;
  }

  private DemoListActivity.DemoListDataModel getDataModel(int[] indices) {
    DemoListActivity.DemoListDataModel model = DemoListActivity.DATA_MODELS.get(indices[0]);
    for (int i = 1; i < indices.length; i++) {
      model = model.datamodels.get(indices[i]);
    }
    return model;
  }

  protected DemoListActivity.DemoListDataModel getDataModel() {
    return mDataModel;
  }
}
