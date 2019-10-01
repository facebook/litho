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

package com.facebook.samples.lithocodelab.examples.modules;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.StateValue;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.assertj.LithoAssertions;
import com.facebook.litho.testing.assertj.SubComponentExtractor;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.TestText;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LearningStateComponentSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testComponentOnClick() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = LearningStateComponent.create(c).canClick(true).build();

    LithoAssertions.assertThat(c, component)
        .has(
            SubComponentExtractor.subComponentWith(
                c,
                TestText.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>notNullValue(null))
                    .build()));
  }

  @Test
  public void testNoComponentOnClick() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = LearningStateComponent.create(c).canClick(false).build();

    LithoAssertions.assertThat(c, component)
        .has(
            SubComponentExtractor.subComponentWith(
                c,
                TestText.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null))
                    .build()));
  }

  @Test
  public void testIncrementClickCount() {
    final StateValue<Integer> count = new StateValue<>();
    count.set(0);
    LearningStateComponentSpec.incrementClickCount(count);

    LithoAssertions.assertThat(count).valueEqualTo(1);
  }
}
