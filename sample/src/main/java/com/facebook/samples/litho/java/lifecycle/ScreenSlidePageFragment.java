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

package com.facebook.samples.litho.java.lifecycle;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoLifecycleProvider;
import com.facebook.litho.LithoLifecycleProviderDelegate;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.R;
import com.facebook.yoga.YogaAlign;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenSlidePageFragment extends Fragment {

  private static final AtomicInteger mId = new AtomicInteger(0);
  private LithoView mLithoView;
  private ConsoleView mConsoleView;
  private boolean wasVisible = false;
  private int mPosition = 0;
  private final LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate =
      new LithoLifecycleProviderDelegate();
  private final ConsoleDelegateListener mConsoleDelegateListener = new ConsoleDelegateListener();
  private final DelegateListener mDelegateListener =
      new DelegateListener() {
        @Override
        public void onDelegateMethodCalled(int type, Thread thread, long timestamp, String id) {
          if (mConsoleView != null) {
            mConsoleView.post(
                new ConsoleView.LogRunnable(
                    mConsoleView,
                    LifecycleDelegateLog.prefix(thread, timestamp, id),
                    LifecycleDelegateLog.log(type)));
          }
        }

        public void setRootComponent(boolean isSync) {}
      };

  public static ScreenSlidePageFragment newInstance(int position) {
    ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
    Bundle args = new Bundle();
    args.putInt("position", position);
    fragment.setArguments(args);
    fragment.setPosition(position);
    return fragment;
  }

  private void setPosition(int position) {
    mPosition = position;
  }

  public ScreenSlidePageFragment() {
    super(R.layout.screen_slide_fragment);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    ViewGroup parent =
        (ViewGroup) inflater.inflate(R.layout.screen_slide_fragment, container, false);

    final ComponentContext c = new ComponentContext(requireContext());
    mLithoView =
        LithoView.create(
            c,
            Column.create(c)
                .child(Text.create(c).text(String.valueOf(mPosition)).alignSelf(YogaAlign.CENTER))
                .child(
                    LifecycleDelegateComponent.create(c)
                        .id(String.valueOf(mId.getAndIncrement()))
                        .delegateListener((mDelegateListener))
                        .consoleDelegateListener(mConsoleDelegateListener)
                        .build())
                .build(),
            mLithoLifecycleProviderDelegate);

    final LinearLayout.LayoutParams params1 =
        new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params1.weight = 1;
    mLithoView.setLayoutParams(params1);
    parent.addView(mLithoView);

    mConsoleView = new ConsoleView(requireContext());
    final LinearLayout.LayoutParams params2 =
        new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params2.weight = 1;
    mConsoleView.setLayoutParams(params2);
    parent.addView(mConsoleView);

    return parent;
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);

    if (wasVisible == isVisibleToUser) {
      return;
    }
    if (isVisibleToUser) {
      wasVisible = true;
      mLithoLifecycleProviderDelegate.moveToLifecycle(
          LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    } else {
      wasVisible = false;
      mLithoLifecycleProviderDelegate.moveToLifecycle(
          LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);
    }
  }
}
