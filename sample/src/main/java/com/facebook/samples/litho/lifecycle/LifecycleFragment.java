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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.samples.litho.R;
import com.facebook.litho.LithoLifecycleProvider;
import com.facebook.litho.LithoLifecycleProviderDelegate;
import com.facebook.litho.LithoView;
import java.util.concurrent.atomic.AtomicInteger;

public class LifecycleFragment extends Fragment {

  private static final AtomicInteger mId = new AtomicInteger(0);
  private ConsoleView mConsoleView;
  private LithoView mLithoView;
  private final LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate =
      new LithoLifecycleProviderDelegate();
  private final ConsoleDelegateListener mConsoleDelegateListener = new ConsoleDelegateListener();

  @Override
  public View onCreateView(
      @Nullable LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    final LinearLayout parent = new LinearLayout(requireContext());
    final ComponentContext c = new ComponentContext(requireContext());
    mLithoView =
        LithoView.create(
            c,
            Column.create(c)
                .child(
                    LifecycleDelegateComponent.create(c)
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

    Button fragmentButton = new Button(requireContext());
    fragmentButton.setText("New Fragment");
    fragmentButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            LifecycleFragment lifecycleFragment = new LifecycleFragment();
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container_view, lifecycleFragment, null)
                .addToBackStack(null)
                .commit();
            mLithoLifecycleProviderDelegate.moveToLifecycle(
                LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);
          }
        });

    mConsoleView = new ConsoleView(requireContext());
    final LinearLayout.LayoutParams params2 =
        new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params2.weight = 1;
    mConsoleView.setLayoutParams(params2);
    parent.addView(mConsoleView);
    parent.addView(fragmentButton);
    return parent;
  }
}
