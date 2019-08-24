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

package com.facebook.litho.sample;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static com.facebook.litho.testing.espresso.LithoViewMatchers.withTestKey;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.testing.espresso.LithoActivityTestRule;
import com.facebook.samples.litho.DemoListActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DemoListActivityEspressoTest {
  @Rule
  public LithoActivityTestRule<DemoListActivity> mActivity =
      new LithoActivityTestRule<>(DemoListActivity.class);

  @Test
  public void testLithographyIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Lithography")))
        .check(matches(allOf(isDisplayed(), isClickable())));
  }

  @Test
  public void testTestKeyLookup() {
    onView(withTestKey("main_screen")).check(matches(isDisplayed()));
  }

  @Test
  public void testPlaygroundIsVisibleAndClickable() {
    onView(componentHostWithText(containsString("Playground")))
        .check(matches(isDisplayed()))
        .check(matches(isClickable()));
  }
}
