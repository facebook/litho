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

package com.facebook.samples.litho.lifecycle;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoLifecycleProviderDelegate;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.R;
import java.util.concurrent.atomic.AtomicInteger;

public class LifecycleFragmentActivity extends NavigatableDemoActivity {

  private static final AtomicInteger mId = new AtomicInteger(0);
  private LithoView mLithoView;
  private final LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate =
      new LithoLifecycleProviderDelegate();
  private final ConsoleDelegateListener mConsoleDelegateListener = new ConsoleDelegateListener();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final LinearLayout parent = new LinearLayout(this);
    final ComponentContext componentContext = new ComponentContext(this);
    mLithoView =
        LithoView.create(
            componentContext,
            Column.create(componentContext)
                .child(
                    LifecycleDelegateComponent.create(componentContext)
                        .id(String.valueOf(mId.getAndIncrement()))
                        .consoleDelegateListener(mConsoleDelegateListener)
                        .build())
                .build(),
            mLithoLifecycleProviderDelegate);
    final LinearLayout.LayoutParams params1 =
        new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params1.weight = 1;
    mLithoView.setLayoutParams(params1);
    parent.addView(mLithoView);
    setContentView(parent);
    FrameLayout frame = new FrameLayout(this);
    frame.setId(R.id.fragment_view);
    setContentView(frame, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    Fragment lifecycleFragment = new LifecycleFragment();
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    ft.add(R.id.fragment_view, lifecycleFragment).commit();
  }
}
