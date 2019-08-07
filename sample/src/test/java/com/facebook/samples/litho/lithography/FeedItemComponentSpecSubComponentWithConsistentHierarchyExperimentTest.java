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

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demonstrates testing sub components based on {@link FeedItemComponentSpec}'s {@link
 * FooterComponentSpec} use.
 */
@RunWith(ComponentsTestRunner.class)
public class FeedItemComponentSpecSubComponentWithConsistentHierarchyExperimentTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setUp() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @After
  public void after() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = false;
  }

  @Test
  public void subComponentWithoutProperties() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Any String");

    // This will match as long as there is a FooterComponent, with any props.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).build()));
  }

  @Test
  public void subComponentWithRawText() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Raw Text");

    // This will match if the component has exactly the specified text as property.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).text("Raw Text").build()));
  }

  @Test
  public void subComponentWithMatcher() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component =
        makeComponentWithTextInSubcomponent(
            "Long Text That We Don't Want To Match In Its Entirety");

    // We can pass in any of the default hamcrest matchers here.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c, TestFooterComponent.matcher(c).text(containsString("Want To Match")).build()));
  }

  @Test
  public void subComponentWithRes() {
    final ComponentContext c = mComponentsRule.getContext();
    String string = c.getAndroidContext().getResources().getString(android.R.string.cancel);
    final Component component = makeComponentWithTextInSubcomponent(string);

    // You can also reference resources here directly.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c, TestFooterComponent.matcher(c).textRes(android.R.string.cancel).build()));
  }

  @Test
  public void footerHasNoClickHandler() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Any Text");

    // Components commonly have conditional handlers assigned. Using the clickHandler matcher
    // we can assert whether or not a given component has a handler attached to them.
    //noinspection unchecked
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                TestFooterComponent.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null))
                    .build()));
  }

  private Component makeComponentWithTextInSubcomponent(String value) {
    final ComponentContext c = mComponentsRule.getContext();
    return FeedItemComponent.create(c).artist(new Artist("Some Name", value, 2001)).build();
  }
}
