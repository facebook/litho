/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
