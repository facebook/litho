/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.helper.ComponentTestHelper.measureAndLayout;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LifecycleMethodsWithExperimentTest {

  private enum LifecycleStep {
    ON_CREATE_LAYOUT,
    ON_PREPARE,
    ON_MEASURE,
    ON_BOUNDS_DEFINED,
    ON_CREATE_MOUNT_CONTENT,
    ON_MOUNT,
    ON_BIND,
    ON_UNBIND,
    ON_UNMOUNT
  }

  private LithoView mLithoView;
  private ComponentTree mComponentTree;
  private LifecycleMethodsComponent mComponent;

  @Before
  public void setup() throws Exception {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;
    mLithoView = new LithoView(RuntimeEnvironment.application);
    mComponent = LifecycleMethodsComponent.create(10);

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree = ComponentTree.create(c, mComponent).build();
    mLithoView.setComponentTree(mComponentTree);
  }

  @After
  public void after() throws Exception {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = false;
  }

  @Test
  public void lifecycle() {
    mLithoView.onAttachedToWindow();
    measureAndLayout(mLithoView);

    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_BIND);

    mLithoView.onDetachedFromWindow();
    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_UNBIND);

    mLithoView.onAttachedToWindow();
    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_BIND);

    mComponentTree.setRoot(LifecycleMethodsComponent.create(20));
    measureAndLayout(mLithoView);
    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_UNMOUNT);

    mComponent = mComponent.makeShallowCopyForTest();
    mComponentTree.setRoot(mComponent);
    measureAndLayout(mLithoView);
    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_BIND);

    mLithoView.onDetachedFromWindow();
    mComponentTree.setRoot(mComponent);
    measureAndLayout(mLithoView);
    assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_UNBIND);
  }

  private static class LifecycleMethodsComponent extends Component {

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      setCurrentStep(LifecycleStep.ON_CREATE_LAYOUT);

      return super.onCreateLayout(c);
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return this == other;
    }

    @Override
    protected void onPrepare(ComponentContext c) {
      setCurrentStep(LifecycleStep.ON_PREPARE);
    }

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected void onMeasure(
        ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
      setCurrentStep(LifecycleStep.ON_MEASURE);

      size.width = mSize;
      size.height = mSize;
    }

    @Override
    protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {
      setCurrentStep(LifecycleStep.ON_BOUNDS_DEFINED);
    }

    @Override
    protected Object onCreateMountContent(Context context) {
      setCurrentStep(LifecycleStep.ON_CREATE_MOUNT_CONTENT);

      return new LifecycleMethodsDrawable(this);
    }

    @Override
    protected void onMount(ComponentContext c, Object convertContent) {
      setCurrentStep(LifecycleStep.ON_MOUNT);
      final LifecycleMethodsDrawable d = (LifecycleMethodsDrawable) convertContent;

      d.setComponent(this);
    }

    @Override
    protected void onUnmount(ComponentContext c, Object mountedContent) {
      setCurrentStep(LifecycleStep.ON_UNMOUNT);
    }

    @Override
    protected void onBind(ComponentContext c, Object mountedContent) {
      setCurrentStep(LifecycleStep.ON_BIND);
    }

    @Override
    protected void onUnbind(ComponentContext c, Object mountedContent) {
      setCurrentStep(LifecycleStep.ON_UNBIND);
    }

    @Override
    protected boolean shouldUpdate(Component previous, Component next) {
      return true;
    }

    @Override
    public MountType getMountType() {
      return MountType.DRAWABLE;
    }

    public static LifecycleMethodsComponent create(int size) {
      return new LifecycleMethodsComponent(size);
    }

    private final int mSize;
    private LifecycleStep mCurrentStep = LifecycleStep.ON_UNMOUNT;

    protected LifecycleMethodsComponent(int size) {
      super("LifecycleMethodsInstance");
      mSize = size;
    }

    LifecycleStep getCurrentStep() {
      return mCurrentStep;
    }

    void setCurrentStep(LifecycleStep currentStep) {
      switch (currentStep) {
        case ON_CREATE_LAYOUT:
          assertThat(mCurrentStep).isEqualTo(LifecycleStep.ON_UNMOUNT);
          break;

        case ON_PREPARE:
          if (getMountType() != MountType.NONE) {
            assertThat(mCurrentStep).isEqualTo(LifecycleStep.ON_UNMOUNT);
          } else {
            assertThat(mCurrentStep).isEqualTo(LifecycleStep.ON_CREATE_LAYOUT);
          }
          break;

        case ON_MEASURE:
          assertThat(
                  mCurrentStep == LifecycleStep.ON_PREPARE
                      || mCurrentStep == LifecycleStep.ON_MEASURE)
              .isTrue();
          break;

        case ON_BOUNDS_DEFINED:
          assertThat(
                  mCurrentStep == LifecycleStep.ON_PREPARE
                      || mCurrentStep == LifecycleStep.ON_MEASURE)
              .isTrue();
          break;

        case ON_CREATE_MOUNT_CONTENT:
          assertThat(mCurrentStep == LifecycleStep.ON_BOUNDS_DEFINED).isTrue();

        case ON_MOUNT:
          assertThat(
                  mCurrentStep == LifecycleStep.ON_BOUNDS_DEFINED
                      || mCurrentStep == LifecycleStep.ON_CREATE_MOUNT_CONTENT)
              .isTrue();
          break;

        case ON_BIND:
          assertThat(
                  mCurrentStep == LifecycleStep.ON_MOUNT || mCurrentStep == LifecycleStep.ON_UNBIND)
              .isTrue();
          break;

        case ON_UNBIND:
          assertThat(mCurrentStep).isEqualTo(LifecycleStep.ON_BIND);
          break;

        case ON_UNMOUNT:
          assertThat(mCurrentStep).isEqualTo(LifecycleStep.ON_UNBIND);
          break;
      }

      mCurrentStep = currentStep;
    }

    @Override
    public Component makeShallowCopy() {
      return this;
    }

    public LifecycleMethodsComponent makeShallowCopyForTest() {
      return (LifecycleMethodsComponent) super.makeShallowCopy();
    }
  }

  private static class LifecycleMethodsDrawable extends Drawable {

    private LifecycleMethodsComponent mComponent;
    private static boolean drawableChecks = false;

    private LifecycleMethodsDrawable(LifecycleMethodsComponent component) {
      assertThat(component.getCurrentStep()).isEqualTo(LifecycleStep.ON_CREATE_MOUNT_CONTENT);
    }

    void setComponent(LifecycleMethodsComponent component) {
      mComponent = component;
      if (drawableChecks) {
        assertThat(mComponent.getCurrentStep()).isEqualTo(LifecycleStep.ON_MOUNT);
      }
    }

    @Override
    public void setBounds(int l, int t, int r, int b) {
      super.setBounds(l, t, r, b);

      if (drawableChecks) {
        assertThat(
                mComponent.getCurrentStep() == LifecycleStep.ON_BIND
                    || mComponent.getCurrentStep() == LifecycleStep.ON_UNBIND)
            .isTrue();
      }
    }

    @Override
    public void draw(Canvas canvas) {
      if (drawableChecks) {
        assertThat(LifecycleStep.ON_BIND).isEqualTo(mComponent.getCurrentStep());
      }
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter cf) {}

    @Override
    public int getOpacity() {
      return 0;
    }
  }
}
