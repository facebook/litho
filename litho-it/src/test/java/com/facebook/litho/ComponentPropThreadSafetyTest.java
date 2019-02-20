/*
 * Copyright 2018-present Facebook, Inc.
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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ContextWrapper;
import android.os.Looper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestWrappedComponentProp;
import com.facebook.litho.testing.TestWrappedComponentPropSpec;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class ComponentPropThreadSafetyTest {

  private ComponentContext mContext;
  private ShadowLooper mLayoutThreadShadowLooper;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(new ContextWrapper(RuntimeEnvironment.application));

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @Test
  public void testThreadSafeConcurrentPropComponentAccess() {
    TestDrawableComponent testComponent =
        Mockito.spy(TestDrawableComponent.create(mContext).build());

    final TestWrappedComponentPropSpec.ComponentWrapper wrapper =
        new TestWrappedComponentPropSpec.ComponentWrapper(testComponent);
    final Component root = TestWrappedComponentProp.create(mContext).wrapper(wrapper).build();

    final ComponentTree componentTree = ComponentTree.create(mContext, root).build();

    componentTree.setRootAndSizeSpec(
        TestWrappedComponentProp.create(mContext).wrapper(wrapper).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        null);

    componentTree.setRootAndSizeSpecAsync(
        TestWrappedComponentProp.create(mContext).wrapper(wrapper).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(testComponent).makeShallowCopy();
  }

  @Test
  public void testThreadSafeConcurrentPropListComponentAccess() {
    TestDrawableComponent testComponent =
        Mockito.spy(TestDrawableComponent.create(mContext).build());
    List<Component> componentList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      componentList.add(testComponent);
    }

    final Component root =
        TestWrappedComponentProp.create(mContext).componentList(componentList).build();

    final ComponentTree componentTree = ComponentTree.create(mContext, root).build();

    componentTree.setRootAndSizeSpec(
        TestWrappedComponentProp.create(mContext).componentList(componentList).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        null);

    componentTree.setRootAndSizeSpecAsync(
        TestWrappedComponentProp.create(mContext).componentList(componentList).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(testComponent, times(19)).makeShallowCopy();
  }
}
