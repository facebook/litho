/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

  private Demos.DemoItem mDataModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int[] indices = getIntent().getIntArrayExtra(DemoListActivity.INDICES);
    if (indices != null) {
      mDataModel = getDataModel(indices);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      setTitle(mDataModel.getName());
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

    // Keep popping indices off the route until we get to an Activity we can navigate back to.
    // If we can't find one, pop back to the root.
    int[] parentIndices = indices;
    Demos.DemoItem item = null;
    while (parentIndices.length > 1 && !(item instanceof Demos.NavigableDemoItem)) {
      parentIndices = Arrays.copyOf(indices, parentIndices.length - 1);
      item = getDataModel(parentIndices);
    }

    if (item != null && item instanceof Demos.NavigableDemoItem) {
      return ((Demos.NavigableDemoItem) item).getIntent(this, parentIndices);
    } else {
      return new Intent(this, SampleRootActivity.class);
    }
  }

  private Demos.DemoItem getDataModel(int[] indices) {
    Demos.DemoItem model = Demos.DEMOS.get(indices[0]);
    for (int i = 1; i < indices.length; i++) {
      if (model instanceof Demos.HasChildrenDemos) {
        model = ((Demos.HasChildrenDemos) model).getDemos().get(indices[i]);
      } else {
        throw new RuntimeException("Unexpected type: " + model);
      }
    }
    return model;
  }

  protected Demos.DemoItem getDataModel() {
    return mDataModel;
  }
}
