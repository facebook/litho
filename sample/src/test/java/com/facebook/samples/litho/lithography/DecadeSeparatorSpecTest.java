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

import static com.facebook.litho.testing.assertj.ComponentConditions.text;
import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.assertj.SubComponentExtractor;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.subcomponents.SubComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DecadeSeparatorSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    mComponent =
        DecadeSeparator.create(mComponentsRule.getContext()).decade(new Decade(2010)).build();
  }

  @After
  public void after() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = false;
  }

  @Test
  public void testSubComponentsWithManualExtraction() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent).extractingSubComponents(c).hasSize(3);
  }

  @Test
  public void subComponentsWithManualExtractionWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent).extractingSubComponentAt(0).extractingSubComponents(c).hasSize(3);
  }

  @Test
  public void testSubComponentByClass() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent).hasSubComponents(SubComponent.of(Text.class));
  }

  @Test
  public void testSubComponentByClassWithExtraction() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .extracting(SubComponentExtractor.subComponents(c))
        .areExactly(
            1,
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == Text.class;
              }
            });
  }

  @Test
  public void subComponentByClassWithExtractionWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;

    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .extractingSubComponents(c)
        .areExactly(
            1,
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == Text.class;
              }
            });
  }

  @Test
  public void subComponentWithText() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .has(subComponentWith(c, textEquals("2010")))
        // Silly things to test for, but left here to demonstrate the API.
        .has(subComponentWith(c, text(containsString("10"))))
        .doesNotHave(subComponentWith(c, textEquals("2011")));
  }

  @Test
  public void subComponentWithTextWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;

    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, textEquals("2010")))
        // Silly things to test for, but left here to demonstrate the API.
        .has(subComponentWith(c, text(containsString("10"))))
        .doesNotHave(subComponentWith(c, textEquals("2011")));
  }
}
