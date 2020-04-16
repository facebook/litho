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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecLifecycleTesterSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class MountSpecLifecycleTest extends BaseLithoComponentTest {

  @Override
  protected void overrideDefaults() {
    mShouldAttachBeforeTest = false;
    mShouldMeasureBeforeTest = false;
    mShouldLayoutBeforeTest = false;
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

    attachLithoView();
    measureLithoView();
    layoutLithoView();

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

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    detachLithoView();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND);
  }

  @Test
  public void lifecycle_onReAttach_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();
    detachLithoView();

    info.clear();

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    measureLithoView();

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    measureLithoViewWithSizeSpec(makeSizeSpec(800, EXACTLY), makeSizeSpec(600, UNSPECIFIED));

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE, LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED);
  }

  @Test
  public void lifecycle_onRemeasureWithExactSize_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    measureLithoViewWithSize(800, 600);

    assertThat(getSteps(info))
        .describedAs(
            "No lifecycle methods should be called because EXACT measures should skip layout calculation.")
        .isEmpty();
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSize_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    measureLithoViewWithSize(800, 600);
    layoutLithoView();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSizeAsNonRoot_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(Column.create(mContext).child(component).build());

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    measureLithoViewWithSize(800, 600);
    layoutLithoView();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onSetShallowCopy_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    mLithoView.setComponent(component.makeShallowCopy());

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component = MountSpecLifecycleTester.create(mContext).steps(info).build();
    mLithoView.setComponent(component);

    attachLithoView();
    measureLithoView();
    layoutLithoView();

    info.clear();

    List<LifecycleStep.StepInfo> newInfo = new ArrayList<>();
    mLithoView.setComponent(MountSpecLifecycleTester.create(mContext).steps(newInfo).build());

    measureLithoView();
    layoutLithoView();

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

  public static List<LifecycleStep> getSteps(List<LifecycleStep.StepInfo> infos) {
    List<LifecycleStep> steps = new ArrayList<>(infos.size());
    for (LifecycleStep.StepInfo info : infos) {
      steps.add(info.step);
    }
    return steps;
  }
}
