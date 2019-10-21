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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.LayoutState.LayoutStateContext;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testShallowCopyCachedLayoutSameLayoutState() {
    mContext = ComponentContext.withComponentTree(mContext, ComponentTree.create(mContext).build());

    final LayoutState layoutState = new LayoutState(mContext);

    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateContext(new LayoutStateContext(layoutState));

    Component component = TestDrawableComponent.create(mContext).unique().build();
    component.measure(c, 100, 100, new Size());
    assertThat(layoutState.getCachedLayout(component)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(layoutState.getCachedLayout(copyComponent)).isNotNull();

    assertThat(layoutState.getCachedLayout(component))
        .isEqualTo(layoutState.getCachedLayout(copyComponent));
  }

  @Test
  public void testShallowCopyCachedLayoutOtherLayoutStateCacheLayoutState() {
    mContext = ComponentContext.withComponentTree(mContext, ComponentTree.create(mContext).build());

    final LayoutState layoutState1 = new LayoutState(mContext);
    final LayoutState layoutState2 = new LayoutState(mContext);

    final ComponentContext c1 = new ComponentContext(mContext);
    c1.setLayoutStateContext(new LayoutStateContext(layoutState1));
    final ComponentContext c2 = new ComponentContext(mContext);
    c2.setLayoutStateContext(new LayoutStateContext(layoutState2));

    Component component = TestDrawableComponent.create(mContext).unique().build();
    component.measure(c1, 100, 100, new Size());
    assertThat(layoutState1.getCachedLayout(component)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(layoutState2.getCachedLayout(copyComponent)).isNull();
  }
}
