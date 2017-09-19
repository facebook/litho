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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.RecyclerBinder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demonstrates testing sub components based on {@link FeedItemComponentSpec}'s {@link
 * FooterComponentSpec} use.
 */
@RunWith(ComponentsTestRunner.class)
public class FeedItemComponentSpecSubComponentTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testSubComponentWithoutProperties() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<FeedItemComponent> component = makeComponent("Any String");

    // This will match as long as there is a FooterComponent, with any props.
    assertThat(c, component).has(subComponentWith(c, TestFooterComponent.matcher(c).build()));
  }

  @Test
  public void testSubComponentWithRawText() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<FeedItemComponent> component = makeComponent("Raw Text");

    // This will match if the component has exactly the specified text as property.
    assertThat(c, component)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).text("Raw Text").build()));
  }

  @Test
  public void testSubComponentWithRes() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<FeedItemComponent> component = makeComponent("Cancel");

    // You can also reference resources here directly.
    assertThat(c, component)
        .has(
            subComponentWith(
                c, TestFooterComponent.matcher(c).textRes(android.R.string.cancel).build()));
  }

  private Component<FeedItemComponent> makeComponent(String value) {
    final ComponentContext c = mComponentsRule.getContext();
    return FeedItemComponent.create(c)
        .artist(new Artist("Some Name", value, 2001))
        .binder(mock(RecyclerBinder.class))
        .build();
  }
}
