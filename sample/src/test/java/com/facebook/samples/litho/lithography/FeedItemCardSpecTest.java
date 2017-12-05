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
import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.assertj.core.api.Java6Assertions.allOf;
import static org.assertj.core.data.Index.atIndex;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.TestCard;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class FeedItemCardSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component mComponent;
  private static final Artist ARTIST = new Artist("Sindre Sorhus", "JavaScript Rockstar", 2010);

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    final ComponentContext c = mComponentsRule.getContext();

    mComponent = FeedItemCard.create(c).artist(ARTIST).build();
  }

  @Test
  public void testShallowSubComponents() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        .extractingSubComponents(c)
        .hasSize(1)
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == Card.class;
              }
            },
            atIndex(0));
  }

  @Test
  public void testDeepSubComponents() {
    final ComponentContext c = mComponentsRule.getContext();

    // N.B. This manual way of testing is not recommended and will be replaced by more high-level
    // matchers, but illustrates how it can be used in case more fine-grained assertions are
    // required.
    assertThat(c, mComponent)
        .extractingSubComponentsDeeply(c)
        .hasSize(16)
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                describedAs(value.getComponentClass() + " with text " + value.getTextContent());
                return value.getComponentClass() == Text.class
                    && "JavaScript Rockstar".equals(value.getTextContent());
              }
            },
            atIndex(7));
  }

  @Test
  public void testDeepSubComponentText() {
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent)
        .has(
            allOf(
                deepSubComponentWith(c, textEquals("JavaScript Rockstar")),
                deepSubComponentWith(c, textEquals("Sindre Sorhus"))));
  }

  @Test
  public void testDeepSubComponentTextType() {
    final ComponentContext c = mComponentsRule.getContext();

    assertThat(c, mComponent).has(deepSubComponentWith(c, typeIs(Text.class)));
  }

  @Test
  public void testDeepMatcherMatching() {
    final ComponentContext c = mComponentsRule.getContext();

    // You can also test nested sub-components by passing in another Matcher where
    // you would normally provide a Component. In this case we provide a Matcher
    // of the FeedItemComponent to the Card Matcher's content prop.
    assertThat(c, mComponent)
        .has(
            deepSubComponentWith(
                c,
                TestCard.matcher(c)
                    .content(TestFeedItemComponent.matcher(c).artist(ARTIST).build())
                    .build()));
  }
}
