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

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import com.facebook.litho.Component;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.subcomponents.SubComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DemoListComponentTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();
  private Component mComponent;

  @Before
  public void setUp() {
    mComponent =
        DemoListComponent.create(mComponentsRule.getContext())
            .dataModels(new ArrayList<DemoListActivity.DemoListDataModel>())
            .build();
  }

  @Test
  public void testSubComponents() {
    assertThat(mComponentsRule.getContext(), mComponent)
        .containsOnlySubComponents(SubComponent.of(RecyclerCollectionComponent.class));
  }
}
