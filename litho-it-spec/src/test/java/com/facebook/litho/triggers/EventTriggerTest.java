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

package com.facebook.litho.triggers;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class EventTriggerTest {

  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testCanTriggerEvent() {
    Handle handle = new Handle();
    ComponentWithTrigger component =
        ComponentWithTrigger.create(mComponentContext).handle(handle).uniqueString("A").build();

    LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, component);

    // The EventTriggers have been correctly applied to lithoView's ComponentTree
    // (EventTriggersContainer),
    // but this hasn't been applied to the ComponentTree inside mComponentContext.
    mComponentContext =
        ComponentContext.withComponentScope(
            ComponentContext.withComponentTree(
                lithoView.getComponentContext(), lithoView.getComponentTree()),
            component,
            "globalKey");

    assertThat(ComponentWithTrigger.testTriggerMethod(mComponentContext, handle)).isEqualTo("A");
  }

  @Test
  public void testCanTriggerEventOnNestedComponent() {
    Handle handle = new Handle();
    ComponentContainer component =
        ComponentContainer.create(mComponentContext)
            .componentWithTriggerHandle(handle)
            .uniqueString("A")
            .build();

    LithoView lithoView = ComponentTestHelper.mountComponent(mComponentContext, component);

    // The EventTriggers have been correctly applied to lithoView's ComponentTree
    // (EventTriggersContainer),
    // but this hasn't been applied to the ComponentTree inside mComponentContext.
    mComponentContext =
        ComponentContext.withComponentScope(
            ComponentContext.withComponentTree(
                lithoView.getComponentContext(), lithoView.getComponentTree()),
            component,
            "globalKey");

    assertThat(ComponentWithTrigger.testTriggerMethod(mComponentContext, handle)).isEqualTo("A");
  }
}
