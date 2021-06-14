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

package com.facebook.litho.widget;

import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LayoutSpec
public class ComponentTreeTesterSpec {
  @PropDefault @Nullable protected static final CountDownLatch unlockWaitingOnCreateLayout = null;
  @PropDefault @Nullable protected static final CountDownLatch lockOnCreateLayoutFinish = null;
  @PropDefault @Nullable protected static final List<LifecycleStep> lifecycleSteps = null;

  @OnCreateLayout
  public static Component onCreateLayout(
      ComponentContext c,
      @Prop(optional = true) @Nullable CountDownLatch unlockWaitingOnCreateLayout,
      @Prop(optional = true) @Nullable CountDownLatch lockOnCreateLayoutFinish,
      @Prop(optional = true) @Nullable List<LifecycleStep> lifecycleSteps) {

    if (unlockWaitingOnCreateLayout != null) {
      unlockWaitingOnCreateLayout.countDown();
    }

    if (lockOnCreateLayoutFinish != null) {
      try {
        lockOnCreateLayoutFinish.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (lifecycleSteps != null && !lifecycleSteps.contains(LifecycleStep.ON_CREATE_LAYOUT)) {
      lifecycleSteps.add(LifecycleStep.ON_CREATE_LAYOUT);
    }

    return Column.create(c).build();
  }
}
