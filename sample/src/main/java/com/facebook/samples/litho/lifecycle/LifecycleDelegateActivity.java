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
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class LifecycleDelegateActivity extends NavigatableDemoActivity {

  private static final AtomicInteger mId = new AtomicInteger(0);
  private LithoView mLithoView;
  private ConsoleView mConsoleView;

  private final DelegateListener mDelegateListener =
      new DelegateListener() {
        @Override
        public void onDelegateMethodCalled(int type, Thread thread, long timestamp, int id) {
          if (mConsoleView != null) {
            mConsoleView.log(type, thread, timestamp, id);
          }
        }

        public void setRootComponent(boolean isSync) {
          if (mLithoView != null) {
            final Random random = new Random();
            final Component root =
                LifecycleDelegateComponent.create(
                        new ComponentContext(LifecycleDelegateActivity.this))
                    .id(mId.getAndIncrement())
                    .key(String.valueOf(random.nextInt())) // Force to reset component.
                    .delegateListener(mDelegateListener)
                    .build();
            if (isSync) {
              mLithoView.setComponent(root);
            } else {
              mLithoView.setComponentAsync(root);
            }
          }
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LinearLayout parent = new LinearLayout(this);
    parent.setOrientation(LinearLayout.VERTICAL);
    parent.setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));

    final ComponentContext componentContext = new ComponentContext(this);
    mLithoView =
        LithoView.create(
            this,
            LifecycleDelegateComponent.create(componentContext)
                .id(mId.getAndIncrement())
                .delegateListener(mDelegateListener)
                .build());
    final LayoutParams params1 = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params1.weight = 1;
    mLithoView.setLayoutParams(params1);
    parent.addView(mLithoView);

    mConsoleView = new ConsoleView(this);
    final LayoutParams params2 = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params2.weight = 1;
    mConsoleView.setLayoutParams(params2);
    parent.addView(mConsoleView);

    setContentView(parent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (mLithoView != null) {
      mLithoView.release();
    }
  }
}
