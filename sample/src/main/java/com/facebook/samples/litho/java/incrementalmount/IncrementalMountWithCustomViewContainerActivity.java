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

package com.facebook.samples.litho.java.incrementalmount;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.utils.IncrementalMountUtils;
import com.facebook.samples.litho.NavigatableDemoActivity;
import com.facebook.samples.litho.R;

public class IncrementalMountWithCustomViewContainerActivity extends NavigatableDemoActivity {

  private boolean mAnimateUp = true;
  private ObjectAnimator mAnimator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_custom_incremental_mount);

    final Button animateButton = findViewById(R.id.button);
    final FrameLayout animatingContainer = findViewById(R.id.animatingContainer);
    final LithoView lithoView = findViewById(R.id.lithoView);

    final ComponentTree componentTree =
        ComponentTree.create(
                lithoView.getComponentContext(),
                SimpleListComponent.create(lithoView.getComponentContext()))
            // Another alternative:
            // If you don't need incremental mount for your view, you can also just turn it off
            // at the ComponentTree level -- however, for complex hierarchies, this could have
            // negative performance implications.
            //
            //   .incrementalMount(false)
            //
            .build();
    lithoView.setComponentTree(componentTree);

    final int windowHeight = getWindowHeight();
    animatingContainer.setTranslationY(windowHeight * .6f);

    animateButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            float target = mAnimateUp ? 0 : windowHeight * .6f;
            mAnimateUp = !mAnimateUp;

            if (mAnimator != null) {
              mAnimator.cancel();
            }

            mAnimator =
                ObjectAnimator.ofFloat(animatingContainer, "translationY", target).setDuration(200);
            mAnimator.setInterpolator(new AccelerateInterpolator());

            // This is the key code block: since the container is animating without notifying the
            // LithoView that its visible bounds are changing, we need to manually notify it each
            // time the visible bounds change. If this code is removed, the rows in the LithoView
            // won't appear during the animation or until
            mAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                  @Override
                  public void onAnimationUpdate(ValueAnimator animation) {
                    IncrementalMountUtils.incrementallyMountLithoViews(animatingContainer);
                  }
                });
            mAnimator.addListener(
                new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                    IncrementalMountUtils.incrementallyMountLithoViews(animatingContainer);
                  }
                });

            mAnimator.start();
          }
        });
  }

  private int getWindowHeight() {
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    return size.y;
  }
}
