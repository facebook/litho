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

package com.facebook.samples.litho.animations.renderthread;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.R;

public class RenderThreadAnimationActivity extends NavigatableDemoActivity
    implements CompoundButton.OnCheckedChangeListener {
  private LithoView mLithoView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_render_thread);

    mLithoView = (LithoView) findViewById(R.id.lithoView);
    buildAndSetComponentTree(true);

    CheckBox checkRT = (CheckBox) findViewById(R.id.checkRT);
    checkRT.setChecked(true);
    checkRT.setOnCheckedChangeListener(this);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    buildAndSetComponentTree(isChecked);
  }

  private void buildAndSetComponentTree(boolean useRT) {
    final ComponentContext context = new ComponentContext(this);
    final Component component = RTAnimationComponent.create(context).useRT(useRT).build();
    mLithoView.setComponentTree(ComponentTree.create(context, component).build());
  }

  void pauseUI(View view) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Ignore
    }
  }
}
