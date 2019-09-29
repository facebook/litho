/*
 * Copyright 2019-present Facebook, Inc.
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

import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.TreePropTestContainerComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreePropWithReconciliationTest {

  private ComponentContext c;

  @Before
  public void setup() {
    ComponentsConfiguration.isReconciliationEnabled = true;
    TestComponentsReporter componentsReporter = new TestComponentsReporter();
    c = new ComponentContext(RuntimeEnvironment.application);
    ComponentsReporter.provide(componentsReporter);
  }

  @After
  public void after() {
    ComponentsConfiguration.isReconciliationEnabled = false;
  }

  @Test
  public void test() {
    Component component = TreePropTestContainerComponent.create(c).build();
    getLithoView(component);
  }

  private LithoView getLithoView(Component component) {
    LithoView lithoView = new LithoView(c);
    lithoView.setComponent(component);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    return lithoView;
  }
}
