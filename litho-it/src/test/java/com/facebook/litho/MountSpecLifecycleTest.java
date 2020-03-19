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

package com.facebook.litho;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.LifecycleStep;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecLifecycleTesterSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountSpecLifecycleTest {

  ComponentContext mContext;
  private ComponentTree mComponentTree;
  LithoView mLithoView;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mLithoView = new LithoView(mContext);
    mComponentTree = ComponentTree.create(mContext).build();
    mLithoView.setComponentTree(mComponentTree);
  }

  @After
  public void after() {
    MountSpecLifecycleTesterSpec.StaticContainer.sLastCreatedView = null;
  }

  @Test
  public void lifecycle_onSetComponentWithoutLayout_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onLayout_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onDetach_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    info.clear();

    detach();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND);
  }

  @Test
  public void lifecycle_onReAttach_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();
    detach();

    info.clear();

    attach();
    measure();
    layout();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    info.clear();

    measure();

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    info.clear();

    measure(800, 600);

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE, LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED);
  }

  @Test
  public void lifecycle_onSetShallowCopy_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    info.clear();

    mLithoView.setComponent(component.makeShallowCopy());

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attach();
    measure();
    layout();

    info.clear();

    List<LifecycleStep.StepInfo> newInfo = new ArrayList<>();
    mLithoView.setComponent(MountSpecLifecycleTester.create(mContext).steps(newInfo).build());

    measure();
    layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on old instance in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);

    assertThat(getSteps(newInfo))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  void attach() {
    mLithoView.onAttachedToWindow();
  }

  void measure() {
    mLithoView.measure(makeMeasureSpec(1080, EXACTLY), makeMeasureSpec(1920, UNSPECIFIED));
  }

  void measure(int width, int height) {
    mLithoView.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(height, UNSPECIFIED));
  }

  void layout() {
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());
  }

  void detach() {
    mLithoView.onDetachedFromWindow();
  }

  public static List<LifecycleStep> getSteps(List<LifecycleStep.StepInfo> infos) {
    List<LifecycleStep> steps = new ArrayList<>(infos.size());
    for (LifecycleStep.StepInfo info : infos) {
      steps.add(info.step);
    }
    return steps;
  }
}
