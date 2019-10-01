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

package com.facebook.samples.litho.lithography;

import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.LithoViewSubComponentDeepExtractor.deepSubComponentWith;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static com.facebook.litho.testing.subcomponents.SubComponent.legacySubComponent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.subcomponents.SubComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class FeedItemComponentSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    final ComponentContext c = mComponentsRule.getContext();
    mComponent =
        FeedItemComponent.create(c)
            .artist(new Artist("Sindre Sorhus", "Rockstar Developer", 2010))
            .build();
  }

  @After
  public void after() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = false;
  }

  @Test
  public void testRecursiveSubComponentExists() {
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent).extractingSubComponents(c).hasSize(2);
  }

  @Test
  public void recursiveSubComponentExistsWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent).extractingSubComponentAt(0).extractingSubComponents(c).hasSize(2);
  }

  @Test
  public void testLithoViewSubComponentMatching() {
    final ComponentContext c = mComponentsRule.getContext();
    final LithoView lithoView = ComponentTestHelper.mountComponent(c, mComponent);

    assertThat(lithoView).has(deepSubComponentWith(textEquals("Sindre Sorhus")));
  }

  @Test
  public void testSubComponentLegacyBridge() {
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent)
        .has(
            subComponentWith(
                c,
                legacySubComponent(
                    SubComponent.of(
                        FooterComponent.create(c).text("Rockstar Developer").build()))));
  }

  @Test
  public void subComponentLegacyBridgeWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                legacySubComponent(
                    SubComponent.of(
                        FooterComponent.create(c).text("Rockstar Developer").build()))));
  }
}
