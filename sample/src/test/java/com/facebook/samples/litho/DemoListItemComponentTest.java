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

package com.facebook.samples.litho;

import static com.facebook.litho.ComponentContext.withComponentScope;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.samples.litho.playground.PlaygroundActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DemoListItemComponentTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Test
  public void testComponentOnClick() {
    final Class activityClassToLaunch = PlaygroundActivity.class;
    final DemoListItemComponent.Builder builder =
        DemoListItemComponent.create(mComponentsRule.getContext())
            .model(new DemoListActivity.DemoListDataModel("My Component", activityClassToLaunch))
            .currentIndices(null);
    // For this test, we mount the view and dispatch the event through the regular
    // Android event mechanism.
    final LithoView lithoView = ComponentTestHelper.mountComponent(builder);

    lithoView.performClick();

    final Intent nextIntent =
        shadowOf(mComponentsRule.getContext().getAndroidContext())
            .getShadowApplication()
            .getNextStartedActivity();
    assertThat(nextIntent.getComponent().getClassName()).isSameAs(activityClassToLaunch.getName());
  }

  @Test
  public void testComponentOnSyntheticEventClick() {
    final Class activityClassToLaunch = PlaygroundActivity.class;
    final Component component =
        DemoListItemComponent.create(mComponentsRule.getContext())
            .model(new DemoListActivity.DemoListDataModel("My Component", activityClassToLaunch))
            .currentIndices(null)
            .build();

    // Here, we make use of Litho's internal event infrastructure and manually dispatch the event.
    final ComponentContext componentContext =
        withComponentScope(mComponentsRule.getContext(), component);
    component
        .getEventDispatcher()
        .dispatchOnEvent(DemoListItemComponent.onClick(componentContext), new ClickEvent());

    final Intent nextIntent =
        shadowOf(mComponentsRule.getContext().getAndroidContext())
            .getShadowApplication()
            .getNextStartedActivity();
    assertThat(nextIntent.getComponent().getClassName()).isSameAs(activityClassToLaunch.getName());
  }
}
