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

package com.facebook.litho.stateupdates;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import java.util.List;

@LayoutSpec
public class ComponentWithSizeAndMeasureCallAndStateSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, StateValue<Integer> count, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_INITIAL_STATE));
    count.set(0);
  }

  @OnCreateLayoutWithSizeSpec
  public static Component onCreateLayout(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      @Prop boolean shouldCacheResult,
      @Prop List<LifecycleStep.StepInfo> steps,
      @Prop(optional = true) @Nullable Component component,
      @Prop(optional = true) @Nullable Component mountSpec,
      @Prop(optional = true) String prefix,
      @CachedValue int expensiveValue,
      @State int count) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC));

    if (component != null) {
      component.measure(
          c,
          widthSpec != 0 ? widthSpec : SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
          heightSpec != 0 ? heightSpec : SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
          new Size(),
          shouldCacheResult);
    }

    final String textPrefix = prefix != null ? prefix : "Count:";
    return Column.create(c)
        .child(Text.create(c).text(textPrefix + " " + count))
        .child(component != null ? component : mountSpec)
        .build();
  }

  @OnAttached
  static void onAttached(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_ATTACHED));
  }

  @OnDetached
  static void onDetached(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_DETACHED));
  }

  @OnCreateTreeProp
  static Rect onCreateTreePropRect(ComponentContext c, @Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_TREE_PROP));
    return new Rect();
  }

  @OnCalculateCachedValue(name = "expensiveValue")
  static int onCalculateExpensiveValue(@Prop List<LifecycleStep.StepInfo> steps) {
    steps.add(new LifecycleStep.StepInfo(LifecycleStep.ON_CALCULATE_CACHED_VALUE));
    return 0;
  }

  @OnUpdateState
  static void onIncrementCount(StateValue<Integer> count) {
    final Integer counter = count.get();
    if (counter == null) {
      throw new RuntimeException("state value is null.");
    }
    count.set(counter + 1);
  }

  public static class Caller implements BaseIncrementStateCaller {
    @Override
    public void increment(final ComponentContext c) {
      ComponentWithSizeAndMeasureCallAndState.onIncrementCountSync(c);
    }
  }
}
