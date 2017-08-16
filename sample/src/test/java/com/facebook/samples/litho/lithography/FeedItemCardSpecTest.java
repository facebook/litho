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
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.assertj.core.api.Java6Assertions.allOf;
import static org.assertj.core.data.Index.atIndex;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.InspectableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class FeedItemCardSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component<FeedItemCard> mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    final ComponentContext c = mComponentsRule.getContext();
    final RecyclerBinder binder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.HORIZONTAL, false))
            .build(c);

    mComponent =
        FeedItemCard.create(c)
            .binder(binder)
            .artist(new Artist("Sindre Sorhus", "JavaScript Rockstar", 2010))
            .build();
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
        .hasSize(14)
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
}
