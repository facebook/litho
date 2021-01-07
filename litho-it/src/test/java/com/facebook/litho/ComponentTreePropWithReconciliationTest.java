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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.view.View;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TreePropTestContainerComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LEGACY)
@RunWith(LithoTestRunner.class)
public class ComponentTreePropWithReconciliationTest {

  private ComponentContext c;

  @Before
  public void setup() {
    TestComponentsReporter componentsReporter = new TestComponentsReporter();
    c = new ComponentContext(getApplicationContext());
    ComponentsReporter.provide(componentsReporter);
  }

  @Test
  public void test() {
    Component component = TreePropTestContainerComponent.create(c).build();
    getLithoView(component);
  }

  private LithoView getLithoView(Component component) {
    LithoView lithoView = new LithoView(c);
    ComponentTree componentTree =
        ComponentTree.create(c, component).isReconciliationEnabled(true).build();
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    return lithoView;
  }
}
